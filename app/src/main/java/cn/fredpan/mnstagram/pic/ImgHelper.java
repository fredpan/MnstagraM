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
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.theartofdev.edmodo.cropper.CropImage;

public class ImgHelper {

    private static final int FIXED_SCALED_PIC_SIZE = 1024;

    private static final int FIXED_SCALED_PIC_WIDTH = FIXED_SCALED_PIC_SIZE;

    private static final int FIXED_SCALED_PIC_HEIGHT = FIXED_SCALED_PIC_SIZE;

    private static final int FIXED_SCALED_THUMBNAIL_SIZE = 100;

    private static final int FIXED_SCALED_THUMBNAIL_WIDTH = FIXED_SCALED_THUMBNAIL_SIZE;

    private static final int FIXED_SCALED_THUMBNAIL_HEIGHT = FIXED_SCALED_THUMBNAIL_SIZE;

    private static final int RESULT_OK = -1;

    public static void takePicWithFixedSize(Uri photoURI, Activity activity){
        CropImage.activity(photoURI)
                .setFixAspectRatio(true)
                .start(activity);
    }

    public static Bitmap getCroppedImg(int resultCode, Intent data) throws Exception {
        CropImage.ActivityResult result = CropImage.getActivityResult(data);
        if (resultCode == RESULT_OK) {
            Uri resultUri = result.getUri();
            return BitmapFactory.decodeFile(resultUri.getEncodedPath());
        } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
            throw result.getError();
        } else {
            throw new IllegalStateException("Crop acivity unsuccessful without proper error result code.");
        }
    }

    public static Bitmap getDownScaledImg(Bitmap originPic) {
        return downScaleImg(originPic, FIXED_SCALED_PIC_WIDTH, FIXED_SCALED_PIC_HEIGHT);
    }

    public static Bitmap getThumbnail(Bitmap originPic) {
        return downScaleImg(originPic, FIXED_SCALED_THUMBNAIL_WIDTH, FIXED_SCALED_THUMBNAIL_HEIGHT);
    }

    private static Bitmap downScaleImg(Bitmap originPic, int width, int height){
        return Bitmap.createScaledBitmap(originPic, width, height, false);
    }
}
