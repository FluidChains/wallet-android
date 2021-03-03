package io.certifico.app.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import io.certifico.app.R;
import io.certifico.app.data.IssuerManager;
import io.certifico.app.data.bitcoin.BitcoinManager;
import io.certifico.app.data.inject.Injector;
import io.certifico.app.data.preferences.SharedPreferencesManager;
import io.certifico.app.data.webservice.request.IssuerIntroductionRequest;
import io.certifico.app.util.DialogUtils;
import io.certifico.app.util.ErrorUtils;
import io.certifico.app.util.StringUtils;

import javax.inject.Inject;

import rx.Observable;
import timber.log.Timber;

public abstract class LMIssuerBaseFragment extends LMFragment {
    protected static final String ARG_ISSUER_URL = "LMIssuerBaseFragment.IssuerUrl";
    protected static final String ARG_CERT_URL = "LMIssuerBaseFragment.CertUrl";
    protected static final String ARG_ISSUER_NONCE = "LMIssuerBaseFragment.IssuerNonce";
    protected static final String ARG_LINK_TYPE = "LMIssuerBaseFragment.LinkType";
    protected static final String ARG_LINK_TYPE_ISSUER = "LMIssuerBaseFragment.LinkTypeIssuer";
    protected static final String ARG_LINK_TYPE_CERT = "LMIssuerBaseFragment.LinkTypeCert";

    protected static final int REQUEST_WEB_AUTH = 1;

    @Inject protected BitcoinManager mBitcoinManager;
    @Inject protected IssuerManager mIssuerManager;
    @Inject protected SharedPreferencesManager mSharedPreferencesManager;

    protected String mIntroUrl;
    protected String mCertUrl;
    protected String mNonce;
    protected String mLinkType;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Injector.obtain(getContext())
                .inject(this);
        setHasOptionsMenu(true);
    }

    protected void handleArgs() {
        Bundle args = getArguments();
        if (args == null) {
            return;
        }
        String issuerUrlString = args.getString(ARG_ISSUER_URL);
        if (!StringUtils.isEmpty(issuerUrlString)) {
            mIntroUrl = issuerUrlString;
        }
        String issuerNonce = args.getString(ARG_ISSUER_NONCE);
        if (!StringUtils.isEmpty(issuerNonce)) {
            mNonce = issuerNonce;
        }

        String certUrl = args.getString(ARG_CERT_URL);
        if (!StringUtils.isEmpty(certUrl)) {
            mCertUrl = certUrl;
        }

        String linkType = args.getString(ARG_LINK_TYPE);
        if (!StringUtils.isEmpty(linkType)) {
            mLinkType = linkType;
        }
    }

    protected void startIssuerIntroduction() {
        Timber.i("Starting Issuer Introduction");
        displayProgressDialog(R.string.fragment_add_issuer_adding_issuer_progress_dialog_message);
        checkVersion(updateNeeded -> {
            if (updateNeeded) {
                hideProgressDialog();
            } else {
                introduceIssuer();
            }
        });
    }

    protected void introduceIssuer() {
        Timber.i("Starting process to identify and introduce issuer at " + mIntroUrl);
        if (mBitcoinManager == null || mIssuerManager == null) {
            Timber.e("Bitcoin Manager or Issuer Manager not available");
            return;
        }

        mIssuerManager.fetchIssuer(mIntroUrl)
                .doOnSubscribe(this::addIssuerOnSubscribe)
                .doOnCompleted(this::addIssuerOnCompleted)
                .doOnError(throwable -> addIssuerOnError())
                .compose(bindToMainThread())
                .flatMap(response -> Observable.combineLatest(
                        mBitcoinManager.getFreshBitcoinAddress(response.getChain()),
                        Observable.just(mNonce),
                        Observable.just(response),
                        IssuerIntroductionRequest::new)
                )
                .subscribe(request -> {
                    Timber.i("Issuer is using %s chain", request.getIssuerResponse().getChain());
                    Timber.i(String.format("Issuer identification at %s succeeded. Beginning introduction step.", mIntroUrl));
                    Timber.i("Skipping introduction for testing purposes");
                    if (request.getIssuerResponse().usesWebAuth()) {
                        performWebAuth(request);
                    } else {
                        performStandardIssuerIntroduction(request);
                    }
                    hideProgressDialog();
                }, throwable -> {
                    Timber.e(throwable, "Error during issuer identification: " + ErrorUtils.getErrorFromThrowable(throwable));
                    displayErrors(throwable, DialogUtils.ErrorCategory.ISSUER, R.string.error_title_message);
                });

    }

    private void performStandardIssuerIntroduction(IssuerIntroductionRequest request) {
        Timber.i("Performing Standard Issuer Introduction");
        mIssuerManager.addIssuer(request)
                .compose(bindToMainThread())
                .subscribe(this::addIssuerOnIssuerAdded,
                        throwable -> displayErrors(throwable, DialogUtils.ErrorCategory.ISSUER, R.string.error_title_message));
    }

    private void performWebAuth(IssuerIntroductionRequest request) {
        Timber.i("Presenting the web view in the Add Issuer screen");
        hideProgressDialog();
        Intent intent = WebAuthActivity.newIntent(getContext(), request);
        startActivityForResult(intent, REQUEST_WEB_AUTH);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_WEB_AUTH) {
            if (!WebAuthActivity.isWebAuthSuccess(data) || resultCode != Activity.RESULT_OK) {
                Timber.i("OAuth authentication failure");
                return;
            }
            String bitcoinAddress = WebAuthActivity.getWebAuthBitcoinAddress(data);
            Timber.i("Got result from OAuth. Fetching Issuer");
            mIssuerManager.fetchIssuer(mIntroUrl)
                    .doOnSubscribe(() -> displayProgressDialog(R.string.fragment_add_issuer_adding_issuer_progress_dialog_message))
                    .compose(bindToMainThread())
                    .map(issuer -> mIssuerManager.saveIssuer(issuer, bitcoinAddress))
                    .subscribe(this::addIssuerOnIssuerAdded, throwable -> {
                        Timber.e(throwable, "Error during issuer introduction: " + ErrorUtils.getErrorFromThrowable(throwable));
                        displayErrors(throwable, DialogUtils.ErrorCategory.ISSUER, R.string.error_title_message);
                    });
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    protected abstract void addIssuerOnSubscribe();
    protected abstract void addIssuerOnCompleted();
    protected abstract void addIssuerOnError();
    protected abstract void addIssuerOnIssuerAdded(String uuid);

}
