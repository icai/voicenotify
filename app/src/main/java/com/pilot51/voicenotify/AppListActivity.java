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

import android.app.Activity;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.usage.UsageStats;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppListActivity extends Activity {
    private Adapter adapter;
    private SideBar sideBar;
    private TextView dialog;
    private ListView lv;
    private static List<App> apps;
    private static ArrayList<String> indexString;
    private static boolean defEnable;
    private static final String KEY_DEFAULT_ENABLE = "defEnable";
    private static final int IGNORE_TOGGLE = 0, IGNORE_ALL = 1, IGNORE_NONE = 2;
    private static final Object SYNC_APPS = new Object();
    private static OnListUpdateListener listener;
    private static boolean isUpdating;
    // private static UsageStatsManager usageStatsManager;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Common.init(this);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.app_list);

        lv = findViewById(R.id.app_list);
        sideBar = findViewById(R.id.sidebar);
        dialog = findViewById(R.id.dialog);
        sideBar.setTextView(dialog);

        lv.setTextFilterEnabled(true);
        // disable scrollBar
        // lv.setFastScrollEnabled(true);
        adapter = new Adapter(this);
        indexString = new ArrayList<>();
        lv.setAdapter(adapter);
        listener = new OnListUpdateListener() {
            @Override
            public void onListUpdated() {
                runOnUiThread(new Runnable() {
                    public void run() {
                        adapter.setData(apps);
                    }
                });
            }

            @Override
            public void onUpdateCompleted() {
                runOnUiThread(new Runnable() {
                    public void run() {
                        setProgressBarIndeterminateVisibility(false);
                    }
                });
                listener = null;
            }
        };

        lv.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                setIgnore((App) adapter.getItem(position), IGNORE_TOGGLE);
                adapter.notifyDataSetChanged();
            }
        });

        sideBar.setOnTouchingLetterChangedListener(new SideBar.OnTouchingLetterChangedListener() {
            @Override
            public void onTouchingLetterChanged(String s) {
                int position = adapter.getPositionForSection(s.charAt(0));
                if (position != -1) {
                    lv.setSelection(position);
                }
            }
        });
        defEnable = Common.getPrefs(this).getBoolean(KEY_DEFAULT_ENABLE, true);
        updateAppsList();
    }


    private interface OnListUpdateListener {
        void onListUpdated();
        void onUpdateCompleted();
    }

    private static void onListUpdated() {
        if (listener != null) listener.onListUpdated();
    }

    private void updateAppsList() {
        Context that = this;
        setProgressBarIndeterminateVisibility(true);
        if (isUpdating) {
            adapter.setData(apps);
            return;
        }
        isUpdating = true;
        new Thread(new Runnable() {
            public void run() {
                synchronized (SYNC_APPS) {
                    apps = Database.getApps();
                    onListUpdated();
                    final boolean isFirstLoad = apps.isEmpty();
                    PackageManager packMan = getPackageManager();

                    // Remove uninstalled
                    for (int a = apps.size() - 1; a >= 0; a--) {
                        App app = apps.get(a);
                        try {
                            packMan.getApplicationInfo(app.getPackage(), 0);
                        } catch (NameNotFoundException e) {
                            if (!isFirstLoad) app.remove();
                            apps.remove(a);
                            onListUpdated();
                        }
                    }

                    // Add new
                    inst:
                    for (ApplicationInfo appInfo : packMan.getInstalledApplications(0)) {
                        for (App app : apps) {
                            if (app.getPackage().equals(appInfo.packageName)) {
                                continue inst;
                            }
                        }
                        
                        // ignored system app
                        if((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                            String label = String.valueOf(appInfo.loadLabel(packMan));

                            App app = new App(appInfo.packageName, appInfo.loadIcon(packMan), label, defEnable);
                            apps.add(app);
                            onListUpdated();
                            if (!isFirstLoad) app.updateDb();
                        }
                    }

                    indexString = new ArrayList<>(getIndexStrings(apps));
                    Collections.sort(indexString, new Comparator<String>() {
                        @Override
                        public int compare(String o1, String o2) {
                            if (o1.equals("@")
                                    || o2.equals("#")) {
                                return 1;
                            } else if (o1.equals("#")
                                    || o2.equals("@")) {
                                return -1;
                            } else {
                                return o1.compareTo(o2);
                            }
                        }
                    });
                    sideBar.setIndexText(indexString);
                    Collections.sort(apps, new Comparator<App>() {
                        @Override
                        public int compare(App o1, App o2) {
                            if (o1.getSortLetters().equals("@")
                                    || o2.getSortLetters().equals("#")) {
                                return 1;
                            } else if (o1.getSortLetters().equals("#")
                                    || o2.getSortLetters().equals("@")) {
                                return -1;
                            } else {
                                return o1.getSortLetters().compareTo(o2.getSortLetters());
                            }
                        }
                    });
//					Collections.sort(apps, new Comparator<App>() {
//						@Override
//						public int compare(App app1, App app2) {
//							return app1.getLabel().compareToIgnoreCase(app2.getLabel());
//						}
//					});
                    onListUpdated();
                    if (isFirstLoad) Database.setApps(apps);
                }
                isUpdating = false;
                if (listener != null) listener.onUpdateCompleted();
            }
        }).start();
    }

    private Set<String> getIndexStrings(final List<App> apps) {
        Set<String> areas = new HashSet<>();
        for(final App app: apps) {
            areas.add(app.getSortLetters());
        }
        return areas;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.app_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.ignore_all:
                setDefaultEnable(false);
                massIgnore(IGNORE_ALL);
                return true;
            case R.id.ignore_none:
                setDefaultEnable(true);
                massIgnore(IGNORE_NONE);
                return true;
            case R.id.filter:
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                assert imm != null; // Prevent Lint warning. Should never be null, I want a crash report if it is.
                imm.toggleSoftInput(0, 0);
                return true;
        }
        return false;
    }

    // need user permission
    static UsageStats getUsageStats(UsageStatsManager usageStatsManager, String packageName) {
        List<UsageStats> stats = getUsageStatsList(usageStatsManager);
        for (UsageStats statItem : stats) {
            if (statItem.getPackageName().equals(packageName)) {
                return statItem;
            }
        }
        return null;
    }
    // need user permission
    static List<UsageStats> getUsageStatsList(UsageStatsManager usageStatsManager) {
        int year = Calendar.getInstance().get(Calendar.YEAR);

        Calendar beginCal = Calendar.getInstance();
        beginCal.set(Calendar.YEAR, year - 1);

        Calendar endCal = Calendar.getInstance();
        endCal.set(Calendar.YEAR, year + 1);
        List<UsageStats> usageStats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_YEARLY, beginCal.getTimeInMillis(), endCal.getTimeInMillis());
        return usageStats;
    }

    /**
     * @param pkg Package name used to find {@link App} in current list or create a new one from system.
     * @param ctx Context required to get default enabled preference and to get package manager for searching system.
     * @return Found or created {@link App}, otherwise null if app not found on system.
     */
    static App findOrAddApp(String pkg, Context ctx) {
        synchronized (SYNC_APPS) {
            if (apps == null) {
                defEnable = Common.getPrefs(ctx).getBoolean(KEY_DEFAULT_ENABLE, true);
                apps = Database.getApps();
            }
            for (App app : apps) {
                if (app.getPackage().equals(pkg)) {
                    return app;
                }
            }
            try {
                PackageManager packMan = ctx.getPackageManager();
                ApplicationInfo tapp = packMan.getApplicationInfo(pkg, 0);
                App app = new App(pkg, tapp.loadIcon(packMan), tapp.loadLabel(packMan).toString(), defEnable);
                apps.add(app.updateDb());
                return app;
            } catch (NameNotFoundException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    private void massIgnore(int ignoreType) {
        for (App app : apps) {
            setIgnore(app, ignoreType);
        }
        adapter.notifyDataSetChanged();
        new Thread(new Runnable() {
            public void run() {
                Database.setApps(apps);
            }
        }).start();
    }

    private void setIgnore(App app, int ignoreType) {
        if (!app.getEnabled() & (ignoreType == IGNORE_TOGGLE | ignoreType == IGNORE_NONE)) {
            app.setEnabled(true, ignoreType == IGNORE_TOGGLE);
            if (ignoreType == IGNORE_TOGGLE) {
                Toast.makeText(this, getString(R.string.app_is_not_ignored, app.getLabel()), Toast.LENGTH_SHORT).show();
            }
        } else if (app.getEnabled() & (ignoreType == IGNORE_TOGGLE | ignoreType == IGNORE_ALL)) {
            app.setEnabled(false, ignoreType == IGNORE_TOGGLE);
            if (ignoreType == IGNORE_TOGGLE) {
                Toast.makeText(this, getString(R.string.app_is_ignored, app.getLabel()), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Set the default enabled value for new apps.
     */
    private void setDefaultEnable(boolean enable) {
        defEnable = enable;
        Common.getPrefs(this).edit().putBoolean(KEY_DEFAULT_ENABLE, defEnable).apply();
    }


}
