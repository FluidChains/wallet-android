package com.fluidcerts.android.app.ui.settings;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.fluidcerts.android.app.R;
import com.fluidcerts.android.app.databinding.FragmentLearnMoreBinding;
import com.fluidcerts.android.app.ui.LMFragment;
import com.fluidcerts.android.app.ui.video.VideoActivity;
import com.smallplanet.labalib.Laba;

import timber.log.Timber;

public class LearnMoreFragment extends LMFragment {

    private FragmentLearnMoreBinding mBinding;

    public static LearnMoreFragment newInstance() {
        return new LearnMoreFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_learn_more, container, false);

        String fileName = "android.resource://" + getActivity().getPackageName() + "/raw/background";

        mBinding.backgroundVideoCover.setAlpha(1.0f);
        mBinding.backgroundVideo.setVideoURI(Uri.parse(fileName));
        mBinding.backgroundVideo.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                Timber.d("SETTING VIDEO SCALING MODE");
                mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
                mp.setLooping(true);
                mp.setScreenOnWhilePlaying(false);

                Laba.Animate(mBinding.backgroundVideoCover, "d1|f0", null);
            }
        });
        mBinding.backgroundVideo.start();


        mBinding.playVideo.setOnClickListener(view2 -> {
            startActivity(new Intent(getContext(), VideoActivity.class));
        });

        mBinding.visitLink.setOnClickListener(v -> {
            String url = "https://www.certifico.io";
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
        });

        return mBinding.getRoot();
    }

}
