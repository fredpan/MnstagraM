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

package cn.fredpan.mnstagram.pic;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import cn.fredpan.mnstagram.MainActivity;
import cn.fredpan.mnstagram.R;
import cn.fredpan.mnstagram.model.Comment;
import cn.fredpan.mnstagram.model.CommentDto;
import cn.fredpan.mnstagram.model.Picture;
import cn.fredpan.mnstagram.model.PictureDto;
import cn.fredpan.mnstagram.model.User;
import cn.fredpan.mnstagram.model.UserDto;

public class PicDetailDisplay extends AppCompatActivity {

    private Picture picture;
    private FirebaseFirestore db;
    private StorageReference picStorage;
    private User user;
    ImageView deletePicBtn;
    private String rootPath;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pic_detail_display);
        Toolbar toolbar = findViewById(R.id.toolbar);
        deletePicBtn = findViewById(R.id.delete_post);
        deletePicBtn.setVisibility(View.INVISIBLE);

        setSupportActionBar(toolbar);


        db = (db == null) ? FirebaseFirestore.getInstance() : db;
        picStorage = (picStorage == null) ? FirebaseStorage.getInstance().getReference() : picStorage;

        //get logged in user info
        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            Toast.makeText(this, "Failed to load the image, Please try again later. :<", Toast.LENGTH_LONG);
        } else {
            String path = (String) getIntent().getSerializableExtra("path"); //Obtaining data
            PictureDto pictureDto = (PictureDto) getIntent().getSerializableExtra("pictureDto"); //Obtaining data
            picture = new Picture(pictureDto, BitmapFactory.decodeFile(path));
            user = (User) getIntent().getSerializableExtra("user");
            initFolders();
            loadAvatarAndInit();
        }
        assert picture != null;
        toolbar.setTitle(user.getUsername() + "'s post");
        enablePicDelete();
    }

    private void enablePicDelete() {
        //check if curr user is the owner
        if (picture.getUid().equals(user.getUid())) {
            deletePicBtn.setVisibility(View.VISIBLE);
            deletePicBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(PicDetailDisplay.this);
                    builder.setMessage("Delete the post?")
                            .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    deletePic();
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.dismiss();
                                }
                            });
                    // Create the AlertDialog object and return it
                    AlertDialog deletePic = builder.create();
                    deletePic.show();
                    deletePic.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(PicDetailDisplay.this, R.color.colorPrimary));
                    deletePic.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(PicDetailDisplay.this, R.color.colorPrimary));
                }
            });
        }
    }

    private void deletePic() {
        final Toast deletingPic = Toast.makeText(PicDetailDisplay.this, "Deleting post...", Toast.LENGTH_LONG);
        deletingPic.show();
        //delete from pic reference db
        db.collection("photos").document(picture.getPid()).delete().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {

                    //delete pic from storage
                    picStorage.child("pictures/" + picture.getStorageRef()).delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {

                                //delete from comment db
                                db.collection("comments").whereEqualTo("pid", picture.getPid()).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                        if (task.isSuccessful()) {
                                            if (task.getResult().size() == 0) {
                                                deletingPic.cancel();
                                                Toast.makeText(PicDetailDisplay.this, "Post deleted", Toast.LENGTH_LONG).show();
                                                Intent mainActivity = new Intent(PicDetailDisplay.this, MainActivity.class);
                                                PicDetailDisplay.this.startActivity(mainActivity);
                                            } else {
                                                for (QueryDocumentSnapshot document : task.getResult()) {
                                                    db.collection("comments").document(document.getId()).delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Void> task) {
                                                            deletingPic.cancel();
                                                            Toast.makeText(PicDetailDisplay.this, "Post deleted", Toast.LENGTH_LONG).show();
                                                            Intent mainActivity = new Intent(PicDetailDisplay.this, MainActivity.class);
                                                            PicDetailDisplay.this.startActivity(mainActivity);
                                                        }
                                                    });
                                                }
                                            }
                                        }
                                    }
                                });
                            }
                        }
                    });
                } else {
                    Toast.makeText(PicDetailDisplay.this, "Failed to delete the post, please try again later", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void displayInfo() {

        ImageView imageView = findViewById(R.id.pic_display);
        imageView.setImageBitmap(picture.getPic());

        final TextView caption = findViewById(R.id.caption_display);
        caption.setText(picture.getCaption());

        final TextView hashtags = findViewById(R.id.hashtags);
        String hashtagStr = "";

        if (picture.getHashtags().size() <= 5) {
            for (String hashtag : picture.getHashtags()) {
                hashtagStr += "#" + hashtag + " ";
            }
            hashtags.setText(hashtagStr);
        } else {
            for (String hashtag : picture.getHashtags().subList(0, 5)) {//only display first five tags
                hashtagStr += "#" + hashtag + " ";
            }
            hashtags.setText(Html.fromHtml(hashtagStr + "<font color=#3F51B5><i> more...</i></font>"));
            final boolean[] isAllShown = {false};
            hashtags.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (isAllShown[0]) {//display
                        isAllShown[0] = false;
                        StringBuilder completeHashTagStr = new StringBuilder();
                        for (String hashtag : picture.getHashtags()) {
                            completeHashTagStr.append("#").append(hashtag).append(" ");
                        }
                        hashtags.setText(completeHashTagStr.toString());
                    } else {//not display
                        isAllShown[0] = true;
                        String abbrHashtagStr = "";
                        for (String hashtag : picture.getHashtags().subList(0, 5)) {//only display first five tags
                            abbrHashtagStr += "#" + hashtag + " ";
                        }
                        hashtags.setText(Html.fromHtml(abbrHashtagStr + "<font color=#3F51B5><i> more...</i></font>"));
                    }
                }
            });
        }

        final RecyclerView comments = findViewById(R.id.comments_list);

        final EditText newComment = findViewById(R.id.new_comment);

        final Button submitComment = findViewById(R.id.submit_comment);

        final TextView noCommentHint = findViewById(R.id.no_comment_hint);

        final List<Comment> commentList = new ArrayList<>();

        final CommentListAdapter mAdapter = new CommentListAdapter(commentList, this, db, user);
        comments.setAdapter(mAdapter);
        GridLayoutManager layoutManager = new GridLayoutManager(this, 1);
        comments.setLayoutManager(layoutManager);
        mAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                Collections.sort(commentList);
                Collections.reverse(commentList);
                if (commentList.size() == 0) {
                    noCommentHint.setVisibility(View.VISIBLE);
                    comments.setVisibility(View.GONE);
                } else {
                    noCommentHint.setVisibility(View.GONE);
                    comments.setVisibility(View.VISIBLE);
                }
            }
        });

        //get all comments for the current pic
        db.collection("comments").whereEqualTo("pid", picture.getPid())
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull final Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (final QueryDocumentSnapshot document : task.getResult()) {
                        final CommentDto commentDto = document.toObject(CommentDto.class);
                        final String uid = commentDto.getUid();
                        db.collection("users").document(uid).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if (task.isSuccessful()) {
                                    DocumentSnapshot documentSnapshot = task.getResult();
                                    UserDto userDto = documentSnapshot.toObject(UserDto.class);
                                    Bitmap tempBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.avatar);
                                    final User currOwner = userDto.generateUser(tempBitmap, uid, "SECURED");

                                    //load avatar
                                    File root = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/" + currOwner.getUid());
                                    if (!root.exists()) {
                                        root.mkdirs();
                                    }
                                    String rootPath = root.getAbsolutePath();
                                    final File img = new File(rootPath, "displayPic.jpg");
                                    if (!img.exists()) {
                                        //load avatar
                                        String path = "pictures/" + currOwner.getUid() + "/" + "displayPic.jpg";
                                        StorageReference displayPicRef = picStorage.child(path);
                                        displayPicRef.getFile(img)
                                                .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                                                    @Override
                                                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                                        currOwner.setAvatar(BitmapFactory.decodeFile(img.getAbsolutePath()));
                                                        mAdapter.notifyDataSetChanged();
                                                    }
                                                });
                                    } else {
                                        currOwner.setAvatar(BitmapFactory.decodeFile(img.getAbsolutePath()));
                                        mAdapter.notifyDataSetChanged();
                                    }

                                    commentList.add(new Comment(currOwner, picture, commentDto.getComment(), new Date(commentDto.getTimeStamp())));
                                    mAdapter.notifyDataSetChanged();
                                } else {
                                    Toast.makeText(PicDetailDisplay.this, getString(R.string.failed_retrieving_users_collection), Toast.LENGTH_LONG).show();
                                    Log.w("PicDetailComment: ", "Error getting user for comment.", task.getException());
                                }
                            }
                        });
                    }
                }
            }
        });


        mAdapter.notifyDataSetChanged();

        //submit comment
        submitComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Toast postingCommentToast = Toast.makeText(PicDetailDisplay.this, "Posting comment", Toast.LENGTH_LONG);
                postingCommentToast.show();
                Date time = new Date();
                if (newComment.getText().toString().equals("")) {
                    newComment.setError("Please enter your comment!");
                } else {
                    final Comment comment = new Comment(user, picture, newComment.getText().toString(), time);
                    newComment.setText("");
                    newComment.setHint(R.string.hint_enter_your_comment);
                    db.collection("comments/").add(new CommentDto(comment, picture.getPid())).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentReference> task) {
                            if (task.isSuccessful()) {
                                // update the UI
                                // clean the UI
                                postingCommentToast.cancel();
                                Toast.makeText(PicDetailDisplay.this, "Comment posted", Toast.LENGTH_LONG).show();
                                // clean the edit text
                                // update comments list
                                commentList.add(comment);
                                mAdapter.notifyDataSetChanged();

                                //hide the keyboard
                                InputMethodManager imm = (InputMethodManager) PicDetailDisplay.this.getSystemService(Activity.INPUT_METHOD_SERVICE);
                                View view = PicDetailDisplay.this.getCurrentFocus();
                                if (view == null) {
                                    view = new View(PicDetailDisplay.this);
                                }
                                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                            }
                        }
                    });
                }

            }
        });
    }

    private void initFolders() {
        File root = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/" + user.getUid());
        if (!root.exists()) {
            root.mkdirs();
        }
        rootPath = root.getAbsolutePath();
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
                            displayInfo();
                        }
                    });
        } else {
            user.setAvatar(BitmapFactory.decodeFile(img.getAbsolutePath()));
            displayInfo();
        }
    }

    private static class CommentListAdapter extends RecyclerView.Adapter<CommentListAdapter.MyViewHolder> {
        FirebaseFirestore userDb;
        User user;
        private List<Comment> mComments;
        private Activity activity;

        public CommentListAdapter(List<Comment> mComments, Activity activity, FirebaseFirestore userDb, User user) {
            this.mComments = mComments;
            this.activity = activity;
            this.userDb = userDb;
            this.user = user;
        }

        // Create new views (invoked by the layout manager)
        @Override
        public CommentListAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent,
                                                                  int viewType) {
            // create a new view
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.comment_list_item, parent, false);
            CommentListAdapter.MyViewHolder vh = new CommentListAdapter.MyViewHolder(v);
            return vh;
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(final CommentListAdapter.MyViewHolder holder, final int position) {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            holder.username.setText(mComments.get(position).getUser().getUsername());
            holder.comment.setText(mComments.get(position).getComment());
            holder.avatar.setImageBitmap(mComments.get(position).getUser().getAvatar());
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return mComments.size();
        }

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public static class MyViewHolder extends RecyclerView.ViewHolder {
            // each data item is just a string in this case
            public TextView username;
            public TextView comment;
            public ImageView avatar;

            public MyViewHolder(View v) {
                super(v);
                username = itemView.findViewById(R.id.comment_username);
                comment = itemView.findViewById(R.id.comment_comment);
                avatar = itemView.findViewById(R.id.comment_avatar);
            }
        }
    }


}
