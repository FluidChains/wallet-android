package com.fluidcerts.android.app.ui.sync;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.fluidcerts.android.app.R;
import com.fluidcerts.android.app.data.inject.Injector;
import com.fluidcerts.android.app.data.preferences.SharedPreferencesManager;
import com.fluidcerts.android.app.databinding.FragmentSyncAdapterSettingsBinding;
import com.fluidcerts.android.app.ui.LMFragment;

import javax.inject.Inject;

import timber.log.Timber;

public class SyncAdapterSettingsFragment extends LMFragment {

    private final String TAG = "Sync.SyncAdapterSettingsFragment ";

    @Inject SharedPreferencesManager mSharedPreferencesManager;

    private FragmentSyncAdapterSettingsBinding mBinding;

    public static SyncAdapterSettingsFragment newInstance() {return new SyncAdapterSettingsFragment(); }

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
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_sync_adapter_settings, container, false);
//        mBinding.syncAdapterSettingsEnableSyncSwitch.setChecked(mSharedPreferencesManager.getSyncAdapterEnabled());
//        mBinding.syncAdapterSettingsEnableSyncSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
//            SyncAdapterSettingsActivity syncActivity = (SyncAdapterSettingsActivity) getActivity();
//            if (syncActivity == null) {
//                return;
//            }
//            if (b) {
//                syncActivity.enableSync();
//            }
//            mSharedPreferencesManager.setSyncAdapterEnabled(b);
//        });
        mBinding.syncAdapterSettingsBackupButton.setOnClickListener(v -> {
            Timber.d(TAG + "Back-up Certificates button pressed");
            SyncAdapterSettingsActivity syncActivity = (SyncAdapterSettingsActivity) getActivity();
            if (syncActivity == null) {
                return;
            }
            syncActivity.backupCerts();
        });
        mBinding.syncAdapterSettingsRestoreButton.setOnClickListener(v -> {
            Timber.d(TAG + "Restore Certificates button pressed");
            SyncAdapterSettingsActivity syncActivity = (SyncAdapterSettingsActivity) getActivity();
            if (syncActivity == null) {
                return;
            } else {
                syncActivity.restoreCerts();
            }
        });
//        mBinding.syncAdapterSettingsShowFiles.setOnClickListener(v -> {
////            File root = new File(getActivity().getApplicationContext().getFilesDir().getAbsolutePath());
////            Timber.i(TAG + root.getAbsolutePath());
////            File[] dirs = root.listFiles();
////            for (File dir: dirs){
////                Timber.i(TAG + dir);
////            }
////            String[] files = getActivity().getApplicationContext().fileList();
////            for (String file: files) {
////                Timber.i(TAG + file);
////            }
//            java.io.File certDir = new java.io.File(getActivity().getApplicationContext().getFilesDir(), "tmp");
//            for (File file: certDir.listFiles()) {
//                Timber.i(TAG + file.getName());
//            }
//
//        });

        return mBinding.getRoot();
    }
}