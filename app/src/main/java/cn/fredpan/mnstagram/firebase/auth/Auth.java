package cn.fredpan.mnstagram.firebase.auth;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import androidx.annotation.NonNull;
import cn.fredpan.mnstagram.firebase.db.UserDb;
import cn.fredpan.mnstagram.model.User;

public class Auth {
    private static FirebaseAuth mAuth;

    private static void init(){
        mAuth = (mAuth == null)? FirebaseAuth.getInstance() : mAuth;
    }

    public static void registration(final User user, String password, final Activity activity){
        init();
        mAuth.createUserWithEmailAndPassword(user.getEmail(), password)
                .addOnCompleteListener(activity, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update User table
                            Log.d("REGISTRATION: ", "createUserWithEmail:success");
                            FirebaseUser currUser = mAuth.getCurrentUser();
                            assert currUser != null;
                            user.setUid(currUser.getUid());
                            UserDb.storeUser(user);
//                            updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.d("REGISTRATION: ", "createUserWithEmail:failure", task.getException());
                            Toast.makeText(activity.getBaseContext(), "Unable to register, please try again.", Toast.LENGTH_SHORT).show();
//                            activity.updateUI(null);
                        }
                    }
                });
    }

    public static void signin(String email, String password, Activity activity){
        init();
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(activity, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("SIGNIN: ", "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
//                            updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.d("SIGNIN: ", "signInWithEmail:failure", task.getException());
//                            Toast.makeText(activity.this, "Authentication failed.",
//                                    Toast.LENGTH_SHORT).show();
//                            updateUI(null);
                        }

                        // ...
                    }
                });
    }

    public static FirebaseUser getUserInfo(){
        init();
//        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
//        if (user != null) {
//            // Name, email address, and profile photo Url
//            String name = user.getDisplayName();
//            String email = user.getEmail();
//            Uri photoUrl = user.getPhotoUrl();
//
//            // Check if user's email is verified
//            boolean emailVerified = user.isEmailVerified();
//
//            // The user's ID, unique to the Firebase project. Do NOT use this value to
//            // authenticate with your backend server, if you have one. Use
//            // FirebaseUser.getIdToken() instead.
//            String uid = user.getUid();
//        }
        return FirebaseAuth.getInstance().getCurrentUser();
    }
}
