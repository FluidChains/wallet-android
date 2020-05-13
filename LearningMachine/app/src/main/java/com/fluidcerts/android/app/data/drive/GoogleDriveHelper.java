package com.fluidcerts.android.app.data.drive;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Pair;

import java.util.Observer;

public class GoogleDriveHelper {

    public static final int BACKUP_CODE = 1;
    public static final int RESTORE_CODE = 2;
    public static final int RESOLVE_SIGN_IN_CODE = 3;

    public static final String SEED_BACKUP_FILENAME = "learningmachine.dat";

    @Nullable
    private static GoogleDriveServiceImpl sGoogleDriveServiceImpl;

    public static void connectAndStartOperation(final Activity activity, Observer observer, final Pair<Integer, Bundle> extra) {
        if (sGoogleDriveServiceImpl == null) {
            sGoogleDriveServiceImpl = new GoogleDriveServiceImpl(activity);
        }
        sGoogleDriveServiceImpl.addObserver(observer);
        sGoogleDriveServiceImpl.connectAndStartOperation(extra);
    }

    public static void disconnect() {
        if (sGoogleDriveServiceImpl != null) {
            sGoogleDriveServiceImpl.disconnect();
            sGoogleDriveServiceImpl = null;
        }
    }

    public static String asyncResult() {
        return sGoogleDriveServiceImpl.mAsyncResult;
    }

    public static void handleActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (sGoogleDriveServiceImpl != null) sGoogleDriveServiceImpl.handleActivityResult(requestCode, resultCode, resultData);
    }
    
}
