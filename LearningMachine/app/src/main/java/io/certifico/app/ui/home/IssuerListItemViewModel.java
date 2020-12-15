package io.certifico.app.ui.home;

import android.content.Context;
import android.content.res.Resources;
import android.databinding.BaseObservable;
import android.databinding.Bindable;

import io.certifico.app.R;
import io.certifico.app.data.model.IssuerRecord;

public class IssuerListItemViewModel extends BaseObservable {

    private IssuerRecord mIssuer;

    public IssuerListItemViewModel() {
    }

    @Bindable
    public String getTitle() {
        if (mIssuer == null) {
            return null;
        }
        return mIssuer.getName();
    }

    public String getNumberOfCertificatesAsString(Context context) {
        if (mIssuer == null) {
            return null;
        }

        Resources resources = context.getResources();

        if (mIssuer.cachedNumberOfCertificatesForIssuer == 0) {
            return "";
        }

        return resources.getQuantityString(R.plurals.certificate_counting,
                mIssuer.cachedNumberOfCertificatesForIssuer,
                mIssuer.cachedNumberOfCertificatesForIssuer);
    }

    public void bindIssuer(IssuerRecord issuer) {
        mIssuer = issuer;
        notifyChange();
    }

    public IssuerRecord getIssuer() {
        return mIssuer;
    }
}
