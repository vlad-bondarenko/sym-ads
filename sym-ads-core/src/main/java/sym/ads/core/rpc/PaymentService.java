package sym.ads.core.rpc;

import io.nem.symbol.sdk.model.transaction.CosignatureSignedTransaction;
import sym.ads.core.rpc.model.Payment;
import sym.ads.core.rpc.model.Result;

/**
 * Created by vbondarenko on 12.07.2020.
 */

public interface PaymentService {

    Result<String> payment(Payment payment);

    Result<String> confirm(String hash, byte[] cosignatureSignedTransactionBytes);
}
