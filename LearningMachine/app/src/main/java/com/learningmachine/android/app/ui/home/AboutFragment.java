package com.learningmachine.android.app.ui.home;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.learningmachine.android.app.BuildConfig;
import com.learningmachine.android.app.data.inject.Injector;
import com.learningmachine.android.app.R;
import com.learningmachine.android.app.databinding.FragmentAboutBinding;
import com.learningmachine.android.app.ui.LMFragment;

import timber.log.Timber;

public class AboutFragment extends LMFragment {

    private FragmentAboutBinding mBinding;

    private static final String ARG_APP_VERSION = "CertificateInfoFragment.CertificateUuid";

    public static AboutFragment newInstance() {return newInstance(null); }

    public static AboutFragment newInstance(String appVersion) {
        Bundle args = new Bundle();
        args.putString(ARG_APP_VERSION, appVersion);

        AboutFragment fragment = new AboutFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        Injector.obtain(getContext())
                .inject(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_about, container, false);
        mBinding.SettingsAboutVersion.setText(BuildConfig.VERSION_NAME);

        return mBinding.getRoot();
    }
}
