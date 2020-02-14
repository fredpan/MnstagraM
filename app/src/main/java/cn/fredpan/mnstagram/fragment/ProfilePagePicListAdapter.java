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

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import cn.fredpan.mnstagram.R;
import cn.fredpan.mnstagram.model.Picture;
import cn.fredpan.mnstagram.pic.ImgHelper;

class ProfilePagePicListAdapter extends RecyclerView.Adapter<ProfilePagePicListAdapter.MyViewHolder> {
    private List<Picture> mPics;
    private Activity activity;

    // Provide a suitable constructor (depends on the kind of dataset)
    public ProfilePagePicListAdapter(Activity activity, List<Picture> mPics) {
        this.activity = activity;
        this.mPics = mPics;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ProfilePagePicListAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent,
                                                                     int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.pic_list_item, parent, false);
        MyViewHolder vh = new MyViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        holder.imgView.setImageBitmap(mPics.get(position).getPic());
        holder.imgView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ImgHelper.displayPreviewImg(activity, mPics.get(position).getPic());
            }
        });
//        holder.imgView.setOnLongClickListener(new View.OnLongClickListener() {
//            @Override
//            public boolean onLongClick(View view) {
//                Intent picDetailActivity = new Intent(activity.getApplicationContext(), PicDetailDisplay.class);
//                String path = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/" + mPics.get(position).getStorageRef();
//                picDetailActivity.putExtra("path", path);
//                Picture picture = mPics.get(position);
//                PictureDto pictureDto = new PictureDto(picture.getUid(), picture.getStorageRef(), picture.getTimestamp(), caption);
//                picDetailActivity.putExtra("pictureDto", pictureDto);
//                activity.startActivity(picDetailActivity);
//                return true;
//            }
//        });
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mPics.size();
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class MyViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public ImageView imgView;

        public MyViewHolder(View v) {
            super(v);
            imgView = itemView.findViewById(R.id.pic_item);
        }
    }


}