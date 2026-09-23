package com.example.roastingme;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.roastingme.network.ApiClient;
import com.example.roastingme.network.TokenManager;
import com.example.roastingme.network.dto.MeResponse;

import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private TextView tvNickname;
    private TextView tvEmail;
    private TextView tvStatus;
    private Button btnLogin;
    private Button btnEdit;
    private Button btnLogout;

    private ApiClient apiClient;
    private TokenManager tokenManager;
    private Call<MeResponse> meCall;

    private MeResponse currentUser;
    private String displayedToken;

    public ProfileFragment() {
        super(R.layout.fragment_profile);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvNickname = view.findViewById(R.id.tv_nickname);
        tvEmail = view.findViewById(R.id.tv_email);
        tvStatus = view.findViewById(R.id.tv_profile_status);
        btnLogin = view.findViewById(R.id.btn_profile_login);
        btnEdit = view.findViewById(R.id.btn_profile_edit);
        btnLogout = view.findViewById(R.id.btn_profile_logout);

        apiClient = ApiClient.getInstance(requireContext());
        tokenManager = TokenManager.getInstance(requireContext());

        btnLogin.setOnClickListener(button ->
                startActivity(new Intent(requireContext(), LoginActivity.class))
        );

        btnEdit.setOnClickListener(button -> {
            if (currentUser == null) return;

            Intent intent = new Intent(requireContext(), ProfileEditActivity.class);
            intent.putExtra(ProfileEditActivity.EXTRA_NICKNAME, currentUser.getNickname());
            startActivity(intent);
        });

        // 로그아웃 클릭 시 확인 다이얼로그 표시
        btnLogout.setOnClickListener(button -> confirmLogout());

        view.findViewById(R.id.btn_open_settings).setOnClickListener(button ->
                ((MainActivity) requireActivity()).openSettingsDrawer()
        );

        showGuest();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMyProfile();
    }

    private void loadMyProfile() {
        cancelProfileRequest();

        String token = tokenManager.getAccessToken();

        if (token == null || token.trim().isEmpty()) {
            showGuest();
            return;
        }

        if (!Objects.equals(displayedToken, token)) {
            currentUser = null;
            displayedToken = token;
            tvNickname.setText("사용자 정보");
            tvEmail.setText("");
        }

        btnLogin.setVisibility(View.GONE);
        btnLogout.setVisibility(View.VISIBLE);
        btnEdit.setVisibility(currentUser == null ? View.GONE : View.VISIBLE);

        // 이미 데이터가 있다면 백그라운드 갱신, 없을 때만 로딩 문구 표시
        if (currentUser == null) {
            tvStatus.setText("프로필을 불러오는 중입니다.");
        }

        meCall = apiClient.getUserApi().getMe();

        meCall.enqueue(new Callback<MeResponse>() {
            @Override
            public void onResponse(Call<MeResponse> call, Response<MeResponse> response) {
                if (response.errorBody() != null) {
                    response.errorBody().close();
                }

                if (!isCurrentRequest(call, token)) return;

                if (response.isSuccessful() && response.body() != null) {
                    currentUser = response.body();

                    tvNickname.setText(currentUser.getNickname());
                    tvEmail.setText(currentUser.getEmail());
                    tvStatus.setText("");

                    btnLogin.setVisibility(View.GONE);
                    btnEdit.setVisibility(View.VISIBLE);
                    btnLogout.setVisibility(View.VISIBLE);

                } else if (response.code() == 401) {
                    tokenManager.clear();
                    showGuest();
                    tvStatus.setText("인증이 만료되었습니다. 다시 로그인해 주세요.");

                } else {
                    tvStatus.setText("프로필 조회 실패: HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(Call<MeResponse> call, Throwable throwable) {
                if (!isCurrentRequest(call, token)) return;

                tvStatus.setText("프로필을 불러오지 못했습니다. 연결을 확인하고 다시 시도해 주세요.");
            }
        });
    }

    private void confirmLogout() {
        new AlertDialog.Builder(requireContext())
                .setTitle("로그아웃")
                .setMessage("정말 로그아웃하시겠습니까?")
                .setPositiveButton("로그아웃", (dialog, which) -> logout())
                .setNegativeButton("취소", null)
                .show();
    }

    private boolean isCurrentRequest(Call<MeResponse> call, String requestToken) {
        return getView() != null
                && !call.isCanceled()
                && call == meCall
                && Objects.equals(requestToken, tokenManager.getAccessToken());
    }

    private void logout() {
        cancelProfileRequest();
        tokenManager.clear();
        showGuest();
        tvStatus.setText("로그아웃했습니다.");
    }

    private void showGuest() {
        currentUser = null;
        displayedToken = null;

        tvNickname.setText("로그인이 필요합니다");
        tvEmail.setText("서비스 이용을 위해 로그인해 주세요.");
        tvStatus.setText("");

        btnLogin.setVisibility(View.VISIBLE);
        btnEdit.setVisibility(View.GONE);
        btnLogout.setVisibility(View.GONE);
    }

    private void cancelProfileRequest() {
        if (meCall != null) {
            meCall.cancel();
            meCall = null;
        }
    }

    @Override
    public void onDestroyView() {
        cancelProfileRequest();

        currentUser = null;
        displayedToken = null;

        tvNickname = null;
        tvEmail = null;
        tvStatus = null;
        btnLogin = null;
        btnEdit = null;
        btnLogout = null;

        super.onDestroyView();
    }
}