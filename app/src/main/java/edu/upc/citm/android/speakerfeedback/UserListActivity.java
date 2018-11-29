package edu.upc.citm.android.speakerfeedback;

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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;


public class UserListActivity extends AppCompatActivity {

    private List<String> itemList;
    private RecyclerView usersList;
    private Adapter adapter;
    private ListenerRegistration usersRegistration;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Listeners
    //---------------------------------------------------------------------------------
    private EventListener<QuerySnapshot> usersListener = new EventListener<QuerySnapshot>() {
        @Override
        public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e)
        {
            if (e != null) {
                Log.e("SpeakerFeedback", "Error al rebre usuaris dins d'un room", e);
                return;
            }

            itemList.clear();
            for(DocumentSnapshot doc : documentSnapshots){
                itemList.add(doc.getString("name"));
            }

            adapter.notifyDataSetChanged();
        }
    };
    //---------------------------------------------------------------------------------

    // Support classes
    //---------------------------------------------------------------------------------
    class ViewHolder extends RecyclerView.ViewHolder
    {
        TextView item;

        ViewHolder(View itemView)
        {
            super(itemView);

            this.item = itemView.findViewById(R.id.item);
        }
    }

    class Adapter extends RecyclerView.Adapter<ViewHolder>
    {
        @Override public int getItemCount() {  return itemList.size();  }

        @NonNull
        @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
            View itemView = getLayoutInflater().inflate(R.layout.users_info , parent, false);
            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position){
            String modelItem = itemList.get(position);
            holder.item.setText(modelItem);
        }
    }
    //---------------------------------------------------------------------------------



    // Override methods
    //---------------------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_user_list);

        itemList = new ArrayList<>();

        usersList = findViewById(R.id.usersView);
        usersList.setLayoutManager(new LinearLayoutManager(this));

        adapter = new Adapter();
        usersList.setAdapter(adapter);

        usersRegistration = db.collection("users").whereEqualTo("room", "testroom").addSnapshotListener(usersListener);
    }
    //---------------------------------------------------------------------------------

}