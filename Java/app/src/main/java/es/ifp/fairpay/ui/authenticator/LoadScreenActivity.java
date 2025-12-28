package es.ifp.fairpay.ui.authenticator;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.util.Timer;
import java.util.TimerTask;

import es.ifp.fairpay.R;
import es.ifp.fairpay.ui.common.OkActivity;

public class LoadScreenActivity extends AppCompatActivity {
    private static final String TAG = "FP";
    protected Intent pasarPantalla;
    protected TimerTask tt;
    protected Timer t;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_load_screen);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.load_screen), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Intent datosPantalla = getIntent();
        // TimerTask para crear una tarea para el temporizador
        tt = new TimerTask() {
            @Override
            public void run() {

                // Se evalua lo que se recibe y según valor carga una pantalla u otra
                switch(datosPantalla.getStringExtra("PANTALLA")) {
                    case "OkActivity":
                        pasarPantalla = new Intent(LoadScreenActivity.this, OkActivity.class);
                        break;
                    case "InicioActivity":
                        pasarPantalla = new Intent(LoadScreenActivity.this, InicioActivity.class);
                        break;
                    default:
                        pasarPantalla = new Intent(LoadScreenActivity.this, LoginActivity.class);
                        break;

                }
                //pasarPantalla = new Intent(LoadScreenActivity.this, LoginActivity.class);
                // Una vez definido el Intent se ejecuta
                startActivity(pasarPantalla);
            }
        };
        // Se instancia un temporizador
        t = new Timer();
        // Al metodo schedule se le pasa la tarea a ejecutar y el tiempo en milisegundos
        t.schedule(tt, 3000);
    }
}