package edu.upc.citm.android.speakerfeedback;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class FirestoreServiceListener extends Service {

    @Override
    public void onCreate()
    {
        super.onCreate();

        Log.i("SpeakerFeedback", "FirestoreServiceListener.onCreate");

        MainActivity.db.collection("rooms").document("OscarTestRoom").collection("polls")
                .addSnapshotListener(pollListener);    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.i("SpeakerFeedback", "FirestoreServiceListener.onStartCommand");

        createForegroundNotification();

        return START_NOT_STICKY;
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


    // Poll listener
    private EventListener<QuerySnapshot> pollListener = new EventListener<QuerySnapshot>() {
        @Override
        public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
            if (e != null) {
                Log.e("SpeakerFreedback", "Error loading polls list");
                return;
            }
            List<Poll> polls = new ArrayList<>();
            int i =0;
            for (DocumentSnapshot doc : documentSnapshots) {
                Poll poll = doc.toObject(Poll.class);

                if (!poll.isOpen())
                   continue;

                poll.setId(doc.getId());
                polls.add(poll);
                Log.i("FirestoreService", "New poll:\n" + poll.toString());

                createNewPollNotification(poll.getQuestion(), i);
                i++;
                }
            }
    };

    private void createForegroundNotification() {
        Intent intent =  new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        Notification notification = new NotificationCompat.Builder(this, App.CHANNEL_ID)
                .setContentTitle("Connected to OscarTestRoom")
                .setSmallIcon(R.drawable.ic_message)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);
    }

    private void createNewPollNotification(String poll, int i)
    {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,0,intent,0);

        Notification notification = new NotificationCompat.Builder(this, App.CHANNEL_ID)
                .setContentTitle(poll)
                .setSmallIcon(R.drawable.ic_message)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationManager.IMPORTANCE_HIGH)
                .build();

        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManagerCompat.notify(i + 2,notification);
    }
}
