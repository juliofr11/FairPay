package es.ifp.fairpay.ui.main.profile;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import es.ifp.fairpay.R;

public class ModificarDatosFragment extends Fragment {

    // 🔹 Referencias a los elementos del layout
    protected EditText editNombre;
    protected EditText editNick;
    protected EditText editTelefono;
    protected Spinner spinnerDivisa;
    protected Button botonAceptar;

    // 🔹 Array de ejemplo para el Spinner
    protected String[] divisas = {"Euro (€)", "Dólar ($)", "Libra (£)", "Yen (¥)"};

    public ModificarDatosFragment() {
        // Constructor vacío requerido
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflamos el layout del fragment
        return inflater.inflate(R.layout.fragment_modificar_datos, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 🔹 Vinculamos los elementos del XML con el código
        editNombre = view.findViewById(R.id.edit1_nombre_modificar);
        editNick = view.findViewById(R.id.edit2_nick_modificar);
        editTelefono = view.findViewById(R.id.edit2_telefono_modificar_modificar);
        spinnerDivisa = view.findViewById(R.id.spinnerDivisa);
        botonAceptar = view.findViewById(R.id.button_aceptar_modificar);

        // 🔹 Cargar opciones en el Spinner
        ArrayAdapter<String> adaptadorDivisa = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                divisas
        );
        spinnerDivisa.setAdapter(adaptadorDivisa);

        // 🔹 Listener del botón "Aceptar"
        botonAceptar.setOnClickListener(v -> {
            String nombre = editNombre.getText().toString().trim();
            String nick = editNick.getText().toString().trim();
            String telefono = editTelefono.getText().toString().trim();
            String divisaSeleccionada = spinnerDivisa.getSelectedItem().toString();

            // Validación básica
            if (nombre.isEmpty() || nick.isEmpty() || telefono.isEmpty()) {
                Toast.makeText(requireContext(), "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            // 🔹 Si quieres volver al fragment de perfil:
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
            navController.navigate(R.id.fragment_perfil);
        });
    }
}