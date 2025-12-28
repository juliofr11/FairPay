package es.ifp.fairpay.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Repositorio encargado de gestionar la información de sesión y credenciales del usuario de forma local.
 * Utiliza SharedPreferences para almacenar y recuperar datos sensibles (clave privada) e identificación (email)
 * dentro del almacenamiento seguro de la aplicación.
 */
public class UserRepository {

    /**
     * Almacena la clave privada del usuario de forma persistente en las preferencias de la aplicación.
     * Esta clave es necesaria para firmar transacciones en la Blockchain.
     *
     * @param contexto     Contexto de la aplicación.
     * @param clavePrivada Cadena de texto que representa la clave privada (wallet) del usuario.
     */
    public void setUsuarioClavePrivada(Context contexto, String clavePrivada) {
        SharedPreferences prefs = contexto.getSharedPreferences("FairPayPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("CURRENT_USER_PRIVATE_KEY", clavePrivada);
        editor.apply();
    }

    /**
     * Recupera la clave privada del usuario almacenada localmente.
     *
     * @param contexto Contexto de la aplicación.
     * @return La clave privada si existe, o una cadena vacía si no se ha guardado ninguna.
     */
    public String getUsuarioClavePrivada(Context contexto) {
        SharedPreferences prefs = contexto.getSharedPreferences("FairPayPrefs", Context.MODE_PRIVATE);
        String clavePrivada = prefs.getString("CURRENT_USER_PRIVATE_KEY", "");
        if (clavePrivada.isEmpty()) {
            Log.w("UserRepository", "No se encontró clave privada guardada.");
        }
        return clavePrivada;
    }

    /**
     * Recupera el email del usuario actual almacenado durante el proceso de login.
     * Utilizado para identificar al usuario en operaciones de base de datos y disputas.
     *
     * @param contexto Contexto de la aplicación.
     * @return El email del usuario actual o una cadena vacía si no está definido.
     */
    public String getUsuarioEmail(Context contexto) {
        SharedPreferences prefs = contexto.getSharedPreferences("FairPayPrefs", Context.MODE_PRIVATE);
        return prefs.getString("CURRENT_USER_EMAIL", "");
    }
}