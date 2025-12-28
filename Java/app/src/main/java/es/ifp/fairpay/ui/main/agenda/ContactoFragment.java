package es.ifp.fairpay.ui.main.agenda;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import es.ifp.fairpay.R;

public class ContactoFragment extends Fragment {

    // Vistas de la interfaz
    private TextView tvNombre, tvWallet, tvAlias;
    private Button btnEnviar, btnEliminar;

    // Variables para almacenar los datos del contacto
    private String contactName;
    private String contactWallet;

    public ContactoFragment() {
        // Constructor público vacío requerido por Android
    }

    // Esta función se encarga de inflar el diseño visual del fragmento para que se muestre en la pantalla
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_contacto, container, false);
    }

    // Esta función se encarga de inicializar los datos del contacto recibido y configurar los botones de acción
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Vinculación de los elementos de la interfaz con las variables
        tvNombre = view.findViewById(R.id.text_contacto_contacto);
        tvWallet = view.findViewById(R.id.text_wallet_contacto);
        tvAlias = view.findViewById(R.id.text_alias_contacto);
        btnEnviar = view.findViewById(R.id.boton_enviar_contacto);
        btnEliminar = view.findViewById(R.id.boton_eliminar_contacto);

        NavController navController = Navigation.findNavController(view);

        // Recuperación de los datos enviados desde la Agenda (Bundle)
        if (getArguments() != null) {
            contactName = getArguments().getString("CONTACT_NAME");
            contactWallet = getArguments().getString("CONTACT_WALLET");

            // Rellenamos la UI con los datos reales del contacto si existen
            if (contactName != null) tvNombre.setText(contactName);
            if (contactWallet != null) tvWallet.setText(contactWallet);

            // Generación de un alias visual estético basado en el nombre del contacto
            if (contactName != null) {
                String[] parts = contactName.split(" ");
                if (parts.length > 0) {
                    tvAlias.setText("@" + parts[0].toLowerCase());
                } else {
                    tvAlias.setText("@usuario");
                }
            }
        }

        // Listener para el botón de enviar dinero, que navega a la pantalla de Operaciones pre-cargando la wallet del vendedor
        btnEnviar.setOnClickListener(v -> {
            if (contactWallet != null) {
                // Preparamos el paquete de datos para OperacionesFragment
                Bundle args = new Bundle();
                // "WALLET_VENDEDOR" es la clave que espera OperacionesFragment
                args.putString("WALLET_VENDEDOR", contactWallet);

                // Navegamos a la pantalla de Operaciones
                try {
                    navController.navigate(R.id.operacionesFragment, args);
                } catch (IllegalArgumentException e) {
                    Toast.makeText(getContext(), "Error navegando a Operaciones", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getContext(), "Error: Contacto sin wallet", Toast.LENGTH_SHORT).show();
            }
        });

        // Listener para el botón de eliminar contacto (navegación a pantalla de confirmación)
        btnEliminar.setOnClickListener(v -> {
            try {
                navController.navigate(R.id.action_contactoFragment_to_eliminarContactoFragment);
            } catch (Exception e) {
                // Gestión silenciosa de errores de navegación
            }
        });
    }
}