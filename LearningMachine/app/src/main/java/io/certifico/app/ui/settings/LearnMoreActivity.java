package io.certifico.app.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import io.certifico.app.ui.LMSingleFragmentActivity;

public class LearnMoreActivity extends LMSingleFragmentActivity {

    private static final String EXTRA_ACTION_BAR_TITLE = "LMSingleFragmentActivity.ActionBarTitle";

    public static Intent newIntent(Context context, String actionBarTitle) {
        Intent intent = new Intent(context, LearnMoreActivity.class);
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
        return LearnMoreFragment.newInstance();
    }

    public String getActionBarTitle() {
        return getIntent().getStringExtra(EXTRA_ACTION_BAR_TITLE);
    }

    @Override
    protected boolean requiresBackNavigation() {
        return true;
    }
}
