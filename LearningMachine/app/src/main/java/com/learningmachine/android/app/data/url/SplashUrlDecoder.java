package com.learningmachine.android.app.data.url;

import com.learningmachine.android.app.util.StringUtils;

import java.io.IOException;
import java.net.URLDecoder;
import android.net.Uri;

import timber.log.Timber;

import static com.learningmachine.android.app.data.url.LaunchType.ADD_CERTIFICATE;
import static com.learningmachine.android.app.data.url.LaunchType.ADD_ISSUER;

public class SplashUrlDecoder {
    private static final String ADD_ISSUER_PATH = "add-issuer";
    private static final String ADD_CERT_PATH = "import-certificate";

    public static LaunchData getLaunchType(String launchUri) {

        if (launchUri == null) {
            launchUri = "";
        }

        LaunchData data;
        if (launchUri.contains(ADD_ISSUER_PATH)) {
            data = handleAddIssuerUri(launchUri);
            if (data != null) {
                return data;
            }
        } else if (launchUri.contains(ADD_CERT_PATH)) {
            data = new LaunchData(ADD_CERTIFICATE, launchUri);
            if (data != null) {
                return data;
            }
        }

        return new LaunchData(LaunchType.ONBOARDING);
    }

    private static LaunchData handleAddIssuerUri(String uriString) {
//        String pathSuffix = getPathSuffix(uriString, ADD_ISSUER_PATH);
//        if (StringUtils.isEmpty(pathSuffix)) {
//            Timber.e("Launch uri missing the issuer path suffix");
//            return null;
//        }

//        String[] issuerParts = pathSuffix.split("/");
//        if (issuerParts.length < 2) {
//            Timber.e("Launch uri missing issuer path parts");
//            return null;
//        }

        try {
            Uri issuerUri = Uri.parse(uriString);
//            String introUrl = URLDecoder.decode(issuerParts[0], "UTF-8");
//            String nonce = URLDecoder.decode(issuerParts[1], "UTF-8");
            String introUrl = URLDecoder.decode(issuerUri.getQueryParameter("issuerProfile"), "UTF-8");
            String nonce = URLDecoder.decode(issuerUri.getQueryParameter("otc"), "UTF-8");
            return new LaunchData(ADD_ISSUER, introUrl, nonce);
        } catch (IOException e) {
            Timber.e(e, "Unable to decode Urls.");
        }

        return null;
    }

    private static LaunchData handleAddCertificateUri(String uriString) {
        String pathSuffix = getPathSuffix(uriString, ADD_CERT_PATH);
        if (StringUtils.isEmpty(pathSuffix)) {
            Timber.e("Launch uri missing the cert path suffix");
            return null;
        }
        try {
            String certUrl = URLDecoder.decode(pathSuffix, "UTF-8");
            return new LaunchData(ADD_CERTIFICATE, certUrl);
        } catch (IOException e) {
            Timber.e(e, "Unable to decode Urls.");
        }

        return null;
    }

    private static String getPathSuffix(String uriString, String delimiter) {
        String[] uriSplit = uriString.split(delimiter);
        if (uriSplit.length < 2) {
            return null;
        }
        return uriSplit[1];
    }

}
