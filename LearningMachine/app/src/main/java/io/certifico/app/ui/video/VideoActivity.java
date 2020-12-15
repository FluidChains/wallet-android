package io.certifico.app.ui.video;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import io.certifico.app.ui.LMSingleFragmentActivity;

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