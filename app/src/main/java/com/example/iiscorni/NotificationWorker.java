package com.example.iiscorni;

import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NotificationWorker extends Worker {

    private int notificationId;
    private String circolareSalvata = "";
    private String URL;
    private String notificationDescription;
    private String sharedPrefKey;

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        if (getTags().contains(getString(R.string.circ_fam_pref))) {
            URL = MainActivity.URL_CIRC_FAM;
            notificationDescription = getString(R.string.fam_notification_description);
            sharedPrefKey = getString(R.string.circ_fam_sharedpref);
        } else if (getTags().contains(getString(R.string.circ_doc_pref))) {
            URL = MainActivity.URL_CIRC_DOC;
            notificationDescription = getString(R.string.doc_notification_description);
            sharedPrefKey = getString(R.string.circ_doc_sharedpref);
        } else if (getTags().contains(getString(R.string.circ_ata_pref))) {
            URL = MainActivity.URL_CIRC_ATA;
            notificationDescription = getString(R.string.ata_notification_description);
            sharedPrefKey = getString(R.string.circ_doc_sharedpref);
        }

        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(getString(R.string.shared_preferences_key), Context.MODE_PRIVATE);
        circolareSalvata = sharedPreferences.getString(sharedPrefKey, "");
        notificationId = sharedPreferences.getInt(getString(R.string.notification_id), 0);

        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());

        StringRequest stringRequest = new StringRequest(Request.Method.GET, URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        String ultimaCircolare = response;

                        Pattern pattern = Pattern.compile("<h3 class=\"media-heading.*?>.*</h3>");
                        Matcher matcher = pattern.matcher(response);
                        try {
                            if (matcher.find()) ultimaCircolare = matcher.group();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        String[] strings = ultimaCircolare.split(">")[1].split("<")[0].split("_");
                        strings[0] = "";
                        String messaggio = "";
                        for(String s : strings){
                            messaggio += (s.equals("")) ? "" : s+" ";
                        }

                        getApplicationContext()
                                .getSharedPreferences(getString(R.string.shared_preferences_key), Context.MODE_PRIVATE)
                                .edit()
                                .putString(sharedPrefKey, ultimaCircolare)
                                .apply();

                        if (!ultimaCircolare.equals(circolareSalvata) && !circolareSalvata.equals("")) {

                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                            intent.putExtra("url", URL);

                            createClickableNotification(getApplicationContext().getString(R.string.notification_channel_id),
                                    R.mipmap.ic_launcher,
                                    notificationDescription,
                                    messaggio,
                                    NotificationCompat.PRIORITY_HIGH,
                                    notificationId+1,
                                    intent);

                            getApplicationContext()
                                    .getSharedPreferences(getString(R.string.shared_preferences_key), Context.MODE_PRIVATE)
                                    .edit()
                                    .putInt(getString(R.string.notification_id), notificationId+1)
                                    .apply();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });

        queue.add(stringRequest);

        return Result.success();
    }

    private void createClickableNotification(String channel, int icon, String title, String text, int priority, int id, Intent intent) {

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), channel)
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(priority)
                .setAutoCancel(true);

        if(intent!=null) {
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(getApplicationContext());
            stackBuilder.addNextIntentWithParentStack(intent);
            PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(resultPendingIntent);
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());

        notificationManager.notify(id, builder.build());
    }

    private String getString(int resId){
        return getApplicationContext().getString(resId);
    }


}
