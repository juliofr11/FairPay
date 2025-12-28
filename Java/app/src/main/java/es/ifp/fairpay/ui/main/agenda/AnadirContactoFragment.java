package es.ifp.fairpay.ui.main.agenda;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import es.ifp.fairpay.R;
import es.ifp.fairpay.ui.main.profile.PerfilFragment;


/**
 * Fragment responsible for displaying the user interface to add a new contact.
 * This class handles the logic for creating the view where users can input
 * the details of a new contact to add to their agenda.
 *
 * <p>It follows the standard Android Fragment lifecycle. The view is inflated from
 * the {@code fragment_anadir_contacto} layout file.</p>
 *
 * <p>This fragment is part of the agenda feature within the application.</p>
 *
 * @see Fragment
 */
public class AnadirContactoFragment extends Fragment {

    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    public AnadirContactoFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment AnadirContactoFragment.
     */
    public static AnadirContactoFragment newInstance(String param1, String param2) {
        AnadirContactoFragment fragment = new AnadirContactoFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_anadir_contacto, container, false);
    }

    // Cuando la vista ya está creada
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }
}