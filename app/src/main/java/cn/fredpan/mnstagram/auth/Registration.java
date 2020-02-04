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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
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

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.File;
import java.io.IOException;

import cn.fredpan.mnstagram.MainActivity;
import cn.fredpan.mnstagram.R;
import cn.fredpan.mnstagram.model.User;
import cn.fredpan.mnstagram.pic.ImgHelper;

public class Registration extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int REQUEST_TAKE_PHOTO = 1;
    EditText emailView;
    EditText passwordView;
    EditText matchPasswordView;
    EditText usernameView;
    EditText bioView;
    ImageView avatarView;
    Button registerBtn;
    Button addAvatarBtn;
    ProgressBar progressBar;
    private FirebaseFirestore db;
    private StorageReference picStorage;
    private FirebaseAuth mAuth;
    private Uri photoURI;
    private String downScaledAvatarPath;
    private Bitmap downScaledBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.registration);

        db = (db == null) ? FirebaseFirestore.getInstance() : db;
        mAuth = (mAuth == null) ? FirebaseAuth.getInstance() : mAuth;
        picStorage = (picStorage == null) ? FirebaseStorage.getInstance().getReference() : picStorage;

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

    private void emailValidation() {
        //used to update the email error hint state
        emailFormatValidation();
    }

    private void emailFormatValidation() {
        emailView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    boolean isValidEmail = (!TextUtils.isEmpty(((EditText) v).getText().toString()) && Patterns.EMAIL_ADDRESS.matcher(((EditText) v).getText().toString()).matches());
                    if (!isValidEmail) {
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
//                    Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
//                    startActivityForResult(cameraIntent, CAMERA_REQUEST);
                    dispatchTakePictureIntent();

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
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, getString(R.string.failed_grant_camera_permission), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == Activity.RESULT_OK) {
            ImgHelper.cropPicWithFixedSize(photoURI, this);
        }
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            try {
                final Bitmap bitmap = ImgHelper.getCroppedImg(resultCode, data);
                if (bitmap != null) {
                    downScaledBitmap = ImgHelper.getDownScaledImg(bitmap);
                    avatarView.setImageBitmap(downScaledBitmap);

                    avatarView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ImgHelper.displayPreviewImg(Registration.this, bitmap);
                        }
                    });


                    downScaledAvatarPath = ImgHelper.saveImg(getExternalFilesDir(Environment.DIRECTORY_PICTURES) + ImgHelper.PIC_TEMP_PATH, "displayPic", downScaledBitmap, 100).getAbsolutePath();
                    // THe original one has been override, the one camera took
                }
            } catch (Exception e) {
                Toast.makeText(Registration.this, getString(R.string.failed_read_write_image), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void registration() {
        registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                if (passwordView.getError() == null && matchPasswordView.getError() == null && emailView.getError() == null) {
                    String email = emailView.getText().toString();
                    String password = passwordView.getText().toString();
                    String passwordMatch = matchPasswordView.getText().toString();
                    String username = usernameView.getText().toString();
                    String bio = bioView.getText().toString();
                    Bitmap avatar = ((BitmapDrawable) avatarView.getDrawable()).getBitmap();
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
                    if (!isValidEmail) {
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
                    if (downScaledAvatarPath == null) {
                        hasError = true;
                    }

                    if (!hasError) {
                        register(user, password);
                    } else {
                        progressBar.setVisibility(View.INVISIBLE);
                        int errorCtr = 0;
                        errorCtr = (emailView.getError() == null) ? errorCtr : errorCtr + 1;
                        errorCtr = (passwordView.getError() == null) ? errorCtr : errorCtr + 1;
                        errorCtr = (matchPasswordView.getError() == null) ? errorCtr : errorCtr + 1;
                        errorCtr = (usernameView.getError() == null) ? errorCtr : errorCtr + 1;
                        errorCtr = (bioView.getError() == null) ? errorCtr : errorCtr + 1;
                        errorCtr = (downScaledAvatarPath == null) ? errorCtr : errorCtr + 1;
                        String msg = (errorCtr <= 1) ? getString(R.string.error_unfixed_before_register_singular) : getString(R.string.error_unfixed_before_register_plural);//not all false -> one is right -> use singular.
                        if (downScaledAvatarPath == null && errorCtr <= 1) {
                            Toast.makeText(Registration.this, getString(R.string.error_empty_avatar), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(Registration.this, msg, Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    progressBar.setVisibility(View.INVISIBLE);
                    int errorCtr = 0;
                    errorCtr = (emailView.getError() == null) ? errorCtr : errorCtr + 1;
                    errorCtr = (passwordView.getError() == null) ? errorCtr : errorCtr + 1;
                    errorCtr = (matchPasswordView.getError() == null) ? errorCtr : errorCtr + 1;
                    errorCtr = (usernameView.getError() == null) ? errorCtr : errorCtr + 1;
                    errorCtr = (bioView.getError() == null) ? errorCtr : errorCtr + 1;
                    errorCtr = (downScaledAvatarPath == null) ? errorCtr : errorCtr + 1;
                    String msg = (errorCtr <= 1) ? getString(R.string.error_unfixed_before_register_singular) : getString(R.string.error_unfixed_before_register_plural);//not all false -> one is right -> use singular.
                    if (downScaledAvatarPath == null && errorCtr <= 1) {
                        Toast.makeText(Registration.this, getString(R.string.error_empty_avatar), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(Registration.this, msg, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    private boolean passwordMatch(String password, String passwordMatch) {
        return password.equals(passwordMatch);
    }

    private void register(final User user, final String password) {

        // register user to auth db

        mAuth.createUserWithEmailAndPassword(user.getEmail(), password)
                .addOnCompleteListener(Registration.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update User table
                            final FirebaseUser currUser = mAuth.getCurrentUser();
                            assert currUser != null;
                            user.setAvatar(null);// Bitmap cannot pass through Intent.

                            //store registered user to user db

                            db.collection("users/").document(currUser.getUid()).set(user.generateUserDto())
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                //store avatar to local pic storage
                                                File img = new File(downScaledAvatarPath);
                                                try {
                                                    ImgHelper.saveImg(getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/" + currUser.getUid(), "displayPic", downScaledBitmap, 100);
                                                } catch (IOException e) {
                                                    Toast.makeText(Registration.this, getString(R.string.failed_read_write_image), Toast.LENGTH_SHORT).show();//Doesn't matter if failed.
                                                }
                                                //store avatar to pic storage
                                                Uri file = Uri.fromFile(img);
                                                String path = "pictures/" + currUser.getUid() + "/" + "displayPic.jpg";
                                                StorageReference displayPicRef = picStorage.child(path);

                                                // store avatar for the current registered user to storage

                                                displayPicRef.putFile(file)
                                                        .addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                                                                if (task.isSuccessful()) {
                                                                    progressBar.setVisibility(View.INVISIBLE);
                                                                    //recheck if is logged in
                                                                    FirebaseUser currTempUser = FirebaseAuth.getInstance().getCurrentUser();
                                                                    if (currTempUser != null && currTempUser.getEmail() != null && currTempUser.getEmail().equals(user.getEmail())) {
                                                                        Intent mainActivity = new Intent(Registration.this, MainActivity.class);
                                                                        user.setUid(currTempUser.getUid());
                                                                        mainActivity.putExtra("user", user);
                                                                        Registration.this.startActivity(mainActivity);
                                                                    } else {
                                                                        // shouldn't go to here as the createUserWithEmailAndPassword also auto sign in
                                                                        Log.d("REGISTRATION: ", "Registered without auto login. Check with FirebaseAuth or the Internet connection.");
                                                                        Intent loginActivity = new Intent(Registration.this, Login.class);
                                                                        loginActivity.putExtra("user", user);
                                                                        Registration.this.startActivity(loginActivity);
                                                                    }
                                                                } else {
//                                                                    failed to upload img?
                                                                }
                                                            }
                                                        });
                                            } else {
//                                                mAuth.getCurrentUser().delete();
//                                                failed to insert into user db?
                                            }
                                        }
                                    });

                        } else {
                            progressBar.setVisibility(View.INVISIBLE);
                            // If sign up fails, display a message to the user.
                            if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                                emailView.setError(getString(R.string.error_duplicated_email_registration));
                            } else if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                //check for both the weak password and the invalid email again. Note here won't pop two exception if both invalid email and weak password.
                                if (((FirebaseAuthInvalidCredentialsException) task.getException()).getErrorCode().equals("ERROR_WEAK_PASSWORD")) {
                                    passwordView.setError(getString(R.string.error_weak_password));
                                }
                                if (((FirebaseAuthInvalidCredentialsException) task.getException()).getErrorCode().equals("ERROR_INVALID_EMAIL")) {
                                    emailView.setError(getString(R.string.error_bad_email_addr_format));
                                }
                            } else {
                                Log.d("REGISTRATION: ", "createUserWithEmail:failure", task.getException());
                                Toast.makeText(Registration.this.getBaseContext(), getString(R.string.failed_registration), Toast.LENGTH_SHORT).show();
                            }

                        }
                    }
                });
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File folder = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES) + ImgHelper.PIC_TEMP_PATH);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            File photoFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES) + ImgHelper.PIC_TEMP_PATH + "/displayPic.jpg");
            photoURI = FileProvider.getUriForFile(this,
                    "cn.fredpan.mnstagram.fileprovider",
                    photoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
        }
    }

}
