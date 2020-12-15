package io.certifico.app.data.inject;

import io.certifico.app.data.log.NoLoggingTree;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.logging.HttpLoggingInterceptor;
import timber.log.Timber;

@Module(includes = DataModule.class)
public class ProductionDataModule {

    @Provides
    @Singleton
    Timber.Tree provideLoggingTree() {
        return new Timber.DebugTree();
    }

    @Provides
    @Singleton
    NetworkParameters providesBitcoinNetworkParameters() {
        return MainNetParams.get();
    }

    @Provides
    HttpLoggingInterceptor.Level providesLogLevel() {
        return HttpLoggingInterceptor.Level.NONE;
    }
}
