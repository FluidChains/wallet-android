package com.fluidcerts.android.app.data.network;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;

public class MultiChainMainNetParams extends MainNetParams {

    MultiChainMainNetParams(int privKeyHeader, int addrHeader, int p2shHddr) {
        super();
        dumpedPrivateKeyHeader = privKeyHeader;
        addressHeader = addrHeader;
        p2shHeader = p2shHddr;
    }
}
