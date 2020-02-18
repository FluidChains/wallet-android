package com.learningmachine.android.app.util;

import android.content.Context;
import android.content.res.AssetManager;
import android.support.annotation.NonNull;

import com.learningmachine.android.app.LMConstants;

import com.google.common.collect.ImmutableList;

import org.bitcoinj.script.Script.ScriptType;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.LegacyAddress;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.wallet.DeterministicKeyChain;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.Protos;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.WalletExtension;
import org.bitcoinj.wallet.KeyChain.KeyPurpose;
import org.bitcoinj.wallet.WalletProtobufSerializer;

import java.security.SecureRandom;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import android.util.Log;
import timber.log.Timber;

public class BitcoinUtils {
    private static final String BIP39_ENGLISH_SHA256 = "ad90bf3beb7b0eb7e5acd74727dc0da96e0a280a258354e7293fb7e211ac03db";
    private static final int WALLET_VERSION = 1;

    public static void init(Context context) {
        if (MnemonicCode.INSTANCE == null) {
            try {
                AssetManager assetManager = context.getAssets();
                InputStream inputStream = assetManager.open("english.txt");
                MnemonicCode.INSTANCE = new MnemonicCode(inputStream, BIP39_ENGLISH_SHA256);
            } catch (IOException e) {
                Timber.e(e, "Unable to read word list.");
            }
        }
    }

    public static List<String> generateMnemonic(byte[] seedData) {
        Timber.i("W4LL3T created");
        if (MnemonicCode.INSTANCE == null) {
            return null;
        }
        try {
            return MnemonicCode.INSTANCE.toMnemonic(seedData);
        } catch (MnemonicException.MnemonicLengthException e) {
            Timber.e(e, "Unable to create mnemonic from word list");
        }
        return null;
    }

    public static Wallet createWallet(NetworkParameters params, String seedPhrase) {
        byte[] entropy;
        try {
            entropy = MnemonicCode.INSTANCE.toEntropy(Arrays.asList(seedPhrase.split(" ")));
        } catch (MnemonicException e) {
            Timber.e(e, "Could not convert passphrase to entropy");
            return null;
        }
        return createWallet(params, entropy);
    }

    @NonNull
    public static Wallet createWallet(NetworkParameters params, byte[] entropy) {
        Timber.i("karim: " + entropy.toString());
        DeterministicSeed deterministicSeed = new DeterministicSeed(entropy,
                LMConstants.WALLET_PASSPHRASE,
                LMConstants.WALLET_CREATION_TIME_SECONDS);
        Timber.i("karim: " + deterministicSeed.toString());
        // m/44'/0'/0'/0

//        // Seed (12 words)
//        SecureRandom random = new SecureRandom();
//        //String seedCode = "zone relief pear light zebra dad alpha phone salad vague trend miracle";
//        DeterministicSeed seed = new DeterministicSeed(random, 128, "", 0);

        // RootKey
        DeterministicKey privateMasterKey = HDKeyDerivation.createMasterPrivateKey(deterministicSeed.getSeedBytes());
        Timber.i("karim: " + privateMasterKey);

        // Use this to generate account path m/44'/248'/0'
        // Note: HARDENED_BIT adds Apostrophe to the path, if the flag is missing the result will be different.
        // iancoleman's app use Hardened bit with Purpose, CoinType and Account.
        ImmutableList<ChildNumber> accountPath = ImmutableList.of(new ChildNumber(44 | ChildNumber.HARDENED_BIT),
                new ChildNumber(248 | ChildNumber.HARDENED_BIT), new ChildNumber(0|ChildNumber.HARDENED_BIT));


//        DeterministicKeyChain chain = DeterministicKeyChain.builder().seed(deterministicSeed).accountPath(accountPath).outputScriptType(ScriptType.P2PKH).build();
        DeterministicKeyChain chain = new DeterministicKeyChain(deterministicSeed, accountPath);

        List mnemonic = chain.getMnemonicCode();

        Timber.i("karim: " + mnemonic.toString());

        Wallet wallet = Wallet.fromSeed(params, deterministicSeed, accountPath);
        Timber.i("karim: " + wallet.toString());
        Timber.i("karim: " + LegacyAddress.fromKey(params,wallet.freshKey(KeyPurpose.RECEIVE_FUNDS)));
        wallet.setVersion(WALLET_VERSION);
        return wallet;
    }

    public static Wallet loadWallet(InputStream walletStream, NetworkParameters networkParameters) throws IOException, UnreadableWalletException {
        WalletExtension[] extensions = {};
        Protos.Wallet proto = WalletProtobufSerializer.parseToProto(walletStream);
        WalletProtobufSerializer serializer = new WalletProtobufSerializer();
        return serializer.readWallet(networkParameters, extensions, proto);
    }

    public static boolean updateRequired(Wallet wallet) {
        return wallet.getVersion() < WALLET_VERSION;
    }

    public static Wallet updateWallet(Wallet wallet) {
        if (updateRequired(wallet)) {
            // Apply version 0 to version 1 changes
            DeterministicSeed keyChainSeed = wallet.getKeyChainSeed();
            NetworkParameters networkParameters = wallet.getNetworkParameters();
            Wallet updatedWallet = Wallet.fromSeed(networkParameters, keyChainSeed, DeterministicKeyChain.BIP44_ACCOUNT_ZERO_PATH);
            updatedWallet.setVersion(WALLET_VERSION);
            return updatedWallet;
        }
        return wallet;
    }

    public static boolean isValidPassphrase(String passphrase) {
        try {
            byte[] entropy = MnemonicCode.INSTANCE.toEntropy(Arrays.asList(passphrase.split(" ")));
            return entropy.length > 0;
        } catch (MnemonicException e) {
            Timber.e(e, "Invalid passphrase");
            return false;
        }
    }
}
