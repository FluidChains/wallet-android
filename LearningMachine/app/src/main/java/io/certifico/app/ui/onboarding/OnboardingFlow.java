package io.certifico.app.ui.onboarding;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

public class OnboardingFlow implements Serializable {

    private int mPosition;
    private List<OnboardingScreen> mScreens;

    public enum FlowType {
        UNKNOWN(Arrays.asList(OnboardingScreen.ACCOUNT_CHOOSER)),
        BACKUP_ONLY(Arrays.asList(OnboardingScreen.WELCOME_BACK, OnboardingScreen.BACKUP_PASSPHRASE)),
        NEW_ACCOUNT(Arrays.asList(OnboardingScreen.ACCOUNT_CHOOSER, OnboardingScreen.VIEW_PASSPHRASE, OnboardingScreen.BACKUP_PASSPHRASE)),
        EXISTING_ACCOUNT(Arrays.asList(OnboardingScreen.ACCOUNT_CHOOSER, OnboardingScreen.PASTE_PASSPHRASE)),
        EXISTING_ACCOUNT_GOOGLE(Arrays.asList(OnboardingScreen.ACCOUNT_CHOOSER, OnboardingScreen.PASTE_PASSPHRASE_GOOGLE));

        private List<OnboardingScreen> mScreens;

        FlowType(List<OnboardingScreen> mScreens) {
            this.mScreens = mScreens;
        }

        public List<OnboardingScreen> getScreens() {
            return mScreens;
        }
    }

    public OnboardingFlow(FlowType flowType) {
        mScreens = flowType.getScreens();
    }

    public List<OnboardingScreen> getScreens() {
        return mScreens;
    }

    public int getPosition() {
        return mPosition;
    }

    public void setPosition(int position) {
        mPosition = position;
    }
}
