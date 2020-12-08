package com.fluidcerts.android.app.ui.home;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;

import com.fluidcerts.android.app.R;
import com.fluidcerts.android.app.ui.LMSingleFragmentActivity;
import com.fluidcerts.android.app.util.FileLoggingTree;

import timber.log.Timber;

public class HomeActivity extends LMSingleFragmentActivity {

    private static final String EXTRA_ISSUER_CHAIN = "HomeActivity.IssuerChain";
    private static final String EXTRA_ISSUER_URL = "HomeActivity.IssuerUrl";
    private static final String EXTRA_CERT_URL = "HomeActivity.CertUrl";
    private static final String EXTRA_ISSUER_NONCE = "HomeActivity.IssuerNonce";
    private static final String EXTRA_LINK_TYPE = "HomeActivity.LinkType";

    public static final String LINK_TYPE_ISSUER = "HomeActivity.LinkTypeIssuer";
    public static final String LINK_TYPE_CERT = "HomeActivity.LinkTypeCert";

    private HomeFragment mLastFragment;

    public static Intent newIntentForIssuer(Context context, String chain, String issuerUrlString, String nonce) {
        Intent intent = new Intent(context, HomeActivity.class);
        intent.putExtra(EXTRA_ISSUER_CHAIN, chain);
        intent.putExtra(EXTRA_ISSUER_URL, issuerUrlString);
        intent.putExtra(EXTRA_ISSUER_NONCE, nonce);
        intent.putExtra(EXTRA_LINK_TYPE, LINK_TYPE_ISSUER);
        return intent;
    }

    public static Intent newIntentForCert(Context context, String certUrl) {
        Intent intent = new Intent(context, HomeActivity.class);
        intent.putExtra(EXTRA_CERT_URL, certUrl);
        intent.putExtra(EXTRA_LINK_TYPE, LINK_TYPE_CERT);
        return intent;
    }

    @Override
    protected Fragment createFragment() {
        Timber.i("Sync.HomeActivity createFragment()");
        String linkType = getIntent().getStringExtra(EXTRA_LINK_TYPE);
        if (LINK_TYPE_ISSUER.equals(linkType)) {
            String issuerChainString = getIntent().getStringExtra(EXTRA_ISSUER_CHAIN);
            String issuerUrlString = getIntent().getStringExtra(EXTRA_ISSUER_URL);
            String nonce = getIntent().getStringExtra(EXTRA_ISSUER_NONCE);
            mLastFragment = HomeFragment.newInstanceForIssuer(issuerChainString, issuerUrlString, nonce);
        } else if (LINK_TYPE_CERT.equals(linkType)) {
            String certUrl = getIntent().getStringExtra(EXTRA_CERT_URL);
            mLastFragment = HomeFragment.newInstanceForCert(certUrl);
        } else {
            mLastFragment = HomeFragment.newInstance();
        }
        return mLastFragment;
    }

    private HomeFragment getLastFragment() {
        if (mLastFragment == null) {
            //Get from super
            mLastFragment = (HomeFragment) getFragment();
        }
        return mLastFragment;
    }

    @Override
    public String getActionBarTitle() {
        return getString(R.string.home_issuers);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        String linkType = intent.getStringExtra(EXTRA_LINK_TYPE);
        if (LINK_TYPE_ISSUER.equals(linkType)) {
            String chain = intent.getStringExtra(EXTRA_ISSUER_CHAIN);
            String issuerUrl = intent.getStringExtra(EXTRA_ISSUER_URL);
            String nonce = intent.getStringExtra(EXTRA_ISSUER_NONCE);
            getLastFragment().updateArgsIssuer(chain, issuerUrl, nonce);
        } else if (LINK_TYPE_CERT.equals(linkType)) {
            String certUrl = intent.getStringExtra(EXTRA_CERT_URL);
            getLastFragment().updateArgsCert(certUrl);
        }

    }

    @Override
    protected void onDestroy() {
        FileLoggingTree.saveLogToFile(this);
        super.onDestroy();
    }
}
