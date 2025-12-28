package es.ifp.fairpay.ui.authenticator;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.mindrot.jbcrypt.BCrypt;
import org.web3j.crypto.Credentials;

import java.io.IOException;
import java.security.GeneralSecurityException;

import es.ifp.fairpay.R;
import es.ifp.fairpay.data.database.DatabaseConnection;
import es.ifp.fairpay.data.security.EncryptManager;
import es.ifp.fairpay.utils.ValidationUtils;

public class RegistroActivity extends AppCompatActivity {
    protected Intent pasarPantalla;
    protected EditText nombre, apellidos, email, telefono, wallet, password, password2;
    protected Button registro;
    protected CheckBox condiciones;
    protected DatabaseConnection databaseConnection;
    private EncryptManager encryptManager;

    // Método de ciclo de vida encargado de inicializar la interfaz y los componentes de seguridad
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registro);

        // Ajustamos el diseño para respetar los márgenes de las barras del sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.splash_screen), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Vinculación de elementos de la interfaz de usuario
        nombre = findViewById(R.id.edit_nombre_registro);
        apellidos = findViewById(R.id.edit_apellidos_registro);
        email = findViewById(R.id.edit_email_registro);
        telefono = findViewById(R.id.edit_telefono_registro);
        wallet = findViewById(R.id.edit_wallet_registro);

        // Configuramos el campo wallet para solicitar explícitamente la clave privada
        wallet.setHint("Introduce tu Clave Privada (0x...)");

        password = findViewById(R.id.edit_password_registro);
        password2 = findViewById(R.id.edit_password2_registro);
        registro = findViewById(R.id.button_registrarse_registro);
        condiciones = findViewById(R.id.check_privacidad_registro);

        databaseConnection = new DatabaseConnection();

        // Inicializamos el gestor de encriptación para el almacenamiento seguro local
        try {
            encryptManager = new EncryptManager(getApplicationContext());
        } catch (GeneralSecurityException | IOException e) {
            Log.e("FairPayCrypto", "Error crítico al inicializar EncryptManager", e);
            Toast.makeText(this, "Error de seguridad irrecuperable.", Toast.LENGTH_LONG).show();
            registro.setEnabled(false);
        }

        // Listener para gestionar el proceso de registro al pulsar el botón
        registro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Validamos que el gestor de seguridad esté activo y los términos aceptados
                if (encryptManager == null || !condiciones.isChecked()) {
                    Toast.makeText(RegistroActivity.this, "Acepte los términos o revise seguridad.", Toast.LENGTH_SHORT).show();
                    return;
                }

                String nombreRegistro = nombre.getText().toString().trim();
                String apellidosRegistro = apellidos.getText().toString().trim();
                String emailRegistro = email.getText().toString().trim();
                String telefonoRegistro = telefono.getText().toString().trim();
                String privateKeyInput = wallet.getText().toString().trim(); // Clave privada introducida por el usuario
                String passwordRegistro = password.getText().toString().trim();
                String password2Registro = password2.getText().toString().trim();

                // Comprobamos que todos los campos obligatorios contengan datos
                if (nombreRegistro.isEmpty() || privateKeyInput.isEmpty() || passwordRegistro.isEmpty()) {
                    Toast.makeText(RegistroActivity.this, "Rellene todos los campos.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!ValidationUtils.isValidEmail(emailRegistro)) {
                    Toast.makeText(RegistroActivity.this, "El formato del correo electrónico no es válido.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Verificamos la longitud de la cadena para asegurar que es una clave privada válida y no una dirección pública
                if (privateKeyInput.length() < 60) {
                    Toast.makeText(RegistroActivity.this, "ERROR: Parece una Dirección Pública. Debes introducir la CLAVE PRIVADA.", Toast.LENGTH_LONG).show();
                    return;
                }

                // Validamos la coincidencia de las contraseñas
                if (!passwordRegistro.equals(password2Registro)) {
                    Toast.makeText(RegistroActivity.this, "Las contraseñas no coinciden.", Toast.LENGTH_SHORT).show();
                    return;
                }else if(!ValidationUtils.isValidPassword(passwordRegistro)){
                    Toast.makeText(RegistroActivity.this, "La contraseña debe tener al menos 8 caracteres.", Toast.LENGTH_SHORT).show();
                    return;
                }


                try {
                    // Derivamos la dirección pública a partir de la clave privada para almacenarla como referencia
                    Credentials credentials = Credentials.create(privateKeyInput);
                    String publicAddress = credentials.getAddress();

                    // Generamos el hash de la contraseña usando BCrypt para almacenamiento seguro
                    String contrasenaHash = BCrypt.hashpw(passwordRegistro, BCrypt.gensalt());

                    // Guardamos una copia encriptada de la clave privada en el almacenamiento local
                    String privateKeyAlias = "pk_" + emailRegistro.replaceAll("[^a-zA-Z0-9]", "_");
                    encryptManager.encryptAndSave(privateKeyAlias, privateKeyInput);

                    // Procedemos a registrar el usuario en la base de datos remota
                    databaseConnection.registrarUsuario(new DatabaseConnection.RegistroListener() {

                                                            // Callback de éxito: guardamos sesión y navegamos a la pantalla de confirmación
                                                            @Override
                                                            public void onRegistroSuccess(String nombreConfirmado, String pkConfirmada) {
                                                                runOnUiThread(()->{
                                                                    // Almacenamos los datos de sesión en preferencias compartidas
                                                                    SharedPreferences prefs = getSharedPreferences("FairPayPrefs", Context.MODE_PRIVATE);
                                                                    SharedPreferences.Editor editor = prefs.edit();

                                                                    // Guardamos la clave privada original para su uso en operaciones de blockchain
                                                                    editor.putString("CURRENT_USER_PRIVATE_KEY", privateKeyInput);
                                                                    editor.putString("CURRENT_USER_NAME", nombreRegistro + " " + apellidosRegistro);
                                                                    editor.putString("CURRENT_USER_EMAIL", emailRegistro);
                                                                    editor.apply();

                                                                    Toast.makeText(RegistroActivity.this, "Registro OK. Wallet: " + publicAddress.substring(0,6)+"...", Toast.LENGTH_SHORT).show();
                                                                    pasarPantalla = new Intent(RegistroActivity.this, LoadScreenActivity.class);
                                                                    pasarPantalla.putExtra("PANTALLA", "OkActivity");
                                                                    startActivity(pasarPantalla);
                                                                });
                                                            }

                                                            // Callback de fallo: notificamos el error al usuario
                                                            @Override
                                                            public void onRegistroFailure(String error) {
                                                                runOnUiThread(()-> Toast.makeText(RegistroActivity.this, error, Toast.LENGTH_SHORT).show());
                                                            }
                                                        }, nombreRegistro, apellidosRegistro, emailRegistro, contrasenaHash, telefonoRegistro,
                            publicAddress,   // Enviamos la dirección pública derivada
                            privateKeyInput  // Enviamos la clave privada original
                    );

                } catch (Exception e) {
                    Log.e("FairPay", "Error en registro: " + e.getMessage());
                    Toast.makeText(RegistroActivity.this, "Clave Privada inválida (formato incorrecto).", Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}