package com.fluidcerts.android.app.data.inject;

import com.fluidcerts.android.app.LMApplication;
import com.fluidcerts.android.app.data.drive.DriveSyncAdapter;
import com.fluidcerts.android.app.data.drive.GoogleDriveServiceImpl;
import com.fluidcerts.android.app.ui.LMFragment;
import com.fluidcerts.android.app.ui.LMIssuerBaseFragment;
import com.fluidcerts.android.app.ui.cert.AddCertificateFileFragment;
import com.fluidcerts.android.app.ui.cert.AddCertificateURLFragment;
import com.fluidcerts.android.app.ui.cert.CertificateFragment;
import com.fluidcerts.android.app.ui.cert.CertificateInfoFragment;
import com.fluidcerts.android.app.ui.cert.VerifyCertificateFragment;
import com.fluidcerts.android.app.ui.home.HomeFragment;
import com.fluidcerts.android.app.ui.issuer.AddIssuerFragment;
import com.fluidcerts.android.app.ui.issuer.IssuerActivity;
import com.fluidcerts.android.app.ui.issuer.IssuerFragment;
import com.fluidcerts.android.app.ui.issuer.IssuerInfoActivity;
import com.fluidcerts.android.app.ui.issuer.IssuerInfoFragment;
import com.fluidcerts.android.app.ui.onboarding.OnboardingActivity;
import com.fluidcerts.android.app.ui.onboarding.OnboardingFragment;
import com.fluidcerts.android.app.ui.onboarding.PastePassphraseFragment;
import com.fluidcerts.android.app.ui.onboarding.ViewPassphraseFragment;
import com.fluidcerts.android.app.ui.onboarding.BackupPassphraseFragment;
import com.fluidcerts.android.app.ui.settings.SettingsFragment;
import com.fluidcerts.android.app.ui.settings.passphrase.RevealPassphraseFragment;
import com.fluidcerts.android.app.ui.splash.SplashActivity;
import com.fluidcerts.android.app.ui.sync.SyncAdapterSettingsActivity;
import com.fluidcerts.android.app.ui.sync.SyncAdapterSettingsFragment;

public interface LMGraph {
    void inject(LMApplication application);

    // Activities
    void inject(SplashActivity activity);
    void inject(IssuerActivity activity);
    void inject(IssuerInfoActivity activity);
    void inject(OnboardingActivity activity);
    void inject(SyncAdapterSettingsActivity activity);

    // Fragments
    void inject(ViewPassphraseFragment fragment);
    void inject(BackupPassphraseFragment fragment);
    void inject(PastePassphraseFragment fragment);
    void inject(HomeFragment fragment);
    void inject(RevealPassphraseFragment fragment);
    void inject(SettingsFragment fragment);
    void inject(AddIssuerFragment fragment);
    void inject(LMIssuerBaseFragment fragment);
    void inject(IssuerFragment fragment);
    void inject(OnboardingFragment fragment);
    void inject(CertificateFragment fragment);
    void inject(LMFragment fragment);
    void inject(VerifyCertificateFragment fragment);
    void inject(AddCertificateURLFragment fragment);
    void inject(AddCertificateFileFragment fragment);
    void inject(IssuerInfoFragment fragment);
    void inject(CertificateInfoFragment fragment);
    void inject(SyncAdapterSettingsFragment fragment);

    // Service
    void inject(GoogleDriveServiceImpl service);
}
