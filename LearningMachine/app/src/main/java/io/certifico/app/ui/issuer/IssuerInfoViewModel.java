package io.certifico.app.ui.issuer;

import android.databinding.BaseObservable;
import android.databinding.Bindable;

import io.certifico.app.data.model.IssuerRecord;
import io.certifico.app.util.DateUtils;

public class IssuerInfoViewModel extends BaseObservable {

    private IssuerRecord mIssuer;

    public IssuerInfoViewModel(IssuerRecord issuer) {
        mIssuer = issuer;
    }

    @Bindable
    public String getTitle() {
        if (mIssuer == null) {
            return null;
        }
        return mIssuer.getName();
    }

    @Bindable
    public String getIssuerUrl() {
        if (mIssuer == null) {
            return null;
        }
        return mIssuer.getIssuerURL();
    }



    @Bindable
    public String getDate() {
        if (mIssuer == null) {
            return null;
        }
        String dateString = mIssuer.getIntroducedOn();
        return DateUtils.formatDateString(dateString);
    }

    @Bindable
    public String getSharedAddress() {
        return mIssuer.getRecipientPubKey();
    }

    @Bindable
    public String getUrl() {
        if (mIssuer == null) {
            return null;
        }
        return mIssuer.getUuid();
    }

    @Bindable
    public String getEmail() {
        if (mIssuer == null) {
            return null;
        }
        return mIssuer.getEmail();
    }

    @Bindable
    public String getChain() {
        if (mIssuer == null) {
            return null;
        }
        return mIssuer.getChain();
    }

    @Bindable
    public String getDescription() {
        if (mIssuer == null) {
            return null;
        }
        return mIssuer.getName(); // TODO: confirm whether there's a separate description field
    }
}
