package sym.ads.core;

import io.nem.symbol.core.utils.ConvertUtils;
import io.nem.symbol.sdk.api.BinarySerialization;
import io.nem.symbol.sdk.api.RepositoryFactory;
import io.nem.symbol.sdk.api.TransactionRepository;
import io.nem.symbol.sdk.infrastructure.BinarySerializationImpl;
import io.nem.symbol.sdk.infrastructure.okhttp.RepositoryFactoryOkHttpImpl;
import io.nem.symbol.sdk.model.account.Account;
import io.nem.symbol.sdk.model.account.PublicAccount;
import io.nem.symbol.sdk.model.message.PlainMessage;
import io.nem.symbol.sdk.model.mosaic.Currency;
import io.nem.symbol.sdk.model.network.NetworkType;
import io.nem.symbol.sdk.model.transaction.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static sym.ads.core.Utils.GSON;

/**
 * Created by vbondarenko on 15.09.2020.
 */

public class AddingCosignaturesAggregateComplete {

    private Duration epochAdjustment;

    @Test
    @Disabled
    void example() throws Exception {
        try (final RepositoryFactory repositoryFactory = new RepositoryFactoryOkHttpImpl(
                "http://api-01.eu-west-1.testnet.symboldev.network:3000")) {

            epochAdjustment = SymConnector.get(repositoryFactory.getEpochAdjustment(), 30, TimeUnit.SECONDS);
            // replace with recipient address


            /* start block 01 */
            NetworkType networkType = repositoryFactory.getNetworkType().toFuture().get();
            Currency networkCurrency = repositoryFactory.getNetworkCurrency().toFuture().get();

            // replace with alice private key
            String alicePrivatekey = "F186F01C81614DCF88B9C517191A8A1EFBBDACF536488C13786604EB63279C72";
            Account aliceAccount = Account.createFromPrivateKey(alicePrivatekey, networkType);

            // replace with bob public key
            String bobPublicKey = "5EDFE0DB0FAD8798F67500F095F4940C802525C0D46D6EEDC1C679E7061FD85B";
            PublicAccount bobPublicAccount = PublicAccount.createFromPublicKey(bobPublicKey, networkType);

            TransferTransaction aliceTransferTransaction = TransferTransactionFactory.create(
                    networkType,
                    getDeadline(),
                    bobPublicAccount.getAddress(),
                    Collections.singletonList(networkCurrency.createRelative(BigInteger.valueOf(100))))
                    .message(PlainMessage.create("payout 100"))
                    .build();

            TransferTransaction bobTransferTransaction = TransferTransactionFactory.create(
                    networkType,
                    getDeadline(),
                    aliceAccount.getAddress(),
                    Collections.singletonList(networkCurrency.createRelative(BigInteger.valueOf(90))))
//                            Collections.singletonList(new Mosaic(new NamespaceId("collectible"), BigInteger.valueOf(1))))
                    .message(PlainMessage.create("payout 90"))
                    .build();

            AggregateTransaction aggregateTransaction = AggregateTransactionFactory.createComplete(
                    networkType,
                    getDeadline(),
                    Arrays.asList(
                            aliceTransferTransaction.toAggregate(aliceAccount.getPublicAccount()),
                            bobTransferTransaction.toAggregate(bobPublicAccount)))
                    .maxFee(BigInteger.valueOf(1000000))
                    .build();
            /* end block 01 */

            /* start block 02 */
            String generationHash = repositoryFactory.getGenerationHash().toFuture().get();
            TransactionRepository transactionRepository = repositoryFactory.createTransactionRepository();

//            SignedTransaction signedTransactionNotComplete = aliceAccount.sign(aggregateTransaction, generationHash);
            SignedTransaction signedTransactionNotComplete = aggregateTransaction.signWith(aliceAccount, generationHash);

            String hash = signedTransactionNotComplete.getHash();
            System.out.println("Signed transaction announce hash: " + hash);

            TransactionAnnounceResponse response = transactionRepository.announce(signedTransactionNotComplete).toFuture().get();
            System.out.println("Transaction announce: " + response.getMessage());

            System.out.println(signedTransactionNotComplete.getPayload());
            /* end block 02 */

            /* start block 03 */
            // replace with bob private key
            String bobPrivateKey = "E3638FA9EF84DBD37ED994123B8FECCE3F6C0111030D3EA698875B9C20A4D1F7";
            Account bobAccount = Account.createFromPrivateKey(bobPrivateKey, networkType);

            SignedTransaction signedTransactionNotCompleteBob = aggregateTransaction.signWith(bobAccount, generationHash);
            String hashBob = signedTransactionNotCompleteBob.getHash();
            System.out.println("Signed transaction announce hash bob: " + hashBob);

            TransactionAnnounceResponse responseBob = transactionRepository.announce(signedTransactionNotCompleteBob).toFuture().get();
            System.out.println("Transaction announce bob: " + responseBob.getMessage());

            System.out.println(signedTransactionNotCompleteBob.getPayload());

            CosignatureSignedTransaction cosignedTransactionBob = CosignatureTransaction
                    .create(aggregateTransaction)
                    .signWith(bobAccount);

            System.out.println(cosignedTransactionBob.getSignature());
            System.out.println(cosignedTransactionBob.getParentHash());
            /* end block 03 */

            /* start block 04 */
            BinarySerialization serialization = BinarySerializationImpl.INSTANCE;

            AggregateTransactionFactory rectreatedAggregateTransactionFromPayload = (AggregateTransactionFactory) serialization
                    .deserializeToFactory(
                            ConvertUtils.getBytes(signedTransactionNotComplete.getPayload()));

            //Added a new cosignature.
            rectreatedAggregateTransactionFromPayload.addCosignatures(cosignedTransactionBob);

            SignedTransaction signedTransactionComplete = aliceAccount
                    .sign(rectreatedAggregateTransactionFromPayload.build(), generationHash);
            System.out.println(signedTransactionComplete.getHash());

            transactionRepository.announce(signedTransactionComplete).toFuture().get();
            /* end block 04 */

        }
    }

    @Test
    @Disabled
    void isWork() throws Exception {
        try (final RepositoryFactory repositoryFactory = new RepositoryFactoryOkHttpImpl("http://api-01.eu-west-1.testnet.symboldev.network:3000")) {
            epochAdjustment = SymConnector.get(repositoryFactory.getEpochAdjustment(), 30, TimeUnit.SECONDS);
            // replace with recipient address

            /* start block 01 */
            NetworkType networkType = repositoryFactory.getNetworkType().toFuture().get();
            Currency networkCurrency = repositoryFactory.getNetworkCurrency().toFuture().get();

            // replace with alice private key
            String alicePrivatekey = "F186F01C81614DCF88B9C517191A8A1EFBBDACF536488C13786604EB63279C72";
            Account aliceAccount = Account.createFromPrivateKey(alicePrivatekey, networkType);

            // replace with bob public key
            String bobPublicKey = "5EDFE0DB0FAD8798F67500F095F4940C802525C0D46D6EEDC1C679E7061FD85B";
            PublicAccount bobPublicAccount = PublicAccount.createFromPublicKey(bobPublicKey, networkType);

            TransferTransaction aliceTransferTransaction = TransferTransactionFactory.create(
                    networkType,
                    getDeadline(),
                    bobPublicAccount.getAddress(),
                    Collections.singletonList(networkCurrency.createRelative(BigInteger.valueOf(100))))
                    .message(PlainMessage.create("payout 100"))
                    .build();

            TransferTransaction bobTransferTransaction = TransferTransactionFactory.create(
                    networkType,
                    getDeadline(),
                    aliceAccount.getAddress(),
                    Collections.singletonList(networkCurrency.createRelative(BigInteger.valueOf(90))))
//                            Collections.singletonList(new Mosaic(new NamespaceId("collectible"), BigInteger.valueOf(1))))
                    .message(PlainMessage.create("payout 90"))
                    .build();

            AggregateTransaction aggregateTransaction = AggregateTransactionFactory.createComplete(
                    networkType,
                    getDeadline(),
                    Arrays.asList(
                            aliceTransferTransaction.toAggregate(aliceAccount.getPublicAccount()),
                            bobTransferTransaction.toAggregate(bobPublicAccount)))
                    .maxFee(BigInteger.valueOf(1000000))
                    .build();
            /* end block 01 */

            /* start block 02 */
            String generationHash = repositoryFactory.getGenerationHash().toFuture().get();

//            SignedTransaction signedTransactionNotComplete = aliceAccount.sign(aggregateTransaction, generationHash);
            SignedTransaction signedTransactionNotComplete = aggregateTransaction.signWith(aliceAccount, generationHash);

            String hash = signedTransactionNotComplete.getHash();
            System.out.println("Signed transaction hash: " + hash);

            System.out.println(signedTransactionNotComplete.getPayload());

            TransactionRepository transactionRepository = repositoryFactory.createTransactionRepository();
            TransactionAnnounceResponse response = transactionRepository.announce(signedTransactionNotComplete).toFuture().get();
            System.out.println("Transaction announce: " + response.getMessage());

//            AggregateTransactionService aggregateTransactionService = new AggregateTransactionServiceImpl(repositoryFactory);
            //          Assertions.assertFalse(aggregateTransactionService.isComplete(signedTransactionNotComplete).toFuture().get());

            /* end block 02 */

            /* start block 03 */

            // replace with bob private key
            String bobPrivateKey = "E3638FA9EF84DBD37ED994123B8FECCE3F6C0111030D3EA698875B9C20A4D1F7";
            Account bobAccount = Account.createFromPrivateKey(bobPrivateKey, networkType);

            SignedTransaction signedTransaction2 = aliceAccount.signTransactionWithCosignatories(
                    aggregateTransaction, Collections.singletonList(bobAccount), generationHash);
            String hash2 = signedTransaction2.getHash();
            System.out.println("Signed transaction hash2: " + hash2);
            System.out.println(signedTransaction2.getPayload());

            TransactionAnnounceResponse response2 = transactionRepository.announce(signedTransaction2).toFuture().get();
            System.out.println("Transaction announce: " + response2.getMessage());
//            Assertions.assertTrue(aggregateTransactionService.isComplete(signedTransaction2).toFuture().get());
        }
    }

    @Test
    @Disabled
    void isWork2() throws Exception {
        try (final RepositoryFactory repositoryFactory = new RepositoryFactoryOkHttpImpl("http://api-01.eu-west-1.testnet.symboldev.network:3000")) {
            epochAdjustment = SymConnector.get(repositoryFactory.getEpochAdjustment(), 30, TimeUnit.SECONDS);
            // replace with recipient address

            /* start block 01 */
            NetworkType networkType = repositoryFactory.getNetworkType().toFuture().get();
            Currency networkCurrency = repositoryFactory.getNetworkCurrency().toFuture().get();

            // replace with alice private key
            String alicePrivatekey = "F186F01C81614DCF88B9C517191A8A1EFBBDACF536488C13786604EB63279C72";
            Account aliceAccount = Account.createFromPrivateKey(alicePrivatekey, networkType);

            // replace with bob public key
            String bobPublicKey = "5EDFE0DB0FAD8798F67500F095F4940C802525C0D46D6EEDC1C679E7061FD85B";
            PublicAccount bobPublicAccount = PublicAccount.createFromPublicKey(bobPublicKey, networkType);

            TransferTransaction aliceTransferTransaction = TransferTransactionFactory.create(
                    networkType,
                    getDeadline(),
                    bobPublicAccount.getAddress(),
                    Collections.singletonList(networkCurrency.createRelative(BigInteger.valueOf(100))))
                    .message(PlainMessage.create("payout 100"))
                    .build();

            TransferTransaction bobTransferTransaction = TransferTransactionFactory.create(
                    networkType,
                    getDeadline(),
                    aliceAccount.getAddress(),
                    Collections.singletonList(networkCurrency.createRelative(BigInteger.valueOf(50))))
//                            Collections.singletonList(new Mosaic(new NamespaceId("collectible"), BigInteger.valueOf(1))))
                    .message(PlainMessage.create("payout 90"))
                    .build();

            AggregateTransaction aggregateTransaction = AggregateTransactionFactory.createComplete(
                    networkType,
                    getDeadline(),
                    Arrays.asList(
                            aliceTransferTransaction.toAggregate(aliceAccount.getPublicAccount()),
                            bobTransferTransaction.toAggregate(bobPublicAccount)))
                    .maxFee(BigInteger.valueOf(1000000))
                    .build();
            /* end block 01 */

            /* start block 02 */
            String generationHash = repositoryFactory.getGenerationHash().toFuture().get();

//            SignedTransaction signedTransactionNotComplete = aliceAccount.sign(aggregateTransaction, generationHash);
            SignedTransaction signedTransactionNotComplete = aggregateTransaction.signWith(aliceAccount, generationHash);

            String hash = signedTransactionNotComplete.getHash();
            System.out.println("Signed transaction hash: " + hash);

            System.out.println(signedTransactionNotComplete.getPayload());

            TransactionRepository transactionRepository = repositoryFactory.createTransactionRepository();
            TransactionAnnounceResponse response = transactionRepository.announce(signedTransactionNotComplete).toFuture().get();
            System.out.println("Transaction announce: " + response.getMessage());

//            AggregateTransactionService aggregateTransactionService = new AggregateTransactionServiceImpl(repositoryFactory);
            //          Assertions.assertFalse(aggregateTransactionService.isComplete(signedTransactionNotComplete).toFuture().get());

            /* end block 02 */

            /* start block 03 */

            // replace with bob private key
            String bobPrivateKey = "E3638FA9EF84DBD37ED994123B8FECCE3F6C0111030D3EA698875B9C20A4D1F7";
            Account bobAccount = Account.createFromPrivateKey(bobPrivateKey, networkType);

            CosignatureSignedTransaction cosignatureSignedTransaction = bobAccount.signCosignatureTransaction(hash);
            String parentHash = cosignatureSignedTransaction.getParentHash();
            System.out.println("parentHash: " + parentHash);

            String json = GSON.toJson(cosignatureSignedTransaction);
            byte[] cosignatureSignedTransactionBytes = json.getBytes(StandardCharsets.UTF_8);

            CosignatureSignedTransaction cosignatureSignedTransaction2 = GSON.fromJson(new String(cosignatureSignedTransactionBytes, StandardCharsets.UTF_8), CosignatureSignedTransaction.class);

//            SignedTransaction signedTransaction2 = aliceAccount.signTransactionWithCosignatories(aggregateTransaction, Collections.singletonList(bobAccount), generationHash);
            SignedTransaction signedTransaction2 = aliceAccount.signTransactionGivenSignatures(
                    aggregateTransaction, Collections.singletonList(cosignatureSignedTransaction), generationHash);
            String hash2 = signedTransaction2.getHash();
            System.out.println("Signed transaction hash2: " + hash2);
            System.out.println(signedTransaction2.getPayload());

            TransactionAnnounceResponse response2 = transactionRepository.announce(signedTransaction2).toFuture().get();
            System.out.println("Transaction announce: " + response2.getMessage());
//            Assertions.assertTrue(aggregateTransactionService.isComplete(signedTransaction2).toFuture().get());
        }
    }

    public Deadline getDeadline() {
        return Deadline.create(epochAdjustment);
    }
}
