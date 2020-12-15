package com.fluidcerts.android.app.data.inject;

import android.app.Application;

import javax.inject.Singleton;

import dagger.Component;
import io.certifico.app.data.inject.ApiModule;
import io.certifico.app.data.inject.LMGraph;
import io.certifico.app.data.inject.LMModule;

@Singleton
@Component(modules = {LMModule.class, DevDataModule.class, ApiModule.class})
public interface LMComponent extends LMGraph {

    final class Initializer {
        public static LMGraph init(Application application) {
            return DaggerLMComponent.builder()
                    .lMModule(new LMModule(application))
                    .build();
        }
    }
}
