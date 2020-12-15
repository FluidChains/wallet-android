package io.certifico.app.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import io.certifico.app.R;
import io.certifico.app.ui.lock.LockScreenActivity;

public abstract class LMSingleFragmentActivity extends LMActivity {

    protected abstract Fragment createFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (this.getClass().getSimpleName().equals("RevealPassphraseActivity")) {
            Intent lockIntent = new Intent(this, LockScreenActivity.class);
            startActivityForResult(lockIntent, 0);
        } else {
            setContentView(R.layout.activity_single_fragment);
            getFragment();
        }
    }

    protected Fragment getFragment() {
        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragment_container);
        if (fragment == null) {
            fragment = createFragment();
            fm.beginTransaction()
                    .add(R.id.fragment_container, fragment)
                    .commit();
        }

        return fragment;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);

        if (resultCode == Activity.RESULT_OK) {
            setContentView(R.layout.activity_single_fragment);
            getFragment();
        }
    }

}
