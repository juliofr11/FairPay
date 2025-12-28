package es.ifp.fairpay.ui.main.operations;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.web3j.utils.Convert;
import org.web3j.abi.datatypes.Type;

import java.math.BigInteger;
import java.util.List;

import es.ifp.fairpay.R;
import es.ifp.fairpay.data.repository.EscrowRepository;
import es.ifp.fairpay.data.repository.OperationsRepository;
import es.ifp.fairpay.data.repository.RepositoryCallback;
import es.ifp.fairpay.data.repository.UserRepository;
import es.ifp.fairpay.data.security.FingerprintCheck;

/**
 * Fragmento que gestiona las operaciones del rol "Comprador" en el sistema de depósito (Escrow).
 * Permite crear nuevos contratos, depositar fondos (Ether), aprobar la liberación de pagos
 * y consultar el estado de operaciones existentes.
 */
public class CompradorFragment extends Fragment {

    // Elementos de la Interfaz de Usuario
    private LinearLayout layoutBuyerMenu, layoutFormsContainer, layoutCreateEscrow, layoutFundEscrow, layoutBuyerApprove, layoutExistingEscrowBuyer;
    private Button btnBuyerNew, btnBuyerExisting, btnBuyerBack;
    private EditText inputPrivateKey, inputSeller, inputEscrowId, inputPrivateKeyFund, inputFundAmount, inputExistingEscrowId;
    private Button btnEnviarEscrow, btnSearchId, btnFormBackBuyer, btnFundEscrow, btnBuyerApprove, btnCheckEscrowStatus, btnExistingBackBuyer;
    private TextView tvLog;

    // Variables lógicas y de contexto
    private Context mContext;
    private BigInteger lastEscrowId = null;
    private String lastTxHash = null;

    // Repositorios de datos
    private UserRepository userRepository;
    private OperationsRepository operationsRepository;
    private EscrowRepository escrowRepository;
    private String currentUserEmail = "";

    // Constantes para la máquina de estados de la operación
    private static final String STATE_NONE = "NONE";
    private static final String STATE_WAITING_ID = "WAITING_ID";
    private static final String STATE_WAITING_FUND = "WAITING_FUND";
    private static final String STATE_WAITING_APPROVE = "WAITING_APPROVE";
    private static final String STATE_WAITING_VERIFY = "WAITING_VERIFY";

    // Claves para persistencia en SharedPreferences
    private static final String PREF_OP_STATE = "OP_STATE";
    private static final String PREF_OP_HASH = "OP_HASH";
    private static final String PREF_OP_ID = "OP_ID";
    private static final String PREF_OP_LOG = "OP_LOG";
    private static final String PREF_OP_USER = "OP_USER";

    /**
     * Método del ciclo de vida del fragmento que se ejecuta al adjuntarse a la actividad.
     * Se utiliza para inicializar el contexto y las instancias de los repositorios de datos
     * necesarios para las operaciones (User, Operations, Escrow).
     *
     * @param context Contexto de la actividad anfitriona.
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
        userRepository = new UserRepository();
        operationsRepository = new OperationsRepository();
        escrowRepository = new EscrowRepository();
    }

    /**
     * Crea y devuelve la jerarquía de vistas asociada al fragmento.
     * Aquí se infla el layout, se configuran los listeners, se restaura el estado previo
     * si existe una operación en curso y se manejan los argumentos de navegación
     * (por ejemplo, venir desde "Operaciones Pendientes" o desde la Agenda).
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_comprador, container, false);
        setupViews(view);

        currentUserEmail = userRepository.getUsuarioEmail(mContext);
        restoreOperationState();
        autoFillKeys();

        // Manejo de argumentos para navegación directa desde otras pantallas (ej. Operaciones Pendientes)
        if (getArguments() != null) {
            String pendingId = getArguments().getString("PENDING_ID");
            int type = getArguments().getInt("PENDING_TYPE");
            if (pendingId != null) {
                clearOperationState();
                handlePendingStart(pendingId, type);
            }
            String walletVendedor = getArguments().getString("WALLET_VENDEDOR");
            if (walletVendedor != null && !walletVendedor.isEmpty()) {
                clearOperationState();
                handleAgendaStart(walletVendedor);
            }
        }
        return view;
    }

    /**
     * Inicializa y vincula las vistas del layout XML.
     */
    private void setupViews(View view) {
        layoutBuyerMenu = view.findViewById(R.id.layout_buyer_menu);
        layoutFormsContainer = view.findViewById(R.id.layout_forms_container);
        layoutCreateEscrow = view.findViewById(R.id.layout_create_escrow);
        layoutFundEscrow = view.findViewById(R.id.layout_fund_escrow);
        layoutBuyerApprove = view.findViewById(R.id.layout_buyer_approve);
        layoutExistingEscrowBuyer = view.findViewById(R.id.layout_existing_escrow_buyer);
        tvLog = view.findViewById(R.id.tv_resultado_log);
        btnBuyerNew = view.findViewById(R.id.btn_buyer_new);
        btnBuyerExisting = view.findViewById(R.id.btn_buyer_existing);
        btnBuyerBack = view.findViewById(R.id.btn_buyer_back);
        inputPrivateKey = view.findViewById(R.id.input_private_key);
        inputSeller = view.findViewById(R.id.input_seller);
        inputEscrowId = view.findViewById(R.id.input_escrow_id);
        inputPrivateKeyFund = view.findViewById(R.id.input_private_key_fund);
        inputFundAmount = view.findViewById(R.id.input_fund_amount);
        inputExistingEscrowId = view.findViewById(R.id.input_existing_escrow_id);
        btnEnviarEscrow = view.findViewById(R.id.button_enviar_create_escrow);
        btnSearchId = view.findViewById(R.id.button_search_id);
        btnFormBackBuyer = view.findViewById(R.id.btn_form_back_buyer);
        btnFundEscrow = view.findViewById(R.id.button_fund_escrow);
        btnBuyerApprove = view.findViewById(R.id.button_buyer_approve);
        btnCheckEscrowStatus = view.findViewById(R.id.btn_check_escrow_status);
        btnExistingBackBuyer = view.findViewById(R.id.btn_existing_back_buyer);
        setupListeners();
    }

    /**
     * Configura los listeners de eventos para botones y acciones de la interfaz.
     */
    private void setupListeners() {
        btnBuyerNew.setOnClickListener(v -> {
            hideKeyboard();
            clearOperationState();
            showView(layoutFormsContainer);
            layoutCreateEscrow.setVisibility(View.VISIBLE);
            tvLog.setText("PASO 1: Comprador inicia el depósito.");
        });
        btnBuyerExisting.setOnClickListener(v -> {
            hideKeyboard();
            showView(layoutExistingEscrowBuyer);
        });
        btnBuyerBack.setOnClickListener(v -> {
            hideKeyboard();
            if (getParentFragment() instanceof OperacionesFragment) {
                ((OperacionesFragment) getParentFragment()).resetUI();
            }
        });
        btnFormBackBuyer.setOnClickListener(v -> {
            hideKeyboard();
            showView(layoutBuyerMenu);
        });
        btnExistingBackBuyer.setOnClickListener(v -> {
            hideKeyboard();
            showView(layoutBuyerMenu);
        });

        btnEnviarEscrow.setOnClickListener(v -> {
            hideKeyboard();
            String pk = inputPrivateKey.getText().toString().trim();
            String seller = inputSeller.getText().toString().trim();

            if (pk.isEmpty() || seller.isEmpty()) {
                Toast.makeText(mContext, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            FingerprintCheck fingerprintCheck = new FingerprintCheck();
            fingerprintCheck.showFingerprintDialog(this, () -> {
                Toast.makeText(mContext, "Huella verificada. Procesando envío...", Toast.LENGTH_SHORT).show();

                userRepository.setUsuarioClavePrivada(mContext, pk);

                performCreateEscrow(pk, seller);
            });
        });
        btnSearchId.setOnClickListener(v -> {
            hideKeyboard();
            if (lastTxHash != null) performSearchEscrowId(lastTxHash);
        });
        btnFundEscrow.setOnClickListener(v -> {
            hideKeyboard();
            String pk = inputPrivateKeyFund.getText().toString().trim();
            String amount = inputFundAmount.getText().toString().trim();
            if (!pk.isEmpty() && lastEscrowId != null) {
                try {
                    performFundEscrow(pk, lastEscrowId, Convert.toWei(amount, Convert.Unit.ETHER).toBigInteger());
                } catch (Exception e) {
                    Toast.makeText(mContext, "Monto inválido", Toast.LENGTH_SHORT).show();
                }
            }
        });
        btnBuyerApprove.setOnClickListener(v -> {
            hideKeyboard();
            String pk = inputPrivateKey.getText().toString().trim();
            if (pk.isEmpty()) pk = inputPrivateKeyFund.getText().toString().trim();
            if (btnBuyerApprove.getText().toString().equals("Aprobar recepción")) {
                if (!pk.isEmpty() && lastEscrowId != null) performApprove(lastEscrowId, pk);
            } else {
                performVerifyCompletion(pk, lastEscrowId);
            }
        });
        btnCheckEscrowStatus.setOnClickListener(v -> {
            hideKeyboard();
            String id = inputExistingEscrowId.getText().toString().trim();
            if (!id.isEmpty()) performCheckStatus(inputPrivateKey.getText().toString(), id);
        });
    }

    /**
     * Persiste el estado actual de la operación en curso delegando en el repositorio.
     * Esto asegura que los datos críticos (Hash, ID) no se pierdan si la app se cierra.
     *
     * @param state Estado actual del flujo (ej. STATE_WAITING_ID).
     * @param hash  Hash de la transacción Ethereum generada.
     * @param id    Identificador del depósito (si ya existe).
     * @param log   Mensaje de registro para mostrar al usuario.
     */
    private void saveState(String state, String hash, String id, String log) {
        operationsRepository.saveOperationState(mContext, state, currentUserEmail, hash, id, log);
    }

    /**
     * Limpia cualquier estado de operación almacenado y restablece las variables locales.
     * Se llama al completar exitosamente una operación o al iniciar una nueva para evitar conflictos.
     */
    private void clearOperationState() {
        operationsRepository.clearOperationState(mContext);
        lastTxHash = null;
        lastEscrowId = null;
    }

    /**
     * Restaura el estado de la interfaz si el usuario salió de la pantalla mientras
     * se realizaba una operación en segundo plano o se esperaba una confirmación.
     */
    private void restoreOperationState() {
        SharedPreferences prefs = operationsRepository.restoreOperationState(mContext);
        if (prefs == null) return;
        String savedUser = prefs.getString(PREF_OP_USER, "");
        if (!savedUser.equals(currentUserEmail)) {
            clearOperationState();
            return;
        }
        String state = prefs.getString(PREF_OP_STATE, STATE_NONE);
        if (state.equals(STATE_NONE)) return;

        String savedHash = prefs.getString(PREF_OP_HASH, null);
        String savedId = prefs.getString(PREF_OP_ID, null);
        String savedLog = prefs.getString(PREF_OP_LOG, "");
        lastTxHash = savedHash;
        if (savedId != null) lastEscrowId = new BigInteger(savedId);

        showView(layoutFormsContainer);
        tvLog.setText(savedLog);
        switch (state) {
            case STATE_WAITING_ID:
                layoutCreateEscrow.setVisibility(View.VISIBLE);
                btnSearchId.setVisibility(View.VISIBLE);
                btnSearchId.setEnabled(true);
                btnSearchId.setText("BUSCAR ID CONFIRMADO");
                break;
            case STATE_WAITING_FUND:
                layoutFundEscrow.setVisibility(View.VISIBLE);
                if (savedId != null) inputEscrowId.setText(savedId);
                btnFundEscrow.setVisibility(View.VISIBLE);
                btnFundEscrow.setEnabled(true);
                break;
            case STATE_WAITING_APPROVE:
                layoutBuyerApprove.setVisibility(View.VISIBLE);
                btnBuyerApprove.setText("Aprobar recepción");
                break;
            case STATE_WAITING_VERIFY:
                layoutBuyerApprove.setVisibility(View.VISIBLE);
                btnBuyerApprove.setText("Comprobar");
                break;
        }
    }

    /**
     * Configura la interfaz cuando se accede desde una "Operación Pendiente" detectada en la Blockchain.
     * Salta directamente al paso necesario (Depósito o Aprobación) según el tipo de operación.
     *
     * @param id   Identificador del depósito pendiente.
     * @param type Tipo de estado pendiente: 1 para realizar pago, 2 para confirmar recepción.
     */
    private void handlePendingStart(String id, int type) {
        showView(layoutFormsContainer);
        layoutBuyerMenu.setVisibility(View.GONE);
        lastEscrowId = new BigInteger(id);
        inputEscrowId.setText(id);
        if (type == 1) {
            layoutFundEscrow.setVisibility(View.VISIBLE);
            tvLog.setText("Depósito Creado ID: " + id + ". Haz el pago.");
        } else if (type == 2) {
            layoutBuyerApprove.setVisibility(View.VISIBLE);
            btnBuyerApprove.setText("Aprobar recepción");
            tvLog.setText("Depósito Pagado ID: " + id + ". Confirma la recepción.");
        }
    }

    /**
     * Configura la interfaz cuando se accede desde la "Agenda" para pagar a un contacto.
     * Pre-carga la dirección de wallet del vendedor y lleva al usuario al primer paso de creación de depósito.
     *
     * @param walletVendedor Dirección de wallet del contacto seleccionado.
     */
    private void handleAgendaStart(String walletVendedor) {
        showView(layoutFormsContainer);
        layoutCreateEscrow.setVisibility(View.VISIBLE);
        if (inputSeller != null) inputSeller.setText(walletVendedor);
        if (tvLog != null) tvLog.setText("INICIAR PAGO A CONTACTO:\nWallet vendedor cargada correctamente.\nPASO 1: Comprador inicia el depósito.");
    }

    /**
     * Gestiona la visibilidad de los diferentes contenedores (vistas) del fragmento.
     * Actúa como un controlador de navegación interno, ocultando todo y mostrando solo la sección solicitada.
     *
     * @param view La vista (Layout) específica que se desea mostrar.
     */
    private void showView(View view) {
        layoutBuyerMenu.setVisibility(View.GONE);
        layoutFormsContainer.setVisibility(View.GONE);
        layoutExistingEscrowBuyer.setVisibility(View.GONE);
        layoutCreateEscrow.setVisibility(View.GONE);
        layoutFundEscrow.setVisibility(View.GONE);
        layoutBuyerApprove.setVisibility(View.GONE);
        if (view != null) view.setVisibility(View.VISIBLE);
    }

    /**
     * Inicia la creación de un nuevo contrato en la Blockchain a través del repositorio.
     */
    private void performCreateEscrow(String pk, String seller) {
        tvLog.setText("Creando...");
        btnEnviarEscrow.setEnabled(false);

        escrowRepository.createEscrow(pk, seller, new RepositoryCallback<String>() {
            @Override
            public void onSuccess(String hash) {
                lastTxHash = hash;
                String msg = "Hash: " + hash + "\nEsperando...";
                tvLog.setText(msg);
                startCountdownTimer();
                btnEnviarEscrow.setEnabled(true);
                saveState(STATE_WAITING_ID, hash, null, msg);
            }

            @Override
            public void onFailure(String error) {
                tvLog.setText("Error: " + error);
                btnEnviarEscrow.setEnabled(true);
            }
        });
    }

    /**
     * Inicia un temporizador visual de 15 segundos.
     * Este tiempo de espera es necesario para dar margen a que la transacción enviada
     * sea minada y confirmada en la Blockchain antes de intentar consultar su ID.
     */
    private void startCountdownTimer() {
        btnSearchId.setVisibility(View.VISIBLE);
        btnSearchId.setEnabled(false);
        new CountDownTimer(15000, 1000) {
            public void onTick(long m) {
                btnSearchId.setText("Procesando... " + m / 1000);
            }

            public void onFinish() {
                btnSearchId.setText("BUSCAR ID CONFIRMADO");
                btnSearchId.setEnabled(true);
            }
        }.start();
    }

    /**
     * Consulta el ID del contrato recién creado utilizando el hash de la transacción.
     */
    private void performSearchEscrowId(String hash) {
        tvLog.setText("Buscando ID...");
        btnSearchId.setEnabled(false);
        String pk = inputPrivateKey.getText().toString();

        escrowRepository.waitForEscrowId(pk, hash, new RepositoryCallback<BigInteger>() {
            @Override
            public void onSuccess(BigInteger id) {
                lastEscrowId = id;
                String msg = "ID encontrado: " + id;
                tvLog.setText(msg);
                inputEscrowId.setText(id.toString());
                layoutCreateEscrow.setVisibility(View.GONE);
                layoutFundEscrow.setVisibility(View.VISIBLE);
                btnSearchId.setVisibility(View.GONE);
                autoFillKeys();
                saveState(STATE_WAITING_FUND, hash, id.toString(), msg);
            }

            @Override
            public void onFailure(String error) {
                tvLog.setText("Error: " + error);
                btnSearchId.setEnabled(true);
            }
        });
    }

    /**
     * Realiza la transferencia de fondos al contrato inteligente.
     */
    private void performFundEscrow(String pk, BigInteger id, BigInteger amountWei) {
        tvLog.setText("Enviando pago...");
        btnFundEscrow.setEnabled(false);

        escrowRepository.fundEscrow(pk, id, amountWei, new RepositoryCallback<String>() {
            @Override
            public void onSuccess(String hash) {
                tvLog.setText("Pago enviado. Confirmando...");
                new CountDownTimer(19000, 1000) {
                    public void onTick(long m) {
                        btnFundEscrow.setText("Procesando... " + m / 1000);
                    }

                    public void onFinish() {
                        String msg = "Pago confirmado. Pendiente aprobar.";
                        tvLog.setText(msg);
                        btnFundEscrow.setEnabled(true);
                        layoutFundEscrow.setVisibility(View.GONE);
                        layoutBuyerApprove.setVisibility(View.VISIBLE);
                        btnBuyerApprove.setText("Aprobar recepción");
                        saveState(STATE_WAITING_APPROVE, hash, id.toString(), msg);
                    }
                }.start();
            }

            @Override
            public void onFailure(String error) {
                tvLog.setText("Error: " + error);
                btnFundEscrow.setEnabled(true);
            }
        });
    }

    /**
     * Aprueba la liberación de fondos al vendedor.
     */
    private void performApprove(BigInteger id, String pk) {
        tvLog.setText("Aprobando...");
        btnBuyerApprove.setEnabled(false);
        escrowRepository.approveRelease(pk, id, new RepositoryCallback<String>() {
            @Override
            public void onSuccess(String hash) {
                String msg = "Aprobación enviada. Hash: " + hash;
                tvLog.setText(msg);
                saveState(STATE_WAITING_VERIFY, hash, id.toString(), msg);
                new CountDownTimer(19000, 1000) {
                    public void onTick(long m) {
                        btnBuyerApprove.setText("Procesando... " + m / 1000);
                    }

                    public void onFinish() {
                        btnBuyerApprove.setText("Comprobar");
                        btnBuyerApprove.setEnabled(true);
                    }
                }.start();
            }

            @Override
            public void onFailure(String error) {
                tvLog.setText("Error: " + error);
                btnBuyerApprove.setEnabled(true);
            }
        });
    }

    /**
     * Verifica en la Blockchain si la operación ha concluido exitosamente.
     */
    private void performVerifyCompletion(String pk, BigInteger id) {
        tvLog.setText("Verificando...");
        btnBuyerApprove.setEnabled(false);

        escrowRepository.getEscrowDetails(pk, id, new RepositoryCallback<List<Type>>() {
            @Override
            public void onSuccess(List<Type> details) {
                btnBuyerApprove.setEnabled(true);
                BigInteger approvals = (BigInteger) details.get(4).getValue();
                if (approvals.compareTo(BigInteger.ZERO) > 0) {
                    showView(null);
                    tvLog.setVisibility(View.VISIBLE);
                    tvLog.setText("FINALIZADO: Depósito completado.");
                    clearOperationState();
                } else {
                    tvLog.setText("Aún no confirmado.");
                }
            }

            @Override
            public void onFailure(String error) {
                tvLog.setText("Error: " + error);
                btnBuyerApprove.setEnabled(true);
            }
        });
    }

    /**
     * Consulta el estado de un depósito existente para determinar qué acción es requerida.
     */
    private void performCheckStatus(String pk, String idStr) {
        BigInteger id = new BigInteger(idStr);
        lastEscrowId = id;
        escrowRepository.getEscrowDetails(pk, id, new RepositoryCallback<List<Type>>() {
            @Override
            public void onSuccess(List<Type> details) {
                BigInteger amount = (BigInteger) details.get(2).getValue();
                boolean isFunded = (Boolean) details.get(3).getValue();
                BigInteger approvals = (BigInteger) details.get(4).getValue();

                if (isFunded && approvals.compareTo(BigInteger.ZERO) > 0) {
                    showView(null);
                    tvLog.setText("Estado: Finalizado.");
                    return;
                }
                showView(layoutFormsContainer);
                if (!isFunded && amount.equals(BigInteger.ZERO)) {
                    tvLog.setText(Html.fromHtml("Estado: Creado pero no pagado. Haz el pago."));
                    layoutFundEscrow.setVisibility(View.VISIBLE);
                    inputEscrowId.setText(idStr);
                } else if (isFunded && approvals.equals(BigInteger.ZERO)) {
                    tvLog.setText(Html.fromHtml("Estado: Pagado. Aprueba recepción."));
                    layoutBuyerApprove.setVisibility(View.VISIBLE);
                    btnBuyerApprove.setText("Aprobar recepción");
                } else {
                    tvLog.setText("Estado desconocido.");
                    showView(layoutBuyerMenu);
                }
            }

            @Override
            public void onFailure(String error) {
                tvLog.setText("Error: " + error);
            }
        });
    }

    /**
     * Rellena automáticamente los campos de clave privada si existe una guardada en el dispositivo.
     * Facilita la experiencia de usuario evitando que tenga que copiar y pegar su clave en cada paso.
     */
    private void autoFillKeys() {
        String myKey = userRepository.getUsuarioClavePrivada(mContext);
        if (myKey != null && !myKey.isEmpty()) {
            if (inputPrivateKey != null) inputPrivateKey.setText(myKey);
            if (inputPrivateKeyFund != null) inputPrivateKeyFund.setText(myKey);
        }
    }

    /**
     * Oculta el teclado virtual del dispositivo.
     * Se utiliza tras pulsar botones de acción para despejar la pantalla y mostrar los resultados o logs.
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