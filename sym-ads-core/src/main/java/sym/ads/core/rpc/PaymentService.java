package sym.ads.core.rpc;

import io.nem.symbol.sdk.model.transaction.CosignatureSignedTransaction;
import sym.ads.core.rpc.model.Payment;
import sym.ads.core.rpc.model.Result;

/**
 * Created by vbondarenko on 12.07.2020.
 */

public interface PaymentService {

    byte[] payment(byte[] paymentBytes);

    byte[] confirm(String hash, byte[] cosignatureSignedTransactionBytes);
}
