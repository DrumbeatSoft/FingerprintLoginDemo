package com.hailong.fingerprintlogindemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Build;
import android.os.CancellationSignal;
import android.util.Base64;

import com.blankj.utilcode.util.SPUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

@RequiresApi(Build.VERSION_CODES.P)
public class BiometricPromptApi28 implements IBiometricPromptImpl {

    private Activity mActivity;
    private BiometricPrompt mBiometricPrompt;
    private BiometricPromptManager.OnBiometricIdentifyCallback mManagerIdentifyCallback;
    private CancellationSignal mCancellationSignal;

    private ACache aCache;

    @RequiresApi(Build.VERSION_CODES.P)
    public BiometricPromptApi28(Activity activity) {
        mActivity = activity;
        aCache = ACache.get(App.getContext());
        mBiometricPrompt = new BiometricPrompt
                .Builder(activity)
                .setTitle(activity.getResources().getString(R.string.title))
                .setDescription(activity.getResources().getString(R.string.touch_2_auth))
                .setSubtitle("")
                .setNegativeButton(activity.getResources().getString(R.string.use_password),
                        activity.getMainExecutor(), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if (mManagerIdentifyCallback != null) {
                                    mManagerIdentifyCallback.onUsePassword();
                                }
                                mCancellationSignal.cancel();
                            }
                        })
                .build();

    }

    @RequiresApi(Build.VERSION_CODES.P)
    @Override
    public void authenticate(boolean loginFlg, @Nullable CancellationSignal cancel,
                             @NonNull BiometricPromptManager.OnBiometricIdentifyCallback callback) {
        mManagerIdentifyCallback = callback;

        mCancellationSignal = cancel;
        if (mCancellationSignal == null) {
            mCancellationSignal = new CancellationSignal();
        }
        mCancellationSignal.setOnCancelListener(new CancellationSignal.OnCancelListener() {
            @Override
            public void onCancel() {
            }
        });

        final KeyGenTool mKeyGenTool = new KeyGenTool(mActivity);
        BiometricPrompt.CryptoObject object;
        if (loginFlg) {
            //解密
            try {
                /**
                 * 可通过服务器保存iv,然后在使用之前从服务器获取
                 */
                //保存用于做AES-CBC
                String ivStr = aCache.getAsString("iv");
                byte[] iv = Base64.decode(ivStr, Base64.URL_SAFE);

                object = new BiometricPrompt.CryptoObject(mKeyGenTool.getDecryptCipher(iv));
                mBiometricPrompt.authenticate(object,
                        new CancellationSignal(), mActivity.getMainExecutor(), new BiometricPromptCallbackImpl());
            } catch (IllegalStateException e) {
                e.printStackTrace();
                onChangeFingerprint();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            //加密
            try {
                object = new BiometricPrompt.CryptoObject(mKeyGenTool.getEncryptCipher());
                mBiometricPrompt.authenticate(object,
                        new CancellationSignal(), mActivity.getMainExecutor(), new BiometricPromptCallbackImpl());
            } catch (IllegalStateException e) {
                e.printStackTrace();
                onChangeFingerprint();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * 指纹发生变更，如新增/删除
     */
    private void onChangeFingerprint() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle("提示")
                .setMessage("指纹信息有变更，需通过其他方式登录后重新启用")
                .setPositiveButton("好的", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            new KeyGenTool(mActivity).clearKey();
                            SPUtils.getInstance().remove(MainActivity.IV_STR_SP_KEY);
                        } catch (Exception exception) {
                            exception.printStackTrace();
                        }
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .create()
                .show();
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private class BiometricPromptCallbackImpl extends BiometricPrompt.AuthenticationCallback {
        @Override
        public void onAuthenticationError(int errorCode, CharSequence errString) {
            super.onAuthenticationError(errorCode, errString);
            mCancellationSignal.cancel();

        }

        @Override
        public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
            super.onAuthenticationHelp(helpCode, helpString);
        }

        @Override
        public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
            super.onAuthenticationSucceeded(result);
            mManagerIdentifyCallback.onSucceeded(result);
            mCancellationSignal.cancel();
        }

        @Override
        public void onAuthenticationFailed() {
            super.onAuthenticationFailed();
        }
    }

}
