package com.smartparking.model;

import java.util.Date;
import java.util.Objects;
import com.smartparking.datastructure.*;

public abstract class User {
    private String account;
    private String password;
    private String name;
    private String phoneNumber;

    public User(String account, String password, String name, String phoneNumber) {
        this.account = account;
        this.password = password;
        this.name = name;
        this.phoneNumber = phoneNumber;
    }


    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}

