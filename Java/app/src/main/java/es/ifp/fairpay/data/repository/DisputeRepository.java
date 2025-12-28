package es.ifp.fairpay.data.repository;

import android.os.Handler;
import android.os.Looper;

import java.util.List;
import java.util.Map;

import es.ifp.fairpay.data.database.DatabaseConnection;

/**
 * Repositorio encargado de gestionar las operaciones relacionadas con las disputas.
 * Actúa como capa intermedia entre la lógica de presentación y la conexión directa a la base de datos.
 */
public class DisputeRepository {

    private final DatabaseConnection dbConnection;

    /**
     * Constructor de la clase.
     * Inicializa la instancia de conexión a la base de datos.
     */
    public DisputeRepository() {
        this.dbConnection = new DatabaseConnection();
    }

    /**
     * Recupera el listado de disputas asociadas a un usuario específico.
     *
     * @param userEmail Email del usuario para realizar la consulta.
     * @param callback  Interfaz de retorno que devuelve una lista de mapas con los datos de las disputas o un error.
     */
    public void obtenerDisputas(String userEmail, RepositoryCallback<List<Map<String, String>>> callback) {
        dbConnection.obtenerDisputas(userEmail, new DatabaseConnection.DataListener<Map<String, String>>() {
            @Override
            public void onDataSuccess(List<Map<String, String>> data) {
                callback.onSuccess(data);
            }

            @Override
            public void onDataFailure(String error) {
                callback.onFailure(error);
            }
        });
    }

    /**
     * Registra una nueva disputa en el sistema.
     *
     * @param userEmail  Email del usuario que inicia la disputa.
     * @param escrowId   Identificador único del depósito en conflicto.
     * @param rol        Rol del usuario que abre la disputa (Comprador o Vendedor).
     * @param motivo     Descripción detallada del motivo de la disputa.
     * @param buyerAddr  Dirección de wallet del comprador.
     * @param sellerAddr Dirección de wallet del vendedor.
     * @param callback   Interfaz de retorno para confirmar el éxito del registro o notificar un error.
     */
    public void abrirDisputa(String userEmail, String escrowId, String rol, String motivo, String buyerAddr, String sellerAddr, RepositoryCallback<String> callback) {
        dbConnection.abrirDisputa(userEmail, escrowId, rol, motivo, buyerAddr, sellerAddr, new DatabaseConnection.OperacionListener() {
            @Override
            public void onOperacionExito(String mensaje) {
                callback.onSuccess(mensaje);
            }

            @Override
            public void onOperacionFallo(String error) {
                callback.onFailure(error);
            }
        });
    }

    /**
     * Actualiza el estado de una disputa a "RESUELTA" tras la decisión de la plataforma.
     *
     * @param escrowId Identificador del depósito asociado a la disputa.
     * @param decision Texto que describe la resolución tomada (ej. devolución a comprador).
     * @param callback Interfaz de retorno para confirmar la actualización.
     */
    public void marcarDisputaResuelta(String escrowId, String decision, RepositoryCallback<String> callback) {
        dbConnection.marcarDisputaResuelta(escrowId, decision, new DatabaseConnection.OperacionListener() {
            @Override
            public void onOperacionExito(String mensaje) {
                callback.onSuccess(mensaje);
            }

            @Override
            public void onOperacionFallo(String error) {
                callback.onFailure(error);
            }
        });
    }

    /**
     * Solicita la intervención de la plataforma cambiando el estado de la disputa a "REVISION".
     *
     * @param escrowId Identificador del depósito sobre el cual se solicita revisión.
     * @param callback Interfaz de retorno para confirmar la solicitud.
     */
    public void solicitarRevision(String escrowId, RepositoryCallback<String> callback) {
        dbConnection.solicitarRevision(escrowId, new DatabaseConnection.OperacionListener() {
            @Override
            public void onOperacionExito(String mensaje) {
                callback.onSuccess(mensaje);
            }

            @Override
            public void onOperacionFallo(String error) {
                callback.onFailure(error);
            }
        });
    }
}