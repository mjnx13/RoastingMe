package com.example.roastingme.network.dto;

import com.google.gson.annotations.SerializedName;

public class SignupRequest {

    @SerializedName("email")
    private final String email;

    @SerializedName("nickname")
    private final String nickname;

    @SerializedName("password")
    private final String password;

    public SignupRequest(String email, String nickname, String password) {
        // 입력값 앞뒤 공백 제거 및 Null 방어 처리
        this.email = (email != null) ? email.trim() : "";
        this.nickname = (nickname != null) ? nickname.trim() : "";
        this.password = (password != null) ? password : "";
    }

    public String getEmail() {
        return email;
    }

    public String getNickname() {
        return nickname;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public String toString() {
        return "SignupRequest{" +
                "email='" + email + '\'' +
                ", nickname='" + nickname + '\'' +
                ", password='[PROTECTED]'" +
                '}';
    }
}