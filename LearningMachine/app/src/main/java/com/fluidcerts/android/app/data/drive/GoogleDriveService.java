package com.fluidcerts.android.app.data.drive;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.util.Collections;

import rx.Observable;
import rx.Single;
import rx.functions.Action1;
import timber.log.Timber;

public class GoogleDriveService {
    private final Drive mDriveService;
    public static final int REQUEST_CODE_SIGN_IN = 1;
    private static final String APP_DATA_FOLDER = "appDataFolder";
    private static final String PASSPHRASE_FILE_NAME = "certifico.seeds";

    public static void log(String message) {
        Timber.d("[Drive] " + message);
    }

    public GoogleDriveService(Drive driveService) {
        mDriveService = driveService;
    }

    public Observable<File> queryFileByName(String name) {
        return queryFiles(String.format("name = '%s'", name));
    }

    public Observable<File> querySeeds() {
        return queryFiles("name contains '.seeds'");
    }

    public Observable<File> queryIssuers() {
        return queryFiles("name contains '.issuer'");
    }

    public Observable<File> queryIssuedAddresses() {
        return queryFiles("name contains '.index'");
    }

    public Observable<File> queryCertificates() {
        return queryFiles("name contains '.json'");
    }

    public Observable<File> queryFiles(String query) {
        return Observable.defer(() -> {
            final FileList fileList;
            try {
                fileList = mDriveService.files().list()
                        .setQ(query)
                        .setSpaces(APP_DATA_FOLDER)
                        .execute();
            } catch (IOException e) {
                return Observable.error(e);
            }
            try {
                fileList.getFiles().get(0);
                log(String.format("queryFiles -> fileList: %s", fileList.getFiles()));
            } catch (IndexOutOfBoundsException e) {
                log(String.format("queryFiles -> fileList: %s", "No files"));
                return Observable.just(null);
            }
            return Observable.from(fileList.getFiles());
        });
    }

    public Observable<String> saveToDriveOrUpdate(File file, String data) {
        return Observable.defer(() -> {
            ByteArrayContent dataStream = ByteArrayContent.fromString("text/plain", data);
            File googleFile;
            try {
                if (file == null) {
                    File metadata = new File()
                            .setParents(Collections.singletonList(APP_DATA_FOLDER))
                            .setMimeType("text/plain")
                            .setName(PASSPHRASE_FILE_NAME);
                    googleFile = mDriveService.files().create(metadata, dataStream).execute();

                } else {
                    googleFile = mDriveService.files().update(file.getId(),null,dataStream)
                            .setFields("id, name, parents")
                            .execute();
                }

                if (googleFile == null) {
                    throw new IOException("Null result when requesting file creation.");
                }
            } catch (Exception e) {
                return Observable.error(e);
            }
            return Observable.just(googleFile.getId());
        });
    }

    public Observable<GoogleDriveFile> downloadFromDrive(File file) {
        return Observable.defer(() -> {
            if (file == null) {
                return Observable.just(null);
            }
            GoogleDriveFile driveFile;
            try {
                log(String.format("downloadingFile -> id: %s | name: %s", file.getId(), file.getName()));
                driveFile = new GoogleDriveFile(
                        file.getName(),
                        mDriveService.files().get(file.getId()).executeMediaAsInputStream());
            } catch (NullPointerException | IOException e) {
                log(String.format("downloadingFile -> error: %s ", e));
                return Observable.error(e);
            }
            log(String.format("File downloaded -> name: %s", file.getName()));
            return Observable.just(driveFile);
        });
    }

    public static void requestSignIn(Activity activity) {
        log("Requesting sign-in");

        final GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestScopes(new Scope(DriveScopes.DRIVE_APPDATA))
                        .build();
        final GoogleSignInClient client = GoogleSignIn.getClient(activity, signInOptions);

        // The result of the sign-in Intent is handled in onActivityResult.
        activity.startActivityForResult(client.getSignInIntent(), REQUEST_CODE_SIGN_IN);
    }

    public static void handleActivityResult(
            Activity activity,
            int requestCode,
            int resultCode,
            Intent resultData,
            Action1<GoogleDriveService> postSignInAction) {
        Timber.i(String.format("handleActivityResult requestCode: %s | resultCode: %s", requestCode, resultCode));
        switch (requestCode) {
            case GoogleDriveService.REQUEST_CODE_SIGN_IN:
                if (resultCode == Activity.RESULT_OK) {
                    handleSignInResult(activity, resultData)
                            .subscribe(drive -> {
                                log("handling SignedResult");
                                if (postSignInAction != null) {
                                    postSignInAction.call(drive);
                                }
                            });
                    break;
                }

                final Bundle extras = resultData.getExtras();
                if (extras != null) {
                    log("result data");
                    for (String key : extras.keySet()) {
                        log(String.format("extra key: %s - value: %s", key, extras.get(key)));
                    }
                }
                break;
        }
    }


    public static Single<GoogleDriveService> handleSignInResult(Activity activity, Intent data) {
        Timber.i("[Drive] handling SignInResult");

        return Single.defer(() -> {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount googleAccount = task.getResult(ApiException.class);
                Timber.i("[Drive] Signed in as " + googleAccount.getEmail());

                // Use the authenticated account to sign in to the Drive service.
                GoogleAccountCredential credential = GoogleAccountCredential
                        .usingOAuth2(activity, Collections.singleton(DriveScopes.DRIVE_APPDATA));
                credential.setSelectedAccount(googleAccount.getAccount());
                Drive drive =
                        new Drive.Builder(
                                AndroidHttp.newCompatibleTransport(),
                                new GsonFactory(),
                                credential)
                                .setApplicationName("FluidCerts")
                                .build();

                return Single.just(new GoogleDriveService(drive));
            } catch (ApiException e) {
                Timber.e("[Drive] Unable to sign in.", e);
                return Single.error(e);
            }
        });
    }
}

