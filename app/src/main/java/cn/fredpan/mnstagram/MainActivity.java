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

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.File;
import java.util.Date;

import cn.fredpan.mnstagram.auth.Login;
import cn.fredpan.mnstagram.model.PictureDto;
import cn.fredpan.mnstagram.model.User;
import cn.fredpan.mnstagram.model.UserDto;
import cn.fredpan.mnstagram.pic.ImgHelper;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;

    private FirebaseAuth mAuth;

    private StorageReference picStorage;

    private static final int REQUEST_TAKE_PHOTO = 1;

    User user;

    MenuItem logoutBtn;

    TextView navUsername;

    TextView navEmail;

    ImageView navAvatar;
    private static final int CAMERA_PERMISSION_CODE = 100;
    FirebaseFirestore db;
    FloatingActionButton takeNewPicBtn;
    private Uri photoURI;
    private String imgName;

    private String rootPath;
//    private String picsPath;
//    private String thumbnailsPath;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = (mAuth == null)? FirebaseAuth.getInstance() : mAuth;
        db = (db == null) ? FirebaseFirestore.getInstance() : db;
        picStorage = (picStorage == null) ? FirebaseStorage.getInstance().getReference() : picStorage;

        //init all folders


        //get logged in user info
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            user = (User) getIntent().getSerializableExtra("user"); //Obtaining data
            initFolders();
            loadAvatarAndInit();
        } else {
            retrieveCurrentLoginUserDataFromDb();
        }
    }

    private void initFolders() {
        File root = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/" + user.getUid());
        if (!root.exists()) {
            root.mkdirs();
        }
        rootPath = root.getAbsolutePath();

//        File pics = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/" + user.getUid() + "/pics");
//        if (!pics.exists()) {
//            pics.mkdirs();
//        }
//        picsPath = pics.getAbsolutePath();
//
//        File thumbnails = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/" + user.getUid() + "/thumbnails");
//        if (!thumbnails.exists()) {
//            thumbnails.mkdirs();
//        }
//        thumbnailsPath = thumbnails.getAbsolutePath();
    }

    private void init() {
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        takeNewPicBtn = findViewById(R.id.take_new_img);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.profile_page, R.id.logout)
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

        displayUserInfo();
        logoutListener();
        takeNewPicListener();
    }

    private void takeNewPicListener() {
        takeNewPicBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchTakePictureIntent();
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
                Bitmap bitmap = ImgHelper.getCroppedImg(resultCode, data);
                Bitmap downScaledBitmap = ImgHelper.getDownScaledImg(bitmap);
                ImgHelper.saveImg(rootPath, imgName, downScaledBitmap, 100);// It overrides the original one, the one camera took
                showImage(downScaledBitmap);
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, getString(R.string.failed_read_write_image), Toast.LENGTH_SHORT).show();
            }
        }
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

    private void displayUserInfo() {
        navUsername.setText(user.getUsername());
        navEmail.setText(user.getEmail());
        navAvatar.setImageBitmap(user.getAvatar());
    }

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

    private void signOut(){
        FirebaseAuth.getInstance().signOut();
        Intent loginActivity = new Intent(MainActivity.this, Login.class);
        MainActivity.this.startActivity(loginActivity);
    }

    public User getUser() {
        return user;
    }

    private void loadAvatarAndInit(){
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
                            init();
                        }
                    });
        } else {
            user.setAvatar(BitmapFactory.decodeFile(img.getAbsolutePath()));
            init();
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File folder = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/" + user.getUid());
            if (!folder.exists()) {
                folder.mkdirs();
            }
            Date date = new Date();
            imgName = String.valueOf(new Timestamp(date).getSeconds());
            System.out.println(imgName);
            File photoFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/" + user.getUid() + "/" + imgName + ".jpg");
            photoURI = FileProvider.getUriForFile(this,
                    "cn.fredpan.mnstagram.fileprovider",
                    photoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
        }
    }

    private void showImage(final Bitmap bitmap) {

        final Dialog builder = new Dialog(this);
        builder.requestWindowFeature(Window.FEATURE_NO_TITLE);
        builder.getWindow().setBackgroundDrawable(
                new ColorDrawable(android.graphics.Color.TRANSPARENT));

        builder.setContentView(R.layout.confirm_pic_taken);

        ImageView imageView = builder.findViewById(R.id.confirm_pic_taken_pic);
        imageView.setImageBitmap(bitmap);

        Button confirmBtn = builder.findViewById(R.id.confirm_pic_taken_confirm);

        Button cancelBtn = builder.findViewById(R.id.confirm_pic_taken_cancel);

        confirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadImg(bitmap);
                builder.dismiss();
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

    private void uploadImg(Bitmap bitmap) {
        //upload to db
        db.collection("photos/").add(new PictureDto(user.getUid(), user.getUid() + "/" + imgName + ".jpg", imgName));

        //upload to storage
        // store pic for the current registered user to storage
        String path = "pictures/" + user.getUid() + "/" + imgName + ".jpg";
        StorageReference displayPicRef = picStorage.child(path);
        displayPicRef.putFile(photoURI);
//                .addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
//                    @Override
//                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
//                        if (task.isSuccessful()) {
//
//
//                        } else {
////                                                                    failed to upload img?
//                        }
//                    }
//                });
    }
}