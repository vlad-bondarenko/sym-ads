package sym.ads.core;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import io.nem.symbol.core.crypto.PrivateKey;
import io.nem.symbol.core.crypto.PublicKey;
import io.nem.symbol.core.utils.ExceptionUtils;
import io.nem.symbol.sdk.api.*;
import io.nem.symbol.sdk.infrastructure.BinarySerializationImpl;
import io.nem.symbol.sdk.infrastructure.CurrencyServiceImpl;
import io.nem.symbol.sdk.infrastructure.okhttp.RepositoryFactoryOkHttpImpl;
import io.nem.symbol.sdk.model.account.Account;
import io.nem.symbol.sdk.model.account.Address;
import io.nem.symbol.sdk.model.account.PublicAccount;
import io.nem.symbol.sdk.model.account.UnresolvedAddress;
import io.nem.symbol.sdk.model.blockchain.BlockInfo;
import io.nem.symbol.sdk.model.message.EncryptedMessage;
import io.nem.symbol.sdk.model.message.Message;
import io.nem.symbol.sdk.model.message.PlainMessage;
import io.nem.symbol.sdk.model.mosaic.Currency;
import io.nem.symbol.sdk.model.mosaic.Mosaic;
import io.nem.symbol.sdk.model.mosaic.MosaicId;
import io.nem.symbol.sdk.model.mosaic.MosaicInfo;
import io.nem.symbol.sdk.model.network.NetworkType;
import io.nem.symbol.sdk.model.transaction.*;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.functions.Functions;
import sym.ads.core.model.*;
import one.nio.serial.Json;
import one.nio.serial.JsonReader;
import one.nio.util.Utf8;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;

import java.math.BigInteger;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by vbondarenko on 13.06.2020.
 */

public final class SymConnector {

    private static final Logger log = getLogger(SymConnector.class);

    private static final SymConnector INSTANCE = new SymConnector();

    private static final BinarySerialization BINARY_SERIALIZATION = BinarySerializationImpl.INSTANCE;

    public final NetworkType networkType;
    public final Currency networkCurrency;
    public final MosaicId networkMosaicId;

    public final Account account;
    public final PrivateKey privateKey;

    private String rootPublicKey = "74F29DCAA05A7DF6228853CEC65B76D93D898ACD5FE110B00019D03A26B6BA8F";
    public PublicAccount rootPublicAccount;
    private Address rootAddress;

    private final String networkGenerationHash;
    private final RepositoryFactory repositoryFactory;
    private final Duration epochAdjustment;
    private final TransactionPaginationStreamer transactionPaginationStreamer;
    private final TransactionRepository transactionRepository;
    private final CurrencyService currencyService;
    private final MosaicRepository mosaicRepository;
    private final int timeoutSeconds;

    private final Listener listener;

    public final BigInteger maxFee;

    private Consumer<Transaction> transactionConsumer;
    private Consumer<BlockInfo> newBlockConsumer;

    private final RingBuffer<DisruptorEvent<Object>> ringBuffer;

    public static SymConnector getInstance() {
        return INSTANCE;
    }

    private SymConnector() {
        try {
            timeoutSeconds = 10;

            Disruptor<DisruptorEvent<Object>> disruptor = new Disruptor<>(
                    DisruptorEvent::new,
                    512,
                    new NameThreadFactory(getClass().getSimpleName() + "-Events"),
                    ProducerType.SINGLE,
                    new BlockingWaitStrategy());
            disruptor.handleEventsWith(this::handleEvent);
            ringBuffer = disruptor.start();

            ShutdownHookHandler.getInstance().addShutdownHook(disruptor::shutdown);

            repositoryFactory = new RepositoryFactoryOkHttpImpl(Settings.getInstance().nodeUrl());
            networkType = repositoryFactory.getNetworkType().toFuture().get();
            networkCurrency = repositoryFactory.getNetworkCurrency().toFuture().get();
            networkMosaicId = networkCurrency.getMosaicId().orElseThrow(() -> new IllegalStateException("Not getting network mosaicId"));
            epochAdjustment = get(repositoryFactory.getEpochAdjustment());

            account = Account.createFromPrivateKey(Settings.getInstance().privateKey(), networkType);
            privateKey = account.getKeyPair().getPrivateKey();

            rootPublicAccount = PublicAccount.createFromPublicKey(rootPublicKey, networkType);
            rootAddress = rootPublicAccount.getAddress();

            listener = createListener();
            Disposable subscribeNewBlock = listener.newBlock().subscribe(blockInfo -> {
                log.debug("Block height: {}", blockInfo.getHeight());

                long sequence = ringBuffer.next();
                DisruptorEvent<Object> event = ringBuffer.get(sequence);
                event.data = blockInfo;

                ringBuffer.publish(sequence);
            });

            Disposable subscribeUnconfirmedAdded = listener.unconfirmedAdded(rootAddress).subscribe(transaction -> {
                log.debug("{}: Unconfirmed added: txId = {}, {}",
                        transaction.getTransactionInfo().flatMap(TransactionInfo::getHash).orElse(null),
                        transaction.getTransactionInfo().flatMap(TransactionInfo::getId).orElse(null),
                        Utf8.toString(transaction.serialize()));
            });

            Disposable subscribeUnconfirmedRemoved = listener.unconfirmedRemoved(rootAddress).subscribe(s -> {
                log.debug("Unconfirmed removed: {}", s);
            });

            Disposable subscribeConfirmed = listener.confirmed(rootAddress).subscribe(transaction -> {
                log.debug("{}: Confirmed: txId = {}, {}",
                        transaction.getTransactionInfo().flatMap(TransactionInfo::getHash).orElse(null),
                        transaction.getTransactionInfo().flatMap(TransactionInfo::getId).orElse(null),
                        Utf8.toString(transaction.serialize()));
            });

            ShutdownHookHandler.getInstance().addShutdownHook(() -> {
                try {
                    listener.close();
                } catch (Exception e) {
                    log.error(e.toString(), e);
                }

                try {
                    repositoryFactory.close();
                } catch (Exception e) {
                    log.error(e.toString(), e);
                }

                closeDisposables(subscribeNewBlock, subscribeUnconfirmedAdded, subscribeUnconfirmedRemoved, subscribeConfirmed);
            });

            transactionRepository = repositoryFactory.createTransactionRepository();
            transactionPaginationStreamer = new TransactionPaginationStreamer(transactionRepository);

            networkGenerationHash = repositoryFactory.getGenerationHash().toFuture().get();

            currencyService = new CurrencyServiceImpl(repositoryFactory);
            mosaicRepository = repositoryFactory.createMosaicRepository();

            maxFee = Settings.getInstance().maxFee();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void handleEvent(DisruptorEvent<Object> event, long sequence, boolean endOfBatch) {
        Object data = event.data;

        log.debug("Handle: {}", data);

        try {
            if (newBlockConsumer != null && data instanceof BlockInfo) {
                newBlockConsumer.accept((BlockInfo) data);
            }
        } catch (Exception e) {
            log.error(e.toString(), e);
        }
    }

    private Listener createListener() {
        Listener listener = repositoryFactory.createListener();
        try {
            listener.open().get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalArgumentException("Listener could not be created or opened. Error "
                    + org.apache.commons.lang3.exception.ExceptionUtils.getMessage(e), e);
        }

        return listener;
    }

    public void setNewBlockConsumer(Consumer<BlockInfo> newBlockConsumer) {
        this.newBlockConsumer = newBlockConsumer;
    }

    public final void setTransactionConsumer(Consumer<Transaction> transactionConsumer) {
        this.transactionConsumer = transactionConsumer;
    }

    public final <T extends Entity<?>> String save(T bean) throws Exception {
        return announce(account, TransferTransactionFactory.create(
                networkType,
                getDeadline(),
                rootAddress,
                Collections.emptyList()
        )
                .message(new PlainMessage(Json.toJson(bean)))
                .maxFee(maxFee)
                .build());
    }

    public final String announce(Account account, Transaction transaction) {
        return announce(transaction.signWith(account, networkGenerationHash));
    }

    public final String announce(SignedTransaction signedTransaction) {
        String hash = signedTransaction.getHash();
        log.info("Signed transaction hash: {}, {}", hash, signedTransaction.getPayload());

        TransactionAnnounceResponse response = get(transactionRepository.announce(signedTransaction));
        log.debug("Transaction announce: {}", response.getMessage());

        return hash;
    }

    public final String load(PublicAccount account, String lastTxId, int pageSize) throws Exception {
        log.debug("lastTxId = {}", lastTxId);

        final AtomicReference<String> firstProcessedTxId = new AtomicReference<>();

        TransactionSearchCriteria searchCriteria = new TransactionSearchCriteria(TransactionGroup.CONFIRMED);
        searchCriteria.setPageSize(pageSize);
        searchCriteria.setAddress(account.getAddress());
        searchCriteria.setId(lastTxId);
        searchCriteria.setOrder(OrderBy.DESC);
        searchCriteria.setTransactionTypes(List.of(TransactionType.TRANSFER));

        final AtomicBoolean isScan = new AtomicBoolean(true);
        Disposable disposable = transactionPaginationStreamer.search(searchCriteria)
                .takeWhile(transaction -> {
                    String txId = transaction.getTransactionInfo().flatMap(TransactionInfo::getId).orElse(null);

                    if (log.isDebugEnabled()) {
                        log.debug("{}: Transaction: txId = {}",
                                transaction.getTransactionInfo().flatMap(TransactionInfo::getHash).orElse(null),
                                txId
                        );
                    }

                    if (txId == null) {
                        log.error("Error transaction: {}", transaction.getTransactionInfo().flatMap(TransactionInfo::getHash).orElse(null));

                        return false;
                    }

                    if (Objects.equals(txId, lastTxId)) {
                        log.info("Found last txId");

                        return false;
                    } else {
                        if (firstProcessedTxId.get() == null) {
                            firstProcessedTxId.set(txId);
                        }

                        return true;
                    }
                })
                .filter(Transaction::isConfirmed)
                .subscribe(
                        transaction -> {
                            if (log.isDebugEnabled()) {
                                log.debug("Transaction process: {}", transaction.getTransactionInfo().flatMap(TransactionInfo::getHash).orElse(null));
                            }

                            transactionConsumer.accept(transaction);
                        },
                        Functions.ON_ERROR_MISSING,
                        () -> {
                            isScan.set(false);
                        }
                );

        while (isScan.get()) {
            TimeUnit.MILLISECONDS.sleep(500);
        }

        disposable.dispose();

        if (firstProcessedTxId.get() == null) {
            return lastTxId;
        }

        return firstProcessedTxId.get();
    }

    public void handleTransactionMessage(Transaction transaction, BiConsumer<Transaction, EntityDto> consumer) {
/*
        if (!transaction.isConfirmed() || transaction.getType() != TransactionType.TRANSFER) {
            return;
        }
*/

        TransferTransaction transferTransaction = (TransferTransaction) transaction;
        Optional<Message> messageOptional = transferTransaction.getMessage();
        if (messageOptional.isPresent()) {
            String msg;

            switch (messageOptional.get().getType()) {
                case PLAIN_MESSAGE:
                    msg = messageOptional.get().getText();

                    break;

                case ENCRYPTED_MESSAGE:
                    msg = ((EncryptedMessage) messageOptional.get()).decryptPayload(
                            PublicKey.fromHexString(""),//todo
                            privateKey
                    );
                    break;

                default:
                    return;
            }

            try {
                JsonReader reader = new JsonReader(Utf8.toBytes(msg));
                EntityDto entityDto = reader.readObject(EntityDto.class);

                if (entityDto.getType() > 0 && entityDto.getType() < 4) {
                    consumer.accept(transaction, entityDto);

                    return;
                }

                log.warn("Unsupported {} type for message", entityDto.getType());
            } catch (Exception e) {
                log.error(e.toString(), e);
            }
        }
    }

    public void handleTransactionMessageType(
            Transaction transaction,
            EntityDto entityDto,
            BiConsumer<String, Ad> consumerAd,
            BiConsumer<String, Site> consumerSite,
            BiConsumer<String, Map<String, Object>> consumerConfig
    ) {
        int type = entityDto.getType();

        if (type == Constants.TYPE_AD && consumerAd != null) {
            try {
                Ad ad;
                if ((ad = Converter.toAd(entityDto)) == null) {
                    consumerAd.accept(entityDto.getId(), null);
                } else {
                    consumerAd.accept(transaction.getTransactionInfo().flatMap(TransactionInfo::getId).orElse(null), ad);
                }
            } catch (Exception e) {
                log.error(e.toString(), e);
            }

            return;
        }

        if (type == Constants.TYPE_SITE && consumerSite != null) {
            try {
                Site site;
                if ((site = Converter.toSite(entityDto)) == null) {
                    consumerSite.accept(entityDto.getId(), null);
                } else {
                    consumerSite.accept(transaction.getTransactionInfo().flatMap(TransactionInfo::getId).orElse(null), site);
                }
            } catch (Exception e) {
                log.error(e.toString(), e);
            }

            return;
        }

        if (type == Constants.TYPE_CONFIG && consumerConfig != null) {
            try {
                PublicAccount signer = transaction.getSigner().orElse(null);
                if (Objects.equals(rootPublicAccount, signer)) {
                    if (entityDto.getData() == null) {
                        log.error("Unsupported remove config");
                    } else {
                        consumerConfig.accept(entityDto.getId(), entityDto.getData());
                    }

                    return;
                }

                log.error("Not root signer {}", signer == null ? null : signer.getAddress().pretty());
            } catch (Exception e) {
                log.error(e.toString(), e);
            }

            return;
        }

        log.error("Unsupported {} type", type);
    }

    public void handleConfig(String id, Map<String, Object> config, Map<Integer, String> categories, Map<String, Object> configs) {
        try {
            if (Constants.CONFIG_CATEGORY.equals(id)) {
                for (Map.Entry<String, Object> entry : config.entrySet()) {
                    if (NumberUtils.isParsable(entry.getKey()) && entry.getValue() instanceof String) {
                        categories.putIfAbsent(Integer.valueOf(entry.getKey()), (String) entry.getValue());

                        continue;
                    }

                    log.error("Wrong category: {}={}", entry.getKey(), entry.getValue());
                }

                return;
            } else if (Constants.CONFIG_OTHER.equals(id)) {
                for (Map.Entry<String, Object> entry : config.entrySet()) {
                    if (Constants.CONFIG_KEY_SET.contains(entry.getKey())) {
                        if (Constants.CONFIG_SERVICE_PUBLIC_KEY.equals(entry.getKey()) && !rootPublicKey.equals(entry.getValue())) {
                            rootPublicKey = (String) entry.getValue();
                            rootPublicAccount = PublicAccount.createFromPublicKey(rootPublicKey, networkType);
                            rootAddress = rootPublicAccount.getAddress();

                            continue;
                        }

                        configs.putIfAbsent(entry.getKey(), entry.getValue());
                    }
                }

                return;
            }

            log.error("Unsupported {} type config", id);
        } catch (Exception e) {
            log.error(e.toString(), e);
        }
    }

    public <T> T get(Observable<T> observable) {
        return get(observable, timeoutSeconds, TimeUnit.SECONDS);
    }

    public static <T> T get(Observable<T> observable, long timeout, TimeUnit unit) {
        return ExceptionUtils.propagate(() -> observable.toFuture().get(timeout, unit));
    }

    public static <T> void handle(Observable<T> observable, long timeout, TimeUnit unit, Consumer<T> consumer) {
        try {
            consumer.accept(observable.toFuture().get(timeout, unit));
        } catch (InterruptedException e) {
            if (!Thread.currentThread().isInterrupted()) {
                Thread.currentThread().interrupt();
            }
        } catch (ExecutionException e) {
            if (RuntimeException.class.isAssignableFrom(e.getCause().getClass())) {
                throw (RuntimeException) e.getCause();
            }
        } catch (TimeoutException e) {
            consumer.accept(null);
        }
    }

    public static Transaction deserialize(byte[] payload) {
        return BINARY_SERIALIZATION.deserialize(payload);
    }

     public static void closeDisposables(Disposable... disposables) {
        for (Disposable disposable : disposables) {
            try {
                disposable.dispose();
            } catch (Exception e) {
                log.error(e.toString(), e);
            }
        }
    }

    public TransferTransaction createTransaction(UnresolvedAddress recipient, List<Mosaic> mosaics, String message) {
        TransferTransactionFactory transferTransactionFactory = TransferTransactionFactory.create(
                networkType,
                getDeadline(),
                recipient,
                mosaics
        );

        if (message != null && !message.isBlank()) {
            transferTransactionFactory.message(new PlainMessage(message));
        }

        return transferTransactionFactory.build();
    }

    public final AggregateTransaction createAggregateTransaction(List<Transaction> transactions) {
        return AggregateTransactionFactory
                .createComplete(
                        networkType,
                        getDeadline(),
                        transactions)
                .maxFee(maxFee)
                .build();
    }

    public final SignedTransaction signAggregateTransaction(AggregateTransaction aggregateTransaction, Account account) {
      return   aggregateTransaction.signWith(account, networkGenerationHash);
    }

    public final SignedTransaction signTransactionGivenSignatures(Account account, AggregateTransaction aggregateTransaction, List<AggregateTransactionCosignature> aggregateTransactionCosignatures){
       return account.signTransactionGivenSignatures(aggregateTransaction, aggregateTransactionCosignatures, networkGenerationHash);
    }

    public final CosignatureSignedTransaction signSignedTransaction(AggregateTransaction aggregateTransaction, SignedTransaction signedTransaction, Account account) {
        CosignatureTransaction cosignatureTransaction = CosignatureTransaction.create(aggregateTransaction);

        return account.signCosignatureTransaction(cosignatureTransaction);
    }

    public final String announceAggregateTransaction(List<Transaction> transactions) {
        final AggregateTransaction aggregateTransaction = AggregateTransactionFactory
                .createComplete(
                        networkType,
                        getDeadline(),
                        transactions)
                .maxFee(maxFee)
                .build();


        SignedTransaction signedTransaction = aggregateTransaction.signWith(account, networkGenerationHash);

        String hash = signedTransaction.getHash();
        log.info("Signed aggregate transaction announce hash: {}", hash);

        TransactionAnnounceResponse response = get(transactionRepository.announce(signedTransaction));
        log.debug("Aggregate transaction announce: {}", response.getMessage());

        return hash;
    }

    public MosaicInfo mosaicInfo(MosaicId mosaicId) {
        return get(mosaicRepository.getMosaic(mosaicId));
    }

    public Deadline getDeadline() {
        return Deadline.create(this.epochAdjustment);
    }

    public Currency getCurrency(MosaicId mosaicId) {
        return get(currencyService.getCurrency(mosaicId));
    }

    public PublicAccount getPublicAccount(String publicKey) {
        return PublicAccount.createFromPublicKey(publicKey, networkType);
    }
}
