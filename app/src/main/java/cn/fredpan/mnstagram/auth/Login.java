package cn.fredpan.mnstagram.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import cn.fredpan.mnstagram.R;

public class Login extends AppCompatActivity {

    EditText emailView;
    EditText passwordView;
    Button logInBtn;
    Button signUpBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.registration);

        //basic components
        emailView = (EditText) findViewById(R.id.email);
        passwordView = (EditText) findViewById(R.id.password);
        logInBtn = (Button) findViewById(R.id.register_btn);
        signUpBtn = (Button) findViewById(R.id.add_avatar);

        logInListener();
        signUpRedirectionListener();
    }

    private void logInListener() {
    }

    private void signUpRedirectionListener() {
        Intent registrationActivity = new Intent(Login.this, Registration.class);
        Login.this.startActivity(registrationActivity);
    }

}
