package io.certifico.app.ui.onboarding;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.certifico.app.R;
import io.certifico.app.databinding.FragmentAccountChooserBinding;
import io.certifico.app.ui.home.HomeActivity;
import io.certifico.app.ui.video.VideoActivity;
import io.certifico.app.util.DialogUtils;
import com.smallplanet.labalib.Laba;

import rx.functions.Action1;
import timber.log.Timber;

public class AccountChooserFragment extends OnboardingFragment {

    private Callback mCallback;
    private FragmentAccountChooserBinding mBinding;

    public interface Callback {
        void onNewAccount();

        void onExistingAccount(boolean isGoogleFlow);

        void onExistingDriveAccount(Action1<Boolean> loadingAction, Action1<Boolean> doneAction);
    }

    public static AccountChooserFragment newInstance() {
        return new AccountChooserFragment();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mCallback = (Callback) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_account_chooser, container, false);

        Laba.Animate(mBinding.newAccountButton, "!^300", () -> {
            return null;
        });
        Laba.Animate(mBinding.existingAccountButton, "!^300", () -> {
            return null;
        });
        Laba.Animate(mBinding.existingAccountGdriveButton, "!^300", () -> {
            return null;
        });

        String fileName = "android.resource://" + getActivity().getPackageName() + "/raw/background";

        mBinding.backgroundVideoCover.setAlpha(1.0f);
        mBinding.backgroundVideo.setVideoURI(Uri.parse(fileName));
        mBinding.backgroundVideo.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                Timber.d("SETTING VIDEO SCALING MODE");
                mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
                mp.setLooping(true);
                mp.setScreenOnWhilePlaying(false);

                Laba.Animate(mBinding.backgroundVideoCover, "d1|f0", null);
            }
        });
        mBinding.backgroundVideo.start();


        mBinding.playVideo.setOnClickListener(view2 -> {
            startActivity(new Intent(getContext(), VideoActivity.class));
        });

        mBinding.newAccountButton.setOnClickListener(view -> mCallback.onNewAccount());
        mBinding.existingAccountButton.setOnClickListener(view -> mCallback.onExistingAccount(false));
        mBinding.existingAccountGdriveButton.setOnClickListener(view -> mCallback.onExistingAccount(true));
//        mBinding.existingAccountGdriveButton.setOnClickListener(view -> {
//            mCallback.onExistingDriveAccount(this::loadingGDrive, this::onDone);
//        });

        mSharedPreferencesManager.setFirstLaunch(true);

        return mBinding.getRoot();
    }

    private void loadingGDrive(boolean display) {
        if (display)
            displayProgressDialog(R.string.onboarding_passphrase_load_gdrive_progress);
        else hideProgressDialog();
    }

    private void onDone(Boolean successful) {
        if (!successful) {
            backupNotFound(getResources().getString(R.string.error_passphrase_backup_not_found_gdrive));
            return;
        }

        if (isVisible()) {
            // if we return to the app by pasting in our passphrase, we
            // must have already backed it up!
            mSharedPreferencesManager.setHasSeenBackupPassphraseBefore(true);
            mSharedPreferencesManager.setWasReturnUser(true);
            mSharedPreferencesManager.setFirstLaunch(false);
            if (continueDelayedURLsFromDeepLinking() == false) {
                startActivity(new Intent(getActivity(), HomeActivity.class));
                getActivity().finish();
            }
        }
    }

    private void backupNotFound(String message) {
        DialogUtils.showAlertDialog(getContext(), this,
                R.drawable.ic_dialog_failure,
                getResources().getString(R.string.error_passphrase_backup_not_found_title),
                message,
                null,
                getResources().getString(R.string.ok_button),
                (btnIdx) -> {
                    return null;
                });
    }

    @Override
    public void onResume() {
        super.onResume();

        mBinding.backgroundVideoCover.setAlpha(1.0f);
        mBinding.backgroundVideo.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        mBinding.backgroundVideoCover.setAlpha(1.0f);
        mBinding.backgroundVideo.stopPlayback();

    }

    @Override
    public void onStop() {
        super.onStop();
        mBinding.backgroundVideoCover.setAlpha(1.0f);
        mBinding.backgroundVideo.stopPlayback();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallback = null;
    }
}
