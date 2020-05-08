package com.fluidcerts.android.app;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.multidex.MultiDexApplication;
import android.webkit.WebView;

import com.fluidcerts.android.app.data.CertificateManager;
import com.fluidcerts.android.app.data.IssuerManager;
import com.fluidcerts.android.app.data.backup.SyncConstants;
import com.fluidcerts.android.app.data.inject.Injector;
import com.fluidcerts.android.app.data.inject.LMComponent;
import com.fluidcerts.android.app.data.inject.LMGraph;
import com.fluidcerts.android.app.data.preferences.SharedPreferencesManager;
import com.fluidcerts.android.app.util.BitcoinUtils;
import com.fluidcerts.android.app.util.FileLoggingTree;
import com.fluidcerts.android.app.util.FileUtils;

import net.danlew.android.joda.JodaTimeAndroid;

import java.util.Locale;

import javax.inject.Inject;

import timber.log.Timber;

public class LMApplication extends MultiDexApplication {

    protected LMGraph mGraph;

    @Inject Timber.Tree mTree;
    @Inject SharedPreferencesManager mPreferencesManager;
    @Inject IssuerManager mIssuerManager;
    @Inject CertificateManager mCertificateManager;

    @Override
    public void onCreate() {
        super.onCreate();

        setupDagger();
        setupTimber();
        setupJodaTime();
        enableWebDebugging();
        setupMnemonicCode();
        Timber.i("Application was launched!");
        logDeviceInfo();
        checkLogFileValidity();
    }

    @Override
    public Object getSystemService(@NonNull String name) {
        if (Injector.matchesService(name)) {
            return mGraph;
        }
        return super.getSystemService(name);
    }

    private void setupDagger() {
        mGraph = LMComponent.Initializer.init(this);
        mGraph.inject(this);
    }

    private void setupTimber() {
        Timber.plant(mTree);
        Timber.plant(new FileLoggingTree());
    }

    private void logDeviceInfo() {
        ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiNetwork = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mobileNetwork = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        String device = Build.DEVICE;
        String display = Build.DISPLAY;
        String fingerprint = Build.FINGERPRINT;
        String hardware = Build.HARDWARE;
        String host = Build.HOST;
        String id = Build.ID;
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        String product = Build.PRODUCT;
        int sdk = Build.VERSION.SDK_INT;
        String codename = Build.VERSION.CODENAME;
        String release = Build.VERSION.RELEASE;
        String versionName = BuildConfig.VERSION_NAME;
        int versionCode = BuildConfig.VERSION_CODE;
        String displayCountry = Locale.getDefault().getDisplayCountry();
        String displayLanguage = Locale.getDefault().getDisplayLanguage();
        Timber.d(String.format(Locale.US, "Device Information:\n" +
                "device: %s\n" +
                "display: %s\n" +
                "fingerprint: %s\n" +
                "hardware: %s\n" +
                "host: %s\n" +
                "id: %s\n" +
                "manufacturer: %s\n" +
                "model: %s\n" +
                "product: %s\n" +
                "sdk: %d\n" +
                "codename: %s\n" +
                "release: %s\n" +
                "app version name: %s\n" +
                "app version code: %d\n" +
                "wifi: %s\n" +
                "mobile: %s\n" +
                "country: %s\n" +
                "language %s\n", device, display, fingerprint, hardware, host, id,
                model, manufacturer, product, sdk, codename, release, versionName, versionCode,
                wifiNetwork, mobileNetwork, displayCountry, displayLanguage));
    }

    //Delete the logs file after 7 days
    private void checkLogFileValidity() {
        long sevenDays = 7 * 24 * 60 * 60 * 1000;
        long lastLogTimestamp = mPreferencesManager.getLastLogDeletedTimestamp();
        if (lastLogTimestamp == 0) {
            mPreferencesManager.setLastLogDeletedTimestamp(System.currentTimeMillis());
        } else if (System.currentTimeMillis() - lastLogTimestamp > sevenDays) {
            FileUtils.deleteLogs(this);
            mPreferencesManager.setLastLogDeletedTimestamp(System.currentTimeMillis());
        }
    }

    protected void setupJodaTime() {
        JodaTimeAndroid.init(getApplicationContext());
    }

    // From https://developers.google.com/web/tools/chrome-devtools/remote-debugging/webviews
    // This enables all WebViews to be inspected by chrome on debuggable builds.
    private void enableWebDebugging() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)) {
                WebView.setWebContentsDebuggingEnabled(true);
            }
        }
    }

    private void setupMnemonicCode() {
        BitcoinUtils.init(getApplicationContext());
    }

    public static Account CreateSyncAccount(Context context) {
        boolean isNew = false;
        // Create the account type and default account
        Account newAccount = new Account(SyncConstants.ACCOUNT_NAME, SyncConstants.ACCOUNT_TYPE);
        // Get an instance of the Android account manager
        AccountManager accountManager = (AccountManager) context.getSystemService(ACCOUNT_SERVICE);
        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
        Timber.i("Karim Just before addACcountExpclitity");
        if (accountManager.addAccountExplicitly(newAccount, null, null)) {
            Timber.i("Karim Account Created");
            /*
             * If you don't set android:syncable="true" in
             * in your <provider> element in the manifest,
             * then call context.setIsSyncable(account, AUTHORITY, 1)
             * here.
             */
            isNew = true;
        } else {
            Timber.i("Karim Account Can't Created Some Error Occurred");
            /*
             * The account exists or some other error occurred. Log this, report it,
             * or handle it internally.
             */
            Account[] accounts = accountManager.getAccountsByType("com.google");
            Timber.i("Karim all accounts" + accounts);
            if (accounts.length > 0) {
                Timber.i("Karim " + accounts[0]);
                return accounts[0];
            }
        }
        if (isNew) {
            ContentResolver.requestSync(newAccount, SyncConstants.AUTHORITY, null);
        }
        return null;
    }

    public static String getGoogleAccount(Context context){
        String acc = null;
        try{
            AccountManager accountManager = (AccountManager) context.getSystemService(ACCOUNT_SERVICE);
            Account accounts[] = accountManager.getAccounts();
            if(accounts ==null){
                acc = "emulator@test.com";
            }
            else{
                for(Account account: accounts){
                    if(account.type.equals("com.google")){
                        Timber.i("Karim Account: " + account.name);
                        acc = account.name;
                    }
                }
            }
            if(acc==null)
                acc = "emulator@test.com";

        }
        catch (Exception e){
            e.printStackTrace();

        }
        return acc;
    }

}
