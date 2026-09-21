package com.example.roastingme.network.dto;

import androidx.annotation.NonNull;
import com.google.gson.annotations.SerializedName;

public class LoginRequest {

    @SerializedName("email")
    @NonNull
    private final String email;

    @SerializedName("password")
    @NonNull
    private final String password;

    public LoginRequest(@NonNull String email, @NonNull String password) {
        this.email = email;
        this.password = password;
    }

    @NonNull
    public String getEmail() {
        return email;
    }

    @NonNull
    public String getPassword() {
        return password;
    }
}