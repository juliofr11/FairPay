package es.ifp.fairpay.data.repository;

/**
 * El RepositoryCallback es el puente de comunicación que permite ejecutar
 * tareas pesadas (como transacciones Blockchain o consultasa bases de datos)
 * en segundo plano sin congelar la pantalla del usuario.
 * Su función es notificar de vuelta a la interfaz gráfica únicamente cuando
 * el trabajo ha terminado, entregando los datos listos para mostrarse (onSuccess)
 * o informando de errores (onFailure) de forma segura y limpia.
 * Interfaz genérica para manejar respuestas asíncronas de los repositorios.
 * @param <T> El tipo de dato que se espera recibir en caso de éxito.
 */
public interface RepositoryCallback<T> {
    void onSuccess(T result);
    void onFailure(String error);
    // Opcional: para actualizaciones de progreso (útil en escaneos)
    default void onProgress(String status) {}
}