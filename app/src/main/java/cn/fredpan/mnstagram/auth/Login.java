package cn.fredpan.mnstagram.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import cn.fredpan.mnstagram.Profile;
import cn.fredpan.mnstagram.R;

public class Login extends AppCompatActivity {

    EditText emailView;
    EditText passwordView;
    Button logInBtn;
    Button signUpBtn;
    private static FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        mAuth = (mAuth == null)? FirebaseAuth.getInstance() : mAuth;

        //basic components
        emailView = (EditText) findViewById(R.id.email);
        passwordView = (EditText) findViewById(R.id.password);
        logInBtn = (Button) findViewById(R.id.sign_in);
        signUpBtn = (Button) findViewById(R.id.sign_up);

        logInListener();
        signUpRedirectionListener();
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
                                    Log.d("SIGNIN: ", "signInWithEmail:success");
                                    Intent profileActivity = new Intent(Login.this, Profile.class);
                                    Login.this.startActivity(profileActivity);
                                } else {
                                    // If sign in fails, display a message to the user.
                                    Log.d("SIGNIN: ", "signInWithEmail:failure", task.getException());
                                    Toast.makeText(Login.this, "Authentication failed. Password or Username incorrect!",
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
