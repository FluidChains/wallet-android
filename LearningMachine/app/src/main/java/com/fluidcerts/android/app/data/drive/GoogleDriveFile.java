package com.fluidcerts.android.app.data.drive;

import java.io.InputStream;

public class GoogleDriveFile {
    public final String name;
    public final InputStream stream;

    public GoogleDriveFile(String test, InputStream stream) {
        this.name = test;
        this.stream = stream;
    }
}
