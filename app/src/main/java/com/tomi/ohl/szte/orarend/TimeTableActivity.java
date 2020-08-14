package com.tomi.ohl.szte.orarend;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsServiceConnection;
import androidx.browser.customtabs.CustomTabsSession;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;

public class TimeTableActivity extends AppCompatActivity {

    CustomTabsClient mCustomTabsClient;
    CustomTabsSession mCustomTabsSession;
    CustomTabsServiceConnection mCustomTabsServiceConnection;
    CustomTabsIntent mCustomTabsIntent;
    final String CUSTOM_TAB_PACKAGE_NAME = "com.android.chrome";

    ExpandableListView expandableListView;
    ExpandableListAdapter expandableListAdapter;
    List<String> expandableListTitle;
    LinkedHashMap<String, List<String>> napok;
    SharedPreferences pref;
    int themeSetting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.timetable);

        //lists init
        expandableListView = findViewById(R.id.kurzusList);
        napok = ExpandableListviewData.getData(getApplicationContext());
        expandableListTitle = new ArrayList<>(napok.keySet());
        expandableListAdapter = new CustomExpandableListview(this, expandableListTitle, napok);
        expandableListView.setAdapter(expandableListAdapter);

        //annyi nap legyen kinyitva, ahany nap van meg hatra
        int nemnyitni = 0;
        switch(Calendar.getInstance().get(Calendar.DAY_OF_WEEK)){
            case Calendar.TUESDAY:
                nemnyitni = 1;
                break;
            case Calendar.WEDNESDAY:
                nemnyitni = 2;
                break;
            case Calendar.THURSDAY:
                nemnyitni = 3;
                break;
            case Calendar.FRIDAY:
                nemnyitni = 0;          //legyen 4, ha pentek is engedelyezve van ExpandableListViewDataban
                break;
        }
        for(int i=expandableListAdapter.getGroupCount()-1; i>nemnyitni-1;i--) {
            expandableListView.expandGroup(i);
        }

        expandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                //expandableListTitle.get(groupPosition)  -->  listaelem felirata

                String osszesAdat = napok.get(expandableListTitle.get(groupPosition)).get(childPosition);
                String[] adatokTomb = osszesAdat.split("_");

                adatok(feldolgozottIdo(adatokTomb[0]),adatokTomb[1],eloadasCheck(adatokTomb[2]),adatokTomb[3],adatokTomb[4]);    //ido,targy,eloadasCheck(igen),tanar[1],terem[1]
                return false;
            }
        });

        //eltűnő actionbar elevation
        final ListView lv = expandableListView;
        lv.setOnScrollListener(new AbsListView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {}

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {
                if (firstVisibleItem == 0) {
                    //lista tetején vagyunk-e
                    View v = lv.getChildAt(0);
                    int offset = (v == null) ? 0 : v.getTop();
                    if (offset == 0) {
                        //teteje
                        getSupportActionBar().setElevation(0);
                    }
                    else
                        getSupportActionBar().setElevation(8);
                }
            }
        });

        //Chrome custom tabs
        mCustomTabsServiceConnection = new CustomTabsServiceConnection() {
            @Override
            public void onCustomTabsServiceConnected(ComponentName componentName, CustomTabsClient customTabsClient) {
                mCustomTabsClient= customTabsClient;
                mCustomTabsClient.warmup(0L);
                mCustomTabsSession = mCustomTabsClient.newSession(null);
                mCustomTabsSession.mayLaunchUrl(Uri.parse("http://www.neptun.u-szeged.hu"), null, null);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mCustomTabsClient= null;
            }
        };

        CustomTabsClient.bindCustomTabsService(this, CUSTOM_TAB_PACKAGE_NAME, mCustomTabsServiceConnection);

        mCustomTabsIntent = new CustomTabsIntent.Builder(mCustomTabsSession)
                .setShowTitle(true)
                .build();

        //Chrome custom tabs end

        //beallitott tema lekerese sharedprefbol
        pref = getSharedPreferences("pref", Context.MODE_PRIVATE);
        themeSetting = pref.getInt("theme", 0);

        //transparent navbar on Q+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            //getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
            getWindow().setNavigationBarColor(Color.TRANSPARENT);
        }

    }

    //ido pontositasa
    String feldolgozottIdo(String ido) {
        String[] idopontok = ido.split("-");
        if (Integer.parseInt(idopontok[1]) - Integer.parseInt(idopontok[0]) == 2)
            return idopontok[0] + ":00-" + (Integer.parseInt(idopontok[1])-1) + ":30";
        else if (Integer.parseInt(idopontok[1]) - Integer.parseInt(idopontok[0]) == 1)
            return idopontok[0] + ":00-" + (Integer.parseInt(idopontok[1])-1) + ":45";
        return ido;
    }

    //Ha 1, eloadasrol van szo, egyebkent gyakorlat
    String eloadasCheck(String szo) {
        String ember;
        if (szo.equals("1"))
            ember="Előadó";
        else ember="Gyakorlatvezető";
        return ember;
    }

    //felugro ablak (elemre kattintva)
    public void adatok(String ido,String targy, String eloadas, String tanar, String terem) {
        (new AlertDialog.Builder(TimeTableActivity.this))
                .setCancelable(true)
                .setTitle(targy)
                .setMessage("Időpont: " + ido
                        +   "\n" + eloadas + ": " + tanar
                        +   "\nTerem: " + terem)
                .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .show();
    }

    //toolbar-ikonok
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true; }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.neptun:
                mCustomTabsIntent.launchUrl(TimeTableActivity.this, Uri.parse("http://www.neptun.u-szeged.hu"));
                return true;
            case R.id.darkmode:
                setTheme();
                return true;
            case R.id.about:
                String verzio = null;
                try {
                    verzio = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                } catch (PackageManager.NameNotFoundException e) {}
                (new AlertDialog.Builder(TimeTableActivity.this))
                        .setCancelable(true)
                        .setTitle("Névjegy")
                        .setMessage("Hallgató: Öhlsläger Tamás\n"
                                + "Érvényesség: 2019/20/2\n"
                                + "Alkalmazásverzió: " + verzio)
                        .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        })
                        .show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //temavaltas
    public void setTheme() {
        final SharedPreferences.Editor editor = pref.edit();

        String[] themes = new String[]{"Rendszerbeállítás", "Automatikus", "Világos", "Sötét"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Téma");
        builder.setSingleChoiceItems(themes, themeSetting, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                        editor.putInt("theme", 0);
                        themeSetting = 0;
                        break;
                    case 1:
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_TIME);
                        editor.putInt("theme", 1);
                        themeSetting = 1;
                        break;
                    case 2:
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                        editor.putInt("theme", 2);
                        themeSetting = 2;
                        break;
                    case 3:
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                        editor.putInt("theme", 3);
                        themeSetting = 3;
                        break;
                }
                editor.apply();
                dialog.cancel();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });
        builder.show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mCustomTabsServiceConnection != null) {
            unbindService(mCustomTabsServiceConnection);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus){
        super.onWindowFocusChanged(hasFocus);

        //move nyil to the right
        expandableListView.setIndicatorBoundsRelative(expandableListView.getWidth()-100,expandableListView.getWidth());
    }

}
