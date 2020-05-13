package com.fluidcerts.android.app.data.drive;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Pair;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Observable;
import java.util.Set;

import timber.log.Timber;

public class GoogleDriveServiceImpl extends Observable implements OnSuccessListener<GoogleSignInAccount>, OnFailureListener {

    private final String TAG = "Sync.GoogleDriveServiceImpl ";
    private final int INVALID = 999;

    private WeakReference<Activity> mActivity;
    private Drive mDriveService;

    private int mRecovered = 0;
    public boolean mInterrupted;
    public String mAsyncResult;

    private int mNextGoogleApiOperation = INVALID;
    private Bundle mNextGoogleApiOperationBundle;

    public GoogleDriveServiceImpl(final Activity activity) {
        mActivity = new WeakReference<>(activity);
    }

    public final void disconnect() {
        mActivity = null;
        mNextGoogleApiOperation = INVALID;
        mNextGoogleApiOperationBundle = new Bundle();
    }

    public final void connectAndStartOperation(final Pair<Integer, Bundle> extra) {
        Timber.i(TAG + "connectAndStartOperation() -> " + extra.first);
        unpackExtras(extra);

        if (!isAuthenticated() || !hasPermissions()) {
            final GoogleSignInOptions signInOptions =
                    new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestEmail()
                            .requestScopes(new Scope(DriveScopes.DRIVE_APPDATA))
                            .build();

            final GoogleSignInClient client = GoogleSignIn.getClient(mActivity.get(), signInOptions);

            mActivity.get().startActivityForResult(client.getSignInIntent(), GoogleDriveHelper.RESOLVE_SIGN_IN_CODE);
        } else {
            onGoogleDriveConnected(mNextGoogleApiOperation);
//            mNextGoogleApiOperation = INVALID;
        }
    }

    public final void handleActivityResult(int requestCode, int resultCode, Intent resultData) {
        switch (requestCode) {
            case GoogleDriveHelper.RESOLVE_SIGN_IN_CODE:
                GoogleSignIn.getSignedInAccountFromIntent(resultData)
                        .addOnSuccessListener(this)
                        .addOnFailureListener(this);
                break;
        }
    }

//--------------------------------------------------------------------------------------------------
//  Event handlers
//--------------------------------------------------------------------------------------------------

    @Override
    public void onSuccess(GoogleSignInAccount googleAccount) {
        GoogleAccountCredential credential =
                GoogleAccountCredential.usingOAuth2(
                        mActivity.get(), Collections.singleton(DriveScopes.DRIVE_APPDATA));
        credential.setSelectedAccount(googleAccount.getAccount());
        mDriveService = new Drive.Builder(
                AndroidHttp.newCompatibleTransport(),
                new GsonFactory(),
                credential)
                .setApplicationName("FluidCerts")
                .build();
        onGoogleDriveConnected(mNextGoogleApiOperation);
//        mNextGoogleApiOperation = INVALID;
    }

    @Override
    public void onFailure(@NonNull Exception e) {
        mNextGoogleApiOperation = INVALID;
    }

    private void onGoogleDriveConnected(final int operation) {
        switch (operation) {
            case GoogleDriveHelper.BACKUP_CODE:
                Timber.i(TAG + "onGoogleDriveConnected() -> BACKUP");
                onBackupToDriveAsync(mNextGoogleApiOperationBundle.getString("encrypted"));
                break;

            case GoogleDriveHelper.RESTORE_CODE:
                Timber.i(TAG + "onGoogleDriveConnected() -> RESTORE");
                onRestoreFromDriveAsync();
                break;

            default:
                Timber.i(TAG + "onGoogleDriveConnected() -> INVALID");
        }
    }

//--------------------------------------------------------------------------------------------------
//  Private methods
//--------------------------------------------------------------------------------------------------

    private boolean isAuthenticated() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(mActivity.get());
        return account != null;
    }

    private boolean hasPermissions() {
        Set<Scope> scopes = GoogleSignIn.getLastSignedInAccount(mActivity.get()).getGrantedScopes();
        for (Scope scope : scopes) {
            if (scope == new Scope(DriveScopes.DRIVE_APPDATA)) {
                return true;
            }
        }
        return false;
    }

    private void unpackExtras(Pair<Integer, Bundle> extra) {
        mNextGoogleApiOperation = extra.first;
        mNextGoogleApiOperationBundle = extra.second;
    }

    private void setmAsyncResult(String result) {
        Timber.i(TAG + "setmAsyncResult() <- " + result);
        mAsyncResult = result;
        if (!mInterrupted) {
            setChanged();
            notifyObservers();
        }
        mInterrupted = false;
    }

    private void recoverFromGoogleAuthExecption() {
        mInterrupted = true;
        if (mRecovered >= 1) {
            Timber.i(TAG + "recoverFromGoogleAuthException() -> already recovered once");
            return;
        }
        mRecovered += 1;
        connectAndStartOperation(new Pair<>(mNextGoogleApiOperation, mNextGoogleApiOperationBundle));
    }

    @SuppressLint("StaticFieldLeak")
    private void onBackupToDriveAsync(String encrypted) {
        final AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... parameters) {
                String result = writeSeedsToDrive(encrypted);
                setmAsyncResult(result);
                return null;
            }
        };
        asyncTask.execute();
    }

    @SuppressLint("StaticFieldLeak")
    private void onRestoreFromDriveAsync() {
        final AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... parameters) {
                readSeedsFromDrive();
                return null;
            }
        };
        asyncTask.execute();
    }

    private String writeSeedsToDrive(String encrypted) {
        List<File> fl = queryFiles();
        try {
            fl.get(0);
        } catch (IndexOutOfBoundsException | NullPointerException e) {
            return createFile(GoogleDriveHelper.SEED_BACKUP_FILENAME, encrypted);
        }
        File lastBackup = fl.get(0);
        return updateFile(lastBackup.getId(), encrypted);
    }

    private void readSeedsFromDrive() {

    }

//--------------------------------------------------------------------------------------------------
//  Low-level api methods
//--------------------------------------------------------------------------------------------------

    private String readFile(String fileId) {
        String contents = null;
        try {
            InputStream is = mDriveService.files().get(fileId).executeMediaAsInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                stringBuilder.append(line);
            }
            contents = stringBuilder.toString();
        } catch (Exception e) {
            Timber.e(e, TAG);
        }
        return contents;
    }

    private String createFile(String filename, String content) {
        Timber.i(TAG + "createFile() <- " + content);
        File metadata = new File()
                .setParents(Collections.singletonList("appDataFolder"))
                .setMimeType("text/plain")
                .setName(filename);

        File file = null;

        try {
            ByteArrayContent contentStream = ByteArrayContent.fromString("text/plain", content);
            file = mDriveService.files().create(metadata, contentStream).execute();
        } catch (UserRecoverableAuthIOException e) {
            Timber.i(TAG + e + " Should recover after this point");
            recoverFromGoogleAuthExecption();
        } catch (NullPointerException | IOException e) {
            Timber.e(TAG + e);
        }

        if (file == null) {
            return null;
        }
        Timber.i(TAG + "createFile() -> " + file.getId());
        return file.getId();
    }

    private String updateFile(String fileId, String content) {
        Timber.i(TAG + "updateFile() <- " + content);
        File file = null;
        try {
            ByteArrayContent contentStream = ByteArrayContent.fromString("text/plain", content);
            file = mDriveService.files().update(fileId, null, contentStream).execute();
        } catch (UserRecoverableAuthIOException e) {
            Timber.i(TAG + e + " Should recover after this point");
            recoverFromGoogleAuthExecption();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (file == null) {
            return null;
        }
        Timber.i(TAG + "updateFile() -> " + file.getId());
        return file.getId();
    }

    private List<File> queryFiles(String folderId) {
        Timber.d(TAG + "queryFiles() <- ");
        FileList fl = null;
        try {
            fl = mDriveService.files().list().setSpaces("appDataFolder")
                    .setFields("files(id, name, parents)")
                    .execute();
        } catch (UserRecoverableAuthIOException e) {
            Timber.d(TAG + e + " Should recover after this point");
            recoverFromGoogleAuthExecption();
        } catch (IOException e) {
            Timber.e(e, TAG);
        }
        if (fl == null) {
            return new FileList().getFiles();
        }
        Timber.d(TAG + "queryFiles() -> ");
        List<File> filterFiles = new ArrayList<>();
        for (File file : fl.getFiles()) {
            if (file.getParents().contains(folderId)) {
                Timber.d(TAG + "             -> " + file);
                filterFiles.add(file);
            }
        }
        return filterFiles;
    }

    private String createFolder(String name) {
        Timber.d(TAG + "createFolder() <- " + name);
        File metadata = new File()
                .setParents(Collections.singletonList("appDataFolder"))
                .setMimeType("application/vnd.google-apps.folder")
                .setName(name);

        File folder = null;

        try {
            folder = mDriveService.files().create(metadata)
                    .setFields("id, name")
                    .execute();
        } catch (UserRecoverableAuthIOException e) {
            Timber.d(TAG + e + " Should recover after this point");
            recoverFromGoogleAuthExecption();
        } catch (NullPointerException | IOException e) {
            Timber.e(TAG + e);
        }

        if (folder == null) {
            return null;
        }
        Timber.d(TAG + "createFolder() -> %s %s", folder.getName(), folder.getId());
        return folder.getId();
    }

    private List<File> queryFolders() {
        Timber.d(TAG + "queryFolders() <- ");
        FileList fl = null;
        try {
            fl = mDriveService.files().list().setSpaces("appDataFolder")
                    .setQ("mimeType='application/vnd.google-apps.folder'")
                    .setFields("files(id, name)")
                    .execute();
        } catch (UserRecoverableAuthIOException e) {
            Timber.d(TAG + e + " Should recover after this point");
            recoverFromGoogleAuthExecption();
        } catch (IOException e) {
            Timber.e(e, TAG);
        }
        if (fl == null) {
            return null;
        }
        Timber.d(TAG + "queryFolders() -> ");
        for (File folder : fl.getFiles()) {
            Timber.d(TAG + "             -> /%s %s", folder.getName(), folder.getId());
        }
        return fl.getFiles();
    }


//    private void syncNow(Account account) {
//        Timber.i(TAG + "syncNow() ...syncing w. Google Drive");
//        Bundle bundle = new Bundle();
//        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
//        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
//        ContentResolver.requestSync(account, SyncConstants.AUTHORITY, bundle);
//    }

}