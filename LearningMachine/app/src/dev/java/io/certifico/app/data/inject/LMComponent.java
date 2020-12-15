package io.certifico.app.data.inject;

import android.app.Application;

import io.certifico.app.data.inject.DaggerLMComponent;

import javax.inject.Singleton;

import dagger.Component;

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
