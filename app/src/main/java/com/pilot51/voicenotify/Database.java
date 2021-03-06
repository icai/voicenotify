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

import android.app.usage.UsageStatsManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;


class Database extends SQLiteOpenHelper {
    private static  Context context;
	private static Database database;
	private static final int DB_VERSION = 1;
	private static final String
			DB_NAME = "apps.db",
			TABLE_NAME = "apps",
			COLUMN_PACKAGE = "package",
			COLUMN_ICON = "icon",
			COLUMN_LABEL = "name",
            COLUMN_LETTER = "letter",
			COLUMN_ENABLED = "is_enabled",
			CREATE_TBL_APPS = "create table if not exists " + TABLE_NAME + "(" + BaseColumns._ID
					+ " integer primary key autoincrement, " + COLUMN_PACKAGE + " text not null, "
                    + COLUMN_ICON + " text not null, "
					+ COLUMN_LABEL + " text not null, "
                    + COLUMN_LETTER + " text not null, "
                    + COLUMN_ENABLED + " integer);";
	
	private Database(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}
	
	/**
	 * Initializes database object if not already initialized.
	 */
	static void init(Context _context) {
        context = _context;
		if (database == null) {
			database = new Database(_context);
		}
	}
	
	/** @return A new List containing all apps from the database. */
	static synchronized List<App> getApps() {
		SQLiteDatabase db = database.getReadableDatabase();
		Cursor cursor = db.query(TABLE_NAME, null, null, null, null, null, COLUMN_LABEL + " COLLATE NOCASE");
		List<App> list = new ArrayList<>();
		while (cursor.moveToNext()) {
			byte[] blob = cursor.getBlob(cursor.getColumnIndex(COLUMN_ICON));
			Bitmap bmp = BitmapFactory.decodeByteArray(blob, 0, blob.length);
			Drawable bd = new BitmapDrawable(context.getResources(), bmp);
			String pkgName = cursor.getString(cursor.getColumnIndex(COLUMN_PACKAGE));
			list.add(new App(
                    pkgName,
                    bd,
					cursor.getString(cursor.getColumnIndex(COLUMN_LABEL)),
                    cursor.getString(cursor.getColumnIndex(COLUMN_LETTER)),
					cursor.getInt(cursor.getColumnIndex(COLUMN_ENABLED)) == 1
			));
		}
		cursor.close();
		db.close();
		return list;
	}

    @NonNull
    static private Bitmap getBitmapFromDrawable(@NonNull Drawable drawable) {
        final Bitmap bmp = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bmp);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bmp;
    }
	
	/**
	 * Clears and sets all apps in database.
	 * @param apps The list of apps to add in the database.
	 */
	static synchronized void setApps(List<App> apps) {
		List<App> list = new ArrayList<>(apps);
		SQLiteDatabase db = database.getWritableDatabase();
		db.delete(TABLE_NAME, null, null);
		ContentValues values;
		for (App app : list) {

            Bitmap bmp = getBitmapFromDrawable(app.getAppIcon());
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.PNG, 100, os);
            values = new ContentValues();
			values.put(COLUMN_PACKAGE, app.getPackage());
			values.put(COLUMN_ICON, os.toByteArray());
			values.put(COLUMN_LABEL, app.getLabel());
			values.put(COLUMN_LETTER, app.getSortLetters());
			values.put(COLUMN_ENABLED, app.getEnabled() ? 1 : 0);
			db.insert(TABLE_NAME, null, values);
		}
		db.close();
	}
	
	/**
	 * Updates app in database matching package name or adds if no match found.
	 * @param app The app to add or update in the database.
	 */
	static synchronized void addOrUpdateApp(App app) {
        Bitmap bmp = getBitmapFromDrawable(app.getAppIcon());
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, os);
		ContentValues values = new ContentValues();
		values.put(COLUMN_PACKAGE, app.getPackage());
        values.put(COLUMN_ICON, os.toByteArray());
		values.put(COLUMN_LABEL, app.getLabel());
        values.put(COLUMN_LETTER, app.getSortLetters());
		values.put(COLUMN_ENABLED, app.getEnabled() ? 1 : 0);
		SQLiteDatabase db = database.getWritableDatabase();
		if (db.update(TABLE_NAME, values, COLUMN_PACKAGE + " = ?", new String[] {app.getPackage()}) == 0) {
			db.insert(TABLE_NAME, null, values);
		}
		db.close();
	}
	
	/**
	 * Updates enabled value of app in database matching package name.
	 * @param app The app to update in the database.
	 */
	static synchronized void updateAppEnable(App app) {
		ContentValues values = new ContentValues();
		values.put(COLUMN_ENABLED, app.getEnabled() ? 1 : 0);
		SQLiteDatabase db = database.getWritableDatabase();
		db.update(TABLE_NAME, values, COLUMN_PACKAGE + " = ?", new String[] {app.getPackage()});
		db.close();
	}
	
	/**
	 * Removes app from database matching package name.
	 * @param app The app to remove from the database.
	 */
	static synchronized void removeApp(App app) {
		SQLiteDatabase db = database.getWritableDatabase();
		db.delete(TABLE_NAME, COLUMN_PACKAGE + " = ?", new String[] {app.getPackage()});
		db.close();
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_TBL_APPS);
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
}
