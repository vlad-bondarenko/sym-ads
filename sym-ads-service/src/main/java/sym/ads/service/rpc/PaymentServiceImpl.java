package sym.ads.service.rpc;

import io.nem.symbol.sdk.model.account.Address;
import io.nem.symbol.sdk.model.mosaic.Mosaic;
import io.nem.symbol.sdk.model.mosaic.MosaicId;
import io.nem.symbol.sdk.model.transaction.*;
import sym.ads.core.BaseClass;
import sym.ads.core.SymConnector;
import sym.ads.core.model.Payout;
import sym.ads.core.rpc.PaymentService;
import sym.ads.core.rpc.model.Payment;
import sym.ads.core.rpc.model.Result;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static sym.ads.core.Constants.CONFIG_SERVICE_MOSAIC_ID;
import static sym.ads.core.Utils.GSON;

/**
 * Created by vbondarenko on 13.07.2020.
 */

public class PaymentServiceImpl extends BaseClass implements PaymentService {

    private final SymConnector symConnector = SymConnector.getInstance();

    private static final long percent = 90;//todo add to config of blockchain
//    private static final BigDecimal rate = BigDecimal.valueOf(1.5);//todo add to config of blockchain

    private final Map<String, Object> configs;

    private static final ConcurrentHashMap<String, AggregateTransaction> AGGREGATE_TRANSACTION_MAP = new ConcurrentHashMap<>();//todo need persist to db, save this after reboot server

    public PaymentServiceImpl(Map<String, Object> configs) {
        this.configs = configs;
    }

    @Override
    public Result<String> payment(byte[] paymentBytes) {
        try {
            Payment payment = GSON.fromJson(new String(paymentBytes, StandardCharsets.UTF_8), Payment.class);
            if (isBlank(payment.getServerId()) || isBlank(payment.getAdId()) || isBlank(payment.getRecipient()) || isBlank(payment.getPublicKey()) || payment.getAmount() == null || payment.getAmount().compareTo(BigInteger.ONE) < 0) {
                return new Result<>(null, -1, "Not valid: " + payment.toString());
            }

            String mosaicId = (String) configs.get(CONFIG_SERVICE_MOSAIC_ID);

            Mosaic mosaic;
            if (mosaicId == null || mosaicId.isBlank()) {
                log.warn("Not found mosaicId = {}, use networkCurrency", mosaicId);

                mosaic = symConnector.networkCurrency.createAbsolute(payment.getAmount());
            } else {
                mosaic = symConnector.getCurrency(new MosaicId(mosaicId)).createAbsolute(payment.getAmount());
            }

            TransferTransaction adTransferTransaction = symConnector.createTransaction(symConnector.rootPublicAccount.getAddress(), Collections.singletonList(mosaic), "serverId=" + payment.getServerId() + ", adId=" + payment.getAdId());

            final BigInteger webAmount = payment.getAmount().multiply(BigInteger.valueOf(percent)).divide(BigInteger.valueOf(100));
            TransferTransaction webTransferTransaction = symConnector.createTransaction(Address.createFromRawAddress(payment.getRecipient()), Collections.singletonList(symConnector.networkCurrency.createAbsolute(webAmount)), "serverId=" + payment.getServerId() + ", adId=" + payment.getAdId());

            AggregateTransaction aggregateTransaction = symConnector.createAggregateTransaction(
                    List.of(
                            adTransferTransaction.toAggregate(symConnector.getPublicAccount(payment.getPublicKey())),
                            webTransferTransaction.toAggregate(symConnector.account.getPublicAccount()))
            );

            SignedTransaction signedTransactionNotComplete = symConnector.signAggregateTransaction(aggregateTransaction, symConnector.account);

            String hash = symConnector.announce(signedTransactionNotComplete);

            AGGREGATE_TRANSACTION_MAP.put(hash, aggregateTransaction);

            return new Result<>(hash, 0, null);
        } catch (Exception e) {
            log.error(e.toString(), e);

            return new Result<>(null, 255, e.toString());
        }
    }

    @Override
    public Result<String> confirm(String hash, byte[] cosignatureSignedTransactionBytes) {
        if (isBlank(hash)) {
            return new Result<>(null, -1, "Not valid hash: " + hash);
        }

        try {
            AggregateTransaction aggregateTransaction = AGGREGATE_TRANSACTION_MAP.get(hash);

            if (aggregateTransaction == null) {
                return new Result<>(null, -1, "Not found aggregateTransaction for hash: " + hash);
            }

            CosignatureSignedTransaction cosignatureSignedTransaction = GSON.fromJson(new String(cosignatureSignedTransactionBytes, StandardCharsets.UTF_8), CosignatureSignedTransaction.class);

            SignedTransaction signedTransaction = symConnector.signTransactionGivenSignatures(symConnector.account, aggregateTransaction, Collections.singletonList(cosignatureSignedTransaction));

            String hashNew = symConnector.announce(signedTransaction);

            if (Objects.equals(hash, hashNew)) {
                AGGREGATE_TRANSACTION_MAP.remove(hash);

                return new Result<>(hash, 0, null);
            }

            return new Result<>(null, -1, "Not announce CosignatureSignedTransaction for hash: " + hash);
        } catch (Exception e) {
            log.error(e.toString(), e);

            return new Result<>(null, 255, e.toString());
        }
    }
}
