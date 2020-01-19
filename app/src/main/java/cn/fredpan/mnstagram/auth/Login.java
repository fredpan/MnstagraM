package cn.fredpan.mnstagram.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import cn.fredpan.mnstagram.MainActivity;
import cn.fredpan.mnstagram.R;
import cn.fredpan.mnstagram.model.User;
import cn.fredpan.mnstagram.model.UserDto;

public class Login extends AppCompatActivity {

    EditText emailView;
    EditText passwordView;
    Button logInBtn;
    TextView signUpBtn;
    private static FirebaseAuth mAuth;
    FirebaseFirestore userDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        mAuth = (mAuth == null)? FirebaseAuth.getInstance() : mAuth;
        userDb = (userDb == null) ? FirebaseFirestore.getInstance() : userDb;

        //basic components
        emailView = (EditText) findViewById(R.id.email);
        passwordView = (EditText) findViewById(R.id.password);
        logInBtn = (Button) findViewById(R.id.sign_in);
        signUpBtn = (TextView) findViewById(R.id.sign_up);

        loginDirectly();

        logInListener();
        signUpRedirectionListener();
    }

    private void loginDirectly() {
        if (mAuth.getCurrentUser() != null) {
            System.out.println(mAuth.getCurrentUser().getEmail());
            Intent profileActivity = new Intent(Login.this, MainActivity.class);
            Login.this.startActivity(profileActivity);
        }
    }

    private void logInListener() {
        logInBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = emailView.getText().toString();
                String password = passwordView.getText().toString();
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(Login.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    // Sign in success, update UI with the signed-in user's information
                                    userDb.collection("users").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                            if (task.isSuccessful()) {
                                                FirebaseUser currUser = FirebaseAuth.getInstance().getCurrentUser();
                                                for (QueryDocumentSnapshot document : task.getResult()) {
                                                    if (document.getId().equals(currUser.getUid())) {
                                                        UserDto userDto = document.toObject(UserDto.class);
                                                        User user = userDto.generateUser(null, currUser.getUid(), currUser.getEmail());
                                                        Intent mainActivity = new Intent(Login.this, MainActivity.class);
                                                        mainActivity.putExtra("user", user);
                                                        Login.this.startActivity(mainActivity);
                                                    }
                                                }
                                            } else {
                                                Toast.makeText(Login.this, getString(R.string.failed_retrieving_users_collection), Toast.LENGTH_LONG).show();
                                                Log.w("LOGIN: ", "Error getting documents.", task.getException());
                                            }
                                        }
                                    });
                                } else {
                                    // If sign in fails, display a message to the used.
                                    Toast.makeText(Login.this, getString(R.string.failed_auth_login),
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });
    }

    private void signUpRedirectionListener() {
        signUpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent registrationActivity = new Intent(Login.this, Registration.class);
                Login.this.startActivity(registrationActivity);
            }
        });
    }

}
