package io.certifico.app.ui.splash;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;

import io.certifico.app.data.bitcoin.BitcoinManager;
import io.certifico.app.data.inject.Injector;
import io.certifico.app.data.preferences.SharedPreferencesManager;
import io.certifico.app.data.url.LaunchData;
import io.certifico.app.data.url.SplashUrlDecoder;
import io.certifico.app.ui.LMActivity;
import io.certifico.app.ui.home.HomeActivity;
import io.certifico.app.ui.lock.LockScreenActivity;
import io.certifico.app.ui.onboarding.OnboardingActivity;

import javax.inject.Inject;

import timber.log.Timber;

import static io.certifico.app.data.url.LaunchType.ADD_CERTIFICATE;
import static io.certifico.app.data.url.LaunchType.ADD_ISSUER;

public class SplashActivity extends LMActivity {

    @Inject SharedPreferencesManager mSharedPreferencesManager;
    @Inject protected BitcoinManager mBitcoinManager;

    private Uri mData;
    private LaunchData mLaunchData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Injector.obtain(this)
                .inject(this);

        Intent intent = getIntent();
        mData = intent.getData();
        String uriString = (mData == null) ? null : mData.toString();

        mLaunchData = SplashUrlDecoder.getLaunchType(uriString);

        // Note: If we have not "logged into" an account yet, then we need to force the user into onboarding
        if(mSharedPreferencesManager.isFirstLaunch() || mSharedPreferencesManager.shouldShowWelcomeBackUserFlow()) {
            if(mLaunchData.getLaunchType() == ADD_ISSUER) {
                mSharedPreferencesManager.setDelayedIssuerURL(mLaunchData.getChain(), mLaunchData.getIntroUrl(), mLaunchData.getNonce());
            }
            if(mLaunchData.getLaunchType() == ADD_CERTIFICATE) {
                mSharedPreferencesManager.setDelayedCertificateURL(mLaunchData.getCertUrl());
            }
            startActivityAndFinish(new Intent(this, OnboardingActivity.class));
            return;
        }

        Intent lockIntent = new Intent(this, LockScreenActivity.class);
        startActivityForResult(lockIntent, 0);
    }

    private void startActivityAndFinish(Intent intent) {
        startActivity(intent);
        finish();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);

        if (resultCode == Activity.RESULT_OK) {
            switch (mLaunchData.getLaunchType()) {

                case ONBOARDING:
                case MAIN:
                    Timber.i("Application was launched from a user activity.");
                    startActivityAndFinish(new Intent(this, HomeActivity.class));
                    break;

                case ADD_ISSUER:
                    Timber.i("Application was launched with this url: " + mData.toString());
                    Intent issuerIntent = HomeActivity.newIntentForIssuer(this,
                            mLaunchData.getChain(),
                            mLaunchData.getIntroUrl(),
                            mLaunchData.getNonce());
                    issuerIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
                    startActivityAndFinish(issuerIntent);
                    break;

                case ADD_CERTIFICATE:
                    Timber.i("Application was launched with this url: " + mData.toString());
                    Intent certificateIntent = HomeActivity.newIntentForCert(this, mLaunchData.getCertUrl());
                    certificateIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
                    startActivityAndFinish(certificateIntent);
                    break;
            }
        }
    }

}
