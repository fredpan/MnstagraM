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

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import cn.fredpan.mnstagram.MainActivity;
import cn.fredpan.mnstagram.R;
import cn.fredpan.mnstagram.model.User;

public class Registration extends AppCompatActivity {

    EditText emailView;
    EditText passwordView;
    EditText matchPasswordView;
    EditText usernameView;
    EditText bioView;
    ImageView avatarView;
    Button registerBtn;
    Button addAvatarBtn;
    ProgressBar progressBar;
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int CAMERA_REQUEST = 1888;
    FirebaseFirestore userDb;
    private static FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.registration);

        userDb = (userDb == null) ? FirebaseFirestore.getInstance() : userDb;
        mAuth = (mAuth == null)? FirebaseAuth.getInstance() : mAuth;

        //basic components
        emailView = findViewById(R.id.email);
        passwordView = findViewById(R.id.password);
        matchPasswordView = findViewById(R.id.matchPassword);
        usernameView = findViewById(R.id.username);
        bioView = findViewById(R.id.bio);
        avatarView = findViewById(R.id.avatar);
        registerBtn = findViewById(R.id.register_btn);
        addAvatarBtn = findViewById(R.id.add_avatar);
        progressBar = findViewById(R.id.progress_bar);

        registration();
        addAvatarListener();
        emailValidation();
        passwordValidation();
    }

    private void passwordValidation() {
        passwordMatchCheck();
        passwordWeaknessCheck();
    }

    private void passwordWeaknessCheck() {
        passwordView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    boolean isValidPassword = ((EditText) v).getText().toString().length() >= 6;
                    if (!isValidPassword) {
                        passwordView.setError(getString(R.string.error_weak_password));
                    }
                }
            }
        });
    }

    private void emailValidation(){
        //used to update the email error hint state
        emailFormatValidation();
    }

    private void emailFormatValidation() {
        emailView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus){
                    boolean isValidEmail = (!TextUtils.isEmpty(((EditText)v).getText().toString()) && Patterns.EMAIL_ADDRESS.matcher(((EditText)v).getText().toString()).matches());
                    if (!isValidEmail){
                        emailView.setError(getString(R.string.error_bad_email_addr_format));
                    }
                }
            }
        });
    }

    private void passwordMatchCheck() {
        matchPasswordView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!passwordMatch(passwordView.getText().toString(), matchPasswordView.getText().toString())) {
                    matchPasswordView.setError(getString(R.string.error_password_not_match));
                }
            }
        });
    }

    private void addAvatarListener() {
        addAvatarBtn.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {
                if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
                } else {
                    Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, CAMERA_REQUEST);
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, getString(R.string.camera_permisson_granted), Toast.LENGTH_LONG).show();
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            } else {
                Toast.makeText(this, getString(R.string.failed_grant_camera_permission), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            avatarView.setImageBitmap(photo);
        }
    }

    private void registration() {
        registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                if (passwordView.getError()==null && matchPasswordView.getError()==null && emailView.getError()==null) {
                    String email = emailView.getText().toString();
                    String password = passwordView.getText().toString();
                    String passwordMatch = matchPasswordView.getText().toString();
                    String username = usernameView.getText().toString();
                    String bio = bioView.getText().toString();
                    Bitmap avatar = ((BitmapDrawable)avatarView.getDrawable()).getBitmap();
                    User user = new User(avatar, username, bio, email);

                    //Final checks
                    boolean hasError = false;
                    if (!passwordMatch(passwordView.getText().toString(), matchPasswordView.getText().toString())) {
                        matchPasswordView.setError(getString(R.string.error_password_not_match));
                        hasError = true;
                    }
                    boolean isValidPassword = passwordView.getText().toString().length() >= 6;
                    if (!isValidPassword) {
                        passwordView.setError(getString(R.string.error_weak_password));
                        hasError = true;
                    }
                    boolean isValidEmail = (!TextUtils.isEmpty(emailView.getText().toString()) && Patterns.EMAIL_ADDRESS.matcher(emailView.getText().toString()).matches());
                    if (!isValidEmail){
                        emailView.setError(getString(R.string.error_bad_email_addr_format));
                        hasError = true;
                    }
                    if (usernameView.getText().toString().length() <= 0) {
                        usernameView.setError(getString(R.string.error_empty_username));
                        hasError = true;
                    }
                    if (bioView.getText().toString().length() <= 0) {
                        bioView.setError(getString(R.string.error_empty_bio));
                        hasError = true;
                    }

                    if(!hasError){
                        register(user, password);
                    }
                }
                progressBar.setVisibility(View.INVISIBLE);
                int errorCtr = 0;
                errorCtr = (emailView.getError() == null)? errorCtr : errorCtr+1;
                errorCtr = (passwordView.getError() == null)? errorCtr : errorCtr+1;
                errorCtr = (matchPasswordView.getError() == null)? errorCtr : errorCtr+1;
                errorCtr = (usernameView.getError() == null)? errorCtr : errorCtr+1;
                errorCtr = (bioView.getError() == null)? errorCtr : errorCtr+1;
                String msg = !(errorCtr <= 1) ? getString(R.string.error_unfixed_before_register_singular) : getString(R.string.error_unfixed_before_register_plural);//not all false -> one is right -> use singular.
                Toast.makeText(Registration.this, msg, Toast.LENGTH_SHORT).show();

            }
        });
    }

    private boolean passwordMatch(String password, String passwordMatch) {
        return password.equals(passwordMatch);
    }

    private void register(final User user, final String password){
        mAuth.createUserWithEmailAndPassword(user.getEmail(), password)
                .addOnCompleteListener(Registration.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressBar.setVisibility(View.INVISIBLE);
                        if (task.isSuccessful()) {
                            // Sign in success, update User table
                            FirebaseUser currUser = mAuth.getCurrentUser();
                            assert currUser != null;
                            //todo avatar
                            user.setAvatar(null);
                            userDb.collection("users/").document(currUser.getUid()).set(user.generateUserDto());
                            //recheck if is logged in
                            FirebaseUser currTempUser = FirebaseAuth.getInstance().getCurrentUser();
                            if (currTempUser != null && currTempUser.getEmail() != null && currTempUser.getEmail().equals(user.getEmail())) {
                                Intent mainActivity = new Intent(Registration.this, MainActivity.class);
                                mainActivity.putExtra("user", user);
                                Registration.this.startActivity(mainActivity);
                            }else {
                                // shouldn't go to here as the createUserWithEmailAndPassword also auto sign in
                                Log.d("REGISTRATION: ", "Registered without auto login. Check with FirebaseAuth or the Internet connection.");
                                Intent loginActivity = new Intent(Registration.this, Login.class);
                                loginActivity.putExtra("user", user);
                                Registration.this.startActivity(loginActivity);
                            }
                        } else {
                            // If sign up fails, display a message to the user.
                            if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                                emailView.setError(getString(R.string.error_duplicated_email_registration));
                            }else if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                //check for both the weak password and the invalid email again. Note here won't pop two exception if both invalid email and weak password.
                                if (((FirebaseAuthInvalidCredentialsException) task.getException()).getErrorCode().equals("ERROR_WEAK_PASSWORD")){
                                    passwordView.setError(getString(R.string.error_weak_password));
                                }
                                if (((FirebaseAuthInvalidCredentialsException) task.getException()).getErrorCode().equals("ERROR_INVALID_EMAIL")){
                                    emailView.setError(getString(R.string.error_bad_email_addr_format));
                                }
                            }else {
                                Log.d("REGISTRATION: ", "createUserWithEmail:failure", task.getException());
                                Toast.makeText(Registration.this.getBaseContext(), getString(R.string.failed_registration), Toast.LENGTH_SHORT).show();
                            }

                        }
                    }
                });
    }

}
