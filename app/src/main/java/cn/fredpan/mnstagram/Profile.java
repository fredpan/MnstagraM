package cn.fredpan.mnstagram;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import cn.fredpan.mnstagram.model.User;

public class Profile extends AppCompatActivity {

    private static FirebaseDatabase db;
    private static DatabaseReference dbRef;

    TextView usernameView;
    TextView bioView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile);

        db = (db==null)? FirebaseDatabase.getInstance():db;
        dbRef = (dbRef == null) ? db.getReference("users") : dbRef;

        //basic components
        usernameView = (TextView) findViewById(R.id.username);
        bioView = (TextView) findViewById(R.id.bio);

        displayUserInfo();
    }

    private void displayUserInfo() {
        // Read from the database
        dbRef.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                User user = dataSnapshot.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).getValue(User.class);
                        usernameView.setText(user.getUsername());
                        bioView.setText(user.getBio());
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w("LOAD USER: ", "Failed to read user.", error.toException());
            }
        });
    }

}
