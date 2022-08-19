package com.example.iiscorni;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.view.GravityCompat;

import com.google.android.material.navigation.NavigationView;

import androidx.drawerlayout.widget.DrawerLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.Menu;
import android.webkit.DownloadListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener{

    public static String BASE_URL = "https://www.istitutocorni.edu.it";
    public static String URL_REGISTRO_ELETTRONICO = "https://web.spaggiari.eu/home/app/default/login.php?ch=scuola";
    public static String REGISTRO_STUDENTI_PACKAGE_NAME = "eu.spaggiari.classevivastudente";
    public static String REGISTRO_FAMIGLIE_PACKAGE_NAME = "eu.spaggiari.classevivafamiglia";
    public static String REGISTRO_DOCENTI_PACKAGE_NAME = "eu.spaggiari.classevivadocente";
    public static String URL_CIRC_ALL = BASE_URL+"/comunicati?cerca=&categoria=0&tipo=comunicati";
    public static String URL_CIRC_FAM = BASE_URL+"/comunicati?cerca=&categoria=30003&tipo=comunicati";
    public static String URL_CIRC_DOC = BASE_URL+"/comunicati?cerca=&categoria=30002&tipo=comunicati";
    public static String URL_CIRC_ATA = BASE_URL+"/comunicati?cerca=&categoria=30004&tipo=comunicati";
    public static String URL_ORARIO_DIURNI = BASE_URL+"/pagine/-orario-corsi-diurni-";
    public static String URL_ORARIO_SERALI = BASE_URL+"/pagine/orario-corsi-serali-2019-2020";

    public boolean connectionAvailable;

    DrawerLayout drawer;
    NavigationView navigationView;
    Toolbar toolbar;
    WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        isConnected();
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, 0, 0);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);
        createNotificationChannel();
        if(connectionAvailable) setWebView();
        else {
            View error = findViewById(R.id.noConnection);
            error.setVisibility(View.VISIBLE);
            error.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    isConnected();
                    if(connectionAvailable) {
                        setWebView();
                        v.setVisibility(View.INVISIBLE);
                    }
                }
            });
        }

        findViewById(R.id.settingsButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
            }
        });

        findViewById(R.id.shareButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, webView.getUrl());
                sendIntent.setType("text/plain");

                Intent shareIntent = Intent.createChooser(sendIntent, null);
                startActivity(shareIntent);
            }
        });
    }

/*    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }*/

/*    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId()==R.id.action_settings){
            startActivity(new Intent(this, SettingsActivity.class));
        } else if(item.getItemId()==R.id.share_url){
            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, webView.getUrl());
            sendIntent.setType("text/plain");

            Intent shareIntent = Intent.createChooser(sendIntent, null);
            startActivity(shareIntent);
        }
        return super.onOptionsItemSelected(item);
    }*/

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item){
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        switch (id){
            case R.id.nav_home:
                webView.loadUrl(BASE_URL);
                break;
            case R.id.nav_registro:
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(REGISTRO_STUDENTI_PACKAGE_NAME);
                if (launchIntent != null) {
                    startActivity(launchIntent);
                } else {
                    launchIntent = getPackageManager().getLaunchIntentForPackage(REGISTRO_FAMIGLIE_PACKAGE_NAME);
                    if(launchIntent!=null){
                        startActivity(launchIntent);
                    }
                    else {
                        launchIntent = getPackageManager().getLaunchIntentForPackage(REGISTRO_DOCENTI_PACKAGE_NAME);
                        if(launchIntent!=null){
                            startActivity(launchIntent);
                        } else{
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(URL_REGISTRO_ELETTRONICO)));
                        }
                    }
                }
                break;
            case R.id.nav_circ_all:
                webView.loadUrl(URL_CIRC_ALL);
                break;
            case R.id.nav_circ_fam:
                webView.loadUrl(URL_CIRC_FAM);
                break;
            case R.id.nav_circ_doc:
                webView.loadUrl(URL_CIRC_DOC);
                break;
            case R.id.nav_circ_ata:
                webView.loadUrl(URL_CIRC_ATA);
                break;
            case R.id.nav_orario_diurni:
                webView.loadUrl(URL_ORARIO_DIURNI);
                break;
            case R.id.nav_orario_serali:
                webView.loadUrl(URL_ORARIO_SERALI);
                break;
            default:
                break;
        }
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void isConnected(){
        ConnectivityManager cm = (ConnectivityManager)getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.Q) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            connectionAvailable = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }else{
            try {
                cm.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback(){
                       @Override
                       public void onAvailable(Network network) {
                           connectionAvailable = true;
                       }
                       @Override
                       public void onLost(Network network) {
                           connectionAvailable = false;
                       }
                   }
                );
            }catch (Exception e){
                connectionAvailable = false;
            }
        }
    }

    public void setWebView(){
        webView = findViewById(R.id.web_view);
        String url = this.getIntent().getStringExtra("url");
        if(url!=null) webView.loadUrl(url);
        else webView.loadUrl(BASE_URL);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        webView.setDownloadListener(new DownloadListener() {

            public void onDownloadStart(String url, String userAgent,
                                        String contentDisposition, String mimetype,
                                        long contentLength) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            }

        });
    }

    public void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.notification_channel_name);
            String description = getString(R.string.notification_channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(getString(R.string.notification_channel_id), name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
