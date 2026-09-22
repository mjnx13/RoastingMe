package com.example.roastingme.network.dto;

import com.google.gson.annotations.SerializedName;

public class UpdateProfileRequest {

    @SerializedName("nickname")
    private final String nickname;

    public UpdateProfileRequest(String nickname) {
        this.nickname = nickname == null ? "" : nickname.trim();
    }

    public String getNickname() {
        return nickname;
    }
}