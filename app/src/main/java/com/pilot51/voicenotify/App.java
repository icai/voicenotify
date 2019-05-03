/*
 * Copyright 2012 Mark Injerd
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

package com.pilot51.voicenotify;

import android.app.usage.UsageStats;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;

import com.pilot51.voicenotify.utils.PinyinUtils;

public class App {
	private final String packageName, label;
	// private UsageStats usageStats;
	private final Drawable appIcon;
	private boolean enabled;

	private String sortLetters;
	App(String pkg, Drawable icon, String name,  boolean enable) {
        packageName = pkg;
        appIcon = icon;
        label = name;
		enabled = enable;
		this.initLetter(label);
    }

    App(String pkg, Drawable icon, String name, String letter, boolean enable) {
        packageName = pkg;
        appIcon = icon;
        label = name;
        enabled = enable;
        this.setSortLetters(letter);
    }
	
	/**
	 * Updates self in database.
	 * @return This instance.
	 */
	App updateDb() {
		Database.addOrUpdateApp(this);
		return this;
	}
	
	void setEnabled(boolean enable, boolean updateDb) {
		enabled = enable;
		if (updateDb) Database.updateAppEnable(this);
	}

	void initLetter(String label) {
        String pinyin = PinyinUtils.getPingYin(label);
        String sortString = pinyin.substring(0, 1).toUpperCase();
        if (sortString.matches("[A-Z]")) {
            this.setSortLetters(sortString.toUpperCase());
        } else {
            this.setSortLetters("#");
        }
	}
	
	/** Removes self from database. */
	void remove() {
		Database.removeApp(this);
	}

	public String getSortLetters() {
        return sortLetters;
    }

    public void setSortLetters(String sortLetters) {
        this.sortLetters = sortLetters;
    }
	
	String getLabel() {
		return label;
	}
	
	String getPackage() {
		return packageName;
	}

	Drawable getAppIcon() {return  appIcon; }

	boolean getEnabled() {
		return enabled;
	}
}
