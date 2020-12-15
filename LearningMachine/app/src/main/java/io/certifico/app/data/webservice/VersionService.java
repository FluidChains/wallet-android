package io.certifico.app.data.webservice;

import io.certifico.app.data.model.Version;

import retrofit2.http.GET;
import rx.Observable;

public interface VersionService {
    @GET("/versions.json")
    Observable<Version> getVersion();
}
