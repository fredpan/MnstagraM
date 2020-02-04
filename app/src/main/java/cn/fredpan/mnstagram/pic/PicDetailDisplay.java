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

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import cn.fredpan.mnstagram.R;
import cn.fredpan.mnstagram.model.Picture;
import cn.fredpan.mnstagram.model.PictureDto;

public class PicDetailDisplay extends AppCompatActivity {

    private Picture picture;
    private ImageView picView;
    private TextView timestamp;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pic_detail_display);
        picView = findViewById(R.id.pic_display);
        timestamp = findViewById(R.id.timestamp);

        //get logged in user info
        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            Toast.makeText(this, "Failed to load the image, Please try again later. :<", Toast.LENGTH_LONG);
        } else {
            String path = (String) getIntent().getSerializableExtra("path"); //Obtaining data
            PictureDto pictureDto = (PictureDto) getIntent().getSerializableExtra("pictureDto"); //Obtaining data
            picture = new Picture(pictureDto, BitmapFactory.decodeFile(path));
        }
        assert picture != null;
        displayInfo();
    }

    private void displayInfo() {
        picView.setImageBitmap(picture.getPic());


        long sec = Integer.valueOf(picture.getTimestamp());
        Date date = new Date(sec * 1000);
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        System.out.println(dateFormat.format(date));
        timestamp.setText("Timestamp: " + dateFormat.format(date));
//        timestamp.setText(picture.getTimestamp());
    }
}
