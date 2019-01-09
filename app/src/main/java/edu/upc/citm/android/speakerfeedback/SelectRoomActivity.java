package edu.upc.citm.android.speakerfeedback;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.nio.channels.SelectableChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SelectRoomActivity extends AppCompatActivity {

    private static final int REGISTER_USER = 0;

    List<Room> roomList = new ArrayList<>();
    List<String> recentRooms = new ArrayList<>();

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    String userId;

    RecyclerView recentRoomsGrid;
    Adapter adapter;

    //Rooms listener
    EventListener<QuerySnapshot> roomsListener = new EventListener<QuerySnapshot>() {
        @Override
        public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {

            if (e != null) {
                Log.e("SpeakerFeedback", e.getMessage());
                return;
            }

            roomList.clear();
            for (DocumentSnapshot document : documentSnapshots) {
                Room room = document.toObject(Room.class);
                roomList.add(room);
            }

            //Removing closed or deleted rooms from the recent rooms list
            for(int i = 0; i < recentRooms.size(); ++i)
            {
                boolean roomClosed = true;
                for(Room room : roomList)
                {
                    if (recentRooms.get(i).equals(room.getName()))
                    {
                        if(room.isOpen())
                        {
                            roomClosed = false;
                            break;
                        }
                    }
                }

                if(roomClosed)
                {
                    recentRooms.remove(i);
                    i--;
                }
            }
            adapter.notifyDataSetChanged();
        }
    };

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView textView;

        public ViewHolder(View itemView) {
            super(itemView);

            // useLogo -> recent room
            textView = itemView.findViewById(R.id.useLogo);
        }
    }

    public class Adapter extends RecyclerView.Adapter<ViewHolder>
    {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
        {
            // users_info -> recent room
            View itemView = getLayoutInflater().inflate(R.layout.users_info, parent, false);
            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {

            String recentName = recentRooms.get(position);
            holder.textView.setText(recentName);

            holder.textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                      RecentRoomClicked(position);
                }
            });
        }

        @Override
        public int getItemCount()
        {
            return recentRooms.size();
        }
    }

    void RecentRoomClicked(int position)
    {
        String room = recentRooms.get(position);

        //Join the clicked room
        Intent intent = new Intent(SelectRoomActivity.this, MainActivity.class);
        intent.putExtra("roomName", room);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_room);


        db.collection("rooms").addSnapshotListener(roomsListener);

        GetOrRegisterUser();

        //Load recent room
        SharedPreferences prefs = getSharedPreferences(userId, MODE_PRIVATE);
        String lastRoomID = prefs.getString("roomID", "");
        String lastRoomPassword = prefs.getString("roomPassword", "");


        prefs = getSharedPreferences("config", MODE_PRIVATE);
        String roomOpened = prefs.getString("roomOpened", "");

    }

    private void GetOrRegisterUser()
    {
        // We search in the app preferences the user ID to know if the user has already registered.
        SharedPreferences prefs = getSharedPreferences("config", MODE_PRIVATE);
        userId = prefs.getString("userId", null);
        if (userId == null) {
            // We register the user. Ask for his name
            Intent intent = new Intent(this, RegisterUserActivity.class);
            startActivityForResult(intent, REGISTER_USER);
            Toast.makeText(this, "Register please", Toast.LENGTH_SHORT).show();
        } else {
            // If the user is registered we update the last_active
            db.collection("users").document(userId).update("last_active", new Date());

            Log.i("SpeakerFeedback", "userId = " + userId);
        }

        prefs.edit().putBoolean("logged", true).apply();
    }

    private void EnterRoom(String roomID, String roomPassword)
    {
        if (!roomID.equals("") || !roomPassword.equals(""))
        {

        }
    }

}
