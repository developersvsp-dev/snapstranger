package com.vaibhav.snapstrangerr;

public class User {
    private String username, password, gender;

    public User() {} // Empty constructor for Firestore

    public User(String username, String password, String gender) {
        this.username = username;
        this.password = password;
        this.gender = gender;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getGender() { return gender; }

    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public void setGender(String gender) { this.gender = gender; }
}
