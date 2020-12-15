package io.certifico.app.ui.issuer;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.certifico.app.R;
import io.certifico.app.data.IssuerManager;
import io.certifico.app.data.bitcoin.BitcoinManager;
import io.certifico.app.data.inject.Injector;
import io.certifico.app.databinding.FragmentIssuerInfoBinding;
import io.certifico.app.ui.LMFragment;

import javax.inject.Inject;

public class IssuerInfoFragment extends LMFragment {

    private static final String ARG_ISSUER_UUID = "IssuerInfoFragment.IssuerUuid";

    @Inject protected IssuerManager mIssuerManager;
    @Inject protected BitcoinManager mBitcoinManager;

    private FragmentIssuerInfoBinding mBinding;

    public static IssuerInfoFragment newInstance(String issuerUuid) {
        Bundle args = new Bundle();
        args.putString(ARG_ISSUER_UUID, issuerUuid);

        IssuerInfoFragment fragment = new IssuerInfoFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Injector.obtain(getContext())
                .inject(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_issuer_info, container, false);

        String issuerUuid = getArguments().getString(ARG_ISSUER_UUID);
        // TODO: should find the bitcoin address that was sent to the issuer
        mIssuerManager.getIssuer(issuerUuid)
                .compose(bindToMainThread())
                .subscribe(issuer -> {
                    IssuerInfoViewModel viewModel = new IssuerInfoViewModel(issuer);
                    mBinding.setIssuerInfo(viewModel);
                });

        return mBinding.getRoot();
    }
}
