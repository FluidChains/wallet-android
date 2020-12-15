package io.certifico.app.ui.lock;

import android.app.Activity;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.fluidcerts.android.app.R;
import io.certifico.app.data.inject.Injector;
import com.fluidcerts.android.app.databinding.FragmentEnterPasswordBinding;
import io.certifico.app.ui.LMFragment;
import io.certifico.app.util.AESCrypt;
import io.certifico.app.util.FileUtils;

import java.security.GeneralSecurityException;

import timber.log.Timber;

public class EnterPasswordFragment extends LMFragment {

    private Boolean mIsGoogleFlow;
    private Boolean mValidPassword = false;
    private Integer mAttempts = 3;

    private FragmentEnterPasswordBinding mBinding;

    public static EnterPasswordFragment newInstance(boolean isGoogleFlow) {
        EnterPasswordFragment instance = new EnterPasswordFragment();
        Bundle args = new Bundle();
        args.putBoolean("isGoogleFlow", isGoogleFlow);
        Timber.i("[Drive] EnterPasswordFragment newInstance isGoogleFlow: " + args.getBoolean("isGoogleFlow"));
        instance.setArguments(args);
        return instance;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        Injector.obtain(getContext())
                .inject(this);
        Bundle args = getArguments();
        mIsGoogleFlow = args.getBoolean("isGoogleFlow");
        Timber.i("[Drive] EnterPasswordFragment isGoogleFlow: " + mIsGoogleFlow);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_enter_password, container, false);
        mBinding.passwordEditText.addTextChangedListener(new EnterPasswordFragment.UpdatePasswordTextWatcher());

        mBinding.decryptButton.setOnClickListener(v -> {

            if (mValidPassword) {
                String key = mBinding.passwordEditText.getText().toString();
                String passphrase = getPassphraseFromEncrypted(key, mIsGoogleFlow);

                if (passphrase != null) {
                    Intent intent = new Intent();
                    intent.putExtra("seed", passphrase);
                    intent.putExtra("isGoogleFlow", mIsGoogleFlow);
                    getActivity().setResult(Activity.RESULT_OK, intent);
                    getActivity().finish();
                } else {
                    mAttempts -= 1;
                    mBinding.attemptsRemainingLabel.setText(String.format("%s attempts remaining", mAttempts));
                    if (mAttempts < 2) {
                        mBinding.attemptsRemainingLabel.setTextColor(getResources().getColor(R.color.orange));
                    }
                    if (mAttempts <= 0) {
                        Intent intent = new Intent();
                        intent.putExtra("isGoogleFlow", mIsGoogleFlow);
                        getActivity().setResult(Activity.RESULT_CANCELED, intent);
                        getActivity().finish();
                    }
                    Toast.makeText(getActivity(), "Invalid password", Toast.LENGTH_LONG).show();
                }
            }

        });

        return mBinding.getRoot();
    }

    private String getPassphraseFromEncrypted(String encryptionKey, boolean gDrive) {
        String encryptedMsg = FileUtils.getSeedFromFile(getActivity(), gDrive,false);
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

            String validation = "";

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