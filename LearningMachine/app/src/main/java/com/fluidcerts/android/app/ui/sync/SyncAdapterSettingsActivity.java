package com.fluidcerts.android.app.ui.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.DateUtils;
import android.util.Pair;

import com.fluidcerts.android.app.R;

import com.fluidcerts.android.app.data.drive.GoogleDriveHelper;
import com.fluidcerts.android.app.data.drive.GoogleDriveServiceImpl;
import com.fluidcerts.android.app.data.inject.Injector;
import com.fluidcerts.android.app.data.preferences.SharedPreferencesManager;
import com.fluidcerts.android.app.ui.LMSingleFragmentActivity;

import java.util.Observer;

import javax.inject.Inject;

import timber.log.Timber;

public class SyncAdapterSettingsActivity extends LMSingleFragmentActivity {

    private final String TAG = "Sync.SyncAdapterSettingsActivity ";
    private static final int SYNC_INTERVAL = (int) (DateUtils.DAY_IN_MILLIS / 1000); // Once per day

    private static final String EXTRA_ACTION_BAR_TITLE = "LMWebActivity.ActionBarTitle";

    public ContentResolver mResolver;
    private Observer mDriveCallbackObserver;
    @Inject SharedPreferencesManager mSharedPreferencesManager;

    public static Intent newIntent(Context context, String actionBarTitle) {
        Intent intent = new Intent(context, SyncAdapterSettingsActivity.class);
        intent.putExtra(EXTRA_ACTION_BAR_TITLE, actionBarTitle);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Injector.obtain(this)
                .inject(this);
        setupActionBar();
        mResolver = getContentResolver();
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        Timber.i(TAG + "onActivityResult() -> Deffering " + requestCode);
        GoogleDriveHelper.handleActivityResult(requestCode, resultCode, resultData);
    }

    private Account getSyncAccount(Context context) {
        AccountManager accountManager = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
        String accountType = context.getResources().getString(R.string.sync_account_type);
        Account[] accounts = accountManager.getAccountsByType(accountType);
        if (accounts.length == 0) {
            Timber.d(TAG + "Must have a Google account installed");
            return null;
        }
        return accounts[0];
    }

//    public void enableSync() {
//        mDriveCallbackObserver = (observable, o) -> {
//            if (observable instanceof GoogleDriveServiceImpl) {
//                Timber.i(TAG + "enableSyncObserver -> update()");
//                GoogleDriveServiceImpl service = (GoogleDriveServiceImpl) observable;
//                String fileId = service.getAsyncResult();
//                if (fileId != null) {
//                    Timber.i(TAG + "sync successful -> " + fileId);
//                    configureSyncAdapter();
//                    mSharedPreferencesManager.setSyncAdapterEnabled(true);
//                    return;
//                }
//                Timber.i(TAG + "sync failed");
//            }
//        };
//        Bundle extra = new Bundle();
//        GoogleDriveHelper.connectAndStartOperation(this,
//                mDriveCallbackObserver,
//                new Pair<>(GoogleDriveHelper.BACKUP_DB_CODE, extra));
//    }

    public void backupCerts() {
        mDriveCallbackObserver = (observable, o) -> {
            if (observable instanceof GoogleDriveServiceImpl) {
                Timber.i(TAG + "enableSyncObserver -> update()");
                GoogleDriveServiceImpl service = (GoogleDriveServiceImpl) observable;
                String fileId = service.getAsyncResult();
                if (fileId != null) {
                    Timber.i(TAG + "sync successful -> " + fileId);
                    return;
                }
                Timber.i(TAG + "sync failed");
            }
        };
        Bundle extra = new Bundle();
        GoogleDriveHelper.connectAndStartOperation(this,
                mDriveCallbackObserver,
                new Pair<>(GoogleDriveHelper.BACKUP_CERTS_CODE, extra));
    }

    public void restoreCerts() {
        mDriveCallbackObserver = (observable, o) -> {
            if (observable instanceof GoogleDriveServiceImpl) {
                Timber.i(TAG + "enableSyncObserver -> update()");
                GoogleDriveServiceImpl service = (GoogleDriveServiceImpl) observable;
                String fileId = service.getAsyncResult();
                if (fileId != null) {
                    Timber.i(TAG + "sync successful -> " + fileId);
                    return;
                }
                Timber.i(TAG + "sync failed");
            }
        };
        Bundle extra = new Bundle();
        GoogleDriveHelper.connectAndStartOperation(this,
                mDriveCallbackObserver,
                new Pair<>(GoogleDriveHelper.RESTORE_CERTS_CODE, extra));
    }

    private void configureSyncAdapter() {
        Timber.i(TAG + "configuring SyncAdapter");
        Account account = getSyncAccount(this);
        String syncAuthority = this.getResources().getString(R.string.sync_authority);
        ContentResolver.setIsSyncable(account, syncAuthority, 1);
        Timber.i(TAG + "SyncAdapter syncable");
        ContentResolver.setSyncAutomatically(account, syncAuthority, true);
        Timber.i(TAG + "SyncAdapter sync automatically");
        ContentResolver.addPeriodicSync(account, syncAuthority, Bundle.EMPTY, SYNC_INTERVAL);
        Timber.v(TAG + "Periodic sync configured with " + SYNC_INTERVAL + " interval");
    }

    public void runSyncAdapter() {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        String syncAuthority = this.getResources().getString(R.string.sync_authority);
        ContentResolver.requestSync(getSyncAccount(this), syncAuthority, bundle);
    }
}