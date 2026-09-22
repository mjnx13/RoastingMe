package com.example.roastingme;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.roastingme.network.ApiClient;
import com.example.roastingme.network.TokenManager;
import com.example.roastingme.network.dto.MeResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private TextView tvNickname;
    private TextView tvEmail;
    private Button btnLogin;

    private ApiClient apiClient;
    private TokenManager tokenManager;
    private Call<MeResponse> meCall;

    // 1. 로그인 결과 수신을 위한 ActivityResultLauncher
    private final ActivityResultLauncher<Intent> loginLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            // 로그인 성공 후 복귀 시 내 프로필 새로고침
                            loadMyProfile();
                        }
                    }
            );

    public ProfileFragment() {
        super(R.layout.fragment_profile);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        // 뷰 바인딩
        tvNickname = view.findViewById(R.id.tv_nickname);
        tvEmail = view.findViewById(R.id.tv_email);
        btnLogin = view.findViewById(R.id.btn_profile_login);

        // 네트워크 및 토큰 매니저 초기화
        apiClient = ApiClient.getInstance(requireContext().getApplicationContext());
        tokenManager = TokenManager.getInstance(requireContext().getApplicationContext());

        // 로그인 버튼 클릭 이벤트
        btnLogin.setOnClickListener(button ->
                loginLauncher.launch(
                        new Intent(requireContext(), LoginActivity.class)
                )
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        // 화면이 활성화될 때마다 로그인 상태 확인 및 데이터 조회
        loadMyProfile();
    }

    private void loadMyProfile() {
        String token = tokenManager.getAccessToken();

        // 토큰이 없는 경우 (비로그인 상태)
        if (token == null || token.trim().isEmpty()) {
            updateUiForGuest();
            return;
        }

        // 토큰이 있는 경우 내 정보 API 호출
        meCall = apiClient.getUserApi().getMe();
        meCall.enqueue(new Callback<MeResponse>() {
            @Override
            public void onResponse(
                    @NonNull Call<MeResponse> call,
                    @NonNull Response<MeResponse> response
            ) {
                if (!isAdded()) return; // 프래그먼트 이탈 방어

                if (response.isSuccessful() && response.body() != null) {
                    MeResponse me = response.body();
                    updateUiForUser(me.getNickname(), me.getEmail());
                } else if (response.code() == 401) {
                    // 토큰이 만료된 경우 초기화 후 비로그인 UI로 전환
                    tokenManager.clear();
                    updateUiForGuest();
                } else {
                    updateUiForGuest();
                }
            }

            @Override
            public void onFailure(
                    @NonNull Call<MeResponse> call,
                    @NonNull Throwable throwable
            ) {
                if (!isAdded()) return;
                // 네트워크 에러 시 안전하게 비로그인 상태 처리
                updateUiForGuest();
            }
        });
    }

    // 로그인 사용자 UI 상태
    private void updateUiForUser(String nickname, String email) {
        tvNickname.setText(nickname != null ? nickname : "회원");
        tvEmail.setText(email != null ? email : "");
        tvEmail.setVisibility(View.VISIBLE);
        btnLogin.setVisibility(View.GONE); // 로그인 상태에서는 로그인 버튼 숨김
    }

    // 비로그인(게스트) UI 상태
    private void updateUiForGuest() {
        tvNickname.setText("로그인이 필요합니다");
        tvEmail.setText("서비스 이용을 위해 로그인해 주세요.");
        tvEmail.setVisibility(View.VISIBLE);
        btnLogin.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        if (meCall != null) {
            meCall.cancel();
        }
        super.onDestroyView();
    }
}