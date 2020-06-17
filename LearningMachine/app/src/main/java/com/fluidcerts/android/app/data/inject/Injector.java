package com.fluidcerts.android.app.data.inject;

import android.content.Context;

import com.fluidcerts.android.app.LMConstants;

public final class Injector {

    private Injector() {
        throw new AssertionError("No instances.");
    }

    @SuppressWarnings("ResourceType") // Explicitly doing a custom service.
    public static LMGraph obtain(Context context) {
        return (LMGraph) context.getApplicationContext()
                .getSystemService(LMConstants.INJECTOR_SERVICE);
    }

    public static boolean matchesService(String name) {
        return LMConstants.INJECTOR_SERVICE.equals(name);
    }
}
