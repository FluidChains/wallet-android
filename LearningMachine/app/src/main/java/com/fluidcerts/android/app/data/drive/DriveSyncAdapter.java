package com.fluidcerts.android.app.data.drive;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Pair;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import java.util.Collections;
import java.util.Observer;

import timber.log.Timber;

public class DriveSyncAdapter extends AbstractThreadedSyncAdapter {

    private static final String TAG = "Sync.SyncAdapter ";

    private Context mContext;
    private ContentResolver mContentResolver;
    private Observer mDriveCallbackObserver;

    public DriveSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        mContext = context;
        mContentResolver = context.getContentResolver();
    }

    public DriveSyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        mContext = context;
        mContentResolver = context.getContentResolver();
    }

    private Drive initGoogleDriveService() {
        // Check for authorized account
        GoogleSignInAccount googleAccount = GoogleSignIn.getLastSignedInAccount(mContext);
        if (googleAccount == null) {
            Timber.i(TAG + "No authorized account found, returning.");
            return null;
        } else {
            Timber.i(TAG + "%s", googleAccount.getAccount());
        }

        // Use the authenticated account to sign in to the Drive service.
        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                mContext,
                Collections.singleton(DriveScopes.DRIVE_FILE));
        credential.setSelectedAccount(googleAccount.getAccount());
        return new Drive.Builder(
                AndroidHttp.newCompatibleTransport(),
                new GsonFactory(),
                credential)
                .setApplicationName("FluidCerts")
                .build();
    }

    @Override
    public void onPerformSync(Account account, Bundle bundle, String s, ContentProviderClient contentProviderClient, SyncResult syncResult) {
        Timber.i(TAG + "Starting synchronization...");
        syncWallet();
    }

    private void syncWallet() {
        Timber.i(TAG + "syncWallet()");
        mDriveCallbackObserver = (observable, o) -> {
            if (observable instanceof GoogleDriveServiceImpl) {
                Timber.i(TAG + "syncObserver -> update()");
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
        Drive driveService = initGoogleDriveService();
        GoogleDriveHelper.connectAndStartOperation(driveService,
                mDriveCallbackObserver,
                new Pair<>(GoogleDriveHelper.BACKUP_CERTS_CODE, extra));
    }

}
