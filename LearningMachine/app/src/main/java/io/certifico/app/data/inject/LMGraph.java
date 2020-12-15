package io.certifico.app.data.inject;

import io.certifico.app.LMApplication;
import io.certifico.app.ui.LMFragment;
import io.certifico.app.ui.LMIssuerBaseFragment;
import io.certifico.app.ui.cert.AddCertificateFileFragment;
import io.certifico.app.ui.cert.AddCertificateURLFragment;
import io.certifico.app.ui.cert.CertificateFragment;
import io.certifico.app.ui.cert.CertificateInfoFragment;
import io.certifico.app.ui.cert.VerifyCertificateFragment;
import io.certifico.app.ui.home.HomeFragment;
import io.certifico.app.ui.issuer.AddIssuerFragment;
import io.certifico.app.ui.issuer.IssuerActivity;
import io.certifico.app.ui.issuer.IssuerFragment;
import io.certifico.app.ui.issuer.IssuerInfoActivity;
import io.certifico.app.ui.issuer.IssuerInfoFragment;
import io.certifico.app.ui.lock.SetPasswordActivity;
import io.certifico.app.ui.lock.SetPasswordFragment;
import io.certifico.app.ui.onboarding.BackupPassphraseFragment;
import io.certifico.app.ui.onboarding.OnboardingActivity;
import io.certifico.app.ui.onboarding.OnboardingFragment;
import io.certifico.app.ui.onboarding.PastePassphraseFragment;
import io.certifico.app.ui.onboarding.ViewPassphraseFragment;
import io.certifico.app.ui.settings.SettingsFragment;
import io.certifico.app.ui.settings.passphrase.RevealPassphraseFragment;
import io.certifico.app.ui.splash.SplashActivity;
import io.certifico.app.ui.sync.SyncAdapterSettingsActivity;
import io.certifico.app.ui.sync.SyncAdapterSettingsFragment;

public interface LMGraph {
    void inject(LMApplication application);

    // Activities
    void inject(SplashActivity activity);
    void inject(IssuerActivity activity);
    void inject(IssuerInfoActivity activity);
    void inject(OnboardingActivity activity);
    void inject(SyncAdapterSettingsActivity activity);
    void inject(SetPasswordActivity activity);

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
    void inject(SetPasswordFragment fragment);
}
