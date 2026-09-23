package com.example.roastingme.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.roastingme.R;
import com.example.roastingme.network.ApiClient;
import com.example.roastingme.network.TokenManager;
import com.example.roastingme.network.dto.MeResponse;
import com.example.roastingme.network.dto.UpdateProfileRequest;
import com.example.roastingme.ui.auth.LoginActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileEditActivity extends AppCompatActivity {

    public static final String EXTRA_NICKNAME = "profile_nickname";

    private EditText nicknameInput;
    private Button saveButton;
    private TextView resultText;
    private Call<MeResponse> updateCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile_edit);

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

        nicknameInput = findViewById(R.id.edit_profile_nickname);
        saveButton = findViewById(R.id.btn_profile_save);
        resultText = findViewById(R.id.text_profile_edit_result);

        if (savedInstanceState == null) {
            nicknameInput.setText(
                    getIntent().getStringExtra(EXTRA_NICKNAME)
            );
        }

        findViewById(R.id.btn_edit_back)
                .setOnClickListener(view -> finish());

        saveButton.setOnClickListener(view -> saveProfile());
    }

    private void saveProfile() {
        String nickname = nicknameInput.getText().toString().trim();

        if (nickname.length() < 3 || nickname.length() > 50) {
            resultText.setText("닉네임은 3~50자로 입력해 주세요.");
            return;
        }

        setLoading(true);
        resultText.setText("저장 중입니다.");

        updateCall = ApiClient.getInstance(getApplicationContext())
                .getUserApi()
                .updateProfile(new UpdateProfileRequest(nickname));

        updateCall.enqueue(new Callback<MeResponse>() {
            @Override
            public void onResponse(
                    Call<MeResponse> call,
                    Response<MeResponse> response) {

                if (call.isCanceled() || isFinishing() || isDestroyed()) {
                    return;
                }

                setLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(
                            ProfileEditActivity.this,
                            "프로필을 수정했습니다.",
                            Toast.LENGTH_SHORT
                    ).show();

                    // 성공 결과를 이전 액티비티에 전달
                    setResult(RESULT_OK);
                    finish();
                } else if (response.code() == 401) {
                    TokenManager.getInstance(getApplicationContext()).clear();

                    Toast.makeText(
                            ProfileEditActivity.this,
                            "인증이 만료되었습니다. 다시 로그인해 주세요.",
                            Toast.LENGTH_SHORT
                    ).show();

                    // 로그인 화면으로 이동 및 스택 정리
                    Intent intent = new Intent(ProfileEditActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    // 서버가 전달한 에러 메시지 추출 (닉네임 중복 등)
                    String errorMsg = "저장 실패: HTTP " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            String serverMsg = response.errorBody().string();
                            if (!serverMsg.isEmpty()) {
                                errorMsg = serverMsg;
                            }
                        }
                    } catch (Exception e) {
                        // ignore parsing exception
                    }
                    resultText.setText(errorMsg);
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
                resultText.setText(
                        "저장 결과를 확인하지 못했습니다. 프로필을 다시 조회해 주세요."
                );
            }
        });
    }

    private void setLoading(boolean loading) {
        saveButton.setEnabled(!loading);
        nicknameInput.setEnabled(!loading);
    }

    @Override
    protected void onDestroy() {
        if (updateCall != null) {
            updateCall.cancel();
        }
        super.onDestroy();
    }
}