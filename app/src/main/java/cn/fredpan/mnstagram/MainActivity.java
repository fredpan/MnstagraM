package cn.fredpan.mnstagram;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import cn.fredpan.mnstagram.auth.Login;
import cn.fredpan.mnstagram.model.User;
import cn.fredpan.mnstagram.model.UserDto;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;

    private static FirebaseAuth mAuth;

    FirebaseFirestore userDb;

    User user;

    MenuItem logoutBtn;

    TextView navUsername;

    TextView navEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = (mAuth == null)? FirebaseAuth.getInstance() : mAuth;
        userDb = (userDb == null) ? FirebaseFirestore.getInstance() : userDb;

        //get logged in user info
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            user = (User) getIntent().getSerializableExtra("user"); //Obtaining data
            init();
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

        displayUserInfo();
        logoutListener();
    }

    private void retrieveCurrentLoginUserDataFromDb() {
                // shouldn't be here
                if (mAuth.getCurrentUser() == null) {
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
                                    }
                                }
                            } else {
                                Toast.makeText(MainActivity.this, getString(R.string.failed_retrieving_users_collection), Toast.LENGTH_LONG).show();
                                Log.w("LOGIN: ", "Error getting documents.", task.getException());
                            }
                            init();
                        }
                    });
                }
            }



    private void displayUserInfo() {
        navUsername.setText(user.getUsername());
        navEmail.setText(user.getEmail());
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
}