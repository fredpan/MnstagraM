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
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import cn.fredpan.mnstagram.R;
import cn.fredpan.mnstagram.model.Comment;
import cn.fredpan.mnstagram.model.CommentDto;
import cn.fredpan.mnstagram.model.Picture;
import cn.fredpan.mnstagram.model.PictureDto;
import cn.fredpan.mnstagram.model.User;

public class PicDetailDisplay extends AppCompatActivity {

    private Picture picture;
    private FirebaseFirestore db;
    private StorageReference picStorage;
    private User user;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pic_detail_display);

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
            loadAvatarAndInit(getExternalFilesDir(Environment.DIRECTORY_PICTURES).getPath());
        }
        assert picture != null;
        displayInfo();
    }

    private void displayInfo() {

        ImageView imageView = findViewById(R.id.pic_display);
        imageView.setImageBitmap(picture.getPic());

        final TextView caption = findViewById(R.id.caption_display);
        caption.setText(picture.getCaption());

        final RecyclerView comments = findViewById(R.id.comments_list);

        final EditText newComment = findViewById(R.id.new_comment);

        final Button submitComment = findViewById(R.id.submit_comment);

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
            }
        });
        db.collection("comments").whereEqualTo("pid", picture.getPid())
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        CommentDto commentDto = document.toObject(CommentDto.class);
                        commentList.add(new Comment(user, picture, commentDto.getComment(), new Date(commentDto.getTimeStamp())));
                        mAdapter.notifyDataSetChanged();
                    }
                }
            }
        });
        //get all comments for the current pic

        mAdapter.notifyDataSetChanged();

        //submit comment
        submitComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Toast postingCommentToast = Toast.makeText(PicDetailDisplay.this, "Posting comment", Toast.LENGTH_LONG);
                postingCommentToast.show();
                Date time = new Date();
                final Comment comment = new Comment(user, picture, newComment.getText().toString(), time);
                db.collection("comments/").add(new CommentDto(comment, picture.getPid())).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentReference> task) {
                        if (task.isSuccessful()) {
                            // update the UI
                            // clean the UI
                            postingCommentToast.cancel();
                            Toast.makeText(PicDetailDisplay.this, "Comment posted", Toast.LENGTH_LONG).show();
                            // clean the edit text
                            newComment.setText("");
                            newComment.setHint(R.string.hint_enter_your_comment);
                            // update comments list
                            commentList.add(comment);
                            mAdapter.notifyDataSetChanged();
                        }
                    }
                });
            }
        });
    }

    private void loadAvatarAndInit(String rootPath) {
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
//                            onAllInfoRetrived();
                        }
                    });
        } else {
            user.setAvatar(BitmapFactory.decodeFile(img.getAbsolutePath()));
//            onAllInfoRetrived();
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

            public MyViewHolder(View v) {
                super(v);
                username = itemView.findViewById(R.id.comment_username);
                comment = itemView.findViewById(R.id.comment_comment);
            }
        }
    }

}
