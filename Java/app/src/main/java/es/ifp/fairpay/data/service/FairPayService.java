package es.ifp.fairpay.data.service;

import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class FairPayService {

    // Configuración de la red y contrato inteligente
    private static final String CONTRACT_ADDRESS = "0xa6e8ec1458dd40bcc9e3aeec38bdf7488ad302be";
    private static final String RPC_URL = "https://sepolia.infura.io/v3/a0e25c046e2b4a548754bf46f48f7b6b";

    private final Web3j web3j;
    private final Credentials credentials;

    // Esta función se encarga de inicializar el servicio de conexión a Web3j y cargar las credenciales del usuario para firmar transacciones
    public FairPayService(String privateKey) {
        this.web3j = Web3j.build(new HttpService(RPC_URL));
        this.credentials = Credentials.create(privateKey);
    }

    // Esta función se encarga de interactuar con el contrato inteligente para crear un nuevo depósito de garantía en la Blockchain
    public String createEscrow(String sellerAddress) throws Exception {
        Function function = new Function(
                "createEscrow",
                Arrays.asList(new Address(credentials.getAddress()), new Address(sellerAddress)),
                Collections.emptyList()
        );
        return sendTransaction(function, BigInteger.ZERO);
    }

    // Esta función se encarga de monitorear los eventos de la Blockchain hasta encontrar el ID del depósito recién creado
    public BigInteger waitForEscrowId(String txHash) throws Exception {
        Event escrowCreatedEvent = new Event("EscrowCreated",
                Arrays.asList(
                        new TypeReference<Uint256>(true) {},
                        new TypeReference<Address>(false) {},
                        new TypeReference<Address>(false) {}
                )
        );

        TransactionReceipt receipt = web3j.ethGetTransactionReceipt(txHash)
                .flowable()
                .timeout(60, TimeUnit.SECONDS)
                .blockingFirst()
                .getTransactionReceipt()
                .orElseThrow(() -> new Exception("Transaccion no minada despues de 60s."));

        String eventHash = EventEncoder.encode(escrowCreatedEvent);

        for (Log log : receipt.getLogs()) {
            List<String> topics = log.getTopics();
            if (topics.isEmpty()) continue;
            if (topics.get(0).equals(eventHash)) {
                String escrowIdHex = topics.get(1);
                return Numeric.decodeQuantity(escrowIdHex);
            }
        }
        throw new Exception("Evento EscrowCreated no encontrado.");
    }

    // Esta función se encarga de enviar los fondos (ETH) al contrato inteligente para asegurar el depósito
    public String fundEscrow(BigInteger escrowId, BigInteger amountInWei) throws Exception {
        Function function = new Function(
                "fund",
                Collections.singletonList(new Uint256(escrowId)),
                Collections.emptyList()
        );
        return sendTransaction(function, amountInWei);
    }

    // Esta función se encarga de autorizar la liberación de fondos una vez cumplidas las condiciones del acuerdo
    public String approveRelease(BigInteger escrowId) throws Exception {
        Function function = new Function(
                "approveRelease",
                Collections.singletonList(new Uint256(escrowId)),
                Collections.emptyList()
        );
        return sendTransaction(function, BigInteger.ZERO);
    }

    // Esta función se encarga de consultar los detalles del depósito (estado, participantes, fondos) almacenados en el contrato inteligente
    public List<org.web3j.abi.datatypes.Type> getEscrowDetails(BigInteger escrowId) throws Exception {
        Function function = new Function(
                "getEscrow",
                Collections.singletonList(new Uint256(escrowId)),
                Arrays.asList(
                        new TypeReference<Address>() {}, // buyer
                        new TypeReference<Address>() {}, // seller
                        new TypeReference<Uint256>() {}, // amount
                        new TypeReference<org.web3j.abi.datatypes.Bool>() {}, // isFunded
                        new TypeReference<Uint256>() {}  // approvalCount
                )
        );

        String encodedFunction = FunctionEncoder.encode(function);

        org.web3j.protocol.core.methods.response.EthCall response = web3j.ethCall(
                        org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                                credentials.getAddress(), CONTRACT_ADDRESS, encodedFunction),
                        DefaultBlockParameterName.LATEST)
                .send();

        if (response.hasError()) {
            throw new Exception("Error leyendo contrato: " + response.getError().getMessage());
        }

        return org.web3j.abi.FunctionReturnDecoder.decode(
                response.getValue(), function.getOutputParameters());
    }

    // Esta función se encarga de encapsular la complejidad técnica (Nonce, Gas, Firma) para enviar transacciones de escritura a la Blockchain
    private String sendTransaction(Function function, BigInteger valueWei) throws Exception {
        String encodedFunction = FunctionEncoder.encode(function);

        EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(
                credentials.getAddress(), DefaultBlockParameterName.LATEST).sendAsync().get();
        BigInteger nonce = ethGetTransactionCount.getTransactionCount();

        BigInteger gasLimit = BigInteger.valueOf(300_000);
        BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();

        RawTransaction rawTransaction = RawTransaction.createTransaction(
                nonce, gasPrice, gasLimit, CONTRACT_ADDRESS, valueWei, encodedFunction
        );

        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
        String hexValue = Numeric.toHexString(signedMessage);

        EthSendTransaction transactionResponse = web3j.ethSendRawTransaction(hexValue).sendAsync().get();

        if (transactionResponse.hasError()) {
            throw new Exception("Error Blockchain: " + transactionResponse.getError().getMessage());
        }
        return transactionResponse.getTransactionHash();
    }

    // Esta función se encarga de ejecutar la resolución de una disputa desde la cuenta de la Plataforma, asignando los fondos según la decisión tomada
    public String resolveDispute(BigInteger escrowId, boolean refundBuyer) throws Exception {
        Function function = new Function(
                "resolveDispute",
                Arrays.asList(
                        new Uint256(escrowId),
                        new org.web3j.abi.datatypes.Bool(refundBuyer)
                ),
                Collections.emptyList()
        );

        return sendTransaction(function, BigInteger.ZERO);
    }
}