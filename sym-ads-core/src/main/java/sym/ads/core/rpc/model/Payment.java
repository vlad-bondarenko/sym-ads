package sym.ads.core.rpc.model;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Arrays;

/**
 * Created by vbondarenko on 12.07.2020.
 */

public class Payment implements Serializable {

    private final String serverId;
    private final String adId;
    private final String recipient;
    private final BigInteger amount;
    private final String publicKey;

    public Payment(String serverId, String adId, String recipient, BigInteger amount, String publicKey) {
        this.serverId = serverId;
        this.adId = adId;
        this.recipient = recipient;
        this.amount = amount;
        this.publicKey = publicKey;
    }

    public String getServerId() {
        return serverId;
    }

    public String getAdId() {
        return adId;
    }

    public String getRecipient() {
        return recipient;
    }

    public BigInteger getAmount() {
        return amount;
    }

    public String getPublicKey() {
        return publicKey;
    }

    @Override
    public String toString() {
        return "Payment{" +
                "serverId='" + serverId + '\'' +
                ", adId='" + adId + '\'' +
                ", recipient='" + recipient + '\'' +
                ", amount=" + amount +
                ", publicKey='" + publicKey + '\'' +
                '}';
    }
}
