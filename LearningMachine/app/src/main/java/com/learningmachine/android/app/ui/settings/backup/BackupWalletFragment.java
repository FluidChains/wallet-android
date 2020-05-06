package com.learningmachine.android.app.ui.settings.backup;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.learningmachine.android.app.BuildConfig;
import com.learningmachine.android.app.R;
import com.learningmachine.android.app.data.inject.Injector;
import com.learningmachine.android.app.databinding.FragmentBackupWalletBinding;
import com.learningmachine.android.app.ui.LMFragment;


public class BackupWalletFragment extends LMFragment {

    private FragmentBackupWalletBinding mBinding;

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public static BackupWalletFragment newInstance() {return newInstance(null, null); }

    public static BackupWalletFragment newInstance(String param1, String param2) {
        BackupWalletFragment fragment = new BackupWalletFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        Injector.obtain(getContext())
                .inject(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_backup_wallet, container, false);

        return mBinding.getRoot();

    }
}
