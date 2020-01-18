package cn.fredpan.mnstagram.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import cn.fredpan.mnstagram.R;
import cn.fredpan.mnstagram.model.UserDto;

public class Profile extends Fragment {

//    private static FirebaseDatabase db;
//    private static DatabaseReference dbRef;
    FirebaseFirestore userDb;
    TextView usernameView;
    TextView bioView;

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.profile, container, false);

        userDb = (userDb == null) ? FirebaseFirestore.getInstance() : userDb;

        //basic components
        usernameView = (TextView) rootView.findViewById(R.id.username);
        bioView = (TextView) rootView.findViewById(R.id.bio);

        displayUserInfo();
        return rootView;
    }

//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.profile);
//
////        db = (db==null)? FirebaseDatabase.getInstance():db;
////        dbRef = (dbRef == null) ? db.getReference("users") : dbRef;
//        userDb = (userDb == null) ? FirebaseFirestore.getInstance() : userDb;
//
//        //basic components
//        usernameView = (TextView) findViewById(R.id.username);
//        bioView = (TextView) findViewById(R.id.bio);
//
//        displayUserInfo();
//    }

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
                    }
                } else {
                    Toast.makeText(getContext(), getActivity().getString(R.string.failed_retrieving_users_collection), Toast.LENGTH_LONG).show();
                    Log.w("LOGIN: ", "Error getting documents.", task.getException());
                }
            }
        });
    }

}
