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

package cn.fredpan.mnstagram.model;

import android.graphics.Bitmap;

public class Picture implements Comparable<Picture> {

    private Bitmap pic;
    private String uid;
    private String pid;
    private String storageRef;
    private String timestamp;
    private String caption;

    public Picture() {
    }

    public Picture(Bitmap pic, String uid, String storageRef, String timestamp, String pid) {
        this.pic = pic;
        this.uid = uid;
        this.storageRef = storageRef;
        this.timestamp = timestamp;
        this.pid = pid;
    }

    public Picture(PictureDto pictureDto, Bitmap pic) {
        this.pic = pic;
        this.uid = pictureDto.getUid();
        this.storageRef = pictureDto.getStorageRef();
        this.timestamp = pictureDto.getTimestamp();
        this.caption = pictureDto.getCaption();
        this.pid = pictureDto.getPid();
    }

    public Bitmap getPic() {
        return pic;
    }

    public void setPic(Bitmap pic) {
        this.pic = pic;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getStorageRef() {
        return storageRef;
    }

    public void setStorageRef(String storageRef) {
        this.storageRef = storageRef;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public int compareTo(Picture picture) {
        if (getTimestamp() == null || picture.getTimestamp() == null) {
            return 0;
        }
        return Integer.valueOf(picture.getTimestamp()).compareTo(Integer.valueOf(getTimestamp()));
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }
}
