package io.certifico.app.ui.sync;

import android.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.fluidcerts.android.app.R;
import io.certifico.app.data.CertificateManager;
import io.certifico.app.data.inject.Injector;
import io.certifico.app.data.preferences.SharedPreferencesManager;
import com.fluidcerts.android.app.databinding.FragmentSyncAdapterSettingsBinding;
import io.certifico.app.ui.LMFragment;
import io.certifico.app.util.DialogUtils;
import io.certifico.app.util.FileUtils;

import javax.inject.Inject;

import rx.Observable;
import timber.log.Timber;

public class SyncAdapterSettingsFragment extends LMFragment {

    private final String TAG = "Sync.SyncAdapterSettingsFragment ";

    @Inject
    SharedPreferencesManager mSharedPreferencesManager;

    @Inject
    CertificateManager mCertificateManager;

    private FragmentSyncAdapterSettingsBinding mBinding;

    public static SyncAdapterSettingsFragment newInstance() {
        return new SyncAdapterSettingsFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        Injector.obtain(getContext())
                .inject(this);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_sync_adapter_settings, container, false);

        mBinding.syncAdapterSettingsBackupButton.setOnClickListener(v -> {
            Timber.d(TAG + "Back-up Certificates button pressed");
            SyncAdapterSettingsActivity syncActivity = (SyncAdapterSettingsActivity) getActivity();
            if (syncActivity == null) return;

            syncActivity.backupCerts(this::loadingBackup, this::onBackupDone,
                    mCertificateManager.getCertificates()
                            .flatMap(cert -> Observable.just(FileUtils.getCertificateFile(getContext(), cert.getUuid()))));
        });
        mBinding.syncAdapterSettingsRestoreButton.setOnClickListener(v -> {
            Timber.d(TAG + "Restore Certificates button pressed");
            SyncAdapterSettingsActivity syncActivity = (SyncAdapterSettingsActivity) getActivity();
            if (syncActivity == null) return;

            syncActivity.restoreCerts(this::loadingRestoreCerts, this::onRestoreCertsDone,
                    driveFile -> {
                        Timber.i("[Drive] adding certificate: " + driveFile.name);
                        return mCertificateManager.addCertificate(driveFile.stream);
                    });
        });

        return mBinding.getRoot();
    }

    private void loadingBackup(boolean loading) {
        if (loading)
            displayProgressDialog(R.string.onboarding_passphrase_save_gdrive_progress);
        else hideProgressDialog();
    }

    private void onBackupDone(boolean successful) {
        Timber.i("[Drive] backup completed, result: " + successful);
        if (successful) {
            DialogUtils.showAlertDialog(getContext(), this,
                    R.drawable.ic_dialog_success,
                    getResources().getString(R.string.onboarding_passphrase_complete_title),
                    getResources().getString(R.string.onboarding_passphrase_complete_title),
                    getResources().getString(R.string.ok_button),
                    null,
                    btnIdx -> null);
            return;
        }

        DialogUtils.showAlertDialog(getContext(), this,
                R.drawable.ic_dialog_failure,
                getResources().getString(R.string.onboarding_passphrase_permissions_error_title),
                getResources().getString(R.string.onboarding_passphrase_permissions_error),
                getResources().getString(R.string.ok_button),
                null,
                btnIdx -> null);
    }

    private void loadingRestoreCerts(boolean loading) {
        if (loading)
            displayProgressDialog(R.string.onboarding_passphrase_load_gdrive_progress);
        else hideProgressDialog();
    }

    private void onRestoreCertsDone(boolean successful) {
        Timber.i("[Drive] backup completed, result: " + successful);
        if (successful) {
            DialogUtils.showAlertDialog(getContext(), this,
                    R.drawable.ic_dialog_success,
                    getResources().getString(R.string.fragment_sync_adapter_settings_restore_button),
                    getResources().getString(R.string.onboarding_passphrase_complete_title),
                    getResources().getString(R.string.ok_button),
                    null,
                    btnIdx -> null);
            return;
        }

        DialogUtils.showAlertDialog(getContext(), this,
                R.drawable.ic_dialog_failure,
                getResources().getString(R.string.onboarding_passphrase_permissions_error_title),
                getResources().getString(R.string.onboarding_passphrase_permissions_error),
                getResources().getString(R.string.ok_button),
                null,
                btnIdx -> null);
    }
}