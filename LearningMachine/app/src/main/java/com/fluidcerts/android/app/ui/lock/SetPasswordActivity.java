package com.fluidcerts.android.app.ui.lock;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.annotation.RequiresApi;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;

import com.fluidcerts.android.app.R;
import com.fluidcerts.android.app.data.inject.Injector;
import com.fluidcerts.android.app.data.preferences.SharedPreferencesManager;
import com.fluidcerts.android.app.databinding.ActivitySetPasswordBinding;
import com.fluidcerts.android.app.dialog.AlertDialogFragment;
import com.fluidcerts.android.app.ui.LMActivity;
import com.fluidcerts.android.app.util.DialogUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.inject.Inject;

import timber.log.Timber;

public class SetPasswordActivity extends AppCompatActivity {

    public static final String KEYSTORE_ENCRYPTION_KEY_ALIAS = "Certifico.Keystore.EncryptionKey";

    @Inject SharedPreferencesManager mSharedPreferencesManager;

    private Boolean mValidPassword = false;

    private ActivitySetPasswordBinding mBinding;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Injector.obtain(this)
                .inject(this);

        String savedPassword = getSavedPassword();

        if (savedPassword != null) {
            Intent intent = new Intent();
            intent.putExtra("encryptionKey", savedPassword);
            setResult(RESULT_OK, intent);
            finish();
            return;
        }

        Cipher cipher = getCipher();
        byte[] iv = cipher.getIV();
        mSharedPreferencesManager.setPasswordIV(iv);

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_set_password);
        mBinding.passwordEditText.addTextChangedListener(new UpdatePasswordTextWatcher());
        mBinding.confirmPasswordText.addTextChangedListener(new UpdatePasswordTextWatcher());

        mBinding.setPasswordButton.setOnClickListener(v -> {

            String password = mBinding.passwordEditText.getText().toString();

            if (mValidPassword) {
                try {
                    byte[] encryptedPassword = cipher.doFinal(password.getBytes("UTF-8"));
                    mSharedPreferencesManager.setLockScreenPassword(encryptedPassword);
                } catch (IllegalBlockSizeException | BadPaddingException | UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                Intent intent = new Intent();
                intent.putExtra("encryptionKey", password);
                setResult(RESULT_OK, intent);
                finish();
            }
        });
        DisplayWarning();
    }

    private String getSavedPassword() {
        byte[] encryptionIv = mSharedPreferencesManager.getPasswordIV();
        byte[] encryptedData = mSharedPreferencesManager.getLockScreenPassword();

        if (encryptionIv == null || encryptedData == null ) {
            return null;
        }

        KeyStore keyStore = null;
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore");
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
        try {
            assert keyStore != null;
            keyStore.load(null);
        } catch (IOException | NoSuchAlgorithmException | CertificateException e) {
            e.printStackTrace();
        }

        KeyStore.SecretKeyEntry secretKeyEntry = null;
        try {
            secretKeyEntry = (KeyStore.SecretKeyEntry) keyStore
                    .getEntry(KEYSTORE_ENCRYPTION_KEY_ALIAS, null);
        } catch (NoSuchAlgorithmException | UnrecoverableEntryException | KeyStoreException e) {
            e.printStackTrace();
        }

        if (secretKeyEntry == null) {
            return null;
        }
        final SecretKey secretKey = secretKeyEntry.getSecretKey();

        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("AES/GCM/NoPadding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        }

        final GCMParameterSpec spec = new GCMParameterSpec(128, encryptionIv);
        try {
            assert cipher != null;
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }

        byte[] decodedData = null;
        try {
            decodedData = cipher.doFinal(encryptedData);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        }

        String passwordPlaintext = null;
        if (decodedData != null) {
            passwordPlaintext = new String(decodedData, StandardCharsets.UTF_8);
        }
        return passwordPlaintext;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private Cipher getCipher() {
        KeyGenerator keyGenerator = null;
        try {
            keyGenerator = KeyGenerator
                    .getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            e.printStackTrace();
        }

        final KeyGenParameterSpec keyGenParameterSpec = new KeyGenParameterSpec.Builder(KEYSTORE_ENCRYPTION_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build();

        try {
            assert keyGenerator != null;
            keyGenerator.init(keyGenParameterSpec);
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        final SecretKey secretKey = keyGenerator.generateKey();

        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("AES/GCM/NoPadding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        }
        try {
            assert cipher != null;
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        return cipher;
    }

    private class UpdatePasswordTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            mBinding.setPasswordButton.setClickable(false);

            String validation = "";

            String password = mBinding.passwordEditText.getText()
                    .toString();
            String confirm = mBinding.confirmPasswordText.getText()
                    .toString();

            int validations = 3;

            if (!password.equals(confirm)) {
                validation += '\n' + "- Passwords must match";
                validations -= 1;
                mValidPassword = false;
            }

            if (password.length() < 8) {
                validation += '\n' + "- Password must be at least 8 characters";
                validations -= 1;
                mValidPassword = false;
            }

            if (!password.matches(".*[!@#$%^&*].*")) {
                validation += '\n' + "- Password must contain at least 1 special character";
                validations -= 1;
                mValidPassword = false;
            }

            if (validations == 3) {
                mValidPassword = true;
                mBinding.setPasswordButton.setClickable(true);
            }

            mBinding.validationLabel.setText(validation);
        }
    }

    public void DisplayWarning() {
        FragmentManager fm = getSupportFragmentManager();
        AlertDialogFragment alertDialogFragment = AlertDialogFragment.newInstance(
                false,
                0,
                R.drawable.ic_dialog_warning,
                "",
                getResources().getString(R.string.fragment_set_password_warning),
                "Ok",
                null,
                null,
                null,
                null);
        alertDialogFragment.show(fm, "SetPasswordActivity.Dialog.Alert");
    }
}