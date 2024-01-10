/*
 * Copyright 2011-2024 Mark Injerd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pilot51.voicenotify

import android.app.Notification
import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object NotifyList {
	private const val HISTORY_LIMIT = 20
	private val list = mutableStateListOf<NotificationInfo>()

	fun addNotification(info: NotificationInfo) {
		if (list.size == HISTORY_LIMIT) {
			list.removeAt(list.size - 1)
		}
		list.add(0, info)
	}

	fun updateInfo(info: NotificationInfo) {
		val index = list.indexOf(info).takeUnless { it == -1 } ?: return
		// Force update to list state by first setting copy
		list[index] = info.copy()
		// Set back to original to ensure future calls can find it again
		list[index] = info
	}

	@Composable
	fun NotificationLogDialog(onDismiss: () -> Unit) {
		LogDialog(
			list = list,
			onDismiss = onDismiss
		)
	}
}

@Composable
private fun LogDialog(
	list: List<NotificationInfo>,
	onDismiss: () -> Unit
) {
	AlertDialog(
		onDismissRequest = onDismiss,
		confirmButton = { },
		dismissButton = {
			TextButton(onClick = onDismiss) {
				Text(stringResource(android.R.string.ok))
			}
		},
		title = {
			Text(
				text = stringResource(R.string.notify_log),
				modifier = Modifier.fillMaxWidth(),
				fontWeight = FontWeight.Medium,
				textAlign = TextAlign.Center
		) },
		text = { ItemList(list) }
	)
}

@Composable
private fun ItemList(list: List<NotificationInfo>) {
	LazyColumn(modifier = Modifier.fillMaxWidth()) {
		itemsIndexed(list) { index, item ->
			Item(item)
			if (index < list.lastIndex) {
				Divider(
					modifier = Modifier.padding(vertical = 16.dp),
					color = Color.Gray,
					thickness = 1.dp
				)
			}
		}
	}
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Item(item: NotificationInfo) {
	var showDetailDialog by remember(item) { mutableStateOf(false) }
	var showIgnoreDialog by remember(item) { mutableStateOf(false) }
	val logMessage = remember(item) {
		item.run {
			val logBuilder = StringBuilder()
			arrayOf(ticker, subtext, contentTitle, contentText, contentInfoText).forEach {
				if (!it.isNullOrEmpty()) {
					if (logBuilder.isNotEmpty()) logBuilder.append("\n")
					logBuilder.append(it)
				}
			}
			logBuilder.toString()
		}
	}
	Column(modifier = Modifier
		.fillMaxWidth()
		.combinedClickable(
			onClick = { showDetailDialog = true },
			onLongClick = { if (item.app != null) showIgnoreDialog = true }
		)
	) {
		Text(
			text = item.time,
			modifier = Modifier.fillMaxWidth(),
			textAlign = TextAlign.Center
		)
		Text(
			text = item.app!!.label,
			modifier = Modifier.fillMaxWidth(),
			fontSize = 24.sp,
			textAlign = TextAlign.Center
		)
		if (logMessage.isNotEmpty()) {
			Text(
				text = logMessage,
				modifier = Modifier.fillMaxWidth(),
				fontSize = 16.sp,
				textAlign = TextAlign.Center
			)
		}
		if (item.ignoreReasons.isNotEmpty()) {
			val interruptedColor = if (isSystemInDarkTheme()) Color.Yellow else Color(0xFFBBBB00)
			Text(
				text = item.getIgnoreReasonsAsText(),
				modifier = Modifier.fillMaxWidth(),
				color = if (item.isInterrupted) interruptedColor else Color.Red,
				fontWeight = FontWeight.Bold,
				textAlign = TextAlign.Center
			)
		}
	}
	if (showDetailDialog) {
		DetailDialog(item) { showDetailDialog = false }
	}
	if (showIgnoreDialog) {
		item.app?.let { IgnoreDialog(it) { showIgnoreDialog = false } }
	}
}

@Composable
private fun DetailDialog(
	item: NotificationInfo,
	onDismiss: () -> Unit
) {
	val context = LocalContext.current
	val scrollState = rememberScrollState()
	AlertDialog(
		onDismissRequest = onDismiss,
		confirmButton = { },
		dismissButton = {
			TextButton(onClick = onDismiss) {
				Text(stringResource(android.R.string.ok))
			}
		},
		title = {
			Text(
				text = stringResource(R.string.notification_details),
				modifier = Modifier.fillMaxWidth(),
				fontWeight = FontWeight.Medium,
				textAlign = TextAlign.Center
			)
		},
		text = {
			Column(
				modifier = Modifier.fillMaxWidth(),
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				Text(item.time)
				item.app?.apply {
					var isEnabled by remember(this) { mutableStateOf(enabled) }
					Text(text = label, fontSize = 24.sp)
					Text(packageName)
					Row(
						modifier = Modifier
							.toggleable(
								value = isEnabled,
								role = Role.Checkbox
							) {
								isEnabled = !isEnabled
								setEnabledWithToast(isEnabled, context)
							}
							.padding(10.dp),
						horizontalArrangement = Arrangement.spacedBy(10.dp),
						verticalAlignment = Alignment.CenterVertically
					) {
						Checkbox(checked = isEnabled, onCheckedChange = null)
						Text(stringResource(R.string.enable))
					}
				}
				Column(
					modifier = Modifier.verticalScroll(scrollState),
					horizontalAlignment = Alignment.CenterHorizontally,
					verticalArrangement = Arrangement.spacedBy(10.dp)
				) {
					item.ticker?.let {
						NotificationPart(R.string.ticker, it)
					}
					item.subtext?.let {
						NotificationPart(R.string.subtext, it)
					}
					item.contentTitle?.let {
						NotificationPart(R.string.title, it)
					}
					item.contentText?.let {
						NotificationPart(R.string.content_text, it)
					}
					item.contentInfoText?.let {
						NotificationPart(R.string.content_info_text, it)
					}
					item.bigContentTitle?.let {
						NotificationPart(R.string.big_content_title, it)
					}
					item.bigContentSummary?.let {
						NotificationPart(R.string.big_content_summary, it)
					}
					item.bigContentText?.let {
						NotificationPart(R.string.big_content_text, it)
					}
					item.ttsMessage?.let {
						NotificationPart(R.string.tts_message, it)
					}
					if (item.ignoreReasons.isNotEmpty()) {
						val interruptedColor = if (isSystemInDarkTheme()) {
							Color.Yellow
						} else Color(0xFFBBBB00)
						NotificationPart(
							partName = if (item.isInterrupted) {
								R.string.interrupted_reason
							} else R.string.ignored_reasons,
							text = item.getIgnoreReasonsAsText(),
							color = if (item.isInterrupted) interruptedColor else Color.Red
						)
					}
				}
			}
		}
	)
}

@Composable
private fun NotificationPart(
	@StringRes partName: Int,
	text: String,
	color: Color = Color.Unspecified
) {
	Column(horizontalAlignment = Alignment.CenterHorizontally) {
		Text(
			text = stringResource(partName),
			color = color,
			fontSize = 18.sp,
			textDecoration = TextDecoration.Underline,
			textAlign = TextAlign.Center
		)
		Text(
			text = text,
			color = color,
			textAlign = TextAlign.Center
		)
	}
}

@Composable
private fun IgnoreDialog(
	app: App,
	onDismiss: () -> Unit
) {
	AlertDialog(
		onDismissRequest = onDismiss,
		confirmButton = {
			val context = LocalContext.current
			TextButton(onClick = {
				app.setEnabledWithToast(!app.enabled, context)
				onDismiss()
			}) {
				Text(stringResource(R.string.yes))
			}
		},
		dismissButton = {
			TextButton(onClick = onDismiss) {
				Text(stringResource(android.R.string.cancel))
			}
		},
		title = {
			Text(
				text = stringResource(
					if (app.enabled) R.string.ignore_app else R.string.unignore_app,
					app.label
				),
				modifier = Modifier.fillMaxWidth(),
				textAlign = TextAlign.Center)
		}
	)
}

@Composable
private fun previewNotification() = Notification().apply {
	`when` = Long.MIN_VALUE
	tickerText = "Ticker"
	extras.apply {
		putString(Notification.EXTRA_SUB_TEXT, stringResource(R.string.test_subtext))
		putString(Notification.EXTRA_TITLE, stringResource(R.string.test_content_title))
		putString(Notification.EXTRA_TEXT, stringResource(R.string.test_content_text))
		putString(Notification.EXTRA_INFO_TEXT, stringResource(R.string.test_content_info))
		putString(Notification.EXTRA_TITLE_BIG, stringResource(R.string.test_big_content_title))
		putString(Notification.EXTRA_SUMMARY_TEXT, stringResource(R.string.test_big_content_summary))
		putString(Notification.EXTRA_BIG_TEXT, stringResource(R.string.test_big_content_text))
	}
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun LogDialogPreview() {
	val previewNotification = previewNotification()
	val list = listOf(
		NotificationInfo(
			app = App(1, "package.name.one", "App Name 1", true),
			notification = previewNotification
		),
		NotificationInfo(
			app = App(2, "package.name.two", "App Name 2", false),
			notification = previewNotification
		)
	)
	AppTheme {
		LogDialog(list) {}
	}
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun DetailDialogPreview() {
	val info = NotificationInfo(
		app = App(1, "package.name.one", "App Name 1", true),
		notification = previewNotification()
	)
	AppTheme {
		DetailDialog(info) {}
	}
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun IgnoreDialogPreview() {
	val app = App(1, "package.name.one", "App Name 1", true)
	AppTheme {
		IgnoreDialog(app) {}
	}
}
