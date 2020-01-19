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

import java.io.Serializable;

public class UserDto implements Serializable {
    private String username;
    private String bio;
    private String displayPicPath;

    public UserDto(String username, String bio, String displayPicPath) {
        this.username = username;
        this.bio = bio;
        this.displayPicPath = displayPicPath;
    }

    public UserDto() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getDisplayPicPath() {
        return displayPicPath;
    }

    public void setDisplayPicPath(String displayPicPath) {
        this.displayPicPath = displayPicPath;
    }

    public User generateUser(Bitmap avatar, String uid, String email){
        return new User(avatar, uid, this.username, this.bio, email);
    }
}
