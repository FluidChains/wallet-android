package io.certifico.app.data.inject;

import android.content.Context;

import io.certifico.app.data.CertificateManager;
import io.certifico.app.data.IssuerManager;
import io.certifico.app.data.bitcoin.BitcoinManager;
import io.certifico.app.data.preferences.SharedPreferencesManager;
import io.certifico.app.data.store.CertificateStore;
import io.certifico.app.data.store.ImageStore;
import io.certifico.app.data.store.IssuerStore;
import io.certifico.app.data.store.LMDatabaseHelper;
import io.certifico.app.data.webservice.CertificateService;
import io.certifico.app.data.webservice.IssuerService;

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
    ImageStore providesImageStore(Context context) {
        return new ImageStore(context);
    }

    @Provides
    @Singleton
    IssuerStore providesIssuerStore(LMDatabaseHelper databaseHelper, ImageStore imageStore) {
        return new IssuerStore(databaseHelper, imageStore);
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
