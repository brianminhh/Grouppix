package com.example.grouppix;

import java.util.List;

public class GroupUser{

    public String username;

    // Default constructor required for calls to
    // DataSnapshot.getValue(User.class)
    public GroupUser() {
    }

    public GroupUser(String username) {
        this.username = username;
    }

    public String getName() {
        return username;
    }
}
