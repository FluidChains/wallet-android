package io.certifico.app.data.error;

public class IssuerAnalyticsException extends Exception {
    public IssuerAnalyticsException() {
        super("Unable to post Issuer analytics, no url.");
    }
}
