package com.fluidcerts.android.app.data.drive;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Pair;

import java.util.Observer;

import timber.log.Timber;

public class GoogleDriveHelper {
    private static final String TAG = "Sync.GoogleDriveHelper ";

    public static final int BACKUP_CODE = 1;
    public static final int RESTORE_CODE = 2;
    public static final int RESOLVE_SIGN_IN_CODE = 3;

    public static final String SEED_BACKUP_PARENTS = "seeds";
    public static final String SEED_BACKUP_FILENAME = "learningmachine.dat";

    public static final String DB_BACKUP_PARENTS = "db";
    public static final String DB_BACKUP_FILENAME = "db.sqlite3";

    @Nullable
    private static GoogleDriveServiceImpl sGoogleDriveServiceImpl;

    public static void connectAndStartOperation(final Activity activity, Observer observer, final Pair<Integer, Bundle> extra) {
        if (sGoogleDriveServiceImpl == null) {
            sGoogleDriveServiceImpl = new GoogleDriveServiceImpl(activity);
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

//    public static String asyncResult() {
//        return sGoogleDriveServiceImpl.mAsyncResult;
//    }

    public static void handleActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (sGoogleDriveServiceImpl != null) sGoogleDriveServiceImpl.handleActivityResult(requestCode, resultCode, resultData);
    }
    
}
