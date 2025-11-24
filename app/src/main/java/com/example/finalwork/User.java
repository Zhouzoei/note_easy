package com.example.finalwork;

import java.util.Date;

public class User {
    private String username;
    private String password;
    private Date registerTime;

    public User() {
        // 默认构造函数
    }

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.registerTime = new Date();
    }

    // Getter 和 Setter 方法
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Date getRegisterTime() {
        return registerTime;
    }

    public void setRegisterTime(Date registerTime) {
        this.registerTime = registerTime;
    }
}