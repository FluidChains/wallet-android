package com.fluidcerts.android.app.data.bitcoin;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.VisibleForTesting;
import android.util.Pair;

import com.fluidcerts.android.app.LMConstants;
import com.fluidcerts.android.app.R;
import com.fluidcerts.android.app.data.error.ExceptionWithResourceString;
import com.fluidcerts.android.app.data.preferences.SharedPreferencesManager;
import com.fluidcerts.android.app.data.store.CertificateStore;
import com.fluidcerts.android.app.data.store.IssuerStore;
import com.fluidcerts.android.app.util.BitcoinUtils;
import com.fluidcerts.android.app.util.StringUtils;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.LegacyAddress;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.bitcoinj.wallet.Wallet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;

import rx.Observable;
import timber.log.Timber;

public class BitcoinManager {

    private static final String PASSPHRASE_DELIMETER = " ";

    private final Context mContext;
    private final NetworkParameters mNetworkParameters;
    private final IssuerStore mIssuerStore;
    private final CertificateStore mCertificateStore;
    private final SharedPreferencesManager mSharedPreferencesManager;
    private HashMap<String,Wallet> mWallet = new HashMap<>();

    public BitcoinManager(Context context, NetworkParameters networkParameters, IssuerStore issuerStore, CertificateStore certificateStore, SharedPreferencesManager sharedPreferencesManager) {
        mContext = context;
        mNetworkParameters = networkParameters;
        mIssuerStore = issuerStore;
        mCertificateStore = certificateStore;
        mSharedPreferencesManager = sharedPreferencesManager;
    }

    private Observable<Wallet> getWallet(String chain) {
        return Observable.defer(() -> {
            Wallet wallet = mWallet.get(chain);
            if (wallet != null) {
                return Observable.just(wallet);
            }
            if (getWalletFile(chain).exists()) {
                return loadWallet(chain);
            } else {
                return createWallet(chain);
            }
        });
    }

    @VisibleForTesting
    protected File getWalletFile(String chain) {
        return new File(mContext.getFilesDir(), String.format("%s.wallet", chain));
    }

    public SharedPreferencesManager getSharedPreferences() {
        return mSharedPreferencesManager;
    }

    private Observable<Wallet> createWallet(String chain) {
        SecureRandom random = new SecureRandom();
        byte[] entropy = random.generateSeed(LMConstants.WALLET_SEED_BYTE_SIZE);
        buildWallet(entropy, chain);
        return Observable.just(mWallet.get(chain));
    }

    private Observable<Wallet> buildWallet(byte[] entropy, String chain) {
        try {
            mWallet.put(chain, BitcoinUtils.createWallet(mNetworkParameters, entropy, chain));
        } catch (Exception e) {
            Timber.e(e);
        }
        return saveWallet(chain);
    }

    private Observable<Wallet> buildWallet(String seedPhrase, String chain) {
        mWallet.put(chain, BitcoinUtils.createWallet(mNetworkParameters, seedPhrase, chain));
        return saveWallet(chain);
    }

    /**
     * @return true if wallet was loaded successfully
     */
    private Observable<Wallet> loadWallet(String chain) {
        try (FileInputStream walletStream = new FileInputStream(getWalletFile(chain))) {
            Wallet wallet = BitcoinUtils.loadWallet(walletStream, mNetworkParameters);
            if (BitcoinUtils.updateRequired(wallet)) {
                Address currentReceiveAddress = wallet.currentReceiveAddress();
                mSharedPreferencesManager.setLegacyReceiveAddress(currentReceiveAddress.toString());
                wallet = BitcoinUtils.updateWallet(wallet);
                wallet.saveToFile(getWalletFile(chain));
                Timber.d("Wallet successfully updated");
            }
            mWallet.put(chain, wallet);
            Timber.d(String.format("Wallet successfully loaded chain -> %s", chain));
            return Observable.just(mWallet.get(chain));
        } catch (UnreadableWalletException e) {
            Timber.e(e, "Wallet is corrupted");
            return Observable.error(e);
        } catch (FileNotFoundException e) {
            Timber.e(e, "Wallet file not found");
            return Observable.error(e);
        } catch (IOException e) {
            Timber.e(e, "Wallet unable to be parsed");
            return Observable.error(e);
        }
    }

    /**
     * @return true if wallet was saved successfully
     */
    private Observable<Wallet> saveWallet(String chain) {
        if (mWallet == null) {
            Exception e = new Exception("Wallet doesn't exist");
            Timber.e(e, "Wallet doesn't exist");
            return Observable.error(e);
        }
        try {
            mWallet.get(chain).saveToFile(getWalletFile(chain));
            Timber.d("Wallet successfully saved");
            return Observable.just(mWallet.get(chain));
        } catch (IOException e) {
            Timber.e(e, "Unable to save Wallet");
            return Observable.error(e);
        }
    }

    public Observable<String> getPassphrase(String chain) {
        return getWallet(chain).map(wallet -> {
            DeterministicSeed seed = wallet.getKeyChainSeed();
            List<String> mnemonicCode = seed.getMnemonicCode();
            return StringUtils.join(PASSPHRASE_DELIMETER, mnemonicCode);
        });
    }

    public void resetEverything() {
        mIssuerStore.reset();
        mCertificateStore.reset();

        String passphraseFileOnExternalStorage = Environment.getExternalStorageDirectory() + "/learningmachine.dat";
        File file = new File(passphraseFileOnExternalStorage);
        if (file.delete()) {
            Timber.i("Seed backup successfully deleted");
        }
        for (String k: mWallet.keySet()) {
            if (getWalletFile(k).delete()) {
                Timber.i(String.format("%s wallet successfully deleted", k));
            }
        }

    }

    public Observable<Wallet> setPassphrase(String chain, String newPassphrase) {
        if (StringUtils.isEmpty(newPassphrase) || !BitcoinUtils.isValidPassphrase(newPassphrase)) {
            return Observable.error(new ExceptionWithResourceString(R.string.error_invalid_passphrase_malformed));
        }
        mIssuerStore.reset();
        mCertificateStore.reset();
        return buildWallet(newPassphrase, chain);
    }

    public Observable<String> getCurrentBitcoinAddress(String chain) {
        return getWallet(chain).map(wallet -> wallet.currentReceiveAddress().toString());
    }

    public Observable<String> getFreshBitcoinAddress(String chain) {
        return getWallet(chain).map(wallet -> wallet.freshReceiveAddress().toString())
                .flatMap(address -> Observable.combineLatest(Observable.just(address), saveWallet(chain), Pair::new))
                .map(pair -> pair.first);
    }

    public boolean isMyIssuedAddress(String chain, String addressString) {
        String legacyReceiveAddress = mSharedPreferencesManager.getLegacyReceiveAddress();
        if (legacyReceiveAddress != null) {
            if (legacyReceiveAddress.equals(addressString)) {
                return true;
            }
        }
        Address address = LegacyAddress.fromBase58(mNetworkParameters, addressString);
        return mWallet.get(chain).getIssuedReceiveAddresses().contains(address);
    }
}
