package edu.upc.citm.android.speakerfeedback;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import edu.upc.citm.android.speakerfeedback.Poll;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.Inflater;

public class MainActivity extends AppCompatActivity
{
    private static final int REGISTER_USER = 0;

    public static FirebaseFirestore db = FirebaseFirestore.getInstance();

    private TextView textView;
    private String userId;
    private String roomId;
    private ListenerRegistration roomRegistration;
    private ListenerRegistration usersRegistration;
    private List<Poll> polls = new ArrayList<>();
    private RecyclerView pollsView;
    private Adapter adapter;


    // Listeners
    //---------------------------------------------------------------------------------
    // Room listener

        private EventListener<DocumentSnapshot> roomListener = new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(DocumentSnapshot documentSnapshot, FirebaseFirestoreException e) {
                if (e != null) {
                    Log.e("SpeakerFeedback", "Error loading rooms/" + roomId, e);
                    return;
                }

                Boolean isOpen = documentSnapshot.getBoolean("open");
                assert isOpen != null;
                if (isOpen) {
                    String name = documentSnapshot.getString("name");
                    setTitle(name);
                }
                else
                {
                    Log.i("SpeakerFeedback", "Room closed, exiting room");
                }

                Log.i("SpeakerFeedback", "Is room open? " + isOpen.toString());
            }
        };

        // User listener
        private EventListener<QuerySnapshot> usersListener = new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
                if (e != null) {
                    Log.e("SpeakerFeedBack", "Error reading users inside room", e);
                    return;
                }
                textView.setText(Integer.toString(documentSnapshots.size()));
            }
        };

        // Poll listener
        private EventListener<QuerySnapshot> pollListener = new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
                if (e != null) {
                    Log.e("SpeakerFreedback", "Error loading polls list");
                    return;
                }
                polls.clear();
                for (DocumentSnapshot doc : documentSnapshots) {
                    Poll poll = doc.toObject(Poll.class);
                    poll.setId(doc.getId());
                    polls.add(poll);
                }
                Log.i("SpeakerFeedback", String.format("New polls loaded, %d polls.", polls.size()));
                adapter.notifyDataSetChanged();
            }
        };

    //---------------------------------------------------------------------------------


    // Support classes
    //---------------------------------------------------------------------------------

        class ViewHolder extends RecyclerView.ViewHolder {

            private TextView questionView;
            private TextView optionsView;
            private TextView labelView;
            private CardView cardView;


            ViewHolder(View itemView) {
                super(itemView);
                questionView = itemView.findViewById(R.id.questionView);
                optionsView = itemView.findViewById(R.id.optionsView);
                labelView = itemView.findViewById(R.id.labelView);
                cardView = itemView.findViewById(R.id.cardView);
                cardView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int pos = getAdapterPosition();
                        onClickCardView(pos);
                    }
                });
            }
        }

        class Adapter extends RecyclerView.Adapter<ViewHolder> {

            @NonNull
            @Override
            public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View itemView = getLayoutInflater().inflate(R.layout.poll, parent, false);
                return new ViewHolder(itemView);
            }

            @Override
            public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
                Poll poll = polls.get(position);
                if (position == 0) {
                    holder.labelView.setVisibility(View.VISIBLE);
                    if (poll.isOpen())
                        holder.labelView.setText(R.string.active); //Active
                    else
                        holder.labelView.setText(R.string.previous); // Previous
                } else {
                    if (!poll.isOpen() && polls.get(position - 1).isOpen()) {
                        holder.labelView.setText(R.string.previous); // Previous
                        holder.labelView.setVisibility(View.VISIBLE);
                    } else {
                        holder.labelView.setVisibility(View.GONE);
                    }
                }

                holder.cardView.setCardElevation(poll.isOpen() ? 5.0f : 0.0f);

                holder.questionView.setText(poll.getQuestion());


                StringBuilder tempOptions = new StringBuilder();
                List<String> options = poll.getOptions();

                for (String iterator : options) {
                    tempOptions.append(iterator);
                    tempOptions.append("\n");
                }

                holder.optionsView.setText(tempOptions.toString());
            }

            @Override
            public int getItemCount() {
                return polls.size();
            }
        }

    //---------------------------------------------------------------------------------


    // Override methods
    //---------------------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.usersCountView);
        pollsView = findViewById(R.id.pollsView);

        adapter = new Adapter();

        pollsView.setLayoutManager(new LinearLayoutManager(this));
        pollsView.setAdapter(adapter);

        roomId = "";

        // Check if the user has already logged in (when you rotate f.e.)
        //SharedPreferences sharedPreferences = getSharedPreferences("config", MODE_PRIVATE);
        // serviceStarted = sharedPreferences.getBoolean("logged", false);

        //if(!serviceStarted)
        startFirestoreListenerService();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId())
        {
            case R.id.coseApp:
                // Stop Firestore service when user closes the app
                stopFirestoreListenerService();


                SharedPreferences prefs = getSharedPreferences("config", MODE_PRIVATE);
                prefs.edit().putString("roomOpened", roomId).apply();


                //Close the app
                finish();
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onStart()
    {
        super.onStart();

        // Get the room the user wants, if for some reason we don't have a room we send the user to "testroom"
        Intent intent = getIntent();
        String roomName = intent.getStringExtra("roomName");
        if(roomName == null) {
            roomName = "testroom";
            Toast.makeText(this, "No room provided, entering testroom", Toast.LENGTH_SHORT).show();
        }
        roomId = roomName;

        roomRegistration = db.collection("rooms").document(roomId)
                .addSnapshotListener(this, roomListener);
        usersRegistration = db.collection("users").whereEqualTo("rooms", roomId)
                .addSnapshotListener(this, usersListener);

        db.collection("rooms").document(roomId).collection("polls")
                .orderBy("start", Query.Direction.DESCENDING)
                .addSnapshotListener(this, pollListener);
    }

    @Override
    protected void onStop()
    {
        super.onStop();

        roomRegistration.remove();
        usersRegistration.remove();

    }


    @Override
    protected void onDestroy() {
        // Remove user from room
        db.collection("users").document(userId).update("room", FieldValue.delete());

        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode) {
            case REGISTER_USER:
                if (resultCode == RESULT_OK) {
                    String name = data.getStringExtra("name");
                    registerUser(name);
                } else {
                    Toast.makeText(this, "Please, register a name", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    //---------------------------------------------------------------------------------


    // Listener services
    //---------------------------------------------------------------------------------
    private void startFirestoreListenerService() {
        Intent intent = new Intent(this, FirestoreServiceListener.class);
        intent.putExtra("room", roomId);
        startService(intent);
    }

    private void stopFirestoreListenerService() {
        Intent intent = new Intent(this, FirestoreServiceListener.class);
        stopService(intent);
    }
    //---------------------------------------------------------------------------------


    // General methods
    //---------------------------------------------------------------------------------

    private void registerUser(String name)
    {
        Map<String, Object> fields = new HashMap<>();
        fields.put("name", name);

        // Save the time the user created his account (Should we update this date every time the user logs in?)
        fields.put("last_active", new Date());

        db.collection("users").add(fields).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
            @Override
            public void onSuccess(DocumentReference documentReference)
            {
                userId = documentReference.getId();
                SharedPreferences prefs = getSharedPreferences("config", MODE_PRIVATE);
                prefs.edit().putString("userId", userId).apply();

                Log.i("SpeakerFeedback", "New user: userId = " + userId);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e("SpeakerFeedback", "Error loading objects", e);
                Toast.makeText(MainActivity.this,
                        "User could not be loaded, try later", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }


    private void saveVote(String pollid, int which)
    {
        Map <String, Object> map = new HashMap<>();
        map.put("option",which);
        map.put("pollid",pollid);
        db.collection("rooms").document(roomId).collection("votes").document(userId).set(map);
    }
    //---------------------------------------------------------------------------------


    // On click listeners
    //---------------------------------------------------------------------------------
    public void onClickUsers(View view)
    {
        Log.i("SpeakerFeedback", "Clicked user bar");
        Intent intent = new Intent(this, UserListActivity.class);
        startActivity(intent);
    }

    public void onClickCardView(int pos)
    {
        final Poll current_poll = polls.get(pos);

        if (current_poll.isOpen()) {
            List<String> optlist = current_poll.getOptions();
            String[] options = new String[optlist.size()];
            for (int i = 0; i < optlist.size(); i++) {
                options[i] = optlist.get(i);
                Log.i("SpeakerFeedback", options[i]);
            }

            Log.i("SpeakerFeedback", "Clicked poll");
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Vote")
                    //.setMessage("Get poll question here")
                    .setItems(options, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            saveVote(current_poll.getId(), which);
                            dialog.dismiss();

                            Toast.makeText(MainActivity.this, "You voted '" + current_poll.getOption(which) + "'\n Thanks for voting", Toast.LENGTH_SHORT).show();
                        }
                    }).show();
        }
    }
    //---------------------------------------------------------------------------------
}