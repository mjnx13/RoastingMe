package com.example.roastingme.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.roastingme.R;
import com.example.roastingme.network.ApiClient;
import com.example.roastingme.network.dto.SignupRequest;
import com.example.roastingme.network.dto.SignupResponse;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignupActivity extends AppCompatActivity {

    public static final String EXTRA_SIGNUP_EMAIL = "signup_email";

    // TextInputLayout 참조
    private TextInputLayout tilEmail, tilNickname, tilPassword, tilConfirm;
    // EditText 참조
    private EditText emailInput, nicknameInput, passwordInput, confirmInput;

    private Button signupButton;
    private TextView resultText;

    private Call<SignupResponse> signupCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (view, insets) -> {
                    Insets bars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );
                    view.setPadding(
                            bars.left, bars.top, bars.right, bars.bottom
                    );
                    return insets;
                }
        );

        // Layout 및 EditText 바인딩
        tilEmail = findViewById(R.id.til_signup_email);
        tilNickname = findViewById(R.id.til_signup_nickname);
        tilPassword = findViewById(R.id.til_signup_password);
        tilConfirm = findViewById(R.id.til_signup_password_confirm);

        emailInput = findViewById(R.id.signup_email);
        nicknameInput = findViewById(R.id.signup_nickname);
        passwordInput = findViewById(R.id.signup_password);
        confirmInput = findViewById(R.id.signup_password_confirm);

        signupButton = findViewById(R.id.btn_signup_submit);
        resultText = findViewById(R.id.text_signup_result);

        signupButton.setOnClickListener(view -> signup());
    }

    private void signup() {
        // 1. 이전 에러 메시지 초기화
        clearErrors();

        String email = emailInput.getText().toString().trim();
        String nickname = nicknameInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        String confirm = confirmInput.getText().toString();

        // 2. 유효성 검사 및 개별 필드 에러 표시
        if (email.length() > 254 || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("올바른 이메일 형식을 입력해 주세요.");
            emailInput.requestFocus();
            return;
        }

        if (nickname.length() < 3 || nickname.length() > 50) {
            tilNickname.setError("닉네임은 3~50자로 입력해 주세요.");
            nicknameInput.requestFocus();
            return;
        }

        if (password.trim().isEmpty()
                || password.length() < 10
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            tilPassword.setError("비밀번호는 10자 이상, 72바이트 이하여야 합니다.");
            passwordInput.requestFocus();
            return;
        }

        if (!password.equals(confirm)) {
            tilConfirm.setError("비밀번호 확인이 일치하지 않습니다.");
            confirmInput.requestFocus();
            return;
        }

        // 3. 서버 요청 준비
        setLoading(true);
        resultText.setText("가입 중입니다...");

        signupCall = ApiClient.getInstance(getApplicationContext())
                .getAuthApi()
                .signup(new SignupRequest(email, nickname, password));

        signupCall.enqueue(new Callback<SignupResponse>() {
            @Override
            public void onResponse(
                    Call<SignupResponse> call,
                    Response<SignupResponse> response) {

                if (call.isCanceled() || isFinishing() || isDestroyed()) {
                    if (response.errorBody() != null) {
                        response.errorBody().close();
                    }
                    return;
                }

                setLoading(false);

                // response.isSuccessful() (200~299 범위 모두 성공으로 처리)
                if (response.isSuccessful() && response.body() != null) {
                    Intent result = new Intent();
                    result.putExtra(
                            EXTRA_SIGNUP_EMAIL,
                            response.body().getEmail()
                    );

                    setResult(RESULT_OK, result);
                    finish();
                    return;
                }

                String fallback;

                if (response.code() == 409) {
                    fallback = "이미 가입된 이메일 또는 닉네임입니다.";
                    tilEmail.setError("이미 가입된 이메일입니다.");
                } else if (response.code() == 400) {
                    fallback = "입력 내용을 다시 확인해 주세요.";
                } else {
                    fallback = "회원가입 실패: HTTP " + response.code();
                }

                resultText.setText(readErrorMessage(response, fallback));
            }

            @Override
            public void onFailure(
                    Call<SignupResponse> call,
                    Throwable throwable) {

                if (call.isCanceled() || isFinishing() || isDestroyed()) {
                    return;
                }

                setLoading(false);
                resultText.setText(
                        "가입 결과를 확인하지 못했습니다. "
                                + "서버와 네트워크를 확인해 주세요."
                );
            }
        });
    }

    private void clearErrors() {
        tilEmail.setError(null);
        tilNickname.setError(null);
        tilPassword.setError(null);
        tilConfirm.setError(null);
        resultText.setText("");
    }

    private String readErrorMessage(
            Response<?> response,
            String fallback) {

        try (ResponseBody error = response.errorBody()) {
            if (error == null) {
                return fallback;
            }

            JsonObject json = JsonParser
                    .parseString(error.string())
                    .getAsJsonObject();

            if (json.has("message")
                    && json.get("message").isJsonPrimitive()) {
                return json.get("message").getAsString();
            }
        } catch (Exception ignored) {
            // JSON 형태가 아니면 기본 안내 사용
        }

        return fallback;
    }

    private void setLoading(boolean loading) {
        signupButton.setEnabled(!loading);
        emailInput.setEnabled(!loading);
        nicknameInput.setEnabled(!loading);
        passwordInput.setEnabled(!loading);
        confirmInput.setEnabled(!loading);
    }

    @Override
    protected void onDestroy() {
        if (signupCall != null) {
            signupCall.cancel();
        }
        super.onDestroy();
    }
}