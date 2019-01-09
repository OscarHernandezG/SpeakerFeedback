package edu.upc.citm.android.speakerfeedback;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.nio.channels.SelectableChannel;
import java.util.ArrayList;
import java.util.List;

public class SelectRoomActivity extends AppCompatActivity {

    List<Room> roomList = new ArrayList<>();
    List<String> recentRooms = new ArrayList<>();

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    String userId;

    RecyclerView recentsGrid;
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

        //Join the room
        Intent intent = new Intent(SelectRoomActivity.this, MainActivity.class);
        intent.putExtra("roomName", room);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_room);
    }
}
