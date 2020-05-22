package com.fluidcerts.android.app.data.drive;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Pair;

import com.fluidcerts.android.app.data.store.LMDatabaseHelper;
import com.google.api.services.drive.Drive;

import rx.Observable;
import java.util.Observer;

import timber.log.Timber;

public class GoogleDriveHelper {
    private static final String TAG = "Sync.GoogleDriveHelper ";

    public static final int BACKUP_SEED_CODE = 2;
    public static final int RESTORE_SEED_CODE = 3;
    public static final int BACKUP_CERTS_CODE = 4;
    public static final int RESTORE_CERTS_CODE = 5;
    public static final int RESOLVE_SIGN_IN_CODE = 1;

    public static final String SEED_BACKUP_PARENTS = "seeds";
    public static final String SEED_BACKUP_FILENAME = "learningmachine.dat";

    public static final String CERTS_BACKUP_PARENTS = "certs";


    @Nullable
    private static GoogleDriveServiceImpl sGoogleDriveServiceImpl;

//    public static Observable<String> connectAndStartOperation(final Activity activity, final Pair<Integer, Bundle> extra) {
//        if (sGoogleDriveServiceImpl == null || !sGoogleDriveServiceImpl.mActivityLaunched) {
//            Timber.d(TAG + "Recreating sGoogleDriveService");
//            sGoogleDriveServiceImpl = new GoogleDriveServiceImpl(activity);
//        }
//        reset();
//        sGoogleDriveServiceImpl.connectAndStartOperation(extra);
//    }

    public static void connectAndStartOperation(final Activity activity, final Observer observer, final Pair<Integer, Bundle> extra) {
        if (sGoogleDriveServiceImpl == null || !sGoogleDriveServiceImpl.mActivityLaunched) {
            Timber.d(TAG + "Recreating sGoogleDriveService");
            sGoogleDriveServiceImpl = new GoogleDriveServiceImpl(activity);
        }
        if (sGoogleDriveServiceImpl.countObservers() >= 1) {
            sGoogleDriveServiceImpl.deleteObservers();
        }
        reset();
        sGoogleDriveServiceImpl.addObserver(observer);
        sGoogleDriveServiceImpl.connectAndStartOperation(extra);
    }

    public static void connectAndStartOperation(final Drive driveService, final Observer observer, final Pair<Integer, Bundle> extra) {
        if (sGoogleDriveServiceImpl == null ) {
            Timber.d(TAG + "sGoogleDriveService DNE");
            sGoogleDriveServiceImpl = new GoogleDriveServiceImpl(driveService);
        }
        if (sGoogleDriveServiceImpl.countObservers() >= 1) {
            sGoogleDriveServiceImpl.deleteObservers();
        }
        reset();
        sGoogleDriveServiceImpl.addObserver(observer);
        sGoogleDriveServiceImpl.connectAndStartOperation(extra);
    }


    private static void reset() {
        Timber.d(TAG + "reset()");
        if (sGoogleDriveServiceImpl != null) {
            sGoogleDriveServiceImpl.reset();
        }
    }
    public static void disconnect() {
        if (sGoogleDriveServiceImpl != null) {
            sGoogleDriveServiceImpl.disconnect();
            sGoogleDriveServiceImpl = null;
        }
    }

    public static void handleActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (sGoogleDriveServiceImpl != null) sGoogleDriveServiceImpl.handleActivityResult(requestCode, resultCode, resultData);
    }
    
}
