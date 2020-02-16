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

import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import cn.fredpan.mnstagram.MainActivity;
import cn.fredpan.mnstagram.R;
import cn.fredpan.mnstagram.model.Picture;
import cn.fredpan.mnstagram.model.PictureDto;
import cn.fredpan.mnstagram.model.Updatable;

public class Global extends Fragment implements Updatable<Picture> {

    FirebaseFirestore userDb;
    RecyclerView recyclerView;
    FirebaseFirestore db;
    FloatingActionButton takeNewPicBtn;
    private StorageReference picStorage;
    private FirebaseVisionImageLabeler labeler;
    private String rootPath;
    private PicListAdapter mAdapter;
    private List<Picture> pic;

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.global, container, false);

        userDb = (userDb == null) ? FirebaseFirestore.getInstance() : userDb;
        labeler = (labeler == null) ? FirebaseVision.getInstance().getCloudImageLabeler() : labeler;

        //basic components
        recyclerView = rootView.findViewById(R.id.global_recycler_view);
        takeNewPicBtn = rootView.findViewById(R.id.take_new_img);

        db = ((MainActivity) getActivity()).getDb();
        picStorage = ((MainActivity) getActivity()).getPicStorage();
        rootPath = (((MainActivity) getActivity()).getRootPath());

        ((MainActivity) getActivity()).getNavigationView().getHeaderView(0);

        pic = new ArrayList<>();
        obtainPics();
        mAdapter = new PicListAdapter(pic, getActivity(), db, ((MainActivity) getActivity()).getUser(), R.layout.global_pic_list_item);
        recyclerView.setAdapter(mAdapter);
        GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), 1);
        recyclerView.setLayoutManager(layoutManager);
        mAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                Collections.sort(pic);
            }
        });

        mAdapter.notifyDataSetChanged();

        return rootView;
    }

    private void obtainPics() {
        final List<PictureDto> refs = new ArrayList<>();
        db.collection("photos")
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    // get all ref
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        PictureDto pictureDto = document.toObject(PictureDto.class);
                        pictureDto.setPid(document.getId());
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

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        ((MainActivity) getActivity()).setCurrentFragment(this);
    }

    @Override
    public List<Picture> getList() {
        return pic;
    }

    @Override
    public void update() {
        mAdapter.notifyDataSetChanged();
    }
}
