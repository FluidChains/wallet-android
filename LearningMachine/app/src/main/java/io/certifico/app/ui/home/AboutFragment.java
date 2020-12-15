package io.certifico.app.ui.home;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.fluidcerts.android.app.BuildConfig;
import io.certifico.app.data.inject.Injector;
import com.fluidcerts.android.app.R;
import com.fluidcerts.android.app.databinding.FragmentAboutBinding;
import io.certifico.app.ui.LMFragment;

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
        mBinding.appVersionTitle.setText(getString(R.string.app_name) + " Version:");
        mBinding.aboutVersionCode.setText(BuildConfig.VERSION_NAME);
        mBinding.contactLink.setOnClickListener(v -> {
            Timber.i("Send email to contact link");

            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            // recipient
            emailIntent.putExtra(Intent.EXTRA_EMAIL  , new String[]{getString(R.string.about_contact_email)});
            // subject
            emailIntent .putExtra(Intent.EXTRA_SUBJECT, "Help with Certifico version " + BuildConfig.VERSION_NAME);
            emailIntent.setType("message/rfc822");
            try {
                startActivity(Intent.createChooser(emailIntent, "Send email..."));
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(getActivity(), "There are no email clients installed.", Toast.LENGTH_SHORT).show();
            }
        });

        return mBinding.getRoot();
    }
}
