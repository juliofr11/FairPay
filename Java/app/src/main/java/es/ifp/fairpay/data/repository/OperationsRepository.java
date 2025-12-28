package es.ifp.fairpay.data.repository;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Repositorio encargado de gestionar la persistencia local del estado de las operaciones.
 * Utiliza SharedPreferences para guardar el progreso de transacciones interrumpidas
 * (creación, fondeo, aprobación) y permitir su recuperación al reiniciar la app o navegar entre pantallas.
 */
public class OperationsRepository {

    /**
     * Guarda el estado actual de una operación en curso para evitar pérdida de datos.
     *
     * @param context   Contexto de la aplicación necesario para acceder a SharedPreferences.
     * @param state     Identificador del estado actual (ej. WAITING_ID, WAITING_FUND).
     * @param userEmail Email del usuario que realiza la operación, para asegurar que se restaura al dueño correcto.
     * @param hash      Hash de la última transacción Blockchain generada (si existe).
     * @param id        ID del depósito asociado (si ya ha sido confirmado).
     * @param logMsg    Mensaje de estado o log para mostrar al usuario al restaurar la vista.
     */
    public void saveOperationState(Context context, String state, String userEmail, String hash, String id, String logMsg) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences("FairPayState", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString("OP_STATE", state);
        if (hash != null) editor.putString("OP_HASH", hash);
        if (id != null) editor.putString("OP_ID", id);
        if (logMsg != null) editor.putString("OP_LOG", logMsg);
        editor.putString("OP_USER", userEmail);
        editor.apply();
    }

    /**
     * Limpia completamente el estado de la operación guardada.
     * Se debe invocar cuando una operación finaliza con éxito o es cancelada explícitamente por el usuario.
     *
     * @param context Contexto de la aplicación.
     */
    public void clearOperationState(Context context) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences("FairPayState", Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }

    /**
     * Recupera el objeto SharedPreferences que contiene los datos de la última operación pendiente.
     *
     * @param context Contexto de la aplicación.
     * @return Objeto SharedPreferences con los datos persistidos o null si el contexto no es válido.
     */
    public SharedPreferences restoreOperationState(Context context) {
        if (context == null) {
            return null;
        }
        return context.getSharedPreferences("FairPayState", Context.MODE_PRIVATE);
    }
}