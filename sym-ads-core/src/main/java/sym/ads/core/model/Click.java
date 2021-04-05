package sym.ads.core.model;

import java.io.Serializable;

/**
 * Created by vbondarenko on 23.05.2020.
 */

public final class Click implements Serializable {

    final String id;
    final String referrer;
    final String host;
    final String country;
    final String destinationId;

    public Click(String id, String referrer, String host, String country, String destinationId) {
        this.id = id;
        this.referrer = referrer;
        this.host = host;
        this.country = country;
        this.destinationId = destinationId;
    }

    public String getId() {
        return id;
    }

    public String getReferrer() {
        return referrer;
    }

    public String getHost() {
        return host;
    }

    public String getCountry() {
        return country;
    }

    public String getDestinationId() {
        return destinationId;
    }
}
