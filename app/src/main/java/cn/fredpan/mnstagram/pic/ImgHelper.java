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
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import cn.fredpan.mnstagram.R;
import cn.fredpan.mnstagram.model.Comment;
import cn.fredpan.mnstagram.model.CommentDto;
import cn.fredpan.mnstagram.model.Picture;
import cn.fredpan.mnstagram.model.User;

public class ImgHelper {

    private static final int FIXED_SCALED_PIC_SIZE = 1024;

    private static final int FIXED_SCALED_PIC_WIDTH = FIXED_SCALED_PIC_SIZE;

    private static final int FIXED_SCALED_PIC_HEIGHT = FIXED_SCALED_PIC_SIZE;

    private static final int FIXED_SCALED_THUMBNAIL_SIZE = 100;

    private static final int FIXED_SCALED_THUMBNAIL_WIDTH = FIXED_SCALED_THUMBNAIL_SIZE;

    private static final int FIXED_SCALED_THUMBNAIL_HEIGHT = FIXED_SCALED_THUMBNAIL_SIZE;

    private static final int RESULT_OK = -1;

    private static final int RESULT_CANCELED = 0;

    public static final String PIC_TEMP_PATH = "/temp";

    public static void cropPicWithFixedSize(Uri photoURI, Activity activity) {
        CropImage.activity(photoURI)
                .setFixAspectRatio(true)
                .start(activity);
    }

    public static void cropPicWithFixedSize(Uri photoURI, Context context, Fragment fragment) {
        CropImage.activity(photoURI)
                .setFixAspectRatio(true)
                .start(context, fragment);
    }

    /**
     * @param resultCode the Result code from the onActivityResult.
     * @param data       Intent
     * @return cropped bitmap. A null will be returned if the crop activity has been cancelled
     * @throws Exception IllegalStateException indicates that the crop activity has unexpected result.
     */
    public static Bitmap getCroppedImg(int resultCode, Intent data) throws Exception {
        CropImage.ActivityResult result = CropImage.getActivityResult(data);
        if (resultCode == RESULT_OK) {
            Uri resultUri = result.getUri();
            return BitmapFactory.decodeFile(resultUri.getEncodedPath());
        } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
            throw result.getError();
        } else if (resultCode == RESULT_CANCELED) {
            return null;
        } else {
            throw new IllegalStateException("Crop activity unsuccessful without proper error result code.");
        }
    }

    public static Bitmap getDownScaledImg(Bitmap originPic) {
        return downScaleImg(originPic, FIXED_SCALED_PIC_WIDTH, FIXED_SCALED_PIC_HEIGHT);
    }

    public static Bitmap getThumbnail(Bitmap originPic) {
        return downScaleImg(originPic, FIXED_SCALED_THUMBNAIL_WIDTH, FIXED_SCALED_THUMBNAIL_HEIGHT);
    }

    public static File saveImg(String path, String imgName, Bitmap img, int quality) throws IOException {
        File folder = new File(path);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File file = new File (path, imgName + ".jpg");
        FileOutputStream out = new FileOutputStream(file);
        img.compress(Bitmap.CompressFormat.JPEG, quality, out);
        out.flush();
        out.close();
        return file;
    }

    public static void displayPreviewImg(Activity activity, Bitmap bitmap) {
        Dialog builder = new Dialog(activity);
        builder.requestWindowFeature(Window.FEATURE_NO_TITLE);
        builder.getWindow().setBackgroundDrawable(
                new ColorDrawable(android.graphics.Color.TRANSPARENT));
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                //nothing;
            }
        });

        ImageView imageView = new ImageView(activity);
        imageView.setImageBitmap(bitmap);
        builder.addContentView(imageView, new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        builder.show();
    }

    public static void displayPicDetail(final Activity activity, final Picture picture, final User user, final FirebaseFirestore db) {
        final Dialog builder = new Dialog(activity);
        builder.requestWindowFeature(Window.FEATURE_NO_TITLE);
        builder.getWindow().setBackgroundDrawable(
                new ColorDrawable(android.graphics.Color.TRANSPARENT));

        builder.setContentView(R.layout.pic_detail);

        ImageView imageView = builder.findViewById(R.id.pic_display);
        imageView.setImageBitmap(picture.getPic());

        final TextView caption = builder.findViewById(R.id.caption_display);
        caption.setText(picture.getCaption());

        final RecyclerView comments = builder.findViewById(R.id.comments_list);

        final EditText newComment = builder.findViewById(R.id.new_comment);

        final Button submitComment = builder.findViewById(R.id.submit_comment);

        final List<Comment> commentList = new ArrayList<>();

        final CommentListAdapter mAdapter = new CommentListAdapter(commentList, activity, db, user);
        comments.setAdapter(mAdapter);
        GridLayoutManager layoutManager = new GridLayoutManager(activity, 1);
        comments.setLayoutManager(layoutManager);
        mAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                Collections.sort(commentList);
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
                final Toast postingCommentToast = Toast.makeText(activity, "Posting comment", Toast.LENGTH_LONG);
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
                            Toast.makeText(activity, "Comment posted", Toast.LENGTH_LONG).show();
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

        builder.show();
    }

    private static Bitmap downScaleImg(Bitmap originPic, int width, int height){
        return Bitmap.createScaledBitmap(originPic, width, height, false);
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
