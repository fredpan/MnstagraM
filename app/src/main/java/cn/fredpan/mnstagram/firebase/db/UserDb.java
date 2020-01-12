package cn.fredpan.mnstagram.firebase.db;

import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import cn.fredpan.mnstagram.model.User;

public class UserDb {

    private static FirebaseDatabase db;
    private static DatabaseReference dbRef;

    private static void init(){
        db = (db==null)? FirebaseDatabase.getInstance():db;
        dbRef = (dbRef == null) ? db.getReference("user") : dbRef;
    }

    public static void storeUser(User user){
        init();
        // Write a message to the database
        dbRef.setValue(user);
        Log.d("LOAD USER: ", "User is: " + user.getUsername());
    }

    public static User getUser(){
        init();
        final User[] user = {null};
        // Read from the database
        dbRef.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                user[0] = dataSnapshot.getValue(User.class);
                Log.d("LOAD USER: ", "User is: " + user[0].getUsername());

            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w("LOAD USER: ", "Failed to read user.", error.toException());
            }
        });
        return user[0];
    }

}
