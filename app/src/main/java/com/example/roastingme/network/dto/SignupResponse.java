package com.example.roastingme.network.dto;

import com.google.gson.annotations.SerializedName;

public class SignupResponse {

    @SerializedName("id")
    private Long id; // 서버 PK가 Long인 경우 (UUID일 경우 String으로 유지)

    @SerializedName("email")
    private String email;

    @SerializedName("nickname")
    private String nickname;

    @SerializedName("createdAt") // 서버 응답이 created_at 이라면 "created_at"으로 변경
    private String createdAt;

    // 기본 생성자 (Gson 역직렬화용)
    public SignupResponse() {
    }

    // 단위 테스트 및 Mock 데이터 생성을 위한 생성자
    public SignupResponse(Long id, String email, String nickname, String createdAt) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email != null ? email : "";
    }

    public String getNickname() {
        return nickname != null ? nickname : "";
    }

    public String getCreatedAt() {
        return createdAt != null ? createdAt : "";
    }

    @Override
    public String toString() {
        return "SignupResponse{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", nickname='" + nickname + '\'' +
                ", createdAt='" + createdAt + '\'' +
                '}';
    }
}