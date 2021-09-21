package io.certifico.app.ui.settings;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.certifico.app.BuildConfig;
import io.certifico.app.R;
import io.certifico.app.data.bitcoin.BitcoinManager;
import io.certifico.app.data.inject.Injector;
import io.certifico.app.databinding.FragmentSettingsBinding;
import io.certifico.app.ui.LMActivity;
import io.certifico.app.ui.LMFragment;
import io.certifico.app.ui.LMWebActivity;
import io.certifico.app.ui.home.AboutActivity;
import io.certifico.app.ui.cert.AddCertificateActivity;
import io.certifico.app.ui.issuer.AddIssuerActivity;
import io.certifico.app.ui.lock.SetPasswordActivity;
import io.certifico.app.ui.onboarding.OnboardingActivity;
import io.certifico.app.ui.settings.passphrase.RevealPassphraseActivity;
import io.certifico.app.util.DialogUtils;
import io.certifico.app.util.FileLoggingTree;
import io.certifico.app.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.inject.Inject;

import timber.log.Timber;


public class SettingsFragment extends LMFragment {

    @Inject
    protected BitcoinManager mBitcoinManager;

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Injector.obtain(getContext())
                .inject(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FragmentSettingsBinding binding = DataBindingUtil.inflate(inflater,
                R.layout.fragment_settings,
                container,
                false);

        binding.settingsRevealPassphraseTextView.setOnClickListener(v -> {
            Timber.i("My passphrase tapped in settings");
            Intent intent = RevealPassphraseActivity.newIntent(getContext());
            startActivity(intent);
        });

        setupReplacePassphrase(binding);

        binding.settingsAddIssuerTextView.setOnClickListener(v -> {
            Timber.i("Add Issuer tapped in settings");
            Intent intent = AddIssuerActivity.newIntent(getContext());
            startActivity(intent);
        });

        binding.settingsAddCredentialTextView.setOnClickListener(v -> {
            Timber.i("Add Credential tapped in settings");
            DialogUtils.showCustomSheet(getContext(), this,
                    R.layout.dialog_add_by_file_or_url,
                    0,
                    "",
                    "",
                    "",
                    "",
                    (btnIdx) -> {
                        if ((int)btnIdx == 0) {
                            Timber.i("Add Credential from URL tapped in settings");
                        } else {
                            Timber.i("User has chosen to add a certificate from file");
                        }
                        Intent intent = AddCertificateActivity.newIntent(getContext(), (int)btnIdx, null);
                        startActivity(intent);
                        return null;
                    },
                    (dialogContent) -> {
                        return null;
                    });

        });

        binding.settingsEmailLogsTextView.setOnClickListener(v -> {
            Timber.i("Share device logs");
            FileLoggingTree.saveLogToFile(getContext());

            File file = FileUtils.getLogsFile(getContext(), false);

            Uri fileUri = FileProvider.getUriForFile(
                    getContext(),
                    "io.certifico.app.fileprovider",
                    file);


            //send file using email
            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            // the attachment
            String type = getContext().getContentResolver()
                    .getType(fileUri);
            emailIntent.setType(type);
            emailIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            // the mail subject
            emailIntent .putExtra(Intent.EXTRA_SUBJECT, "Logcat content for Chertero");
            emailIntent.setType("message/rfc822");
            startActivity(Intent.createChooser(emailIntent , "Send email..."));
        });

        binding.settingsBackupGdriveTextView.setOnClickListener(v -> {
            Timber.i("Backup to Google Drive tapped in settings");
            mBitcoinManager.getPassphrase().subscribe(mPassphrase -> {
                ((LMActivity)getActivity()).askToSavePassphraseToGoogleDrive(mPassphrase, (loading) -> {
                    if ((boolean) loading) {
                        displayProgressDialog(R.string.onboarding_passphrase_save_gdrive_progress);
                    } else {
                        hideProgressDialog();
                    }
                    return null;
                },(passphrase) -> {
                    Timber.i("Sync.BackupPassphraseFragment onGdrive() -> " + passphrase);
                    if(passphrase == null) {

                        DialogUtils.showAlertDialog(getContext(), this,
                                R.drawable.ic_dialog_failure,
                                getResources().getString(R.string.onboarding_passphrase_permissions_error_title),
                                getResources().getString(R.string.onboarding_passphrase_permissions_error_gdrive),
                                getResources().getString(R.string.ok_button),
                                null,
                                (btnIdx) -> null);
                        return null;
                    }

                    DialogUtils.showAlertDialog(getContext(), this,
                            R.drawable.ic_dialog_success,
                            getResources().getString(R.string.onboarding_passphrase_complete_title),
                            getResources().getString(R.string.onboarding_passphrase_save_gdrive_complete),
                            getResources().getString(R.string.ok_button),
                            null,
                            (btnIdx) -> null);
                    return null;
                });

            });

        });

        binding.settingsAboutPassphraseTextView.setOnClickListener(v -> {
            Timber.i("About passphrase tapped in settings");
            String actionBarTitle = getString(R.string.about_passphrases_title);
            String endPoint = getString(R.string.about_passphrases_endpoint);
            Intent intent = LMWebActivity.newIntent(getContext(), actionBarTitle, endPoint);
            startActivity(intent);
        });

        binding.settingsPrivacyPolicyTextView.setOnClickListener(v -> {
            Timber.i("Privacy statement tapped in settings");
            String actionBarTitle = getString(R.string.settings_privacy_policy);
            String endPoint = getString(R.string.settings_privacy_policy_endpoint);
            Intent intent = LMWebActivity.newIntent(getContext(), actionBarTitle, endPoint);
            startActivity(intent);
        });

        binding.settingsLearnMore.setOnClickListener(v -> {
            Timber.i("Sync Settings tapped in settings");
            String actionBarTitle = getString(R.string.settings_learn_more);
            Intent intent = LearnMoreActivity.newIntent(getContext(), actionBarTitle);
            startActivity(intent);
        });

        binding.settingsAbout.setOnClickListener(v -> {
            Timber.i("About tapped in settings");
            String actionBarTitle = getString(R.string.settings_about);
            Intent intent = AboutActivity.newIntent(getContext(), actionBarTitle);
            startActivity(intent);
        });

        return binding.getRoot();
    }

    @Override
    public void onStop() {
        Timber.i("Dismissing the settings screen");
        super.onStop();
    }

    private void deleteSavedEncryptionKey() {
        KeyStore keyStore = null;
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore");
        } catch (KeyStoreException e) {
            Timber.e(e);
        }
        try {
            keyStore.load(null);
        } catch (IOException | NoSuchAlgorithmException | CertificateException e) {
            Timber.e(e);
        }

        try {
            keyStore.deleteEntry(SetPasswordActivity.KEYSTORE_ENCRYPTION_KEY_ALIAS);
        } catch (KeyStoreException e) {
            Timber.e(e);
        }

    }

    private void setupReplacePassphrase(FragmentSettingsBinding binding) {

        if (BuildConfig.DEBUG == false) {
            binding.settingsLogoutSeparator.setVisibility(View.GONE);
            binding.settingsLogout.setVisibility(View.GONE);
            return;
        }

        binding.settingsLogout.setOnClickListener(v -> {

            DialogUtils.showAlertDialog(getContext(), this,
                    R.drawable.ic_dialog_failure,
                    getResources().getString(R.string.settings_logout_title),
                    getResources().getString(R.string.settings_logout_message),
                    getResources().getString(R.string.settings_logout_button_title),
                    getResources().getString(R.string.onboarding_passphrase_cancel),
                    (btnIdx) -> {
                        if((int)btnIdx == 1) {
                            mBitcoinManager.resetEverything();
                            deleteSavedEncryptionKey();
                            Intent intent = new Intent(getActivity(), OnboardingActivity.class);
                            startActivity(intent);
                            getActivity().finish();
                        }
                        return null;
                    });


        });
    }

}


