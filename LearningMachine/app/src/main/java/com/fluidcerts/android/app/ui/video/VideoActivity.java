package com.fluidcerts.android.app.ui.video;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.fluidcerts.android.app.ui.LMSingleFragmentActivity;

public class VideoActivity extends LMSingleFragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected Fragment createFragment() {
        return VideoFragment.newInstance();
    }

}