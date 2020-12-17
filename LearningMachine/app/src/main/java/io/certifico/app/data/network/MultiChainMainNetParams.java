package io.certifico.app.data.network;

import com.google.common.collect.ImmutableList;

import org.bitcoinj.core.Context;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.KeyChain;
import org.bitcoinj.wallet.Wallet;

import io.certifico.app.util.BitcoinUtils;

public class MultiChainMainNetParams {

    public static Wallet createWallet(String chain, byte[] entropy, int usedAddresses) {

        String mChainCode;
        int mPurpose;
        int mCoin;
        int mAccount;
        NetworkParameters mNetworkParameters;

        switch(chain) {
            case "RUTAMainnet":
                mChainCode = "Rutanio seed";
                mPurpose = 44;
                mCoin = 462;
                mAccount = 0;
                mNetworkParameters = RutaMainNetParams.get();
                break;
            case "EXOSMainnet":
                mChainCode = "CivX seed";
                mPurpose = 44;
                mCoin = 248;
                mAccount = 0;
                mNetworkParameters = ExosMainNetParams.get();
                break;
            default:
                throw new UnsupportedOperationException(String.format("Unrecognized chain type %s", chain));
        }

        DeterministicSeed deterministicSeed = BitcoinUtils.createDeterministicSeed(entropy);

        ImmutableList<ChildNumber> accountPath = ImmutableList.of(
                new ChildNumber(mPurpose | ChildNumber.HARDENED_BIT),
                new ChildNumber(mCoin | ChildNumber.HARDENED_BIT),
                new ChildNumber(mAccount | ChildNumber.HARDENED_BIT)
        );

        Context context = new Context(mNetworkParameters);
        Context.propagate(context);
        Wallet wallet = Wallet.fromSeed(mNetworkParameters, deterministicSeed, accountPath, mChainCode);

        // discard the first 'n' addresses when restoring the wallet to account for those already
        // used in responding to issuer introductions
        if (usedAddresses > 0) {
            wallet.getActiveKeyChain().getKeys(KeyChain.KeyPurpose.RECEIVE_FUNDS, usedAddresses);
        }

        return wallet;
    }

    public static NetworkParameters getNetParams(String chain) {
        switch(chain) {
            case "RUTAMainnet":
                return RutaMainNetParams.get();
            case "EXOSMainnet":
                return ExosMainNetParams.get();
            default:
                throw new UnsupportedOperationException(String.format("Unrecognized chain type %s", chain));
        }
    }

}
