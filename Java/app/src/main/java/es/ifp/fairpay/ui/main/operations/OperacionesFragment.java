package es.ifp.fairpay.ui.main.operations;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
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
 * Fragmento principal que actúa como el panel de control ("Dashboard") para las operaciones transaccionales.
 * Desde aquí, el usuario puede navegar a los flujos de Comprador, Vendedor y Disputas.
 * También incluye la funcionalidad de escaneo de la Blockchain para detectar operaciones pendientes
 * que requieren acción por parte del usuario.
 */
public class OperacionesFragment extends Fragment {

    // Elementos de la Interfaz de Usuario
    private Button btnRealizarOperacion, btnPendingOps, btnMainDisputes;
    private LinearLayout layoutMainMenu, layoutPendingSubcategories, layoutPendingContainer;
    private Button btnSelectBuyer, btnSelectSeller, btnCatCreated, btnCatFunded, btnCatApproved;
    private ScrollView scrollPendingResults;
    private TextView tvPendingStatus, tvResultadoLog;
    private CardView cardMainLog;

    // Dependencias y Contexto
    private Context mContext;
    private UserRepository userRepository;
    private EscrowRepository escrowRepository;

    /**
     * Método del ciclo de vida ejecutado al adjuntar el fragmento a la actividad.
     * Inicializa los repositorios necesarios para gestionar usuarios y contratos Escrow.
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
        userRepository = new UserRepository();
        escrowRepository = new EscrowRepository();
    }

    /**
     * Infla el diseño de la interfaz y configura las vistas y escuchadores iniciales.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_operaciones, container, false);
        setupViews(view);
        setupListeners();
        return view;
    }

    /**
     * Se ejecuta después de crear la vista. Gestiona la navegación profunda (Deep Linking)
     * interna de la aplicación, por ejemplo, cuando se llega desde la Agenda para pagar a un contacto.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Verificar si existen argumentos para iniciar una operación directa (ej. Pago a contacto)
        if (getArguments() != null) {
            String walletVendedor = getArguments().getString("WALLET_VENDEDOR");
            if (walletVendedor != null && !walletVendedor.isEmpty()) {
                hideAllSections();
                btnRealizarOperacion.setVisibility(View.VISIBLE);
                btnRealizarOperacion.setText("Cerrar operaciones");
                Bundle args = new Bundle();
                args.putString("WALLET_VENDEDOR", walletVendedor);
                CompradorFragment fragment = new CompradorFragment();
                fragment.setArguments(args);
                loadFragment(fragment, "CompradorDirect");
            }
        }
    }

    /**
     * Vincula los componentes de la UI con el código.
     * Ajusta programáticamente el tamaño del ScrollView de resultados pendientes para una mejor UX.
     */
    private void setupViews(View view) {
        btnRealizarOperacion = view.findViewById(R.id.button_realizar_operacion);
        btnPendingOps = view.findViewById(R.id.btn_pending_ops);
        btnMainDisputes = view.findViewById(R.id.btn_main_disputes);
        layoutMainMenu = view.findViewById(R.id.layout_main_menu);
        btnSelectBuyer = view.findViewById(R.id.btn_select_buyer);
        btnSelectSeller = view.findViewById(R.id.btn_select_seller);
        layoutPendingSubcategories = view.findViewById(R.id.layout_pending_subcategories);
        scrollPendingResults = view.findViewById(R.id.scroll_pending_results);
        layoutPendingContainer = view.findViewById(R.id.layout_pending_container);
        tvPendingStatus = view.findViewById(R.id.tv_pending_status);
        btnCatCreated = view.findViewById(R.id.btn_cat_created);
        btnCatFunded = view.findViewById(R.id.btn_cat_funded);
        btnCatApproved = view.findViewById(R.id.btn_cat_approved);
        cardMainLog = view.findViewById(R.id.card_result_log);
        tvResultadoLog = view.findViewById(R.id.tv_resultado_log);

        // Configura una altura fija para el área de resultados pendientes (aprox. 8 elementos)
        if (scrollPendingResults != null) {
            ViewGroup.LayoutParams params = scrollPendingResults.getLayoutParams();
            if (params != null) {
                // 400dp permiten visualizar una lista cómoda sin ocupar toda la pantalla
                params.height = (int) (400 * getResources().getDisplayMetrics().density);
                scrollPendingResults.setLayoutParams(params);
            }
        }
    }

    /**
     * Define la lógica de navegación y las acciones de los botones principales.
     */
    private void setupListeners() {
        // Botón principal "Realizar Operación" (Toggle para mostrar/ocultar menú de roles)
        btnRealizarOperacion.setOnClickListener(v -> {
            hideKeyboard();
            if (btnRealizarOperacion.getText().toString().startsWith("Cerrar")) resetUI();
            else {
                hideAllSections();
                btnRealizarOperacion.setVisibility(View.VISIBLE);
                btnRealizarOperacion.setText("Cerrar operaciones");
                layoutMainMenu.setVisibility(View.VISIBLE);
            }
        });

        // Navegación a Fragmentos específicos
        btnSelectBuyer.setOnClickListener(v -> loadFragment(new CompradorFragment(), "Comprador"));
        btnSelectSeller.setOnClickListener(v -> loadFragment(new VendedorFragment(), "Vendedor"));

        // Botón "Operaciones Pendientes" (Toggle para mostrar filtros de búsqueda)
        btnPendingOps.setOnClickListener(v -> {
            hideKeyboard();
            if (btnPendingOps.getText().toString().startsWith("Cerrar")) resetUI();
            else {
                hideAllSections();
                btnPendingOps.setVisibility(View.VISIBLE);
                btnPendingOps.setText("Cerrar operaciones pendientes");
                layoutPendingSubcategories.setVisibility(View.VISIBLE);
                closeChildFragment();
            }
        });

        // Filtros de operaciones pendientes: Creadas, Pagadas, Aprobadas
        btnCatCreated.setOnClickListener(v -> { scrollPendingResults.setVisibility(View.VISIBLE); performScanAndPopulate(true, 1); });
        btnCatFunded.setOnClickListener(v -> { scrollPendingResults.setVisibility(View.VISIBLE); performScanAndPopulate(true, 2); });
        btnCatApproved.setOnClickListener(v -> { scrollPendingResults.setVisibility(View.VISIBLE); performScanAndPopulate(true, 3); });

        // Botón "Disputas"
        btnMainDisputes.setOnClickListener(v -> {
            hideKeyboard();
            if (btnMainDisputes.getText().toString().startsWith("Cerrar")) resetUI();
            else {
                hideAllSections();
                btnMainDisputes.setVisibility(View.VISIBLE);
                btnMainDisputes.setText("Cerrar disputas");
                loadFragment(new DisputasFragment(), "Disputas");
            }
        });
    }

    /**
     * Inicia el escaneo de la Blockchain a través del repositorio para encontrar operaciones
     * que coincidan con los criterios del usuario (pendientes y por tipo de estado).
     *
     * @param isPending  Debe ser true para buscar solo operaciones no finalizadas.
     * @param filterType Filtro específico: 1 (Creado/Impago), 2 (Pagado/Esperando envío), 3 (Enviado/Esperando aprobación).
     */
    private void performScanAndPopulate(boolean isPending, int filterType) {
        layoutPendingContainer.removeAllViews();
        layoutPendingContainer.addView(tvPendingStatus);
        tvPendingStatus.setText("Iniciando escaneo...");
        tvPendingStatus.setVisibility(View.VISIBLE);

        String pk = userRepository.getUsuarioClavePrivada(mContext);
        // Recuperamos IDs ya completados localmente para optimizar la visualización
        Set<String> completedIds = mContext.getSharedPreferences("FairPayState", Context.MODE_PRIVATE).getStringSet("COMPLETED_IDS", new HashSet<>());

        escrowRepository.scanPendingOperations(pk, new RepositoryCallback<EscrowRepository.PendingOpResult>() {
            @Override
            public void onProgress(String status) {
                if (tvPendingStatus != null) tvPendingStatus.setText(status);
            }

            @Override
            public void onSuccess(EscrowRepository.PendingOpResult result) {
                if (result == null) {
                    // Fin del escaneo: si no hay vistas extra en el contenedor, no se encontró nada.
                    if (layoutPendingContainer.getChildCount() <= 1) tvPendingStatus.setText("No se encontraron operaciones.");
                    else tvPendingStatus.setVisibility(View.GONE);
                    return;
                }

                // Procesar cada resultado parcial encontrado en tiempo real
                processScanResult(result, isPending, filterType, completedIds);
            }

            @Override
            public void onFailure(String error) {
                tvPendingStatus.setText("Error: " + error);
            }
        });
    }

    /**
     * Analiza un resultado individual del escaneo y determina si debe mostrarse al usuario
     * basándose en su rol (Comprador/Vendedor) y el estado del contrato.
     */
    private void processScanResult(EscrowRepository.PendingOpResult res, boolean isPending, int filterType, Set<String> completedIds) {
        BigInteger id = res.id;
        String buyer = res.details.get(0).getValue().toString();
        String seller = res.details.get(1).getValue().toString();
        BigInteger amount = (BigInteger) res.details.get(2).getValue();
        boolean isFunded = (Boolean) res.details.get(3).getValue();
        BigInteger approvals = (BigInteger) res.details.get(4).getValue();

        boolean amIBuyer = buyer.equalsIgnoreCase(res.myAddress);
        boolean amISeller = seller.equalsIgnoreCase(res.myAddress);

        // Filtrado lógico según el tipo de botón presionado
        if ((amIBuyer || amISeller) && isPending && !completedIds.contains(id.toString())) {
            if (filterType == 1 && amIBuyer && amount.equals(BigInteger.ZERO) && !isFunded && approvals.equals(BigInteger.ZERO)) {
                addPendingButton(layoutPendingContainer, "Depósito CREADO ID: " + id, id.toString(), 1);
            } else if (filterType == 2 && amIBuyer && amount.compareTo(BigInteger.ZERO) > 0 && isFunded && approvals.equals(BigInteger.ZERO)) {
                addPendingButton(layoutPendingContainer, "Depósito PAGADO ID: " + id, id.toString(), 2);
            } else if (filterType == 3 && amISeller && amount.compareTo(BigInteger.ZERO) > 0 && isFunded && approvals.equals(BigInteger.ONE)) {
                addPendingButton(layoutPendingContainer, "Depósito APROBADO (1/2) ID: " + id, id.toString(), 3);
            }
        }
    }

    /**
     * Carga y muestra un fragmento hijo (Comprador, Vendedor, Disputas) en el contenedor principal.
     * Oculta el menú principal para dar foco a la nueva pantalla.
     *
     * @param fragment La instancia del fragmento a mostrar.
     * @param tag      Etiqueta identificativa para la transacción del fragmento.
     */
    private void loadFragment(Fragment fragment, String tag) {
        layoutMainMenu.setVisibility(View.GONE);
        getChildFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment, tag).commit();
    }

    /**
     * Cierra (elimina) el fragmento hijo actualmente activo en el contenedor.
     * Se utiliza al volver al menú principal o al resetear la interfaz.
     */
    private void closeChildFragment() {
        Fragment c = getChildFragmentManager().findFragmentById(R.id.fragment_container);
        if (c != null) getChildFragmentManager().beginTransaction().remove(c).commit();
    }

    /**
     * Restablece la interfaz al estado inicial, cerrando fragmentos hijos y mostrando el menú principal.
     */
    public void resetUI() {
        hideKeyboard();
        closeChildFragment();
        btnRealizarOperacion.setVisibility(View.VISIBLE);
        btnRealizarOperacion.setText("Iniciar operaciones");
        btnPendingOps.setVisibility(View.VISIBLE);
        btnPendingOps.setText("Operaciones pendientes");
        btnMainDisputes.setVisibility(View.VISIBLE);
        btnMainDisputes.setText("Disputas");
        layoutMainMenu.setVisibility(View.GONE);
        layoutPendingSubcategories.setVisibility(View.GONE);
        scrollPendingResults.setVisibility(View.GONE);
        tvResultadoLog.setText("");
    }

    private void hideAllSections() {
        btnRealizarOperacion.setVisibility(View.GONE);
        btnPendingOps.setVisibility(View.GONE);
        btnMainDisputes.setVisibility(View.GONE);
    }

    /**
     * Genera dinámicamente un botón para una operación pendiente encontrada y lo añade a la lista.
     */
    private void addPendingButton(LinearLayout container, String text, String id, int type) {
        Button btn = new Button(mContext);
        btn.setText(text);
        btn.setAllCaps(false);
        // Código de colores para diferenciar tipos de tareas
        if (type == 1 || type == 2) {
            btn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#B8E4C9"))); // Verde para Comprador
            btn.setTextColor(Color.BLACK);
        } else if (type == 3) {
            btn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F7C6DA"))); // Rosa para Vendedor
            btn.setTextColor(Color.BLACK);
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 5, 0, 5);
        btn.setLayoutParams(params);

        btn.setOnClickListener(v -> handlePendingAction(id, type));
        container.addView(btn);
    }

    /**
     * Maneja la acción al pulsar una operación pendiente, redirigiendo al fragmento adecuado
     * (Comprador o Vendedor) con los argumentos necesarios para retomar el flujo.
     */
    private void handlePendingAction(String id, int type) {
        hideKeyboard();
        layoutPendingSubcategories.setVisibility(View.GONE);
        scrollPendingResults.setVisibility(View.GONE);

        Bundle args = new Bundle();
        args.putString("PENDING_ID", id);
        args.putInt("PENDING_TYPE", type);

        // Determina el destino según el tipo de operación
        Fragment target = (type == 1 || type == 2) ? new CompradorFragment() : new VendedorFragment();
        target.setArguments(args);

        hideAllSections();
        btnRealizarOperacion.setVisibility(View.VISIBLE);
        btnRealizarOperacion.setText("Cerrar operaciones");
        loadFragment(target, "PendingAction");
    }

    /**
     * Oculta el teclado virtual del dispositivo si está visible.
     * Mejora la experiencia de usuario limpiando la pantalla tras pulsar botones.
     */
    private void hideKeyboard() {
        if (getActivity() != null && getActivity().getCurrentFocus() != null)
            ((InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
    }
}