package edu.upc.citm.android.speakerfeedback;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SelectRoomActivity extends AppCompatActivity {

    private static final int REGISTER_USER = 0;

    TextView roomIdText;
    TextView roomPasswordText;

    private boolean testLastRoom = true;

    static public List<Room> roomList = new ArrayList<>();
    static public List<String> recentRooms = new ArrayList<>();

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    String userId;

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

            EnterRoom();
        }
    };




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_room);

        roomIdText = findViewById(R.id.roomIdText);
        roomPasswordText = findViewById(R.id.roomPassword);

        db.collection("rooms").addSnapshotListener(roomsListener);

        GetOrRegisterUser();

        // prefs = getSharedPreferences("config", MODE_PRIVATE);
        // String roomOpened = prefs.getString("roomOpened", "");
    }

    @Override
    protected void onStop() {

        UpdateOpenRooms();

        super.onStop();
    }

    private void UpdateOpenRooms() {
        int openRoom = 0;

        for (String roomID : recentRooms)
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

        roomInfoEditor.putInt("Size", recentRooms.size());
        roomInfoEditor.apply();
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

    private void EnterRoom() {
        if (testLastRoom) {
            // //Load recent room
            SharedPreferences roomInfo = getSharedPreferences("PreviousRoom", 0);
            String roomID = roomInfo.getString("roomID", "");

            boolean exists = false;
            Room desiredRoom = null;

            if (!roomID.equals("")) {
                for (Room room : roomList) {
                    if (room.getName().equals(roomID)) {
                        exists = true;
                        desiredRoom = room;
                        break;
                    }
                }

                if (exists && desiredRoom != null) {
                    if (desiredRoom.isOpen()) {
                        Toast.makeText(this, "Logging into " + roomID, Toast.LENGTH_SHORT).show();

                        if (!recentRooms.contains(roomID)) {
                            recentRooms.add(roomID);
                        }
                        //Join the room
                        Intent intent = new Intent(SelectRoomActivity.this, MainActivity.class);
                        intent.putExtra("roomID", desiredRoom.getName());
                        intent.putExtra("userId", userId);
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, roomID + " is now closed", Toast.LENGTH_SHORT).show();
                    }
                } else
                    Toast.makeText(this, roomID + " does no longer exist", Toast.LENGTH_SHORT).show();
            }
            testLastRoom = false;
        }
    }

    public void enterRoomClick(View view)
    {
        String roomID = roomIdText.getText().toString();
        String roomPassword = roomPasswordText.getText().toString();

        if (!roomID.equals("") || !roomPassword.equals(""))
        {
            boolean exists = false;
            Room desiredRoom = null;

            for(Room room : roomList)
            {
                if(room.getName().equals(roomID))
                {
                    exists = true;
                    desiredRoom = room;
                    break;
                }
            }

            if (exists && desiredRoom != null && desiredRoom.isOpen())
            {
                if (desiredRoom.getPassword().equals(roomPassword))
                {
                    Toast.makeText(this, "Password Correct, Logging into " + roomID, Toast.LENGTH_SHORT).show();

                    // Save the room id in the shared preferences
                    SharedPreferences roomInfo = getSharedPreferences("PreviousRoom", 0);
                    SharedPreferences.Editor roomInfoEditor = roomInfo.edit();

                    roomInfoEditor.putString("roomID", roomID);
                    roomInfoEditor.apply();

                    // Add the desired room to the recent rooms
                    if (!recentRooms.contains(roomID)) {
                        recentRooms.add(roomID);
                    }

                    //Join the room
                    Intent intent = new Intent(SelectRoomActivity.this, MainActivity.class);
                    intent.putExtra("roomID", desiredRoom.getName());
                    intent.putExtra("userId", userId);
                    startActivity(intent);
                }
                else
                {
                    Toast.makeText(this, "Password Incorrect, Try again", Toast.LENGTH_SHORT).show();
                    roomPasswordText.setText("");
                }
            }
            else
                Toast.makeText(this, "Room does not exist, check the Room ID", Toast.LENGTH_SHORT).show();

        }
    }

    public void openRoomsClick(View view)
    {
        Intent intent = new Intent(SelectRoomActivity.this, roomView.class);
        intent.putExtra("userId", userId);
        startActivity(intent);
    }
}
