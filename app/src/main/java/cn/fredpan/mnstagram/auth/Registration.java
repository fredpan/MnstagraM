package cn.fredpan.mnstagram.auth;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import cn.fredpan.mnstagram.R;
import cn.fredpan.mnstagram.firebase.auth.Auth;
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
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int CAMERA_REQUEST = 1888;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.registration);

        //basic components
        emailView = (EditText) findViewById(R.id.email);
        passwordView = (EditText) findViewById(R.id.password);
        matchPasswordView = (EditText) findViewById(R.id.matchPassword);
        usernameView = (EditText) findViewById(R.id.username);
        bioView = (EditText) findViewById(R.id.bio);
        avatarView = (ImageView) findViewById(R.id.avatar);
        registerBtn = (Button) findViewById(R.id.register_btn);
        addAvatarBtn = (Button) findViewById(R.id.add_avatar);

        registration();
        addAvatarListener();
    }

    private void addAvatarListener() {
        addAvatarBtn.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {
                if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
                }
                else
                {
                    Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, CAMERA_REQUEST);
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE)
        {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            }
            else
            {
                System.out.println(grantResults[0]);
                System.out.println(PackageManager.PERMISSION_GRANTED);
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
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
                String email = emailView.getText().toString();
                String password = passwordView.getText().toString();
                String passwordMatch = matchPasswordView.getText().toString();
                String username = usernameView.getText().toString();
                String bio = bioView.getText().toString();
                Bitmap avatar = ((BitmapDrawable)avatarView.getDrawable()).getBitmap();

                User user = new User(avatar, username, bio, email);

                //check password match
                if (passwordMatch(password, passwordMatch)){
                    Auth.registration(user, password, Registration.this);
                }else {
                    Toast.makeText(Registration.this, "Password you entered does not match", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private boolean passwordMatch(String password, String passwordMatch) {
        return password.equals(passwordMatch);
    }

}
