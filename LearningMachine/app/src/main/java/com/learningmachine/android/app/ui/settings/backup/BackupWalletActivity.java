package com.learningmachine.android.app.ui.settings.backup;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.os.Bundle;

import com.learningmachine.android.app.ui.LMSingleFragmentActivity;

public class BackupWalletActivity extends LMSingleFragmentActivity {

    private static final String EXTRA_ACTION_BAR_TITLE = "LMWebActivity.ActionBarTitle";

    public static Intent newIntent(Context context, String actionBarTitle) {
        Intent intent = new Intent(context, BackupWalletActivity.class);
        intent.putExtra(EXTRA_ACTION_BAR_TITLE, actionBarTitle);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupActionBar();
    }

    @Override
    public Fragment createFragment() {
        return BackupWalletFragment.newInstance();
    }

    public String getActionBarTitle() {
        return getIntent().getStringExtra(EXTRA_ACTION_BAR_TITLE);
    }

    @Override
    protected boolean requiresBackNavigation() {
        return true;
    }

}
