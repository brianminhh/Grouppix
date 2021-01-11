package com.example.grouppix;

import java.util.List;

public class Groups{

    public String name;

    // Default constructor required for calls to
    // DataSnapshot.getValue(User.class)
    public Groups() {
    }

    public Groups(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
