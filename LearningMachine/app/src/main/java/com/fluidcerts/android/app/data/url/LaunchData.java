package com.fluidcerts.android.app.data.url;

public class LaunchData {

    private LaunchType mLaunchType;

    private String mChain;
    private String mIntroUrl;
    private String mNonce;

    private String mCertUrl;

    public LaunchData(LaunchType launchType) {
        mLaunchType = launchType;
    }

    public LaunchData(LaunchType launchType, String chain, String introUrl, String nonce) {
        mLaunchType = launchType;
        mIntroUrl = introUrl;
        mNonce = nonce;
        mChain = chain;
    }

    public LaunchData(LaunchType launchType, String certUrl) {
        mLaunchType = launchType;
        mCertUrl = certUrl;
    }

    public LaunchType getLaunchType() {
        return mLaunchType;
    }

    public String getChain() {
        return mChain;
    }

    public String getIntroUrl() {
        return mIntroUrl;
    }

    public String getNonce() {
        return mNonce;
    }

    public String getCertUrl() {
        return mCertUrl;
    }
}
