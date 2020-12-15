package io.certifico.app.ui.onboarding;

import android.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.view.ViewPager;

import com.fluidcerts.android.app.R;
import io.certifico.app.data.CertificateManager;
import io.certifico.app.data.bitcoin.BitcoinManager;
import io.certifico.app.data.inject.Injector;
import io.certifico.app.data.preferences.SharedPreferencesManager;
import com.fluidcerts.android.app.databinding.ActivityOnboardingBinding;
import io.certifico.app.ui.LMActivity;
import io.certifico.app.ui.onboarding.OnboardingFlow.FlowType;

import javax.inject.Inject;

import rx.functions.Action1;
import timber.log.Timber;

public class OnboardingActivity extends LMActivity implements AccountChooserFragment.Callback {

    @Inject
    SharedPreferencesManager mSharedPreferencesManager;

    @Inject
    BitcoinManager mBitcoinManager;

    @Inject
    CertificateManager mCertificateManager;

    private static final String SAVED_FLOW = "onboardingFlow";

    private OnboardingFlow mOnboardingFlow;
    private OnboardingAdapter mAdapter;
    private ActivityOnboardingBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Injector.obtain(this)
                .inject(this);

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_onboarding);
        mBinding.onboardingViewPager.addOnPageChangeListener(mOnPageChangeListener);

        if (savedInstanceState == null) {
            if (mSharedPreferencesManager.shouldShowWelcomeBackUserFlow()) {
                mOnboardingFlow = new OnboardingFlow(FlowType.BACKUP_ONLY);
            } else {
                mOnboardingFlow = new OnboardingFlow(FlowType.UNKNOWN);
            }
        } else {
            mOnboardingFlow = (OnboardingFlow) savedInstanceState.getSerializable(SAVED_FLOW);
        }

        setupAdapter();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(SAVED_FLOW, mOnboardingFlow);
    }

    @Override
    public void onBackPressed() {
        if (navigateBackward()) {
            return;
        }

        //This is the last screen in the app.
        //Tapping back should close the app.
        finishAffinity();
    }

    public void onContinuePastWelcomeScreen() {
        navigateForward();
    }

    public void onBackupPassphrase() {
        navigateForward();
    }

    @Override
    public void onNewAccount() {
        replaceScreens(FlowType.NEW_ACCOUNT);
    }

    @Override
    public void onExistingAccount(boolean isGoogleFlow) {
        if (!isGoogleFlow) {
            replaceScreens(FlowType.EXISTING_ACCOUNT);
            return;
        }
        replaceScreens(FlowType.EXISTING_ACCOUNT_GOOGLE);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onExistingDriveAccount(Action1<Boolean> loadingAction, Action1<Boolean> onDoneAction) {
        Timber.i("[Drive] existing drive account");
//        askRestoreFromGoogleDrive(loadingAction, onDoneAction,
//                passphrase -> {
//                    Timber.i("[Drive] setting passphrase");
//                    return mBitcoinManager.setPassphrase(passphrase);
//                },
//                driveFile -> {
//                    Timber.i("[Drive] adding certificate: " + driveFile.name);
//                    return mCertificateManager.addCertificate(driveFile.stream);
//                });
    }

    private void setupAdapter() {
        mAdapter = new OnboardingAdapter(getSupportFragmentManager(), mOnboardingFlow.getScreens());
        mBinding.onboardingViewPager.setAdapter(mAdapter);
        mBinding.onboardingViewPager.setCurrentItem(mOnboardingFlow.getPosition());
    }

    private void replaceScreens(FlowType flowType) {
        mOnboardingFlow = new OnboardingFlow(flowType);
        mOnboardingFlow.setPosition(1);

        mAdapter.setScreens(mOnboardingFlow.getScreens());
        mBinding.onboardingViewPager.getAdapter().notifyDataSetChanged();
        mBinding.onboardingViewPager.setCurrentItem(mOnboardingFlow.getPosition());
    }

    private void navigateForward() {
        int position = mOnboardingFlow.getPosition() + 1;
        mBinding.onboardingViewPager.setCurrentItem(position);
        mOnboardingFlow.setPosition(position);
    }

    private boolean navigateBackward() {
        int position = mOnboardingFlow.getPosition();
        if (position <= 0) {
            return false;
        }

        OnboardingFragment currentFragment = mAdapter.getCurrentFragment();
        if (currentFragment != null && !currentFragment.isBackAllowed()) {
            // disallow back if the fragment requests it
            return true;
        }

        mOnboardingFlow.setPosition(position - 1);
        mBinding.onboardingViewPager.setCurrentItem(mOnboardingFlow.getPosition());
        return true;
    }

    private ViewPager.OnPageChangeListener mOnPageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int i, float v, int i1) {
        }

        @Override
        public void onPageSelected(int position) {
            mOnboardingFlow.setPosition(position);

            OnboardingFragment currentFragment = mAdapter.getCurrentFragment();
            if (currentFragment != null) {
                currentFragment.onUserVisible();
            }
        }

        @Override
        public void onPageScrollStateChanged(int i) {
        }
    };


    public boolean isOnAccountsScreen() {
        return mOnboardingFlow.getPosition() == 0;
    }


}
