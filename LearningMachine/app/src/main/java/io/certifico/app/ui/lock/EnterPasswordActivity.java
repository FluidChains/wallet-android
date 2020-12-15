package io.certifico.app.ui.lock;

import android.app.Activity;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.security.keystore.UserNotAuthenticatedException;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

import com.fluidcerts.android.app.R;
import com.fluidcerts.android.app.databinding.ActivityEnterPasswordBinding;

import io.certifico.app.util.AESCrypt;
import io.certifico.app.util.FileUtils;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import timber.log.Timber;

public class EnterPasswordActivity extends AppCompatActivity {

    private static final String KEY_NAME = "Certifico.Keystore.PassAttemptTimeout";
    private static final int AUTHENTICATION_DURATION_SECONDS = 30;

    private Boolean mIsGoogleFlow;
    private Boolean mValidPassword = false;
    private Integer mAttempts = 3;

    private ActivityEnterPasswordBinding mBinding;

    @Override
    public void onBackPressed() {
        Timber.i("[Drive] Back pressed");
        Intent intent = new Intent();
        intent.putExtra("isGoogleFlow", mIsGoogleFlow);
        setResult(Activity.RESULT_CANCELED, intent);
        finish();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_enter_password);
        super.onCreate(savedInstanceState);
        mIsGoogleFlow = getIntent().getBooleanExtra("isGoogleFlow", false);
        Timber.i("[Drive] EnterPasswordActivity isGoogleFlow: " + mIsGoogleFlow);

        mBinding.passwordEditText.addTextChangedListener(new UpdatePasswordTextWatcher());

        mBinding.decryptButton.setOnClickListener(v -> {

            Timber.i("[Drive] mAttempts: " + mAttempts);
            if (mAttempts <= 0 ){
                createKey();
                if (!timeoutExpired()) {
                    mBinding.attemptsRemainingLabel.setTextColor(getResources().getColor(R.color.white));
                    mBinding.attemptsRemainingLabel.setText("Please wait 30s");
                    return;
                }
            }

            if (mValidPassword) {
                String key = mBinding.passwordEditText.getText().toString();
                String passphrase = getPassphraseFromEncrypted(key, mIsGoogleFlow);

                if (passphrase != null) {
                    Intent intent = new Intent();
                    intent.putExtra("seed", passphrase);
                    intent.putExtra("isGoogleFlow", mIsGoogleFlow);
                    setResult(Activity.RESULT_OK, intent);
                    finish();
                } else {
                    if (mAttempts > 0) {
                        mAttempts -= 1;
                    } else {
                        mAttempts = 0;
                    }
                    mBinding.attemptsRemainingLabel.setText(String.format("%s attempts remaining", mAttempts));
                    if (mAttempts < 2) {
                        mBinding.attemptsRemainingLabel.setTextColor(getResources().getColor(R.color.orange));
                    }
//                    if (mAttempts <= 0) {
//                        Intent intent = new Intent();
//                        intent.putExtra("isGoogleFlow", mIsGoogleFlow);
//                        setResult(Activity.RESULT_CANCELED, intent);
//                        finish();
//                    }
                    Toast.makeText(this, "Invalid password", Toast.LENGTH_LONG).show();
                }
            }

        });

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void createKey() {
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            KeyGenerator keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");

            keyGenerator.init(new KeyGenParameterSpec.Builder(KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    .setUserAuthenticationValidityDurationSeconds(AUTHENTICATION_DURATION_SECONDS)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build());
            keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException | NoSuchProviderException
                | InvalidAlgorithmParameterException | KeyStoreException
                | CertificateException | IOException e) {
            throw new RuntimeException("Key creation failed", e);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean timeoutExpired() {
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            SecretKey secretKey = (SecretKey) keyStore.getKey(KEY_NAME, null);
            Cipher cipher = Cipher.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/"
                            + KeyProperties.ENCRYPTION_PADDING_PKCS7);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            cipher.doFinal(mAttempts.toString().getBytes());
            Timber.i("[Drive] timeoutExpired: false");
            return false;
        } catch (UserNotAuthenticatedException e) {
            Timber.i("[Drive] UserNotAuthenticatedException: true");
            return true;
        } catch (KeyPermanentlyInvalidatedException e) {
            Timber.i("Keys invalidated after creation");
            Timber.i("[Drive] KeyPermanentlyInvalidatedException: true");
            return true;
        } catch (BadPaddingException | IllegalBlockSizeException | KeyStoreException |
                CertificateException | UnrecoverableKeyException | IOException
                | NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    private String getPassphraseFromEncrypted(String encryptionKey, boolean gDrive) {
        String encryptedMsg = FileUtils.getSeedFromFile(this, gDrive,false);
        Timber.i("[Drive] encryptedMsg: " + encryptedMsg);
        if (encryptedMsg == null) {
            return null;
        }
        try {
            String content = AESCrypt.decrypt(encryptionKey, encryptedMsg);
            Timber.i("[Drive] decryptedMsg: " + content);
            if (content.startsWith("mneumonic:")) {
                return content.substring(10).trim();
            }
        } catch (GeneralSecurityException e){
            Timber.e(e, "[Drive] Could not decrypt passphrase.");
        }
        return null;
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
            mBinding.decryptButton.setClickable(false);

            String password = mBinding.passwordEditText.getText()
                    .toString();

            if (password.length() < 1) {
                mValidPassword = false;
            } else {
                mValidPassword = true;
                mBinding.decryptButton.setClickable(true);
            }

        }
    }
}