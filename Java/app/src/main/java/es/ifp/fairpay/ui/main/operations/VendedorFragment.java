package es.ifp.fairpay.ui.main.operations;

import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.web3j.abi.datatypes.Type;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import es.ifp.fairpay.R;
import es.ifp.fairpay.data.repository.EscrowRepository;
import es.ifp.fairpay.data.repository.RepositoryCallback;
import es.ifp.fairpay.data.repository.UserRepository;

/**
 * Fragmento que gestiona las operaciones del rol "Vendedor" en el sistema de depósito (Escrow).
 * Permite al vendedor verificar si un depósito ha sido financiado por el comprador
 * y ejecutar la liberación final de los fondos (segunda aprobación) una vez cumplidas las condiciones.
 */
public class VendedorFragment extends Fragment {

    // Elementos de la Interfaz de Usuario
    private EditText inputSellerPrivateKey, inputSellerEscrowId;
    private Button btnSellerCheckStatus, btnSellerReceivePayment, btnSellerBack;
    private TextView tvLog;

    // Dependencias y Contexto
    private Context mContext;
    private UserRepository userRepository;
    private EscrowRepository escrowRepository;

    // Constante para almacenamiento local de operaciones finalizadas
    private static final String PREF_COMPLETED_IDS = "COMPLETED_IDS";

    /**
     * Método del ciclo de vida ejecutado al adjuntar el fragmento a la actividad.
     * Inicializa los repositorios necesarios.
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
        userRepository = new UserRepository();
        escrowRepository = new EscrowRepository();
    }

    /**
     * Infla el diseño de la interfaz, vincula las vistas y configura el estado inicial.
     * Si se reciben argumentos (ej. desde operaciones pendientes), pre-carga los datos.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_vendedor, container, false);
        inputSellerPrivateKey = view.findViewById(R.id.input_seller_private_key);
        inputSellerEscrowId = view.findViewById(R.id.input_seller_escrow_id);
        btnSellerCheckStatus = view.findViewById(R.id.btn_seller_check_status);
        btnSellerReceivePayment = view.findViewById(R.id.btn_seller_receive);
        btnSellerBack = view.findViewById(R.id.btn_seller_back);
        tvLog = view.findViewById(R.id.tv_resultado_log);

        setupListeners();
        autoFillKeys();

        // Manejo de navegación desde "Operaciones Pendientes"
        if (getArguments() != null) {
            String pendingId = getArguments().getString("PENDING_ID");
            if (pendingId != null) {
                inputSellerEscrowId.setText(pendingId);
                tvLog.setText("Depósito Aprobado ID: " + pendingId + ". Libera los fondos.");
            }
        }
        return view;
    }

    /**
     * Configura los listeners de los botones para las acciones del vendedor.
     */
    private void setupListeners() {
        btnSellerBack.setOnClickListener(v -> {
            hideKeyboard();
            if (getParentFragment() instanceof OperacionesFragment) ((OperacionesFragment) getParentFragment()).resetUI();
        });
        btnSellerCheckStatus.setOnClickListener(v -> {
            hideKeyboard();
            String idStr = inputSellerEscrowId.getText().toString().trim();
            if (!idStr.isEmpty()) performCheck(inputSellerPrivateKey.getText().toString().trim(), new BigInteger(idStr));
        });
        btnSellerReceivePayment.setOnClickListener(v -> {
            hideKeyboard();
            String idStr = inputSellerEscrowId.getText().toString().trim();
            if (!idStr.isEmpty()) performReceive(inputSellerPrivateKey.getText().toString().trim(), new BigInteger(idStr));
        });
    }

    /**
     * Consulta el estado del depósito en la Blockchain para informar al vendedor.
     * Verifica si los fondos están depositados y cuántas aprobaciones tiene el contrato.
     *
     * @param pk Clave privada del vendedor.
     * @param id Identificador del depósito.
     */
    private void performCheck(String pk, BigInteger id) {
        tvLog.setText("Consultando...");
        btnSellerCheckStatus.setEnabled(false);

        escrowRepository.getEscrowDetails(pk, id, new RepositoryCallback<List<Type>>() {
            @Override
            public void onSuccess(List<Type> details) {
                btnSellerCheckStatus.setEnabled(true);
                boolean isFunded = (Boolean) details.get(3).getValue();
                BigInteger approvals = (BigInteger) details.get(4).getValue();
                if (isFunded && approvals.equals(BigInteger.ONE)) tvLog.setText("Listo para recibir pago.");
                else if (!isFunded) tvLog.setText("No pagado aún.");
                else tvLog.setText("Pagado, esperando aprobación.");
            }
            @Override
            public void onFailure(String error) {
                tvLog.setText("Error: " + error);
                btnSellerCheckStatus.setEnabled(true);
            }
        });
    }

    /**
     * Verifica si es posible reclamar el pago y, en caso afirmativo, inicia el proceso de aprobación final.
     * Se requiere que el contrato esté financiado y tenga ya una aprobación (del comprador).
     */
    private void performReceive(String pk, BigInteger id) {
        tvLog.setText("Reclamando...");
        btnSellerReceivePayment.setEnabled(false);

        escrowRepository.getEscrowDetails(pk, id, new RepositoryCallback<List<Type>>() {
            @Override
            public void onSuccess(List<Type> details) {
                boolean isFunded = (Boolean) details.get(3).getValue();
                BigInteger approvals = (BigInteger) details.get(4).getValue();
                if (isFunded && approvals.equals(BigInteger.ONE)) {
                    performApproveRelease(id, pk);
                } else {
                    tvLog.setText("No puedes reclamar aún.");
                    btnSellerReceivePayment.setEnabled(true);
                }
            }
            @Override
            public void onFailure(String error) {
                tvLog.setText("Error: " + error);
                btnSellerReceivePayment.setEnabled(true);
            }
        });
    }

    /**
     * Ejecuta la transacción de aprobación final (release) en la Blockchain.
     * Al completarse, marca la operación como finalizada localmente para que no aparezca como pendiente.
     */
    private void performApproveRelease(BigInteger id, String pk) {
        escrowRepository.approveRelease(pk, id, new RepositoryCallback<String>() {
            @Override
            public void onSuccess(String hash) {
                tvLog.setText("Aprobación enviada. Hash: " + hash);
                // Guardar localmente el ID como completado para evitar que el escáner lo vuelva a mostrar
                Set<String> ids = mContext.getSharedPreferences("FairPayState", Context.MODE_PRIVATE).getStringSet(PREF_COMPLETED_IDS, new HashSet<>());
                Set<String> newIds = new HashSet<>(ids);
                newIds.add(id.toString());
                mContext.getSharedPreferences("FairPayState", Context.MODE_PRIVATE).edit().putStringSet(PREF_COMPLETED_IDS, newIds).apply();

                new CountDownTimer(19000, 1000) {
                    public void onTick(long m) { btnSellerReceivePayment.setText("Procesando... " + m / 1000); }
                    public void onFinish() { btnSellerReceivePayment.setText("Recibir pago"); btnSellerReceivePayment.setEnabled(true); tvLog.setText("FINALIZADO. Depósito liberado."); }
                }.start();
            }
            @Override
            public void onFailure(String error) {
                tvLog.setText("Error: " + error);
            }
        });
    }

    /**
     * Rellena automáticamente el campo de clave privada si el usuario ya ha iniciado sesión previamente.
     */
    private void autoFillKeys() {
        String myKey = userRepository.getUsuarioClavePrivada(mContext);
        if (myKey != null && !myKey.isEmpty() && inputSellerPrivateKey != null)
            inputSellerPrivateKey.setText(myKey);
    }

    /**
     * Oculta el teclado virtual para mejorar la visibilidad de los logs y resultados.
     */
    private void hideKeyboard() {
        if (getActivity() != null) {
            View v = getActivity().getCurrentFocus();
            if (v != null) {
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        }
    }
}