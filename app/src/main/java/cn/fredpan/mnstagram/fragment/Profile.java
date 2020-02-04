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

package cn.fredpan.mnstagram.fragment;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import cn.fredpan.mnstagram.MainActivity;
import cn.fredpan.mnstagram.R;
import cn.fredpan.mnstagram.model.Picture;
import cn.fredpan.mnstagram.model.PictureDto;
import cn.fredpan.mnstagram.model.User;
import cn.fredpan.mnstagram.pic.ImgHelper;

public class Profile extends Fragment {

    FirebaseFirestore userDb;
    TextView usernameView;
    TextView bioView;
    ImageView avatarView;
    User user;
    RecyclerView recyclerView;
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int REQUEST_TAKE_PHOTO = 1;
    FirebaseFirestore db;
    private StorageReference picStorage;
    private ProfilePagePicListAdapter mAdapter;
    FloatingActionButton takeNewPicBtn;
    private Uri photoURI;
    private String rootPath;
    private String imgName;
    private List<Picture> pic;
    private File photoFile;
    Toast postingPicHint;
    private boolean takeNewPicBtnDisabled;

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.profile, container, false);

        userDb = (userDb == null) ? FirebaseFirestore.getInstance() : userDb;

        //basic components
        usernameView = rootView.findViewById(R.id.username);
        bioView = rootView.findViewById(R.id.bio);
        avatarView = rootView.findViewById(R.id.profile_avatar);
        recyclerView = rootView.findViewById(R.id.my_recycler_view);
        takeNewPicBtn = rootView.findViewById(R.id.take_new_img);
        takeNewPicBtnDisabled = false;

        user = ((MainActivity) getActivity()).getUser();
        db = ((MainActivity) getActivity()).getDb();
        picStorage = ((MainActivity) getActivity()).getPicStorage();
        rootPath = (((MainActivity) getActivity()).getRootPath());

        takeNewPicListener();

        initPicLists();

        displayUserInfo();

        return rootView;
    }

    private void takeNewPicListener() {
        takeNewPicBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (takeNewPicBtnDisabled) {
                    if (postingPicHint != null) {
                        postingPicHint.cancel();
                    }
                    Toast.makeText(getActivity(), "Please wait for photo posted before taking a new one.", Toast.LENGTH_SHORT).show();
                } else {
                    if (getActivity().checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
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
                Toast.makeText(getActivity(), getString(R.string.camera_permisson_granted), Toast.LENGTH_LONG).show();
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(getActivity(), getString(R.string.failed_grant_camera_permission), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == Activity.RESULT_OK) {
            ImgHelper.cropPicWithFixedSize(photoURI, getContext(), this);
        }
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            try {
                Bitmap bitmap = ImgHelper.getCroppedImg(resultCode, data);
                Bitmap downScaledBitmap = ImgHelper.getDownScaledImg(bitmap);
                ImgHelper.saveImg(rootPath, imgName, downScaledBitmap, 100);// It overrides the original one, the one camera took
                showImage(downScaledBitmap);
            } catch (Exception e) {
                Toast.makeText(getActivity(), getString(R.string.failed_read_write_image), Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            // Create the File where the photo should go
            File folder = new File(getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/" + user.getUid());
            if (!folder.exists()) {
                folder.mkdirs();
            }

            Date date = new Date();
            imgName = String.valueOf(new Timestamp(date).getSeconds());
            photoFile = new File(getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/" + user.getUid() + "/" + imgName + ".jpg");
            photoURI = FileProvider.getUriForFile(getActivity(),
                    "cn.fredpan.mnstagram.fileprovider",
                    photoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
        }
    }

    private void showImage(final Bitmap bitmap) {

        final Dialog builder = new Dialog(getActivity());
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
                uploadImg();
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

    private void uploadImg() {
        //Toast
        postingPicHint = Toast.makeText(getContext(), "Posting picture...", Toast.LENGTH_LONG);
        postingPicHint.show();

        //upload to db
        db.collection("photos/").add(new PictureDto(user.getUid(), user.getUid() + "/" + imgName + ".jpg", imgName));

        //upload to storage
        // store pic for the current registered user to storage
        String path = "pictures/" + user.getUid() + "/" + imgName + ".jpg";
        StorageReference displayPicRef = picStorage.child(path);


        Bitmap overlayBitmap = overlay(BitmapFactory.decodeFile(photoFile.getAbsolutePath()), ((BitmapDrawable) ContextCompat.getDrawable(getContext(), R.drawable.uploading_hint)).getBitmap());

        final Picture temp = new Picture(new PictureDto(), overlayBitmap);
        temp.setTimestamp(String.valueOf(new Timestamp(new Date()).getSeconds()));
        pic.add(temp);
        mAdapter.notifyDataSetChanged();
        takeNewPicBtnDisabled = true;
        displayPicRef.putFile(photoURI)
                .addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if (task.isSuccessful()) {
                            pic.remove(temp);
                            mAdapter.notifyDataSetChanged();
                            Picture picture = new Picture(new PictureDto(user.getUid(), user.getUid() + "/" + imgName + ".jpg", imgName), BitmapFactory.decodeFile(photoFile.getAbsolutePath()));
                            pic.add(picture);
                            mAdapter.notifyDataSetChanged();
                            //Toast
                            Toast.makeText(getContext(), "Picture posted", Toast.LENGTH_SHORT).show();
                        } else {
                            pic.remove(temp);
                            mAdapter.notifyDataSetChanged();
                            //Toast
                            Toast.makeText(getContext(), "Failed to post picture, please try again later!", Toast.LENGTH_LONG).show();
                        }
                        takeNewPicBtnDisabled = false;
                    }
                });
    }

    private void initPicLists() {

        pic = new ArrayList<>();
        obtainPics();
        //Changes in content do not change the layout size of the RecyclerView
//        recyclerView.setHasFixedSize(true);

        // specify an adapter (see also next example)
        mAdapter = new ProfilePagePicListAdapter(getActivity(), pic);
        recyclerView.setAdapter(mAdapter);
        GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), 3);
        recyclerView.setLayoutManager(layoutManager);
        mAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                Collections.sort(pic);
            }
        });

        mAdapter.notifyDataSetChanged();
    }

    private void obtainPics() {
        final List<PictureDto> refs = new ArrayList<>();
        db.collection("photos").whereEqualTo("uid", user.getUid())
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    // get all ref
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        PictureDto pictureDto = document.toObject(PictureDto.class);
                        refs.add(pictureDto);
                    }
                    for (final PictureDto ref : refs) {
                        File file = new File(getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/" + ref.getStorageRef());
                        if (file.exists()) {
                            pic.add(new Picture(ref, BitmapFactory.decodeFile(file.getAbsolutePath())));
                            mAdapter.notifyDataSetChanged();
                        } else {
                            final File localFile = new File(getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/" + ref.getStorageRef());
                            StorageReference picRef = picStorage.child("pictures/" + ref.getStorageRef());
                            picRef.getFile(localFile)
                                    .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                                        @Override
                                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                            pic.add(new Picture(ref, BitmapFactory.decodeFile(localFile.getAbsolutePath())));
                                            mAdapter.notifyDataSetChanged();
                                        }
                                    });
                        }
                    }

                } else {
                    Toast.makeText(getActivity(), getString(R.string.failed_retrieving_users_collection), Toast.LENGTH_LONG).show();
                    Log.w("LOGIN: ", "Error getting pictures, Please check your internet connection.", task.getException());
                }
            }
        });
    }

    private void displayUserInfo() {
        usernameView.setText(user.getUsername());
        bioView.setText(user.getBio());
        avatarView.setImageBitmap(user.getAvatar());
        avatarView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ImgHelper.displayPreviewImg(getActivity(), user.getAvatar());
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

}
