package cn.fredpan.mnstagram;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import cn.fredpan.mnstagram.model.UserDto;

public class Profile extends AppCompatActivity {

//    private static FirebaseDatabase db;
//    private static DatabaseReference dbRef;
    FirebaseFirestore userDb;
    TextView usernameView;
    TextView bioView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile);

//        db = (db==null)? FirebaseDatabase.getInstance():db;
//        dbRef = (dbRef == null) ? db.getReference("users") : dbRef;
        userDb = (userDb == null) ? FirebaseFirestore.getInstance() : userDb;

        //basic components
        usernameView = (TextView) findViewById(R.id.username);
        bioView = (TextView) findViewById(R.id.bio);

        displayUserInfo();
    }

    private void displayUserInfo() {
        // Read from the database
        userDb.collection("users").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        if (document.getId().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                            UserDto userDto = document.toObject(UserDto.class);
                            usernameView.setText(userDto.getUsername());
                            bioView.setText(userDto.getBio());
                        }
                        Log.d("LOGIN: ", document.getId() + " => " + document.getData());
                    }
                } else {
                    Log.w("LOGIN: ", "Error getting documents.", task.getException());
                }
            }
        });
    }

}
