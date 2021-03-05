package io.certifico.app.util;

import android.content.Context;
import android.content.res.AssetManager;
import android.support.annotation.NonNull;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.wallet.DeterministicKeyChain;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.Protos;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.WalletExtension;
import org.bitcoinj.wallet.WalletProtobufSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import io.certifico.app.LMConstants;
import io.certifico.app.data.network.MultiChainMainNetParams;
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

    @NonNull
    public static DeterministicSeed createDeterministicSeed(byte[] entropy) {
        return new DeterministicSeed(entropy,
                LMConstants.WALLET_PASSPHRASE,
                LMConstants.WALLET_CREATION_TIME_SECONDS);
    }

    @NonNull
    public static Wallet createWallet(byte[] entropy, String chain, int usedAddresses) {
        Wallet wallet = MultiChainMainNetParams.createWallet(chain, entropy, usedAddresses);
        wallet.setVersion(WALLET_VERSION);
        return wallet;
    }

    public static Wallet createWallet(String seedPhrase, String chain, int usedAddresses) {
        byte[] entropy;
        try {
            entropy = MnemonicCode.INSTANCE.toEntropy(Arrays.asList(seedPhrase.split(" ")));
        } catch (MnemonicException e) {
            Timber.e(e, "Could not convert passphrase to entropy");
            return null;
        }
        return createWallet(entropy, chain, usedAddresses);
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
