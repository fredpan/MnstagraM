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
