package com.hailong.fingerprintlogindemo;

import android.os.CancellationSignal;

import androidx.annotation.NonNull;

interface IBiometricPromptImpl {

    void authenticate(boolean loginFlg, @NonNull CancellationSignal cancel,
                      @NonNull BiometricPromptManager.OnBiometricIdentifyCallback callback);

}
