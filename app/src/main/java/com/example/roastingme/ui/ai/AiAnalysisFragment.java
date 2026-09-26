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
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.roastingme.R;

import java.io.File;

public class AiAnalysisFragment extends Fragment {

    private AiAnalysisViewModel viewModel;
    private ImageView previewImageView;
    private ProgressBar progressBar;
    private Uri cameraTempUri;

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) viewModel.processSelectedImage(requireContext(), uri);
            });

    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && cameraTempUri != null) {
                    viewModel.processSelectedImage(requireContext(), cameraTempUri);
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

        // ViewModel 데이터 관찰
        setupObservers();
    }

    private void setupObservers() {
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (progressBar != null) {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getPreprocessResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                previewImageView.setImageBitmap(result.getOriginalBitmap());
                Toast.makeText(requireContext(),
                        "전처리 완료 (" + result.getProcessingTimeMs() + "ms)",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openCamera() {
        try {
            File photoFile = new File(requireContext().getExternalFilesDir("Pictures"), "temp_ai_image.jpg");
            cameraTempUri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    photoFile
            );
            cameraLauncher.launch(cameraTempUri);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "카메라를 실행할 수 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }
}