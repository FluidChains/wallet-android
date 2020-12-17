package io.certifico.app.util;

import android.content.Context;
import android.security.KeyPairGeneratorSpec;
import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Calendar;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.security.auth.x500.X500Principal;

import timber.log.Timber;

public class KeyStoreUtils {

    private static final String KEYSTORE_PROVIDER_ANDROID_KEYSTORE = "AndroidKeyStore";

    private static KeyStore getKeyStore() throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER_ANDROID_KEYSTORE);
        keyStore.load(null);
        return keyStore;
    }

    private static void createKeyEntry(Context context, KeyStore keyStore, String keyAlias) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, KeyStoreException {
        if (!keyStore.containsAlias(keyAlias)) {
            Timber.d(String.format("Creating KeyStore key entry %s", keyAlias));
            Calendar start = Calendar.getInstance();
            Calendar end = Calendar.getInstance();
            end.add(Calendar.YEAR, 40);
            KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(context)
                    .setAlias(keyAlias)
                    .setSubject(new X500Principal("CN=certifico.io, O=Certifico"))
                    .setSerialNumber(BigInteger.ONE)
                    .setStartDate(start.getTime())
                    .setEndDate(end.getTime())
                    .build();
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", KEYSTORE_PROVIDER_ANDROID_KEYSTORE);
            generator.initialize(spec);
            generator.generateKeyPair();
        }
    }

    public static void deleteKeyEntry(String keyAlias) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        KeyStore keyStore = getKeyStore();
        keyStore.deleteEntry(keyAlias);
    }

    public static String encrypt(Context context, String keyAlias, String toEncrypt) throws CertificateException,
            NoSuchAlgorithmException, KeyStoreException, IOException, UnrecoverableEntryException, NoSuchProviderException,
            NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {

        if (keyAlias.isEmpty()) {
            throw new IllegalArgumentException("'keyAlias' cannot be empty");
        }
        if (toEncrypt.isEmpty()) {
            throw new IllegalArgumentException("'toEncrypt' cannot be empty");
        }

        KeyStore keyStore = getKeyStore();
        createKeyEntry(context, keyStore, keyAlias);

        KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(keyAlias, null);
        PublicKey publicKey = privateKeyEntry.getCertificate().getPublicKey();

        Cipher input = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        input.init(Cipher.ENCRYPT_MODE, publicKey);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CipherOutputStream cipherOutputStream = new CipherOutputStream(
                outputStream, input);
        cipherOutputStream.write(toEncrypt.getBytes("UTF-8"));
        cipherOutputStream.close();

        byte [] encryptedBytes = outputStream.toByteArray();
        return Base64.encodeToString(encryptedBytes, Base64.DEFAULT);

    }

    public static String decrypt(String keyAlias, String toDecrypt) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, UnrecoverableEntryException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException {
        if (keyAlias.isEmpty()) {
            throw new IllegalArgumentException("'keyAlias' cannot be empty");
        }
        if (toDecrypt.isEmpty()) {
            throw new IllegalArgumentException("'toDecrypt' cannot be empty");
        }

        KeyStore keyStore = getKeyStore();
        KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry)keyStore.getEntry(keyAlias, null);
        PrivateKey privateKey = privateKeyEntry.getPrivateKey();

        Cipher output = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        output.init(Cipher.DECRYPT_MODE, privateKey);

        CipherInputStream cipherInputStream = new CipherInputStream(
                new ByteArrayInputStream(Base64.decode(toDecrypt, Base64.DEFAULT)), output);
        ArrayList<Byte> values = new ArrayList<>();
        int nextByte;
        while ((nextByte = cipherInputStream.read()) != -1) {
            values.add((byte)nextByte);
        }

        byte[] bytes = new byte[values.size()];
        for(int i = 0; i < bytes.length; i++) {
            bytes[i] = values.get(i).byteValue();
        }

        return new String(bytes, 0, bytes.length, "UTF-8");

    }

}