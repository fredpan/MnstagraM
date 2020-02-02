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

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;

import cn.fredpan.mnstagram.auth.Login;
import cn.fredpan.mnstagram.model.User;
import cn.fredpan.mnstagram.model.UserDto;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;

    private FirebaseAuth mAuth;

    private StorageReference picStorage;

    FirebaseFirestore userDb;

    User user;

    MenuItem logoutBtn;

    TextView navUsername;

    TextView navEmail;

    ImageView navAvatar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = (mAuth == null)? FirebaseAuth.getInstance() : mAuth;
        userDb = (userDb == null) ? FirebaseFirestore.getInstance() : userDb;
        picStorage = (picStorage == null) ? FirebaseStorage.getInstance().getReference() : picStorage;

        //get logged in user info
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            user = (User) getIntent().getSerializableExtra("user"); //Obtaining data
            loadAvatarAndInit();
        } else {
            retrieveCurrentLoginUserDataFromDb();
        }
    }

    private void init() {
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
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
    }

    private void retrieveCurrentLoginUserDataFromDb() {
        if (mAuth.getCurrentUser() == null) {// shouldn't be here
            Toast.makeText(MainActivity.this, getString(R.string.failed_loading_logged_in_user_info_relogin_required), Toast.LENGTH_LONG).show();
            signOut();
            throw new IllegalStateException("Accessed to the MainActivity while not login or the current user info cannot be found on mAuth.");
        } else {
            userDb.collection("users").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        FirebaseUser currUser = FirebaseAuth.getInstance().getCurrentUser();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            if (document.getId().equals(currUser.getUid())) {
                                UserDto userDto = document.toObject(UserDto.class);
                                user = userDto.generateUser(null, currUser.getUid(), currUser.getEmail());
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
        File folder = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/" + user.getUid());
        if (!folder.exists()) {
            folder.mkdirs();
        }
        final File img = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/" + user.getUid() + "/displayPic.jpg");
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
}