package com.example.roastingme.ui.ai;

import android.content.Context;
import android.net.Uri;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.roastingme.ai.model.ImagePreprocessResult;
import com.example.roastingme.ai.preprocess.ImagePreprocessUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AiAnalysisViewModel extends ViewModel {

    private final MutableLiveData<ImagePreprocessResult> preprocessResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public LiveData<ImagePreprocessResult> getPreprocessResult() {
        return preprocessResult;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    /**
     * 백그라운드 스레드에서 전처리 수행
     */
    public void processSelectedImage(Context context, Uri imageUri) {
        isLoading.setValue(true);
        executorService.execute(() -> {
            ImagePreprocessResult result = ImagePreprocessUtils.processImage(context, imageUri);
            preprocessResult.postValue(result);
            isLoading.postValue(false);
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}