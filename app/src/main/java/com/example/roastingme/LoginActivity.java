package com.example.roastingme;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.roastingme.network.ApiClient;
import com.example.roastingme.network.TokenManager;
import com.example.roastingme.network.dto.LoginRequest;
import com.example.roastingme.network.dto.LoginResponse;
import com.example.roastingme.network.dto.MeResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText emailInput;
    private EditText passwordInput;
    private Button loginButton;
    private TextView resultText;

    private ApiClient apiClient;
    private TokenManager tokenManager;

    private Call<LoginResponse> loginCall;
    private Call<MeResponse> meCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (view, insets) -> {
                    Insets systemBars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );
                    view.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );
                    return insets;
                }
        );

        emailInput = findViewById(R.id.edit_email);
        passwordInput = findViewById(R.id.edit_password);
        loginButton = findViewById(R.id.btn_login);
        resultText = findViewById(R.id.text_login_result);

        // 이전 단계에서 완성한 싱글톤 인스턴스 연동
        apiClient = ApiClient.getInstance(getApplicationContext());
        tokenManager = TokenManager.getInstance(getApplicationContext());

        loginButton.setOnClickListener(view -> login());

        // 자동 로그인 체크: 이미 저장된 토큰이 있으면 곧바로 내 정보 조회
        checkAutoLogin();
    }

    private void checkAutoLogin() {
        String savedToken = tokenManager.getAccessToken();
        if (savedToken != null && !savedToken.trim().isEmpty()) {
            setLoading(true);
            showResult("저장된 인증 정보로 로그인 중...");
            loadMyProfile();
        }
    }

    private void login() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            showResult("이메일과 비밀번호를 입력해 주세요.");
            return;
        }

        setLoading(true);
        showResult("로그인 중입니다.");

        loginCall = apiClient.getAuthApi().login(
                new LoginRequest(email, password)
        );

        loginCall.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(
                    Call<LoginResponse> call,
                    Response<LoginResponse> response) {

                if (response.errorBody() != null) {
                    response.errorBody().close();
                }

                if (call.isCanceled() || isFinishing() || isDestroyed()) {
                    return;
                }

                LoginResponse body = response.body();

                if (response.isSuccessful() && body != null) {
                    String token = body.getAccessToken();

                    if (token == null || token.trim().isEmpty()) {
                        setLoading(false);
                        showResult("로그인 응답에 토큰이 없습니다.");
                        return;
                    }

                    // EncryptedSharedPreferences에 안전하게 토큰 보관
                    tokenManager.saveAccessToken(token);
                    passwordInput.setText("");

                    // 저장 직후 AuthInterceptor가 다음 요청부터 Bearer 헤더를 자동 주입함
                    loadMyProfile();
                    return;
                }

                setLoading(false);

                if (response.code() == 401) {
                    showResult("이메일 또는 비밀번호가 올바르지 않습니다.");
                } else if (response.code() == 400) {
                    showResult("이메일과 비밀번호의 입력 형식을 확인해 주세요.");
                } else {
                    showResult("로그인 실패: HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(
                    Call<LoginResponse> call,
                    Throwable throwable) {

                if (call.isCanceled() || isFinishing() || isDestroyed()) {
                    return;
                }

                setLoading(false);
                showResult("요청 처리에 실패했습니다. 서버와 네트워크 상태를 확인해 주세요.");
            }
        });
    }

    private void loadMyProfile() {
        meCall = apiClient.getUserApi().getMe();

        meCall.enqueue(new Callback<MeResponse>() {
            @Override
            public void onResponse(
                    Call<MeResponse> call,
                    Response<MeResponse> response) {

                if (response.errorBody() != null) {
                    response.errorBody().close();
                }

                if (call.isCanceled() || isFinishing() || isDestroyed()) {
                    return;
                }

                setLoading(false);
                MeResponse me = response.body();

                if (response.isSuccessful() && me != null) {
                    showResult("인증 성공!\n" + me.getNickname() + "님 환영합니다.");

                    // TODO: 메인 화면 진입 처리 예시
                    // navigateToMainActivity();
                } else if (response.code() == 401) {
                    tokenManager.clear();
                    showResult("인증이 만료되었습니다. 다시 로그인해 주세요.");
                } else {
                    showResult("내 정보 조회 실패: HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(
                    Call<MeResponse> call,
                    Throwable throwable) {

                if (call.isCanceled() || isFinishing() || isDestroyed()) {
                    return;
                }

                setLoading(false);
                showResult("사용자 정보를 불러오지 못했습니다. 네트워크를 확인해 주세요.");
            }
        });
    }

    private void setLoading(boolean loading) {
        loginButton.setEnabled(!loading);
        emailInput.setEnabled(!loading);
        passwordInput.setEnabled(!loading);
    }

    private void showResult(String message) {
        resultText.setText(message);
    }

    @Override
    protected void onDestroy() {
        if (loginCall != null) {
            loginCall.cancel();
        }
        if (meCall != null) {
            meCall.cancel();
        }
        super.onDestroy();
    }
}