package es.ifp.fairpay.data.security;

import android.content.Context;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import java.util.concurrent.Executor;

/**
 * Clase de Backend para gestionar la autenticación biométrica (huella dactilar).
 * AHORA Acepta una acción (Runnable) para ejecutarla solo en caso de éxito
 * y permite la operación si el dispositivo no tiene biometría.
 */
public class FingerprintCheck {

    /**
     * Muestra el popup de autenticación biométrica y ejecuta una acción si tiene éxito.
     * Si el dispositivo no es compatible, ejecuta la acción directamente.
     *
     * @param fragment El Fragment desde el que se llama.
     * @param onSuccessAction La acción (código) que se debe ejecutar tras una verificación exitosa o si no hay biometría.
     */
    public void showFingerprintDialog(Fragment fragment, final Runnable onSuccessAction) {
        Context context = fragment.getContext();
        if (context == null || fragment.getActivity() == null) {
            Toast.makeText(context, "Contexto no disponible para la autenticación.", Toast.LENGTH_SHORT).show();
            return;
        }

        BiometricManager biometricManager = BiometricManager.from(context);
        int canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG);

        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(context, "Para más seguridad agregue una huella digital.", Toast.LENGTH_SHORT).show();
            if (onSuccessAction != null) {
                onSuccessAction.run();
            }
            return;
        }

        Executor executor = ContextCompat.getMainExecutor(context);

        BiometricPrompt biometricPrompt = new BiometricPrompt(fragment, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(context, "Autenticación cancelada: " + errString, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                if (onSuccessAction != null) {
                    onSuccessAction.run();
                }
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(context, "Huella no reconocida. Inténtalo de nuevo.", Toast.LENGTH_SHORT).show();
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Verificación requerida")
                .setSubtitle("Usa tu huella para confirmar la acción")
                .setNegativeButtonText("Cancelar")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }
}
    