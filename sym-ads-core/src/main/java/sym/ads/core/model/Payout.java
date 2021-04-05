package sym.ads.core.model;

/**
 * Created by vbondarenko on 16.10.2020.
 */

public final class Payout {

    private final String hash;

    public Payout(String hash) {
        this.hash = hash;
    }

    public String getHash() {
        return hash;
    }
}
