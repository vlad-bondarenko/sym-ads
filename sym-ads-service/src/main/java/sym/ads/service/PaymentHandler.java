package sym.ads.service;

import io.nem.symbol.sdk.model.account.Address;
import io.nem.symbol.sdk.model.blockchain.BlockInfo;
import io.nem.symbol.sdk.model.mosaic.MosaicId;
import io.nem.symbol.sdk.model.transaction.*;
import one.nio.http.Response;
import one.nio.util.Utf8;
import sym.ads.core.BaseClass;
import sym.ads.core.SymConnector;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static sym.ads.core.Constants.CONFIG_SERVICE_MOSAIC_ID;

/**
 * Created by vbondarenko on 12.07.2020.
 */

public final class PaymentHandler extends BaseClass implements Consumer<BlockInfo> {

    private final SymConnector symConnector = SymConnector.getInstance();

    private static final byte[] BYTES_ERROR = Utf8.toBytes("ERROR");

    private static final long percent = 90;
//    private static final BigDecimal rate = BigDecimal.valueOf(1.5);//todo add to config of blockchain

    private final Map<String, Object> configs;

    public PaymentHandler(Map<String, Object> configs) {
        this.configs = configs;
    }

    public Response payment(String serverId, String adId, String recipient, long amount, String publicKey) {
        try {
            if (serverId.isBlank() || adId.isBlank() || recipient.isBlank() || amount < 1) {
                return Response.ok(BYTES_ERROR);
            }

            String mosaicId = (String) configs.get(CONFIG_SERVICE_MOSAIC_ID);

            if (mosaicId == null || mosaicId.isBlank()) {
                return Response.ok(BYTES_ERROR);
            }

            TransferTransaction adTransferTransaction = symConnector.createTransaction(symConnector.rootPublicAccount.getAddress(), Collections.singletonList(symConnector.getCurrency(new MosaicId(mosaicId)).createAbsolute(amount)), "serverId=" + serverId + ", adId=" + adId);

            TransferTransaction webTransferTransaction = symConnector.createTransaction(Address.createFromRawAddress(recipient), Collections.singletonList(symConnector.networkCurrency.createAbsolute(amount * percent / 100)), "serverId=" + serverId + ", adId=" + adId);

            AggregateTransaction aggregateTransaction = symConnector.createAggregateTransaction(
                    List.of(
                            adTransferTransaction.toAggregate(symConnector.getPublicAccount(publicKey)),
                            webTransferTransaction.toAggregate(symConnector.account.getPublicAccount()))
            );

            SignedTransaction signedTransactionNotComplete = symConnector.signAggregateTransaction(aggregateTransaction, symConnector.account);

            String hash = signedTransactionNotComplete.getHash();
            log.info("Signed transaction hash: {}, {}" , hash, signedTransactionNotComplete.getPayload());

            return Response.ok(aggregateTransaction.serialize());
        } catch (Exception e) {
            log.error(e.toString(), e);
        }

        return Response.ok(BYTES_ERROR);
    }

    @Override
    public void accept(BlockInfo blockInfo) {
        log.debug("New block");
    }
}
