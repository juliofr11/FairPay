package es.ifp.fairpay.ui.main.agenda;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;

import es.ifp.fairpay.R;

public class AgendaFragment extends Fragment {

    protected ListView lista;
    // Usamos dos listas paralelas: una para mostrar nombres y otra oculta para las wallets
    protected ArrayList<String> listaNombres = new ArrayList<String>();
    protected ArrayList<String> listaWallets = new ArrayList<String>();

    protected ArrayAdapter<String> adaptador;
    protected Button anadirContactos;

    public AgendaFragment() {
        // Constructor público vacío requerido por Android
    }

    // Esta función se encarga de inflar el diseño visual del fragmento para que se muestre en la pantalla
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_agenda, container, false);
    }

    // Esta función se encarga de inicializar la lógica de la lista de contactos y configurar la navegación al hacer clic en un elemento
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Vinculación de los elementos de la interfaz
        lista = view.findViewById(R.id.listView_contactos_agenda);
        anadirContactos = view.findViewById(R.id.boton_anadirContacto_agenda);
        NavController navController = Navigation.findNavController(view);

        // Limpieza e inicialización de datos de prueba (Hardcoded)
        listaNombres.clear();
        listaWallets.clear();

        listaNombres.add("Juan Antonio Garcia");
        listaWallets.add("0x7f0215e059183f9720525ffe4c233101b3d93ba5");

        listaNombres.add("Manuel Montes Díaz");
        listaWallets.add("0x7f0215e059183f9720525ffe4c233101b3d93ba5");

        listaNombres.add("David Gonzalez Fernández");
        listaWallets.add("0x43b05f553ddeff68eec47248c9b06ed48b100b97");

        listaNombres.add("Laura Martín");
        listaWallets.add("0x0000000000000000000000000000000000000000");

        // Configuración del adaptador para mostrar visualmente solo los nombres en la lista
        adaptador = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, listaNombres);
        lista.setAdapter(adaptador);

        // Listener para el botón de añadir nuevo contacto
        anadirContactos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navController.navigate(R.id.action_agendaFragment_to_anadirContactoFragment);
            }
        });

        // Listener para detectar la selección de un contacto y navegar a su ficha detallada
        lista.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // Recuperamos los datos sincronizados de las listas paralelas usando la posición clicada
                String nombreSeleccionado = listaNombres.get(position);
                String walletSeleccionada = listaWallets.get(position);

                // Preparamos el paquete de datos para enviarlo al siguiente fragmento (ContactoFragment)
                Bundle bundle = new Bundle();

                // Datos visuales para la ficha
                bundle.putString("CONTACT_NAME", nombreSeleccionado);
                bundle.putString("CONTACT_WALLET", walletSeleccionada);

                // Dato funcional para operaciones futuras (botón enviar dinero)
                bundle.putString("WALLET_VENDEDOR", walletSeleccionada);

                // Ejecutamos la navegación pasando los datos
                navController.navigate(R.id.contactoFragment, bundle);
            }
        });
    }
}