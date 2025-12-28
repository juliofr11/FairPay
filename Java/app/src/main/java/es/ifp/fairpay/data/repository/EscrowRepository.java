package es.ifp.fairpay.data.repository;

import android.os.Handler;
import android.os.Looper;

import org.web3j.crypto.Credentials;
import org.web3j.abi.datatypes.Type;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import es.ifp.fairpay.data.service.FairPayService;

/**
 * Repositorio encargado de gestionar todas las interacciones con la Blockchain (Web3).
 * Encapsula la lógica de conexión al contrato inteligente, gestión de hilos en segundo plano
 * y retorno de resultados al hilo principal de la interfaz de usuario.
 */
public class EscrowRepository {

    private final ExecutorService executor;
    private final Handler mainHandler;

    /**
     * Constructor de la clase.
     * Inicializa los hilos para operaciones y comunicar resultados a la UI.
     */
    public EscrowRepository() {
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Inicia la creación de un nuevo contrato de depósito (Escrow) en la Blockchain.
     *
     * @param privateKey     Clave privada del usuario (Comprador) para firmar la transacción.
     * @param sellerAddress  Dirección pública (Wallet) del vendedor beneficiario.
     * @param callback       Interfaz de retorno que devuelve el hash de la transacción generada o un error.
     */
    public void createEscrow(String privateKey, String sellerAddress, RepositoryCallback<String> callback) {
        executor.execute(() -> {
            try {
                FairPayService service = new FairPayService(privateKey);
                String hash = service.createEscrow(sellerAddress);
                postSuccess(callback, hash);
            } catch (Exception e) {
                postError(callback, e.getMessage());
            }
        });
    }

    /**
     * Realiza la transferencia de fondos (Ether) al contrato inteligente asociado al ID especificado.
     *
     * @param privateKey Clave privada del usuario para firmar la transferencia.
     * @param escrowId   Identificador del depósito a financiar.
     * @param amountWei  Cantidad a transferir expresada en Wei.
     * @param callback   Interfaz de retorno con el hash de la transacción o error.
     */
    public void fundEscrow(String privateKey, BigInteger escrowId, BigInteger amountWei, RepositoryCallback<String> callback) {
        executor.execute(() -> {
            try {
                FairPayService service = new FairPayService(privateKey);
                String hash = service.fundEscrow(escrowId, amountWei);
                postSuccess(callback, hash);
            } catch (Exception e) {
                postError(callback, e.getMessage());
            }
        });
    }

    /**
     * Ejecuta la aprobación por parte del comprador para liberar los fondos al vendedor.
     *
     * @param privateKey Clave privada del comprador.
     * @param escrowId   Identificador del depósito a aprobar.
     * @param callback   Interfaz de retorno con el hash de la transacción o error.
     */
    public void approveRelease(String privateKey, BigInteger escrowId, RepositoryCallback<String> callback) {
        executor.execute(() -> {
            try {
                FairPayService service = new FairPayService(privateKey);
                String hash = service.approveRelease(escrowId);
                postSuccess(callback, hash);
            } catch (Exception e) {
                postError(callback, e.getMessage());
            }
        });
    }

    /**
     * Permite a la plataforma (rol administrativo) resolver una disputa activa, decidiendo el destino de los fondos.
     *
     * @param privateKey  Clave privada de la cuenta administradora de la plataforma.
     * @param escrowId    Identificador del depósito en disputa.
     * @param refundBuyer Booleano que indica la decisión: true para devolver al comprador, false para liberar al vendedor.
     * @param callback    Interfaz de retorno con el hash de la resolución o error.
     */
    public void resolveDispute(String privateKey, BigInteger escrowId, boolean refundBuyer, RepositoryCallback<String> callback) {
        executor.execute(() -> {
            try {
                FairPayService service = new FairPayService(privateKey);
                String hash = service.resolveDispute(escrowId, refundBuyer);
                postSuccess(callback, hash);
            } catch (Exception e) {
                postError(callback, e.getMessage());
            }
        });
    }

    /**
     * Monitoriza la red Blockchain esperando la confirmación de una transacción para obtener el ID del depósito generado.
     *
     * @param privateKey Clave privada (necesaria para inicializar el servicio, aunque sea lectura de eventos).
     * @param txHash     Hash de la transacción de creación a rastrear.
     * @param callback   Interfaz de retorno con el ID (BigInteger) del nuevo depósito o error.
     */
    public void waitForEscrowId(String privateKey, String txHash, RepositoryCallback<BigInteger> callback) {
        executor.execute(() -> {
            try {
                // Se usa una clave dummy si viene vacía solo para lecturas que no requieren firma real
                String pk = (privateKey == null || privateKey.isEmpty()) ? "0x01" : privateKey;
                FairPayService service = new FairPayService(pk);
                BigInteger id = service.waitForEscrowId(txHash);
                postSuccess(callback, id);
            } catch (Exception e) {
                postError(callback, e.getMessage());
            }
        });
    }

    /**
     * Consulta el estado actual y los detalles de un depósito específico directamente del contrato inteligente.
     *
     * @param privateKey Clave privada para inicializar la conexión.
     * @param escrowId   Identificador del depósito a consultar.
     * @param callback   Interfaz de retorno con una lista de tipos (Type) de Web3j conteniendo los datos del contrato.
     */
    public void getEscrowDetails(String privateKey, BigInteger escrowId, RepositoryCallback<List<Type>> callback) {
        executor.execute(() -> {
            try {
                String pk = (privateKey == null || privateKey.isEmpty()) ? "0x01" : privateKey;
                FairPayService service = new FairPayService(pk);
                List<Type> details = service.getEscrowDetails(escrowId);
                postSuccess(callback, details);
            } catch (Exception e) {
                postError(callback, e.getMessage());
            }
        });
    }

    /**
     * Realiza un barrido secuencial de IDs en el contrato para identificar operaciones pendientes asociadas a la wallet del usuario.
     *
     * @param privateKey Clave privada del usuario para derivar su dirección pública y filtrar los resultados.
     * @param callback   Interfaz de retorno que notifica progreso, hallazgos parciales o finalización (null).
     */
    public void scanPendingOperations(String privateKey, RepositoryCallback<PendingOpResult> callback) {
        executor.execute(() -> {
            String pk = (privateKey == null || privateKey.isEmpty()) ? "0x01" : privateKey;
            FairPayService service = new FairPayService(pk);
            String myAddress;

            try {
                if (privateKey != null && !privateKey.isEmpty()) {
                    myAddress = Credentials.create(privateKey).getAddress();
                } else {
                    postError(callback, "Se requiere clave privada para escanear");
                    return;
                }
            } catch (Exception e) {
                postError(callback, "Clave privada inválida");
                return;
            }

            int consecutiveFailures = 0;
            int currentId = 0;

            // Bucle de escaneo secuencial
            while (consecutiveFailures < 5) {
                try {
                    BigInteger id = BigInteger.valueOf(currentId);
                    List<Type> details = service.getEscrowDetails(id);

                    if (details != null && !details.isEmpty()) {
                        consecutiveFailures = 0;
                        // Notificar progreso a la UI
                        final String statusMsg = "Escaneando ID: " + currentId + "...";
                        mainHandler.post(() -> callback.onProgress(statusMsg));

                        // Enviar resultado parcial encontrado
                        PendingOpResult result = new PendingOpResult(id, details, myAddress);
                        mainHandler.post(() -> callback.onSuccess(result));
                    } else {
                        consecutiveFailures++;
                    }
                } catch (Exception e) {
                    consecutiveFailures++;
                }
                currentId++;
            }
            // Finalizar escaneo enviando null como señal de término
            mainHandler.post(() -> callback.onSuccess(null));
        });
    }

    /**
     * Clase auxiliar para encapsular los resultados de una operación de escaneo.
     */
    public static class PendingOpResult {
        public BigInteger id;
        public List<Type> details;
        public String myAddress;

        public PendingOpResult(BigInteger id, List<Type> details, String myAddress) {
            this.id = id;
            this.details = details;
            this.myAddress = myAddress;
        }
    }

    private <T> void postSuccess(RepositoryCallback<T> callback, T result) {
        mainHandler.post(() -> callback.onSuccess(result));
    }

    private <T> void postError(RepositoryCallback<T> callback, String error) {
        mainHandler.post(() -> callback.onFailure(error));
    }
}