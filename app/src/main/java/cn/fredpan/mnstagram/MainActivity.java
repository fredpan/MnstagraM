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

package cn.fredpan.mnstagram;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import cn.fredpan.mnstagram.auth.Login;
import cn.fredpan.mnstagram.model.Picture;
import cn.fredpan.mnstagram.model.PictureDto;
import cn.fredpan.mnstagram.model.Updatable;
import cn.fredpan.mnstagram.model.User;
import cn.fredpan.mnstagram.model.UserDto;
import cn.fredpan.mnstagram.pic.ImgHelper;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private FirebaseAuth mAuth;
    private StorageReference picStorage;
    User user;
    MenuItem logoutBtn;
    TextView navUsername;
    TextView navEmail;
    ImageView navAvatar;
    FirebaseFirestore db;
    private String rootPath;
    NavigationView navigationView;
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int REQUEST_TAKE_PHOTO = 1;
    Toast postingPicHint;
    private FloatingActionButton takeNewPicBtn;
    private boolean takeNewPicBtnDisabled;
    private File photoFile;
    private EditText newPicCaption;
    private Switch autoHashtags;
    private EditText customHashtag;
    private Bitmap resultBitmap;
    private Uri photoURI;
    private String imgName;
    private FirebaseVisionImageLabeler labeler;
    private Fragment currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Since this activity and its fragment requires info from db, the init will continue after all the necessary info is ready. See onAllInfoRetrieved for continues initialization.

        // prepare all dbs
        mAuth = (mAuth == null) ? FirebaseAuth.getInstance() : mAuth;
        db = (db == null) ? FirebaseFirestore.getInstance() : db;
        picStorage = (picStorage == null) ? FirebaseStorage.getInstance().getReference() : picStorage;
        labeler = (labeler == null) ? FirebaseVision.getInstance().getCloudImageLabeler() : labeler;

        //get logged in user info
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            user = (User) getIntent().getSerializableExtra("user"); //Obtaining data
            initFolders();
            loadAvatarAndInit();
        } else {
            retrieveCurrentLoginUserDataFromDb();
        }
//        setMyFragment(new Profile());
    }


    private void takeNewPicListener() {
        takeNewPicBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (takeNewPicBtnDisabled) {
                    if (postingPicHint != null) {
                        postingPicHint.cancel();
                    }
                    Toast.makeText(MainActivity.this, "Please wait for photo posted before taking a new one.", Toast.LENGTH_SHORT).show();
                } else {
                    if (MainActivity.this.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
                    } else {
//                    Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
//                    startActivityForResult(cameraIntent, CAMERA_REQUEST);
                        dispatchTakePictureIntent();

                    }
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, getString(R.string.camera_permisson_granted), Toast.LENGTH_LONG).show();
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(MainActivity.this, getString(R.string.failed_grant_camera_permission), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == Activity.RESULT_OK) {
            ImgHelper.cropPicWithFixedSize(photoURI, MainActivity.this);
        }
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            try {
                resultBitmap = ImgHelper.getCroppedImg(resultCode, data);
                Bitmap downScaledBitmap = ImgHelper.getDownScaledImg(resultBitmap);
                ImgHelper.saveImg(rootPath, imgName, downScaledBitmap, 100);// It overrides the original one, the one camera took
                showImage(downScaledBitmap);
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, getString(R.string.failed_read_write_image), Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(MainActivity.this.getPackageManager()) != null) {
            // Create the File where the photo should go
            File folder = new File(MainActivity.this.getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/" + user.getUid());
            if (!folder.exists()) {
                folder.mkdirs();
            }

            Date date = new Date();
            imgName = String.valueOf(new Timestamp(date).getSeconds());
            photoFile = new File(MainActivity.this.getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/" + user.getUid() + "/" + imgName + ".jpg");
            photoURI = FileProvider.getUriForFile(MainActivity.this,
                    "cn.fredpan.mnstagram.fileprovider",
                    photoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
        }
    }

    private void showImage(final Bitmap bitmap) {

        final Dialog builder = new Dialog(MainActivity.this);
        builder.requestWindowFeature(Window.FEATURE_NO_TITLE);
        builder.getWindow().setBackgroundDrawable(
                new ColorDrawable(android.graphics.Color.TRANSPARENT));

        builder.setContentView(R.layout.confirm_pic_taken);

        ImageView imageView = builder.findViewById(R.id.confirm_pic_taken_pic);
        imageView.setImageBitmap(bitmap);

        Button confirmBtn = builder.findViewById(R.id.confirm_pic_taken_confirm);

        Button cancelBtn = builder.findViewById(R.id.confirm_pic_taken_cancel);

        autoHashtags = builder.findViewById(R.id.auto_hashtags);

        customHashtag = builder.findViewById(R.id.custom_hashtag);

        final TextView customHashtagCtr = builder.findViewById(R.id.custom_hashtag_ctr);

        customHashtag.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().length() == 120) {
                    customHashtagCtr.setTextColor(Color.RED);
                } else {
                    customHashtagCtr.setTextColor(Color.BLACK);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String currentText = editable.toString();
                int currentLength = currentText.length();
                customHashtagCtr.setText(currentLength + "/120");
            }
        });

        newPicCaption = builder.findViewById(R.id.new_pic_caption);

        final TextView newPicCaptionCtr = builder.findViewById(R.id.new_pic_caption_ctr);

        newPicCaption.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().length() == 120) {
                    newPicCaptionCtr.setTextColor(Color.RED);
                } else {
                    newPicCaptionCtr.setTextColor(Color.BLACK);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String currentText = editable.toString();
                int currentLength = currentText.length();
                newPicCaptionCtr.setText(currentLength + "/120");
            }
        });

        final LinearLayout hashtagsBlock = builder.findViewById(R.id.hashtags_block);

        autoHashtags.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    hashtagsBlock.setVisibility(View.GONE);
                } else {
                    hashtagsBlock.setVisibility(View.VISIBLE);
                }
            }
        });

        confirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final List<String> hashtag = new ArrayList<>();

                if (autoHashtags.isChecked()) {
                    //auto generate hashtag.
                    FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(resultBitmap);
                    labeler.processImage(image)
                            .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionImageLabel>>() {
                                @Override
                                public void onSuccess(List<FirebaseVisionImageLabel> labels) {
                                    for (FirebaseVisionImageLabel label : labels) {
                                        String text = label.getText();
                                        String entityId = label.getEntityId();
                                        float confidence = label.getConfidence();
                                        /*Ex:
                                            text: Cat
                                            entityId: /m/01yrx
                                            confidence: 0.98893553
                                         */
                                        if (confidence >= 0.7) {
                                            hashtag.add(text);
                                        }
                                    }
                                    uploadImg(hashtag);
                                    builder.dismiss();
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    // Task failed with an exception
                                    Toast.makeText(MainActivity.this, "Auto generate hashtags failed. Please try again", Toast.LENGTH_SHORT).show();
                                }
                            });
                } else {
                    String input = customHashtag.getText().toString();
                    String[] result = input.split(",");
                    for (int i = 0; i < result.length; i++) {
                        String temp = result[i].trim();
                        if (temp.length() > 0 && temp.charAt(0) == '#') {
                            temp = temp.substring(1);
                        }
                        result[i] = temp;
                    }
                    hashtag.addAll(Arrays.asList(result));
                    uploadImg(hashtag);
                    builder.dismiss();
                }

            }
        });

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                builder.dismiss();
            }
        });

        builder.show();

    }

    private void uploadImg(final List<String> hashtag) {
        //Toast
        postingPicHint = Toast.makeText(MainActivity.this, "Posting picture...", Toast.LENGTH_LONG);
        postingPicHint.show();

        final Updatable<Picture> curr = (Updatable<Picture>) currentFragment;
        final List<Picture> pic = curr.getList();
        curr.update();

        //upload to storage
        // store pic for the current registered user to storage
        String path = "pictures/" + user.getUid() + "/" + imgName + ".jpg";
        StorageReference displayPicRef = picStorage.child(path);

        Bitmap overlayBitmap = overlay(BitmapFactory.decodeFile(photoFile.getAbsolutePath()), ((BitmapDrawable) ContextCompat.getDrawable(MainActivity.this, R.drawable.uploading_hint)).getBitmap());
        final Picture temp = new Picture(new PictureDto(), overlayBitmap);//the temp img does not have uid or pid
        temp.setTimestamp(String.valueOf(new Timestamp(new Date()).getSeconds()));
        pic.add(temp);
        curr.update();
        takeNewPicBtnDisabled = true;

        //upload to FireBase storage
        //upload img to storage first then update db. This way, when db failed, the img is just redundant on the storage, data is sill consistent.
        displayPicRef.putFile(photoURI)
                .addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if (task.isSuccessful()) {
                            //upload to db
                            db.collection("photos/").add(new PictureDto(user.getUid(), user.getUid() + "/" + imgName + ".jpg", imgName, newPicCaption.getText().toString(), hashtag)).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentReference> photoDbTask) {
                                    if (photoDbTask.isSuccessful()) {
                                        //upload to db completed
                                        pic.remove(temp);
                                        curr.update();
                                        PictureDto pictureDto = new PictureDto(user.getUid(), user.getUid() + "/" + imgName + ".jpg", imgName, newPicCaption.getText().toString(), hashtag);
                                        pictureDto.setPid(photoDbTask.getResult().getId());
                                        Picture picture = new Picture(pictureDto, BitmapFactory.decodeFile(photoFile.getAbsolutePath()));
                                        pic.add(picture);
                                        curr.update();
                                        //Toast
                                        Toast.makeText(MainActivity.this, "Picture posted", Toast.LENGTH_SHORT).show();
                                    } else {
                                        pic.remove(temp);
                                        curr.update();
                                        //Toast
                                        Toast.makeText(MainActivity.this, "Failed to post picture, please try again later!", Toast.LENGTH_LONG).show();
                                    }
                                    takeNewPicBtnDisabled = false;
                                }
                            });
                        } else {
                            pic.remove(temp);
                            curr.update();
                            //Toast
                            Toast.makeText(MainActivity.this, "Failed to post picture, please try again later!", Toast.LENGTH_LONG).show();
                        }
                        takeNewPicBtnDisabled = false;
                    }
                });


    }

    private Bitmap overlay(Bitmap bmp1, Bitmap bmp2) {
        Bitmap bmOverlay = Bitmap.createBitmap(bmp1.getWidth(), bmp1.getHeight(), bmp1.getConfig());
        Canvas canvas = new Canvas(bmOverlay);
        canvas.drawBitmap(bmp1, new Matrix(), null);
        float left = 110;
        float top = 110;
        RectF dst = new RectF(left, top, left + 220, top + 220); // width=100, height=120
        canvas.drawBitmap(bmp2, null, dst, null);
        canvas.drawColor(Color.argb(100, 193, 193, 193));
        return bmOverlay;
    }


    private void onAllInfoRetrived() {

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.profile_page, R.id.logout, R.id.global_page)
                .setDrawerLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        Menu menuNav = navigationView.getMenu();
        View navHeaderView = navigationView.getHeaderView(0);

        //get components
        logoutBtn = menuNav.findItem(R.id.logout);
        navEmail = navHeaderView.findViewById(R.id.nav_email);
        navUsername = navHeaderView.findViewById(R.id.nav_username);
        navAvatar = navHeaderView.findViewById(R.id.nav_avatar);
        takeNewPicBtn = findViewById(R.id.take_new_img);
        takeNewPicBtnDisabled = false;


        displayUserInfo();
        logoutListener();
        takeNewPicListener();
    }

    private void initFolders() {
        File root = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/" + user.getUid());
        if (!root.exists()) {
            root.mkdirs();
        }
        rootPath = root.getAbsolutePath();
    }

    private void retrieveCurrentLoginUserDataFromDb() {
        if (mAuth.getCurrentUser() == null) {// shouldn't be here
            Toast.makeText(MainActivity.this, getString(R.string.failed_loading_logged_in_user_info_relogin_required), Toast.LENGTH_LONG).show();
            signOut();
            throw new IllegalStateException("Accessed to the MainActivity while not login or the current user info cannot be found on mAuth.");
        } else {
            db.collection("users").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        FirebaseUser currUser = FirebaseAuth.getInstance().getCurrentUser();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            if (document.getId().equals(currUser.getUid())) {
                                UserDto userDto = document.toObject(UserDto.class);
                                user = userDto.generateUser(null, currUser.getUid(), currUser.getEmail());
                                initFolders();
                                loadAvatarAndInit();
                                break;
                            }
                        }
                    } else {
                        Toast.makeText(MainActivity.this, getString(R.string.failed_retrieving_users_collection), Toast.LENGTH_LONG).show();
                        Log.w("LOGIN: ", "Error getting documents.", task.getException());
                    }
                }
            });
        }
    }

    private void loadAvatarAndInit() {
        final File img = new File(rootPath, "displayPic.jpg");
        if (!img.exists()) {
            //load avatar
            String path = "pictures/" + user.getUid() + "/" + "displayPic.jpg";
            StorageReference displayPicRef = picStorage.child(path);
            displayPicRef.getFile(img)
                    .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                            user.setAvatar(BitmapFactory.decodeFile(img.getAbsolutePath()));
                            onAllInfoRetrived();
                        }
                    });
        } else {
            user.setAvatar(BitmapFactory.decodeFile(img.getAbsolutePath()));
            onAllInfoRetrived();
        }
    }

    //listeners

    private void logoutListener() {
        logoutBtn.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage("Log out of " + user.getUsername() + " ?")
                        .setPositiveButton("Logout", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                signOut();
                            }
                        })
                        .setNegativeButton("Back", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        });
                // Create the AlertDialog object and return it
                AlertDialog logoutConfirm = builder.create();
                logoutConfirm.show();
                logoutConfirm.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(MainActivity.this, R.color.colorPrimary));
                logoutConfirm.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(MainActivity.this, R.color.colorPrimary));

                return true;
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    //helper methods

    private void displayUserInfo() {
        navUsername.setText(user.getUsername());
        navEmail.setText(user.getEmail());
        navAvatar.setImageBitmap(user.getAvatar());
        navAvatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ImgHelper.displayPreviewImg(MainActivity.this, user.getAvatar());
            }
        });
    }

    private void signOut() {
        FirebaseAuth.getInstance().signOut();
        Intent loginActivity = new Intent(MainActivity.this, Login.class);
        MainActivity.this.startActivity(loginActivity);
    }

    //Getter methods

    public User getUser() {
        return user;
    }

    public FirebaseFirestore getDb() {
        return db;
    }

    public StorageReference getPicStorage() {
        return picStorage;
    }

    public String getRootPath() {
        return rootPath;
    }

    public NavigationView getNavigationView() {
        return navigationView;
    }

    public void setCurrentFragment(Fragment currentFragment) {
        this.currentFragment = currentFragment;
    }

    private void setMyFragment(Fragment fr) {
        try {
            FragmentManager fm = getSupportFragmentManager();
            if (fr != null) {
                FragmentTransaction ft = fm.beginTransaction();
                ft.replace(R.id.nav_host_fragment, fr, null);
                ft.commitAllowingStateLoss();
            }
        } catch (IllegalStateException e) {
            Log.d("OPEN SPECIFIC FRAGMENT:", "ISE setting main view " + e.getMessage());
        }
    }
}