package edu.upc.citm.android.speakerfeedback;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class roomView extends AppCompatActivity {

    Adapter adapter;
    private String userId;
    private RecyclerView roomsView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_view);


        SharedPreferences prefs = getSharedPreferences("Open Size", MODE_PRIVATE);
        int size =  prefs.getInt("Size", 0);

        for (int i =0; i < size; ++i) {
            SharedPreferences room = getSharedPreferences("OpenRoom" + Integer.toString(i), MODE_PRIVATE);
            String roomID = room.getString("roomID", "");

            if (!SelectRoomActivity.recentRooms.contains(roomID))
                SelectRoomActivity.recentRooms.add(roomID);
        }

        roomsView = findViewById(R.id.roomsView);

        adapter = new Adapter();

        Intent intent = getIntent();
        userId = intent.getStringExtra("userId");

        roomsView.setLayoutManager(new LinearLayoutManager(this));
        roomsView.setAdapter(adapter);
    }

    @Override
    protected void onStop() {

        int openRoom = 0;

        for (String roomID : SelectRoomActivity.recentRooms)
        {
            // Save the room id in the shared preferences
            SharedPreferences roomInfo = getSharedPreferences("OpenRoom" + Integer.toString(openRoom), 0);
            SharedPreferences.Editor roomInfoEditor = roomInfo.edit();

            roomInfoEditor.putString("roomID", roomID);
            roomInfoEditor.apply();

            openRoom++;
        }

        SharedPreferences roomInfo = getSharedPreferences("Open Size", 0);
        SharedPreferences.Editor roomInfoEditor = roomInfo.edit();

        roomInfoEditor.putInt("Size", SelectRoomActivity.recentRooms.size());
        roomInfoEditor.apply();

        super.onStop();
    }

    //Rooms listener
    EventListener<QuerySnapshot> roomsListener = new EventListener<QuerySnapshot>() {
        @Override
        public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {

            if (e != null) {
                Log.e("SpeakerFeedback", e.getMessage());
                return;
            }



            for (DocumentSnapshot document : documentSnapshots) {
                Room room = document.toObject(Room.class);
                SelectRoomActivity.roomList.add(room);
            }

            //Removing closed or deleted rooms from the recent rooms list
            for(int i = 0; i < SelectRoomActivity.recentRooms.size(); ++i)
            {
                boolean roomClosed = true;
                for(Room room : SelectRoomActivity.roomList)
                {
                    if (SelectRoomActivity.recentRooms.get(i).equals(room.getName()))
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
                    SelectRoomActivity.recentRooms.remove(i);
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

            textView = itemView.findViewById(R.id.questionView);
        }
    }

    public class Adapter extends RecyclerView.Adapter<ViewHolder>
    {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = getLayoutInflater().inflate(R.layout.room, parent, false);
            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, final int position)
        {
            String recentName = SelectRoomActivity.recentRooms.get(position);
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
            return SelectRoomActivity.recentRooms.size();
        }
    }

    void RecentRoomClicked(int position)
    {
        String room = SelectRoomActivity.recentRooms.get(position);

        //Join the room
        Intent intent = new Intent(roomView.this, MainActivity.class);
        intent.putExtra("roomID", room);
        intent.putExtra("userId", userId);
        startActivity(intent);

        finish();
    }
}
