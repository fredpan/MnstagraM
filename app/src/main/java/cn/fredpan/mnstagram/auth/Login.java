/*
 * MIT License
 *
 * Copyright (c) 2020 Liren Pan (https://github.com/fredpan)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * 1) Enjoy your coding
 * 2) Have a nice day
 * 3) Be happy
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package cn.fredpan.mnstagram.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import cn.fredpan.mnstagram.MainActivity;
import cn.fredpan.mnstagram.R;
import cn.fredpan.mnstagram.model.User;
import cn.fredpan.mnstagram.model.UserDto;

public class Login extends AppCompatActivity {

    EditText emailView;
    EditText passwordView;
    Button logInBtn;
    TextView signUpBtn;
    ProgressBar progressBar;
    private static FirebaseAuth mAuth;
    FirebaseFirestore userDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        mAuth = (mAuth == null)? FirebaseAuth.getInstance() : mAuth;
        userDb = (userDb == null) ? FirebaseFirestore.getInstance() : userDb;

        //basic components
        emailView = findViewById(R.id.email);
        passwordView = findViewById(R.id.password);
        logInBtn = findViewById(R.id.sign_in);
        signUpBtn = findViewById(R.id.sign_up);
        progressBar = findViewById(R.id.progress_bar);

        //init folders
        initFolders();

        loginDirectly();

        logInListener();
        signUpRedirectionListener();
        emailFormatCheckListener();
        passwordLengthCheckListener();
    }

    /**
     * init all the necessary folders here
     */
    private void initFolders() {
//        File folder = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES) + ImgHelper.PIC_TEMP_PATH);
//        if (!folder.exists()) {
//            folder.mkdirs();
//        }
    }

    private void passwordLengthCheckListener() {
        passwordView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (!hasFocus) {
                    if (passwordView.getText().toString().length()<6) {
                        passwordView.setError(getString(R.string.error_password_length_too_short));
                        progressBar.setVisibility(View.INVISIBLE);
                    }
                }
            }
        });
    }

    private void emailFormatCheckListener() {
        emailView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (!hasFocus) {
                    if (!Patterns.EMAIL_ADDRESS.matcher(emailView.getText().toString()).matches()) {
                        emailView.setError(getString(R.string.error_bad_email_addr_format));
                        progressBar.setVisibility(View.INVISIBLE);
                    }
                }
            }
        });
    }

    private void loginDirectly() {
        if (mAuth.getCurrentUser() != null) {
            Intent profileActivity = new Intent(Login.this, MainActivity.class);
            Login.this.startActivity(profileActivity);
        }
    }

    private void logInListener() {
        logInBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressBar.setVisibility(View.VISIBLE);
                String email = emailView.getText().toString();
                //check email input format
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches() || passwordView.getText().toString().length() < 6) {
                    if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        emailView.setError(getString(R.string.error_bad_email_addr_format));
                    }
                    if (passwordView.getText().toString().length() < 6) {
                        passwordView.setError(getString(R.string.error_password_length_too_short));
                    }
                    progressBar.setVisibility(View.INVISIBLE);
                    Toast.makeText(Login.this, getString(R.string.failed_auth_login),
                            Toast.LENGTH_SHORT).show();
                }else {
                    String password = passwordView.getText().toString();
                    mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(Login.this, new OnCompleteListener<AuthResult>() {
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
                                                            progressBar.setVisibility(View.INVISIBLE);
                                                            Login.this.startActivity(mainActivity);
                                                        }
                                                    }
                                                } else {
                                                    progressBar.setVisibility(View.INVISIBLE);
                                                    Toast.makeText(Login.this, getString(R.string.failed_retrieving_users_collection), Toast.LENGTH_LONG).show();
                                                    Log.w("LOGIN: ", "Error getting documents.", task.getException());
                                                }
                                            }
                                        });
                                    } else {
                                        progressBar.setVisibility(View.INVISIBLE);
                                        // If sign in fails, display a message to the used.
                                        Toast.makeText(Login.this, getString(R.string.failed_auth_login),
                                                Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                }
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
