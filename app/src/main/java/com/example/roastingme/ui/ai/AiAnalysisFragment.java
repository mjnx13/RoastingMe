package com.example.roastingme.ui.ai;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.roastingme.R;
import com.example.roastingme.ai.model.ImageQualityResult;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AiAnalysisFragment extends Fragment {

    private AiAnalysisViewModel viewModel;
    private ImageView previewImageView;
    private ProgressBar progressBar;
    private Uri cameraTempUri;

    // 갤러리 연동
    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    viewModel.processAndValidateImage(uri);
                }
            });

    // 카메라 연동
    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && cameraTempUri != null) {
                    viewModel.processAndValidateImage(cameraTempUri);
                }
            });

    public AiAnalysisFragment() {
        super(R.layout.fragment_ai_analysis);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(AiAnalysisViewModel.class);

        previewImageView = view.findViewById(R.id.iv_analysis_preview);
        progressBar = view.findViewById(R.id.pb_loading);

        view.findViewById(R.id.btn_open_gallery).setOnClickListener(v -> galleryLauncher.launch("image/*"));
        view.findViewById(R.id.btn_open_camera).setOnClickListener(v -> openCamera());

        setupObservers();
    }

    private void setupObservers() {
        // 1. 로딩 상태 관찰
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (progressBar != null) {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
        });

        // 2. 1단계 전처리 이미지 관찰 (null일 경우 ImageView 비우기)
        viewModel.getPreprocessResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null && result.getOriginalBitmap() != null) {
                previewImageView.setImageBitmap(result.getOriginalBitmap());
            } else {
                previewImageView.setImageBitmap(null);
            }
        });

        // 3. 2단계 품질 검증 결과 관찰 (Event 래퍼 사용으로 중복 다이얼로그 방지)
        viewModel.getQualityResultEvent().observe(getViewLifecycleOwner(), event -> {
            if (event == null || getContext() == null) return;

            ImageQualityResult qResult = event.getContentIfNotHandled();
            if (qResult == null) return; // 이미 처리된 이벤트면 무시

            if (qResult.isPass()) {
                Toast.makeText(requireContext(),
                        "품질 검증 통과 (밝기: " + (int) qResult.getBrightnessScore() +
                                ", 선명도: " + (int) qResult.getBlurScore() +
                                ", 공간밀도: " + String.format(Locale.US, "%.2f", qResult.getSpaceScore()) + ")",
                        Toast.LENGTH_SHORT).show();
            } else {
                showQualityFailureDialog(qResult.getMessage());
            }
        });
    }

    // 품질 미달 시 안내 다이얼로그 출력
    private void showQualityFailureDialog(String errorMessage) {
        new AlertDialog.Builder(requireContext())
                .setTitle("이미지 분석 불가 안내")
                .setMessage(errorMessage)
                .setPositiveButton("카메라 재촬영", (dialog, which) -> openCamera())
                .setNegativeButton("갤러리에서 선택", (dialog, which) -> galleryLauncher.launch("image/*"))
                .setNeutralButton("취소", null)
                .show();
    }

    // 타임스탬프를 적용한 고유 임시 파일 생성으로 카메라 촬영
    private void openCamera() {
        if (getContext() == null) return;

        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String imageFileName = "JPEG_" + timeStamp + "_";
            File storageDir = requireContext().getExternalFilesDir("Pictures");
            File photoFile = File.createTempFile(imageFileName, ".jpg", storageDir);

            cameraTempUri = FileProvider.getUriForFile(
                    requireContext().getApplicationContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    photoFile
            );
            cameraLauncher.launch(cameraTempUri);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "카메라 실행에 실패했습니다.", Toast.LENGTH_SHORT).show();
        }
    }
}