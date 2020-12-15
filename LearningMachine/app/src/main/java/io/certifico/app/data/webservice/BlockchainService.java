package io.certifico.app.data.webservice;

import io.certifico.app.data.model.TxRecord;

import retrofit2.http.GET;
import retrofit2.http.Path;
import rx.Observable;

public interface BlockchainService {
    @GET("/rawtx/{transaction_id}")
    Observable<TxRecord> getBlockchain(@Path("transaction_id") String transactionId);
}
