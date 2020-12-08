package com.fluidcerts.android.app.data.inject;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.fluidcerts.android.app.data.CertificateManager;
import com.fluidcerts.android.app.data.IssuerManager;
import com.fluidcerts.android.app.data.bitcoin.BitcoinManager;
import com.fluidcerts.android.app.data.preferences.SharedPreferencesManager;
import com.fluidcerts.android.app.data.store.CertificateStore;
import com.fluidcerts.android.app.data.store.ImageStore;
import com.fluidcerts.android.app.data.store.IssuerStore;
import com.fluidcerts.android.app.data.store.LMDatabaseHelper;
import com.fluidcerts.android.app.data.webservice.CertificateService;
import com.fluidcerts.android.app.data.webservice.IssuerService;

import org.bitcoinj.core.NetworkParameters;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class DataModule {

    @Provides
    @Singleton
    BitcoinManager providesBitcoinManager(Context context, NetworkParameters networkParameters, IssuerStore issuerStore, CertificateStore certificateStore, SharedPreferencesManager sharedPreferencesManager) {
        return new BitcoinManager(context, networkParameters, issuerStore, certificateStore, sharedPreferencesManager);
    }

    @Provides
    @Singleton
    LMDatabaseHelper provideLmDatabase(Context context) {
        return new LMDatabaseHelper(context);
    }

    @Provides
    @Singleton
    SQLiteDatabase providesSQLiteDatabase(LMDatabaseHelper databaseHelper) {
        return databaseHelper.getWritableDatabase();
    }

    @Provides
    @Singleton
    ImageStore providesImageStore(Context context) {
        return new ImageStore(context);
    }

    @Provides
    @Singleton
    IssuerStore providesIssuerStore(Context context, LMDatabaseHelper databaseHelper, ImageStore imageStore) {
        return new IssuerStore(context, databaseHelper, imageStore);
    }

    @Provides
    @Singleton
    IssuerManager providesIssuerManager(IssuerStore issuerStore, IssuerService issuerService) {
        return new IssuerManager(issuerStore, issuerService);
    }

    @Provides
    @Singleton
    CertificateManager providesCertificateManager(Context context, CertificateStore certificateStore,
                                                  IssuerStore issuerStore, CertificateService certificateService, BitcoinManager bitcoinManager, IssuerManager issuerManager) {
        return new CertificateManager(context, certificateStore, issuerStore, certificateService, bitcoinManager, issuerManager);
    }

    @Provides
    @Singleton
    CertificateStore providesCertificateStore(LMDatabaseHelper databaseHelper) {
        return new CertificateStore(databaseHelper);
    }

    @Provides
    @Singleton
    SharedPreferencesManager providesSharedPreferencesManager(Context context) {
        return new SharedPreferencesManager(context);
    }
}
