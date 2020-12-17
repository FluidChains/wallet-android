package io.certifico.app.data;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.certifico.app.data.error.IssuerAnalyticsException;
import io.certifico.app.data.model.IssuerRecord;
import io.certifico.app.data.store.IssuerStore;
import io.certifico.app.data.webservice.IssuerService;
import io.certifico.app.data.webservice.request.IssuerAnalytic;
import io.certifico.app.data.webservice.request.IssuerIntroductionRequest;
import io.certifico.app.data.webservice.response.IssuerResponse;
import io.certifico.app.util.GsonUtil;
import io.certifico.app.util.StringUtils;

import java.io.IOException;
import java.util.List;

import rx.Observable;
import timber.log.Timber;

public class IssuerManager {

    private IssuerStore mIssuerStore;
    private IssuerService mIssuerService;

    public IssuerManager(IssuerStore issuerStore, IssuerService issuerService) {
        mIssuerStore = issuerStore;
        mIssuerService = issuerService;
    }

    public Observable<IssuerRecord> getIssuer(String issuerUuid) {
        return Observable.just(mIssuerStore.loadIssuer(issuerUuid));
    }

    public Observable<IssuerRecord> getIssuerForCertificate(String certUuid) {
        return Observable.just(mIssuerStore.loadIssuerForCertificate(certUuid));
    }

    public Observable<List<IssuerRecord>> getIssuers() {
        return Observable.just(mIssuerStore.loadIssuers());
    }

    public Observable<IssuerResponse> fetchIssuer(String url) {
        return mIssuerService.getIssuer(url);
    }

    public Observable<String> serializeIssuer(IssuerResponse response) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String serialized = gson.toJson(response);
        return Observable.just(serialized);
    }

    public Observable<IssuerResponse> deserializeIssuer(String json) {
        Gson gson = new Gson();
        IssuerResponse issuerResponse = (IssuerResponse) gson.fromJson(json, IssuerResponse.class);
        return Observable.just(issuerResponse);
    }

    public Observable<String> addIssuer(IssuerIntroductionRequest request) {
        IssuerResponse issuer = request.getIssuerResponse();
        return mIssuerService.postIntroduction(issuer.getIntroUrl(), request)
                .map(aVoid -> saveIssuer(issuer, request.getBitcoinAddress()));
    }

    public String saveIssuer(IssuerResponse issuer, String recipientPubKey) {
        mIssuerStore.saveIssuerResponse(issuer, recipientPubKey);
        return issuer.getUuid();
    }

    public Observable<Void> certificateViewed(String certUuid) {
        return sendAnalyticsAction(certUuid, IssuerAnalytic.Action.VIEWED);
    }

    public Observable<Void> certificateVerified(String certUuid) {
        return sendAnalyticsAction(certUuid, IssuerAnalytic.Action.VERIFIED);
    }

    public Observable<Void> certificateShared(String certUuid) {
        return sendAnalyticsAction(certUuid, IssuerAnalytic.Action.SHARED);
    }

    private Observable<Void> sendAnalyticsAction(String certUuid, IssuerAnalytic.Action action) {
        return getIssuerForCertificate(certUuid).flatMap(issuer -> {
            String issuerAnalyticsUrlString = issuer.getAnalyticsUrlString();
            if (StringUtils.isEmpty(issuerAnalyticsUrlString)) {
                return Observable.error(new IssuerAnalyticsException());
            }
            IssuerAnalytic issuerAnalytic = new IssuerAnalytic(certUuid, action);
            return mIssuerService.postIssuerAnalytics(issuerAnalyticsUrlString, issuerAnalytic);
        });
    }
}
