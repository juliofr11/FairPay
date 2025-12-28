package es.ifp.fairpay.ui.common;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import es.ifp.fairpay.R;
import es.ifp.fairpay.ui.authenticator.LoginActivity;

public class OkActivity extends AppCompatActivity {

    protected Button aceptar;
    protected Intent pasarPantalla;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ok);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.ModificarDatosOk), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

       aceptar = (Button) findViewById(R.id.button_aceptar_DisputarMensaje);

       aceptar.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               pasarPantalla = new Intent(OkActivity.this, LoginActivity.class);
               startActivity(pasarPantalla);
           }
       });

    }
}