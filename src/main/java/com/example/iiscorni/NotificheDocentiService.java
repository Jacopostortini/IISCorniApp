package com.example.iiscorni;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Intent;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NotificheDocentiService extends Service {

    private boolean isServiceRunning = false;
    private String circolareSalvata = "";
    //starts from 50 to reserve ids from 3 to 49 to circolari famiglie
    private int notificationId = 50;

    public NotificheDocentiService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(MainActivity.NOTIFICATION_DOC_ID, createForegroundNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startService();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        isServiceRunning = false;
        super.onDestroy();
    }

    public void startService(){
        if(isServiceRunning) return;
        isServiceRunning = true;

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (isServiceRunning){
                    searchForChanges();
                    try {
                        Thread.sleep(MainActivity.NOTIFICATION_CHECKING_INTERVAL);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public void searchForChanges(){
        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());

        StringRequest stringRequest = new StringRequest(Request.Method.GET, MainActivity.URL_CIRC_DOC,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        String ultimaCircolare=response;

                        Pattern pattern = Pattern.compile("<h3 class=\"media-heading.*?>.*</h3>");
                        Matcher matcher = pattern.matcher(response);
                        try {
                            if (matcher.find()) ultimaCircolare = matcher.group();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        if(!ultimaCircolare.equals(circolareSalvata) && !circolareSalvata.equals("")){
                            circolareSalvata = ultimaCircolare;

                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                            intent.putExtra("url", MainActivity.URL_CIRC_DOC);

                            createClickableNotification(getString(R.string.notification_channel_id),
                                    R.mipmap.ic_launcher,
                                    getString(R.string.notification_title),
                                    getString(R.string.doc_notification_description),
                                    NotificationCompat.PRIORITY_DEFAULT,
                                    notificationId,
                                    intent);
                            notificationId++;
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });

        queue.add(stringRequest);
    }

    public void createClickableNotification(String channel, int icon, String title, String text, int priority, int id, Intent intent){
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(getApplicationContext());
        stackBuilder.addNextIntentWithParentStack(intent);
        PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), channel)
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(priority)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());

        notificationManager.notify(id, builder.build());
    }

    public Notification createForegroundNotification(){
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(getApplicationContext());
        stackBuilder.addNextIntentWithParentStack(new Intent(getApplicationContext(), SettingsActivity.class));
        PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), getString(R.string.notification_channel_id))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.doc_service_running_notification_title))
                .setContentText(getString(R.string.doc_service_running_notification_description))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent);

        return builder.build();
    }
}
