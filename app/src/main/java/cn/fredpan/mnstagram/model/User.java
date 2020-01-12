package cn.fredpan.mnstagram.model;

import android.graphics.Bitmap;

public class User {
    private Bitmap avatar;
    private String uid;
    private String username;
    private String bio;
    private String email;

    public User(Bitmap avatar, String username, String bio, String email) {
        this.avatar = avatar;
        this.username = username;
        this.bio = bio;
        this.email = email;
    }

    public User(Bitmap avatar, String uid, String username, String bio, String email) {
        this.avatar = avatar;
        this.uid = uid;
        this.username = username;
        this.bio = bio;
        this.email = email;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Bitmap getAvatar() {
        return avatar;
    }

    public void setAvatar(Bitmap avatar) {
        this.avatar = avatar;
    }
}
