package es.ifp.fairpay.ui.main.profile.help;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import java.util.ArrayList;

import es.ifp.fairpay.R;

public class HelpFragment extends Fragment {
    protected ListView list;
    protected ArrayList<String> listaArray = new ArrayList<String>();
    protected ArrayAdapter<String> adaptador;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ayuda, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //referencia de la listview
        list = (ListView) view.findViewById(R.id.listView_ayuda);

        //añadimos opciones de ayuda
        listaArray.add(getString(R.string.disputar_transaccion_ayuda));
        listaArray.add(getString(R.string.informar_problema_ayuda));
        listaArray.add(getString(R.string.preguntas_frecuentes_ayuda));

        adaptador = new ArrayAdapter<String>(requireContext(),android.R.layout.simple_list_item_1 , listaArray);
        list.setAdapter(adaptador);

        /**
         * metodo por el que al pulsar un item del listview nos manda
         * al fragment de ayuda correspondiente
         */
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //código que se ejecutará cuando el usuario haga clic en un ítem.
                NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);

                // 2. Decidir a dónde ir según la posición del clic
                switch (position) {
                    case 0:
                        // El usuario ha pulsado "Disputar transacción" (el primer elemento)
                        // Le decimos al NavController que ejecute la acción para ir al fragmento disputar transaccion
                        navController.navigate(R.id.fragment_DisputarTransaccion);
                        break;
                    case 1:
                        // El usuario ha pulsado "Informar de un problema" (el segundo elemento)
                        // Reemplaza 'action_fragmentAyuda_to_informarProblemaFragment' con el ID de tu acción.
                        navController.navigate(R.id.fragment_informarDeUnProblema);
                        break;
                    case 2:
                        // El usuario ha pulsado "Preguntas frecuentes" (el tercer elemento)
                        // Reemplaza 'action_fragmentAyuda_to_preguntasFrecuentesFragment' con el ID de tu acción.
                        navController.navigate(R.id.fragment_PreguntasFrecuentes);
                        break;}
              }
        });
        }
}
