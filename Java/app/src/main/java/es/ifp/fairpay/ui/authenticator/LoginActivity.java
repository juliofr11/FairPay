package es.ifp.fairpay.ui.authenticator;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import es.ifp.fairpay.R;
import es.ifp.fairpay.data.database.DatabaseConnection;

public class LoginActivity extends AppCompatActivity {

    protected Intent pasarPantalla;
    protected EditText email, password;
    protected Button login, registro;
    protected DatabaseConnection db;

    // Método principal que se ejecuta al crear la actividad, inicializa la UI y los listeners
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // Ajustamos el padding para respetar las barras del sistema (EdgeToEdge)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Vinculamos los componentes visuales del layout con las variables locales
        email = findViewById(R.id.edit_email_login);
        password = findViewById(R.id.edit_password_login);
        login = findViewById(R.id.button_sesion_login);
        registro = findViewById(R.id.button_registro_login);

        // Instanciamos la conexión a la base de datos
        db = new DatabaseConnection();

        // Listener encargado de gestionar el evento de clic en el botón de login
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String emailLogin = email.getText().toString().trim();
                String passwordLogin = password.getText().toString().trim();

                // Validamos que los campos obligatorios contengan datos
                if (emailLogin.isEmpty() || passwordLogin.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Rellene todos los campos", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Iniciamos la petición asíncrona a la base de datos para verificar credenciales
                db.loginUsuario(new DatabaseConnection.LoginListener() {

                    // Método que se ejecuta si la autenticación en base de datos es correcta
                    @Override
                    public void onLoginSuccess(String nombreRecuperado, String apellidosRecuperados, String privateKeyRecuperada) {
                        runOnUiThread(() -> {
                            // Verificación de integridad básica de la clave privada recuperada
                            if (privateKeyRecuperada.length() < 60) {
                                Toast.makeText(LoginActivity.this, "Error: Usuario con datos corruptos. Regístrese de nuevo.", Toast.LENGTH_LONG).show();
                                return;
                            }

                            // Almacenamos los datos de sesión en SharedPreferences para persistencia local
                            SharedPreferences prefs = getSharedPreferences("FairPayPrefs", Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();

                            editor.putString("CURRENT_USER_NAME", nombreRecuperado + " " + apellidosRecuperados);
                            editor.putString("CURRENT_USER_EMAIL", emailLogin);
                            editor.putString("CURRENT_USER_PRIVATE_KEY", privateKeyRecuperada);

                            editor.apply();

                            Toast.makeText(LoginActivity.this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show();

                            // Navegamos a la pantalla de carga para inicializar la app
                            pasarPantalla = new Intent(LoginActivity.this, LoadScreenActivity.class);
                            pasarPantalla.putExtra("PANTALLA", "InicioActivity");
                            startActivity(pasarPantalla);
                        });
                    }

                    // Método que gestiona los errores de login (contraseña incorrecta o usuario no encontrado)
                    @Override
                    public void onLoginFailure(String error) {
                        runOnUiThread(() -> {
                            Toast.makeText(LoginActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                        });
                    }
                }, emailLogin, passwordLogin);
            }
        });

        // Listener para redirigir al usuario a la pantalla de registro
        registro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pasarPantalla = new Intent(LoginActivity.this, RegistroActivity.class);
                startActivity(pasarPantalla);
            }
        });
    }
}