package com.learningmachine.android.app.data.backup;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import java.util.Collections;

import timber.log.Timber;

public class DriveSyncAdapter extends AbstractThreadedSyncAdapter {

    private static final String TAG = "Sync.SyncAdapter ";

    private Context mContext;
    private ContentResolver mContentResolver;

    private Drive mGoogleDriveService;
    private DriveServiceHelper mDriveServiceHelper;

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

    @Override
    public void onPerformSync(Account account, Bundle bundle, String s, ContentProviderClient contentProviderClient, SyncResult syncResult) {
        Timber.i(TAG + "Starting synchronization...");
        if (mGoogleDriveService == null) {
            initGoogleDriveService(mContext, account.name);
        }
        syncWallet();
    }

    private void initGoogleDriveService(Context context, String accountName) {
        // Check for authorized account
        GoogleSignInAccount googleAccount = GoogleSignIn.getLastSignedInAccount(context);
        if (googleAccount == null) {
            Timber.i(TAG + "No authorized account found, returning.");
            return;
        }

        // Use the authenticated account to sign in to the Drive service.
        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                                                context,
                                                Collections.singleton(DriveScopes.DRIVE_FILE));
        credential.setSelectedAccount(googleAccount.getAccount());
        mGoogleDriveService = new Drive.Builder(
                                    AndroidHttp.newCompatibleTransport(),
                                    new GsonFactory(),
                                    credential)
                                    .setApplicationName(accountName)
                                    .build();
    }

    private void syncWallet() {
        Timber.i(TAG + "syncWallet()");
    }

    private void readFile(String fileId) {
        if (mDriveServiceHelper != null) {
            Timber.d(TAG + "Reading file " + fileId);

            mDriveServiceHelper.readFile(fileId)
                    .addOnSuccessListener(nameAndContent -> {
                        String name = nameAndContent.first;
                        String content = nameAndContent.second;

                        Timber.i(name);
                        Timber.i(content);
                    })
                    .addOnFailureListener(exception ->
                            Timber.e(TAG + "Couldn't read file.", exception));
        }
    }
}
