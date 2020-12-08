package com.fluidcerts.android.app.data.network;

import android.os.Build;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;

import com.fluidcerts.android.app.LMConstants;
import com.fluidcerts.android.app.util.BitcoinUtils;
import com.google.common.collect.ImmutableList;

import org.bitcoinj.core.LegacyAddress;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.KeyChain;
import org.bitcoinj.wallet.Wallet;

import java.util.List;

import timber.log.Timber;

public class MultiChainParams {

    private String mChainName;
    private String mChainCode;
    private int mPurpose;
    private int mCoin;
    private int mAccount;
    private int mPrivateKeyHeader;
    private int mAddressHeader;
    private int mP2shHeader;

    public MultiChainParams(String chain) {
        mChainName = chain;
        switch(chain) {
            case "RUTAMainnet":
                mChainCode = "Rutanio seed";
                mPurpose = 44;
                mCoin = 462;
                mAccount = 0;
                mPrivateKeyHeader = 188;
                mAddressHeader = 60;
                mP2shHeader = 122;
                break;
            case "EXOSMainnet":
                mChainCode = "CivX seed";
                mPurpose = 44;
                mCoin = 248;
                mAccount = 0;
                mPrivateKeyHeader = 156;
                mAddressHeader = 28;
                mP2shHeader = 87;
                break;
            default:
                throw new UnsupportedOperationException(String.format("Unrecognized chain type %s", chain));
        }
    }

    public Wallet createWallet(byte[] entropy, int usedAddresses) {

        DeterministicSeed deterministicSeed = BitcoinUtils.createDeterministicSeed(entropy);

        ImmutableList<ChildNumber> accountPath = ImmutableList.of(
                            new ChildNumber(mPurpose | ChildNumber.HARDENED_BIT),
                            new ChildNumber(mCoin | ChildNumber.HARDENED_BIT),
                            new ChildNumber(mAccount | ChildNumber.HARDENED_BIT)
                    );

        NetworkParameters networkParameters = new MultiChainMainNetParams(mPrivateKeyHeader, mAddressHeader, mP2shHeader);

        Wallet wallet = Wallet.fromSeed(networkParameters, deterministicSeed, accountPath, mChainCode);

        // discard the first 'n' addresses when restoring the wallet to account for those already
        // used in responding to issuer introductions
        if (usedAddresses > 0) {
            wallet.getActiveKeyChain().getKeys(KeyChain.KeyPurpose.RECEIVE_FUNDS, usedAddresses);
        }

        return wallet;
    }

    public NetworkParameters getNetParams() {
        return new MultiChainMainNetParams(mPrivateKeyHeader, mAddressHeader, mP2shHeader);
    }
}
