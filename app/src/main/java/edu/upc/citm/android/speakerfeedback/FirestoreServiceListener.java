package edu.upc.citm.android.speakerfeedback;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class FirestoreServiceListener extends Service {

    @Override
    public void onCreate()
    {
        super.onCreate();

        Log.i("SpeakerFeedback", "FirestoreServiceListener.onCreate");

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.i("SpeakerFeedback", "FirestoreServiceListener.onStartCommand");

        createForegroundNotification();

        return START_NOT_STICKY;
    }

    private void createForegroundNotification() {
        Intent intent =  new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        Notification notification = new NotificationCompat.Builder(this, App.CHANNEL_ID)
                .setContentTitle(String.format("Connected to testRoom"))
                .setSmallIcon(R.drawable.ic_message)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);
    }

    @Override
    public void onDestroy()
    {
        Log.i("SpeakerFeedback", "FirestoreServiceListener.onDestroy");
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }
}
