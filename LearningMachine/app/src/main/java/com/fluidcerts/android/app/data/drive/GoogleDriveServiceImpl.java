package com.fluidcerts.android.app.data.drive;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Pair;

import com.fluidcerts.android.app.data.CertificateManager;
import com.fluidcerts.android.app.data.inject.Injector;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Observable;
import java.util.Set;

import javax.inject.Inject;

import timber.log.Timber;

public class GoogleDriveServiceImpl extends Observable implements OnSuccessListener<GoogleSignInAccount>, OnFailureListener {

    private final String TAG = "Sync.GoogleDriveServiceImpl ";
    private final int INVALID = 999;

    public boolean mActivityLaunched;

    private WeakReference<Activity> mActivity;
    private Drive mDriveService;
    @Inject CertificateManager mCertificateManager;

    private boolean mInterrupted = false;
    private int mRecovered = 0;

    private String mAsyncResult;

    private int mNextGoogleApiOperation = INVALID;
    private Bundle mNextGoogleApiOperationBundle;

    GoogleDriveServiceImpl(final Drive driveService) {
        mActivityLaunched = false;
        mDriveService = driveService;
    }

    GoogleDriveServiceImpl(final Activity activity) {
        mActivityLaunched = true;
        mActivity = new WeakReference<>(activity);
        Injector.obtain(mActivity.get())
                .inject(this);
    }

    public final void reset() {
        mInterrupted = false;
        mRecovered = 0;
        mNextGoogleApiOperation = INVALID;
        mNextGoogleApiOperationBundle = new Bundle();
    }

    public final void disconnect() {
        mActivity = null;
        mDriveService = null;
        reset();
    }

    public final void connectAndStartOperation(final Pair<Integer, Bundle> extra) {
        Timber.d(TAG + "connectAndStartOperation() -> " + extra.first);
        unpackExtras(extra);
        Timber.d(TAG + "connectAndStartOperation() -> activityLaunched: " + mActivityLaunched);

        if (mActivityLaunched && (mDriveService == null || !isAuthenticated() || !hasPermissions())) {
            Timber.d(TAG + "connectAndStartOperation() here");
            final GoogleSignInOptions signInOptions =
                    new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestEmail()
                            .requestScopes(new Scope(DriveScopes.DRIVE_APPDATA))
                            .build();

            final GoogleSignInClient client = GoogleSignIn.getClient(mActivity.get(), signInOptions);

            mActivity.get().startActivityForResult(client.getSignInIntent(), GoogleDriveHelper.RESOLVE_SIGN_IN_CODE);
        } else {
            onGoogleDriveConnected(mNextGoogleApiOperation);
        }
    }

    public final void handleActivityResult(int requestCode, int resultCode, Intent resultData) {
        Timber.d(TAG + "handleActivityResult() resultCode -> " + resultCode);
        switch (requestCode) {
            case GoogleDriveHelper.RESOLVE_SIGN_IN_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    GoogleSignIn.getSignedInAccountFromIntent(resultData)
                            .addOnSuccessListener(this)
                            .addOnFailureListener(this);
                    break;
                }
                setAsyncResult(null);
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
    }

    @Override
    public void onFailure(@NonNull Exception e) {
        reset();
    }

    private void onGoogleDriveConnected(final int operation) {
        switch (operation) {
            case GoogleDriveHelper.BACKUP_SEED_CODE:
                Timber.d(TAG + "onGoogleDriveConnected() -> BACKUP SEED");
                onBackupSeedToDriveAsync(mNextGoogleApiOperationBundle.getString("encrypted"));
                break;

            case GoogleDriveHelper.RESTORE_SEED_CODE:
                Timber.d(TAG + "onGoogleDriveConnected() -> RESTORE SEED");
                onRestoreSeedFromDriveAsync();
//                onRestoreDbFromDriveAsync();
                break;

            case GoogleDriveHelper.BACKUP_CERTS_CODE:
                Timber.d(TAG + "onGoogleDriveConnected() -> BACKUP DB");
                onBackupCertsToDriveAsync();
                break;

            case GoogleDriveHelper.RESTORE_CERTS_CODE:
                Timber.d(TAG + "onGoogleDriveConnected() -> RESTORE DB");
                onRestoreCertsFromDriveAsync();
                break;

            default:
                Timber.d(TAG + "onGoogleDriveConnected() -> INVALID");
        }
    }

//--------------------------------------------------------------------------------------------------
//  Private methods - Util
//--------------------------------------------------------------------------------------------------

    private boolean isAuthenticated() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(mActivity.get());
        Timber.d(TAG + "isAuthenticated() -> " + account);
        return account != null;
    }

    private boolean hasPermissions() {
        if (mActivity != null) {
            Set<Scope> scopes = GoogleSignIn.getLastSignedInAccount(mActivity.get()).getGrantedScopes();
            for (Scope scope : scopes) {
                Timber.d(TAG + "hasPermissions() -> " + scope);
                if (scope.equals(new Scope(DriveScopes.DRIVE_APPDATA))) {
                    return true;
                }
            }
        }
        return false;
    }

    private void unpackExtras(Pair<Integer, Bundle> extra) {
        mNextGoogleApiOperation = extra.first;
        mNextGoogleApiOperationBundle = extra.second;
    }

    private void setAsyncResult(String result) {
        mAsyncResult = result;
        if (!mInterrupted) {
            Timber.d(TAG + "setmAsyncResult() <- " + result);
            setChanged();
            notifyObservers();
        }
        Timber.d(TAG + "setmAsyncResult() suppressed <- " + result);
        mInterrupted = false;
    }

    public String getAsyncResult() {
        String result = mAsyncResult;
        mAsyncResult = null;
        return result;
    }

    private void recoverFromGoogleAuthExecption() {
        mInterrupted = true;
        if (mRecovered >= 1) {
            Timber.d(TAG + "recoverFromGoogleAuthException() -> already recovered once");
            return;
        }
        mRecovered += 1;
        connectAndStartOperation(new Pair<>(mNextGoogleApiOperation, mNextGoogleApiOperationBundle));
    }

//--------------------------------------------------------------------------------------------------
//  Private methods - ServiceImpl Async Actions
//--------------------------------------------------------------------------------------------------

    @SuppressLint("StaticFieldLeak")
    private void onBackupSeedToDriveAsync(String encrypted) {
        final AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... parameters) {
                String folderId = createFolderIfNotExists(GoogleDriveHelper.SEED_BACKUP_PARENTS);
                if (folderId != null) {
                    String result = writeSeedsToDrive(folderId, encrypted);
                    setAsyncResult(result);
                }
                return null;
            }
        };
        asyncTask.execute();
    }

    @SuppressLint("StaticFieldLeak")
    private void onRestoreSeedFromDriveAsync() {
        final AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... parameters) {
                String folderId = getFolderIfExists(GoogleDriveHelper.SEED_BACKUP_PARENTS);
                String result = readSeedsFromDrive(folderId);
                setAsyncResult(result);
                return null;
            }
        };
        asyncTask.execute();
    }

    @SuppressLint("StaticFieldLeak")
    private void onBackupCertsToDriveAsync() {
        final AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... parameters) {
                String folderId = createFolderIfNotExists(GoogleDriveHelper.CERTS_BACKUP_PARENTS);
                if (folderId != null) {
                    String result = writeCertsToDrive(folderId, getCertFiles());
                    setAsyncResult(result);
                }
                return null;
            }
        };
        asyncTask.execute();
    }

    @SuppressLint("StaticFieldLeak")
    private void onRestoreCertsFromDriveAsync() {
        final AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... parameters) {
                String folderId = getFolderIfExists(GoogleDriveHelper.CERTS_BACKUP_PARENTS);
                List<File> files = readCertsFromDrive(folderId);

                java.io.File tmpDir = getTempDirectory();
                for (File file: files) {
                    InputStream is = readFile(file.getId());
                    java.io.File tempCert = new java.io.File(tmpDir, file.getName());
                    Timber.i(TAG + "%s %s <-", tempCert.getName(), tempCert.getTotalSpace());
                    createLocalFile(is, tempCert);
                    Timber.i(TAG + "%s -> %s", tempCert.getName(), tempCert.getTotalSpace());
                    mCertificateManager.addCertificate(tempCert)
                            .subscribe(uuid -> {
                                Timber.d("Certificate copied");
                            }, throwable -> {
                                Timber.e(throwable, "Importing failed with error");
                            });
                    tempCert.delete();
                }
                setAsyncResult("Restore Successful");
                return null;
            }
        };
        asyncTask.execute();
    }

//--------------------------------------------------------------------------------------------------
//  Mid-level api methods
//--------------------------------------------------------------------------------------------------

    private String createFolderIfNotExists(String name) {
        List<File> fl = queryFolders();
        try {
            fl.get(0);
        } catch (IndexOutOfBoundsException | NullPointerException e) {
            return createFolder(name);
        }
        for (File folder : fl) {
            if (folder.getName().equals(name)) {
                return folder.getId();
            }
        }
        return createFolder(name);
    }

    private String getFolderIfExists(String name) {
        List<File> fl = queryFolders();
        try {
            fl.get(0);
        } catch (IndexOutOfBoundsException | NullPointerException e) {
            return null;
        }
        for (File folder : fl) {
            if (folder.getName().equals(name)) {
                return folder.getId();
            }
        }
        return null;
    }

    private String writeSeedsToDrive(String parents, String encrypted) {
        ByteArrayContent contentStream = ByteArrayContent
                                            .fromString("text/plain", encrypted);
        List<File> fl = queryFiles(parents);
        try {
            fl.get(0);
        } catch (IndexOutOfBoundsException | NullPointerException e) {
            return createFile(parents,
                        GoogleDriveHelper.SEED_BACKUP_FILENAME,
                        contentStream);
        }
        File lastBackup = fl.get(0);
        return updateFile(lastBackup.getId(), contentStream);
    }

    private String readSeedsFromDrive(String parents) {
        List<File> fl = queryFiles(parents);
        try {
            fl.get(0);
        } catch (IndexOutOfBoundsException | NullPointerException e) {
            return null;
        }
        File lastBackup = fl.get(0);
        return readStringFromFile(lastBackup.getId());
    }

//    private String writeDbToDrive(String parents) {
//        byte[] bytes;
//        try {
//            bytes = readLocalFileToByteArray(getAppDbFile());
//        } catch (IOException e) {
//            return null;
//        }
//        ByteArrayContent contentStream = new ByteArrayContent("application/x-sqlite3", bytes);
//        List<File> fl = queryFiles(parents);
//        try {
//            fl.get(0);
//        } catch (IndexOutOfBoundsException | NullPointerException e) {
//            return createFile(parents,
//                    GoogleDriveHelper.DB_BACKUP_FILENAME,
//                    contentStream);
//        }
//        File lastBackup = fl.get(0);
//        return updateFile(lastBackup.getId(), contentStream);
//    }

    private InputStream readDbFromDrive(String parents) {
        List<File> fl = queryFiles(parents);
        try {
            fl.get(0);
        } catch (IndexOutOfBoundsException | NullPointerException e) {
            return null;
        }
        File lastBackup = fl.get(0);
        return readFile(lastBackup.getId());
    }

    private String writeCertsToDrive(String parents, java.io.File[] localFiles) {
        List<String> driveFiles = new ArrayList<>();
        List<java.io.File> toUpload = new ArrayList<>();
        for (File file: queryFiles(parents)) {
            driveFiles.add(file.getName());
        }
        for (java.io.File file: localFiles) {
            if (driveFiles.contains(file.getName())) {
                continue;
            } else {
                toUpload.add(file);
            }
        }
        boolean success = true;
        for (java.io.File file : toUpload) {
            byte[] bytes;
            try {
                bytes = readLocalFileToByteArray(file);
            } catch (IOException e) {
                return null;
            }
            ByteArrayContent contentStream = new ByteArrayContent("application/json", bytes);
            if (createFile(parents, file.getName(), contentStream) == null) {
                success = false;
            }

        }

        return (success) ? "success" : null;
    }

    private List<File> readCertsFromDrive(String parents) {
        List<File> fl = queryFiles(parents);
        try {
            fl.get(0);
        } catch (IndexOutOfBoundsException | NullPointerException e) {
            return null;
        }

        return fl;
    }

//--------------------------------------------------------------------------------------------------
//  Low-level api methods
//--------------------------------------------------------------------------------------------------

    private InputStream readFile(String fileId) {
        Timber.d(TAG + "readFile() <- " + fileId);
        InputStream is = null;
        try {
            is = mDriveService.files().get(fileId).executeMediaAsInputStream();
        } catch (UserRecoverableAuthIOException e) {
            Timber.d(TAG + e + " Should recover after this point");
            recoverFromGoogleAuthExecption();
        } catch (Exception e) {
            Timber.e(e, TAG);
        }
        Timber.d(TAG + "readFile() -> " + is);
        return is;
    }

    private String readStringFromFile(String fileId) {
        Timber.d(TAG + "readStringFromFile() <- " + fileId);
        String contents = null;
        try (InputStream is = readFile(fileId);
             BufferedReader br = new BufferedReader(new InputStreamReader(is)))
        {
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                stringBuilder.append(line);
            }
            contents = stringBuilder.toString();
        } catch (Exception e) {
            Timber.e(e, TAG);
        }
        Timber.d(TAG + "readStringFromFile() -> " + contents);
        return contents;
    }

    private String createFile(String parents, String filename, ByteArrayContent content) {
        Timber.d(TAG + "createFile() <- " + content);
        File metadata = new File()
                .setParents(Collections.singletonList(parents))
                .setMimeType("text/plain")
                .setName(filename);
        File file = null;
        try {
            file = mDriveService.files().create(metadata, content)
                    .setFields("id, name, parents")
                    .execute();
        } catch (UserRecoverableAuthIOException e) {
            Timber.d(TAG + e + " Should recover after this point");
            recoverFromGoogleAuthExecption();
        } catch (NullPointerException | IOException e) {
            Timber.e(TAG + e);
        }

        if (file == null) {
            return null;
        }
        Timber.d(TAG + "createFile() -> %s %s %s", file.getId(), file.getName(), file.getParents());
        return file.getId();
    }

    private String updateFile(String fileId, ByteArrayContent content) {
        Timber.d(TAG + "updateFile() <- " + content);
        File file = null;
        try {
            file = mDriveService.files().update(fileId, null, content)
                    .setFields("id, name, parents")
                    .execute();
        } catch (UserRecoverableAuthIOException e) {
            Timber.d(TAG + e + " Should recover after this point");
            recoverFromGoogleAuthExecption();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (file == null) {
            return null;
        }
        Timber.d(TAG + "updateFile() -> " + file.getId());
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

        Timber.d(TAG + "queryFiles() -> ");
        if (fl == null) {
            return new FileList().getFiles();
        }
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
        } catch (UserRecoverableAuthIOException | NullPointerException e) {
            Timber.d(TAG + e + " Should recover after this point");
            recoverFromGoogleAuthExecption();
        } catch (IOException e) {
            Timber.e(e, TAG);
        }

        Timber.d(TAG + "queryFolders() -> ");
        if (fl == null) {
            return null;
        }
        for (File folder : fl.getFiles()) {
            Timber.d(TAG + "             -> /%s %s", folder.getName(), folder.getId());
        }
        return fl.getFiles();
    }

//--------------------------------------------------------------------------------------------------
//  IO methods
//--------------------------------------------------------------------------------------------------

//    private java.io.File getAppDbFile() {
//        return mActivity.get().getApplicationContext().getDatabasePath(GoogleDriveHelper.DB_BACKUP_FILENAME);
//    }

    private java.io.File[] getCertFiles() {
        java.io.File certDir = new java.io.File(mActivity.get().getApplicationContext().getFilesDir(), "certs");
        return certDir.listFiles();
    }

    private java.io.File getTempDirectory() {
        java.io.File certDir = new java.io.File(mActivity.get().getFilesDir(), "tmp");
        certDir.mkdirs();
        return certDir;
    }

    private byte[] readLocalFileToByteArray(java.io.File file) throws IOException {
        try (RandomAccessFile f = new RandomAccessFile(file, "r")) {
            byte[] data = new byte[(int) f.length()];
            f.readFully(data);
            return data;
        }
    }

    private java.io.File createLocalFile(final InputStream src, java.io.File file) {
        if (src != null) {
            try {
                writeStreamToFileOutput(src, new FileOutputStream(file));
            } catch (IOException e) {
                Timber.e(TAG + e);
            }
        }
        return file;
    }

    private void writeStreamToFileOutput(final InputStream is, final FileOutputStream os) throws IOException {
        try {
            final byte[] buffer = new byte[4 * 1024]; // or other buffer size
            int read;

            while ((read = is.read(buffer)) != -1) {
                os.write(buffer, 0, read);
            }
            os.flush();

        } finally {
            is.close();
            os.close();
        }
    }

//    private void syncNow(Account account) {
//        Timber.d(TAG + "syncNow() ...syncing w. Google Drive");
//        Bundle bundle = new Bundle();
//        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
//        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
//        ContentResolver.requestSync(account, SyncConstants.AUTHORITY, bundle);
//    }

}