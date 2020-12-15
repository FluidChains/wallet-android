package io.certifico.app.ui.sync;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;

import io.certifico.app.data.drive.GoogleDriveFile;
import io.certifico.app.ui.LMSingleFragmentActivity;

import java.io.File;
import java.util.function.Function;

import rx.Observable;
import rx.functions.Action1;
import timber.log.Timber;

public class SyncAdapterSettingsActivity extends LMSingleFragmentActivity {

    private final String TAG = "Sync.SyncAdapterSettingsActivity ";

    private static final String EXTRA_ACTION_BAR_TITLE = "LMWebActivity.ActionBarTitle";

    public static Intent newIntent(Context context, String actionBarTitle) {
        Intent intent = new Intent(context, SyncAdapterSettingsActivity.class);
        intent.putExtra(EXTRA_ACTION_BAR_TITLE, actionBarTitle);
        return intent;
    }

    @Override
    public Fragment createFragment() {
        return SyncAdapterSettingsFragment.newInstance();
    }

    public String getActionBarTitle() {
        return getIntent().getStringExtra(EXTRA_ACTION_BAR_TITLE);
    }

    @Override
    protected boolean requiresBackNavigation() {
        return true;
    }

    public void backupCerts(Action1<Boolean> loadingAction, Action1<Boolean> onDoneAction, Observable<File> getFilesObservable) {
        Timber.i("[Drive] Backing-up Certificates");
        askBackUpToGoogleDrive(loadingAction, onDoneAction, getFilesObservable);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void restoreCerts(Action1<Boolean> loadingAction, Action1<Boolean> onDoneAction, Function<GoogleDriveFile, Observable<String>> addCertificateFunc) {
        Timber.i("[Drive] Restoring Certificates");
        askToRestoreCertificates(loadingAction, onDoneAction, addCertificateFunc);
    }
}