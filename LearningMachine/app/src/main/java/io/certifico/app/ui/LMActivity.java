package io.certifico.app.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;

import io.certifico.app.data.drive.GoogleDriveFile;
import io.certifico.app.data.drive.GoogleDriveService;
import io.certifico.app.ui.home.HomeActivity;
import io.certifico.app.ui.issuer.IssuerActivity;
import io.certifico.app.ui.lock.EnterPasswordActivity;
import io.certifico.app.ui.lock.SetPasswordActivity;
import io.certifico.app.ui.onboarding.OnboardingActivity;
import io.certifico.app.ui.settings.SettingsActivity;
import io.certifico.app.util.AESCrypt;
import io.certifico.app.util.FileUtils;
import com.smallplanet.labalib.Laba;
import com.trello.rxlifecycle.LifecycleProvider;
import com.trello.rxlifecycle.LifecycleTransformer;
import com.trello.rxlifecycle.RxLifecycle;
import com.trello.rxlifecycle.android.ActivityEvent;
import com.trello.rxlifecycle.android.RxLifecycleAndroid;

import org.bitcoinj.wallet.Wallet;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.GeneralSecurityException;
import java.util.Scanner;
import java.util.function.Function;

import javax.annotation.Nonnull;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;
import timber.log.Timber;

public abstract class LMActivity extends AppCompatActivity implements LifecycleProvider<ActivityEvent> {

    protected static Class lastImportantClassSeen = HomeActivity.class;
    public static final Integer REQUEST_CODE_SET_ENCRYPTION_KEY = 3;
    public static final Integer REQUEST_CODE_DECRYPTION_KEY = 4;

    // Used by LifecycleProvider interface to transform lifeycycle events into a stream of events through an observable.
    private final BehaviorSubject<ActivityEvent> mLifecycleSubject = BehaviorSubject.create();
    private Observable.Transformer mMainThreadTransformer;
    protected GoogleDriveService mDriveService;
    protected Action0 drivePendingAction;
    protected Function drivePendingFunction;
    protected String mEncryptionKey;

    public void safeGoBack() {

        // ideallt what we want to do here is to safely go back to a known good activity in our flow.
        // for example, if we enter the app at home, go to settings, add a cert, go to cert info,
        // and delete the cert, where should we go back to?  Ideally that would be settings.
        //
        // however, if we do the same thing but go to issuers, then the cert, then going
        // back should go to the issuer activity...

        Intent intent = new Intent(this, lastImportantClassSeen);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLifecycleSubject.onNext(ActivityEvent.CREATE);
        Laba.setContext(getBaseContext());
    }

    @Override
    protected void onStart() {
        super.onStart();
        mLifecycleSubject.onNext(ActivityEvent.START);
        /*
         Toolbar in CertificatePagerActivity isn't being created properly because of a timing issue in the onCreate of LMActivity.
         CertificatePagerActivity is subclassing LMActivity and getSupportActionBar in setupActionBar is coming up null and not setting the proper toolbar
         so moving it to onStart sets the proper toolbar.
         */
        setupActionBar();

        Class c = this.getClass();
        if (c == HomeActivity.class || c == IssuerActivity.class || c == SettingsActivity.class) {
            lastImportantClassSeen = c;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mLifecycleSubject.onNext(ActivityEvent.RESUME);
        Timber.i("Sync.LMActivity onResume() Resumed");

        if (didReceivePermissionsCallback) {
            if (tempPassphrase != null && passphraseCallback != null) {
                if (didSucceedInPermissionsRequest) {
                    savePassphraseToDevice(tempPassphrase, passphraseCallback);
                } else {
                    savePassphraseToDevice(null, passphraseCallback);
                }
                tempPassphrase = null;
                passphraseCallback = null;
            }

            if (passphraseCallback != null) {
                if (didSucceedInPermissionsRequest) {
                    startEnterEncryptionKeyActivity(false);
//                    getSavedPassphraseFromDevice(passphraseCallback);
                } else {
                    startEnterEncryptionKeyActivity(false);
//                    getSavedPassphraseFromDevice(passphraseCallback);
                }
                passphraseCallback = null;
            }

            didReceivePermissionsCallback = false;
            didSucceedInPermissionsRequest = false;
        }
    }

    @Override
    protected void onPause() {
        mLifecycleSubject.onNext(ActivityEvent.PAUSE);
        super.onPause();
    }

    @Override
    protected void onStop() {
        mLifecycleSubject.onNext(ActivityEvent.STOP);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mLifecycleSubject.onNext(ActivityEvent.DESTROY);
        super.onDestroy();
    }

    @Nonnull
    @Override
    public Observable<ActivityEvent> lifecycle() {
        return mLifecycleSubject.asObservable();
    }

    @Nonnull
    @Override
    public <T> LifecycleTransformer<T> bindUntilEvent(@Nonnull ActivityEvent event) {
        return RxLifecycle.bindUntilEvent(mLifecycleSubject, event);
    }

    @Nonnull
    @Override
    public <T> LifecycleTransformer<T> bindToLifecycle() {
        return RxLifecycleAndroid.bindActivity(mLifecycleSubject);
    }

    /**
     * Used to compose an observable so that it observes results on the main thread and binds until activity Destruction
     */
    @SuppressWarnings("unchecked")
    protected <T> Observable.Transformer<T, T> bindToMainThread() {

        if (mMainThreadTransformer == null) {
            mMainThreadTransformer = (Observable.Transformer<T, T>) observable -> observable.observeOn(AndroidSchedulers.mainThread())
                    .compose(bindUntilEvent(ActivityEvent.DESTROY));
        }

        return (Observable.Transformer<T, T>) mMainThreadTransformer;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /* Keyboard */

    public void hideKeyboard() {
        if (getCurrentFocus() != null && getCurrentFocus().getWindowToken() != null) {
            InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    /* ActionBar */

    protected void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) {
            return;
        }

        actionBar.setDisplayShowTitleEnabled(true);
        String title = getActionBarTitle();
        if (!TextUtils.isEmpty(title)) {
            actionBar.setTitle(title);
        }

        // decide to display home caret
        if (requiresBackNavigation()) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public String getActionBarTitle() {
        return (String) getTitle();
    }

    /* Navigation */

    protected boolean requiresBackNavigation() {
        return false;
    }

    /* Saving passphrases to device */

    @FunctionalInterface
    public interface Callback<A, R> {
        public R apply(A a);
    }

    private String tempPassphrase = null;
    private Callback passphraseCallback = null;

    public static String pathToSavedPassphraseFile() {
        return Environment.getExternalStorageDirectory() + "/certifico.seeds";
    }

    private String getDeviceId(Context context) {
        final String deviceId = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        if (deviceId == null || deviceId.length() == 0) {
            return "NOT_IDEAL_KEY";
        }
        return deviceId;
    }

    private void savePassphraseToDevice(String passphrase, Callback passphraseCallback) {
        if (passphrase == null) {
            passphraseCallback.apply(null);
            return;
        }

        if (mEncryptionKey == null) {
            startSetEncryptionKeyActivity();
            return;
        }

        String passphraseFile = pathToSavedPassphraseFile();
        try (PrintWriter out = new PrintWriter(passphraseFile)) {
            String encryptionKey = mEncryptionKey;
            mEncryptionKey = null;
            String mneumonicString = "mneumonic:" + passphrase;
            try {
                String encryptedMsg = AESCrypt.encrypt(encryptionKey, mneumonicString);
                out.println(encryptedMsg);
                passphraseCallback.apply(passphrase);
            } catch (GeneralSecurityException e) {
                Timber.e(e, "Could not encrypt passphrase.");
                passphraseCallback.apply(null);
            }
        } catch (Exception e) {
            Timber.e(e, "Could not write to passphrase file");
        }
    }

    public void askToSavePassphraseToDevice(String passphrase, Callback passphraseCallback) {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                this.drivePendingAction = () -> savePassphraseToDevice(passphrase, passphraseCallback);
                savePassphraseToDevice(passphrase, passphraseCallback);
            } else {
                tempPassphrase = passphrase;
                this.passphraseCallback = passphraseCallback;
                this.drivePendingAction = () -> savePassphraseToDevice(passphrase, passphraseCallback);
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            }
        } else {
            this.drivePendingAction = () -> savePassphraseToDevice(passphrase, passphraseCallback);
            savePassphraseToDevice(passphrase, passphraseCallback);
        }
    }

    public void askToGetPassphraseFromDevice(Callback passphraseCallback) {

        String encryptedMsg = FileUtils.getSeedFromFile(this, false,false);
        if (encryptedMsg == null) {
            passphraseCallback.apply(null);
            return;
        }

        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
//                getSavedPassphraseFromDevice(passphraseCallback);
                this.passphraseCallback = passphraseCallback;
//                this.drivePendingAction = () -> getSavedPassphraseFromDevice(passphraseCallback);
                startEnterEncryptionKeyActivity(false);
            } else {
                this.passphraseCallback = passphraseCallback;
//                this.drivePendingAction = () -> getSavedPassphraseFromDevice(passphraseCallback);
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                startEnterEncryptionKeyActivity(false);
            }
        } else {
//            getSavedPassphraseFromDevice(passphraseCallback);
            this.passphraseCallback = passphraseCallback;
//            this.drivePendingAction = () -> getSavedPassphraseFromDevice(passphraseCallback);
            startEnterEncryptionKeyActivity(false);
        }
    }

    private boolean getSavedPassphraseFromDevice(Callback passphraseCallback) {
        String passphraseFile = OnboardingActivity.pathToSavedPassphraseFile();
        try {
            String encryptedMsg = new Scanner(new java.io.File(passphraseFile)).useDelimiter("\\Z").next();
            String encryptionKey = getDeviceId(getApplicationContext());
            try {
                String content = AESCrypt.decrypt(encryptionKey, encryptedMsg);
                if (content.startsWith("mneumonic:")) {
                    passphraseCallback.apply(content.substring(10).trim());
                    return true;
                }
            } catch (GeneralSecurityException e) {
                Timber.e(e, "Could not decrypt passphrase.");
            }
        } catch (Exception e) {
            // note: this is a non-critical feature, so if this fails nbd
        }

        passphraseCallback.apply(null);
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        Timber.i("[Drive] requestCode: " + requestCode);
        if (requestCode == REQUEST_CODE_SET_ENCRYPTION_KEY) {
            if (resultCode == RESULT_OK) {
                mEncryptionKey = resultData.getStringExtra("encryptionKey");
                Timber.i("[Drive] mEncryptionKey: " + mEncryptionKey);
                if (this.drivePendingAction != null) {
                    this.drivePendingAction.call();
                    //                this.drivePendingAction = null;
                }
            }
        } else if (requestCode == REQUEST_CODE_DECRYPTION_KEY) {
            assert resultData != null;
            Boolean isGoogleFlow = resultData.getBooleanExtra("isGoogleFlow", false);
            if (resultCode == RESULT_OK) {
                String seed = resultData.getStringExtra("seed");
                if (isGoogleFlow) {
                    this.drivePendingFunction.apply(seed);
                } else {
//                    this.drivePendingAction.call();
                    this.passphraseCallback.apply(seed);
                }
            } else {
                if (isGoogleFlow) {
                    this.drivePendingFunction.apply(null);
                } else {
                    this.passphraseCallback.apply(null);
                }
            }
        } else {
            Timber.i("Sync.LMActivity onActivityResult() -> Deffering " + requestCode);
            GoogleDriveService.handleActivityResult(this, requestCode, resultCode, resultData, (drive) -> {
                this.mDriveService = drive;
                if (this.drivePendingAction != null) {
                    this.drivePendingAction.call();
//                    this.drivePendingAction = null;
                }
            });
        }
    }

//    public static AlertDialog showInputDialog(Context context, String message, Callback callback) {
//        AlertDialog.Builder builder = new AlertDialog.Builder(context);
//        builder.setTitle(message);
//        final EditText input = new EditText(context);
//        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
//        input.setHint("Password");
//        builder.setView(input);
//        builder.setPositiveButton("OK", (dialog, which) -> {
//            callback.apply(input.getText().toString());
//            input.setText("");
//            dialog.dismiss();
//        });
//        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
//        AlertDialog dialog = builder.create();
//        dialog.show();
//        return dialog;
//    }

    private void startSetEncryptionKeyActivity() {
        Intent intent = new Intent(this, SetPasswordActivity.class);
        startActivityForResult(intent, REQUEST_CODE_SET_ENCRYPTION_KEY);
    }

    private void startEnterEncryptionKeyActivity(boolean isGoogleFlow) {
        Intent intent = new Intent(this, EnterPasswordActivity.class);
        intent.putExtra("isGoogleFlow", isGoogleFlow);
        startActivityForResult(intent, REQUEST_CODE_DECRYPTION_KEY);
    }

    public void askToSavePassphraseToGoogleDrive(String passphrase, Callback loadingCallback, Callback passphraseCallback) {
        Timber.i("[Drive] askToSavePassphraseToGoogleDrive");
        if (this.mDriveService == null) {
            GoogleDriveService.requestSignIn(this);
            this.drivePendingAction = () -> savePassphraseToGoogleDrive(passphrase, loadingCallback, passphraseCallback);
        } else {
            this.drivePendingAction = () -> savePassphraseToGoogleDrive(passphrase, loadingCallback, passphraseCallback);
            savePassphraseToGoogleDrive(passphrase, loadingCallback, passphraseCallback);
        }
    }

    private void savePassphraseToGoogleDrive(String passphrase, Callback loadingCallback, Callback passphraseCallback) {
        Timber.i("[Drive] savePassphraseToGoogleDrive");
        if (mEncryptionKey == null) {
            startSetEncryptionKeyActivity();
            return;
        }
        try {
            String encrypted = null;
            try {
                encrypted = getEncryptedPassphrase(passphrase, mEncryptionKey);
                mEncryptionKey = null;
            } catch (GeneralSecurityException e) {
                passphraseCallback.apply(null);
            }
            loadingCallback.apply(true);
            String finalEncrypted = encrypted;
            this.mDriveService.querySeeds()
                    .flatMap((file) -> this.mDriveService.saveData(file, finalEncrypted))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doAfterTerminate(() -> loadingCallback.apply(false))
                    .subscribe((fileId) -> passphraseCallback.apply(passphrase),
                            (e) -> {
                                Timber.i(e, "[Drive] create file error: " + e.getMessage());
                                passphraseCallback.apply(null);
                            });
        } catch (Exception e) {
            Timber.e(e, "error saving passphrase to google drive.");
            passphraseCallback.apply(null);
        } finally {
            this.drivePendingAction = null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void askRestoreFromGoogleDrive(
            Action1<Boolean> loadingAction,
            Function<String, Observable<Wallet>> passphraseLoadedFunc,
            Function<GoogleDriveFile, Observable<String>> addCertificateFunc) {
        Timber.i("[Drive] askRestoreFromGoogleDrive");
        if (this.mDriveService == null) {
            this.drivePendingAction = () -> restoreFromGoogleDrive(loadingAction, passphraseLoadedFunc, addCertificateFunc);
            GoogleDriveService.requestSignIn(this);
        } else {
            this.drivePendingAction = () -> restoreFromGoogleDrive(loadingAction, passphraseLoadedFunc, addCertificateFunc);
            restoreFromGoogleDrive(loadingAction, passphraseLoadedFunc, addCertificateFunc);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void restoreFromGoogleDrive(
            Action1<Boolean> loadingAction,
            Function<String, Observable<Wallet>> passphraseLoadedFunc,
            Function<GoogleDriveFile, Observable<String>> addCertificateFunc) {

        loadingAction.call(true);

        this.mDriveService.queryCertificates()
                .subscribeOn(Schedulers.io())
                .flatMap(file -> this.mDriveService.downloadDriveFile(file))
                .flatMap(driveFile -> FileUtils.saveCertificate(this, driveFile))
                .flatMap(wallet -> this.mDriveService.querySeeds())
                .flatMap(file -> this.mDriveService.downloadDriveFile(file))
                .flatMap(driveFile -> FileUtils.saveSeed(this, driveFile))
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterTerminate(() -> {
                    Timber.i("[Drive] doOnTerminated");
                    loadingAction.call(false);
                })
                .doOnError(e -> {
                    Timber.i(e, "[Drive] adding certificate error: " + e.getMessage());
                    if (e.equals(new IOException())){
                        Timber.i(e, "[Drive] IO Auth error: " + e.getMessage());
                    }
                    passphraseLoadedFunc.apply(null);
                })
                .doOnCompleted(() -> {
                    Timber.i("[Drive] doOnCompleted");
                    loadingAction.call(false);
                })
                .subscribe(seed -> {
                    Timber.i("[Drive] seed: " + seed);
                    if (seed) {
                        drivePendingFunction = passphraseLoadedFunc;
                        startEnterEncryptionKeyActivity(true);
                    } else {
                        runOnUiThread(() -> passphraseLoadedFunc.apply(null));
                    }
                });

        this.drivePendingAction = null;

//        this.mDriveService.querySeeds()
//                .subscribeOn(Schedulers.io())
//                .flatMap(file -> this.mDriveService.downloadDriveFile(file))
//                .flatMap(driveFile -> FileUtils.saveSeed(this, driveFile))
//                .flatMap(wallet -> {
//                    Timber.i("[Drive] wallet: " + wallet);
//                    return this.mDriveService.queryCertificates();
//                })
//                .flatMap(file -> {
//                    Timber.i("[Drive] adding certificates: ");
//                    return this.mDriveService.downloadDriveFile(file);
//                })
//                .flatMap(driveFile -> FileUtils.saveCertificate(this, driveFile))
//                .observeOn(AndroidSchedulers.mainThread())
//                .doAfterTerminate(() -> {
//                    Timber.i("[Drive] doOnTerminated");
//                    loadingAction.call(false);
//                })
//                .doOnError(e -> {
//                    Timber.i(e, "[Drive] adding certificate error: " + e.getMessage());
//                    passphraseLoadedFunc.apply(null);
//                })
//                .doOnCompleted(() -> {
//                    Timber.i("[Drive] doOnCompleted");
//                    loadingAction.call(false);
//                })
//                .subscribe(certId -> showInputDialog(this, getResources().getString(R.string.activity_paste_passphrase_decrypt), (encryptionKey) -> {
//                    String passphrase = getPassphraseFromEncrypted((String) encryptionKey);
//                    Timber.i("[Drive] passphrase: " + passphrase);
//                    passphraseLoadedFunc.apply(passphrase);
//                    return true;
//                }));
    }

    public void askBackUpToGoogleDrive(Action1<Boolean> loadingAction, Action1<Boolean> onDoneAction, Observable<File> getFilesObservable) {
        if (this.mDriveService == null) {
            GoogleDriveService.requestSignIn(this);
            this.drivePendingAction = () -> backupToGoogleDrive(loadingAction, onDoneAction, getFilesObservable);
        } else {
            backupToGoogleDrive(loadingAction, onDoneAction, getFilesObservable);
        }
    }

    private void backupToGoogleDrive(Action1<Boolean> loadingAction, Action1<Boolean> onDoneAction, Observable<File> getFilesObservable) {
        loadingAction.call(true);
        getFilesObservable
                .subscribeOn(Schedulers.io())
                .flatMap(file -> mDriveService.uploadFile(file))
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterTerminate(() -> loadingAction.call(false))
                .doOnError(e -> {
                    Timber.i(e, "[Drive] upload error: " + e.getMessage());
                    onDoneAction.call(false);
                })
                .doOnCompleted(() -> onDoneAction.call(true))
                .subscribe(file -> Timber.i("[Drive] certificate uploaded: " + file.getName()));
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void askToRestoreCertificates(
            Action1<Boolean> loadingAction,
            Action1<Boolean> onDoneAction,
            Function<GoogleDriveFile, Observable<String>> addCertificateFunc
    ) {
        if (this.mDriveService == null) {
            GoogleDriveService.requestSignIn(this);
            this.drivePendingAction = () -> restoreCertificates(loadingAction, onDoneAction, addCertificateFunc);
        } else {
            restoreCertificates(loadingAction, onDoneAction, addCertificateFunc);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void restoreCertificates(
            Action1<Boolean> loadingAction,
            Action1<Boolean> onDoneAction,
            Function<GoogleDriveFile, Observable<String>> addCertificateFunc) {
        loadingAction.call(true);
        this.mDriveService.queryCertificates()
                .subscribeOn(Schedulers.io())
                .flatMap(file -> this.mDriveService.downloadDriveFile(file))
                .flatMap(addCertificateFunc::apply)
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterTerminate(() -> loadingAction.call(false))
                .doOnError(e -> {
                    Timber.i(e, "[Drive] adding certificate error: " + e.getMessage());
                    onDoneAction.call(false);
                })
                .doOnCompleted(() -> onDoneAction.call(true))
                .subscribe(certId -> Timber.i("[Drive] certificate added: " + certId));
    }

    private String getEncryptedPassphrase(String passphrase, String key) throws GeneralSecurityException {
        String mneumonicString = "mneumonic:" + passphrase;
        String encryptedMsg = AESCrypt.encrypt(key, mneumonicString);
        Timber.i("[Drive] encryptedMsg: " + encryptedMsg);
        return encryptedMsg;
    }

//    private String getPassphraseFromEncrypted(String encryptionKey) {
//        String encryptedMsg = FileUtils.getSeedFromFile(this, false);
//        Timber.i("[Drive] encryptedMsg: " + encryptedMsg);
//        try {
////            String encryptedMsg = new Scanner(new File(passphraseFile)).useDelimiter("\\Z").next();
////            Timber.i("[Drive] encryptedMsg: " + encryptedMsg);
//            try {
//                String content = AESCrypt.decrypt(encryptionKey, encryptedMsg);
//                if (content.startsWith("mneumonic:")) {
//                    return content.substring(10).trim();
//                }
//            }catch (GeneralSecurityException e){
//                Timber.e(e, "Could not decrypt passphrase.");
//            }
//        } catch(Exception e) {
//            // note: this is a non-critical feature, so if this fails nbd
//        }
//        return null;
//    }

    private boolean didReceivePermissionsCallback = false;
    private boolean didSucceedInPermissionsRequest = false;

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Note: this really sucks, but android will crash if we try and display dialogs in the permissions
        // result callback.  So we delay this until onResume is called on the activity
        if (permissions.length > 0) {
            didReceivePermissionsCallback = true;
            didSucceedInPermissionsRequest = true;
            for (int i=0; i < permissions.length-1; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    didSucceedInPermissionsRequest = false;
                }
            }
        }
    }

}
