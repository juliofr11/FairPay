package es.ifp.fairpay.ui.main;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import es.ifp.fairpay.R;
import es.ifp.fairpay.ui.authenticator.LoginActivity;

import java.util.Timer;
import java.util.TimerTask;
public class MainActivity extends AppCompatActivity {
    protected Intent pasarPantalla;
    protected TimerTask tt;
    protected Timer t;

    // Variable global para almacenar la clave privada en memoria si fuera necesario
    private String usuarioClavePrivada = "";

    // Método principal que inicializa la pantalla de carga (Splash) y configura los elementos visuales
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.splash_screen), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Intentamos recuperar la clave privada de las preferencias compartidas para tenerla disponible
        SharedPreferences prefs = getSharedPreferences("FairPayPrefs", Context.MODE_PRIVATE);
        String savedKey = prefs.getString("CURRENT_USER_PRIVATE_KEY", "");
        if (!savedKey.isEmpty()) {
            this.usuarioClavePrivada = savedKey;
            Log.d("MainActivity", "Clave privada recuperada: " + savedKey);
        }

        tt = new TimerTask() {
            @Override
            public void run() {
                pasarPantalla = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(pasarPantalla);
                finish(); // Finaliza esta activity para que el usuario no pueda volver a ella
            }
        };
        t = new Timer();
        t.schedule(tt, 5000);
    }


    // Método para obtener la clave privada almacenada en esta actividad
    public String getUsuarioClavePrivada() {
        return this.usuarioClavePrivada;
    }
}