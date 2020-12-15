package io.certifico.app.ui.lock;

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

import com.fluidcerts.android.app.R;
import io.certifico.app.data.inject.Injector;

import com.fluidcerts.android.app.databinding.FragmentSetPasswordBinding;
import io.certifico.app.ui.LMActivity;
import io.certifico.app.ui.LMFragment;

public class SetPasswordFragment extends LMFragment {

    private Boolean mValidPassword = false;

    private FragmentSetPasswordBinding mBinding;

    public static SetPasswordFragment newInstance() {
        return new SetPasswordFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        Injector.obtain(getContext())
                .inject(this);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_set_password, container, false);
        mBinding.passwordEditText.addTextChangedListener(new UpdatePasswordTextWatcher());
        mBinding.confirmPasswordText.addTextChangedListener(new UpdatePasswordTextWatcher());

        mBinding.setPasswordButton.setOnClickListener(v -> {

            String password = mBinding.passwordEditText.getText().toString();

            if (mValidPassword) {
                Intent intent = new Intent();
                intent.putExtra("encryptionKey", password);
                getActivity().setResult(LMActivity.REQUEST_CODE_SET_ENCRYPTION_KEY, intent);
                getActivity().finish();
            }
        });

        return mBinding.getRoot();
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
}