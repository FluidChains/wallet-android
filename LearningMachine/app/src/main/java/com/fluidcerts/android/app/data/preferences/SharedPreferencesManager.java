package com.fluidcerts.android.app.data.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

public class SharedPreferencesManager {

    private static final String PREF_NAME = "LearningMachine";
    private static final String PREF_FIRST_LAUNCH = "SharedPreferencesManager.FirstLaunch";
    private static final String PREF_RETURN_USER = "SharedPreferencesManager.ReturnUser";
    private static final String PREF_SEEN_BACKUP_PASSPHRASE= "SharedPreferencesManager.SeenBackupPassphrase";
    private static final String PREF_LEGACY_RECEIVE_ADDRESS = "SharedPreferencesManager.LegacyReceiveAddress";

    private static final String DELAYED_ISSUER_URL = "SharedPreferencesManager.DelayedIssuer.URL";
    private static final String DELAYED_ISSUER_NONCE = "SharedPreferencesManager.DelayedIssuer.Nonce";

    private static final String DELAYED_CERTIFICATE_URL = "SharedPreferencesManager.DelayedCertificate.URL";

    private static final String PREF_LAST_LOG_DELETED_TIMESTAMP = "SharedPreferencesManager.Logs.LogsDeletedTimestamp";

    private static final String PREF_SYNC_ADAPTER = "SharedPreferencesManager.SyncAdapter";

    private static final String PREF_APP_PASS = "SharedPreferencesManager.AppPass";
    private static final String PREF_APP_PASS_IV = "SharedPreferencesManager.AppPass.IV";

    private SharedPreferences mPrefs;

    public SharedPreferencesManager(Context context) {
        mPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public String getDelayedIssuerURL() {
        return mPrefs.getString(DELAYED_ISSUER_URL, "");
    }
    public String getDelayedIssuerNonce() {
        return mPrefs.getString(DELAYED_ISSUER_NONCE, "");
    }

    public void setDelayedIssuerURL(String issuerURL, String issuerNonce) {
        mPrefs.edit()
                .putString(DELAYED_ISSUER_URL, issuerURL)
                .putString(DELAYED_ISSUER_NONCE, issuerNonce)
                .apply();
    }

    public String getDelayedCertificateURL() {
        return mPrefs.getString(DELAYED_CERTIFICATE_URL, "");
    }

    public void setDelayedCertificateURL(String certificateURL) {
        mPrefs.edit()
                .putString(DELAYED_CERTIFICATE_URL, certificateURL)
                .apply();
    }

    public boolean isFirstLaunch() {
        return mPrefs.getBoolean(PREF_FIRST_LAUNCH, true);
    }

    public void setFirstLaunch(boolean firstLaunch) {
        mPrefs.edit()
                .putBoolean(PREF_FIRST_LAUNCH, firstLaunch)
                .apply();
    }

    public boolean shouldShowWelcomeBackUserFlow() {
        // if this is not a first time user and we have not stored the special preference key
        return isFirstLaunch() == false && mPrefs.contains(PREF_SEEN_BACKUP_PASSPHRASE) == false;
    }

    public boolean hasSeenBackupPassphraseBefore() {
        return mPrefs.getBoolean(PREF_SEEN_BACKUP_PASSPHRASE, false);
    }

    public void setHasSeenBackupPassphraseBefore(boolean hasSeenBackupPassphraseBefore) {
        mPrefs.edit()
                .putBoolean(PREF_SEEN_BACKUP_PASSPHRASE, hasSeenBackupPassphraseBefore)
                .apply();
    }

    public boolean wasReturnUser() {
        return mPrefs.getBoolean(PREF_RETURN_USER, true);
    }

    public void setWasReturnUser(boolean returnUser) {
        mPrefs.edit()
                .putBoolean(PREF_RETURN_USER, returnUser)
                .apply();
    }

    public String getLegacyReceiveAddress() {
        return mPrefs.getString(PREF_LEGACY_RECEIVE_ADDRESS, null);
    }

    public void setLegacyReceiveAddress(String receiveAddress) {
        mPrefs.edit()
                .putString(PREF_LEGACY_RECEIVE_ADDRESS, receiveAddress)
                .apply();
    }

    public void setLastLogDeletedTimestamp(long timestamp) {
        mPrefs.edit()
                .putLong(PREF_LAST_LOG_DELETED_TIMESTAMP, timestamp)
                .apply();
    }

    public long getLastLogDeletedTimestamp() {
        return mPrefs.getLong(PREF_LAST_LOG_DELETED_TIMESTAMP, 0);
    }

    public void setPasswordIV(byte[] array) {
        String string = Base64.encodeToString(array, Base64.DEFAULT);
        mPrefs.edit()
                .putString(PREF_APP_PASS_IV, string)
                .apply();
    }

    public byte[] getPasswordIV() {
        String string = mPrefs.getString(PREF_APP_PASS_IV, null);
        if (string == null) {
            return null;
        }
        return Base64.decode(string, Base64.DEFAULT);
    }

    public void setLockScreenPassword(byte[] array) {
        String string = Base64.encodeToString(array, Base64.DEFAULT);
        mPrefs.edit()
                .putString(PREF_APP_PASS, string)
                .apply();
    }

    public byte[] getLockScreenPassword() {
        String string = mPrefs.getString(PREF_APP_PASS, null);
        if (string == null) {
            return null;
        }
        return Base64.decode(string, Base64.DEFAULT);
    }

    public void setSyncAdapterEnabled(boolean enabled) {
        mPrefs.edit()
                .putBoolean(PREF_SYNC_ADAPTER, enabled)
                .apply();
    }

    public boolean getSyncAdapterEnabled() {
        return mPrefs.getBoolean(PREF_SYNC_ADAPTER, false);
    }

}
