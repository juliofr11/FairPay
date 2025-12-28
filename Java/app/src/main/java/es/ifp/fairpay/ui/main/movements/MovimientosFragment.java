package es.ifp.fairpay.ui.main.movements;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.web3j.abi.datatypes.Type;
import org.web3j.crypto.Credentials;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import es.ifp.fairpay.data.service.FairPayService;
import es.ifp.fairpay.R;
import es.ifp.fairpay.ui.main.MainActivity;

public class MovimientosFragment extends Fragment {

    // Constante para identificar la Wallet de la Plataforma (Admin)
    private static final String PLATFORM_WALLET = "0x06fd7bBCCa9Fd7d800a39c82660a04daC676F9f7";

    // Componentes de la interfaz de usuario
    private AutoCompleteTextView inputSearchWallet;
    private Button btnSearchMovements;
    private Button btnBack;
    private TextView tvStatus;
    private LinearLayout containerResults;

    private Context mContext;
    private String currentUserPrivateKey = "0x01"; // Valor por defecto seguro para inicialización

    public MovimientosFragment() {
        // Constructor público vacío requerido por Android
    }

    public static MovimientosFragment newInstance() {
        return new MovimientosFragment();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
    }

    // Esta función se encarga de inflar el diseño visual del fragmento
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_movimientos, container, false);
    }

    // Esta función se encarga de inicializar la lógica de búsqueda, cargar la clave del usuario y configurar los listeners
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Vinculación de vistas
        inputSearchWallet = view.findViewById(R.id.input_search_wallet);
        btnSearchMovements = view.findViewById(R.id.btn_search_movements);
        btnBack = view.findViewById(R.id.btn_movements_back);
        tvStatus = view.findViewById(R.id.tv_movements_status);
        containerResults = view.findViewById(R.id.container_movements_results);

        // Configuración inicial de datos del usuario
        loadUserKeyAndSetupAutocomplete();

        // Listener para el botón de búsqueda
        btnSearchMovements.setOnClickListener(v -> {
            // Ocultamos el teclado para mejorar la visibilidad
            hideKeyboard();

            String wallet = inputSearchWallet.getText().toString().trim();
            if (wallet.isEmpty()) {
                Toast.makeText(mContext, "Por favor, introduce una dirección de wallet.", Toast.LENGTH_SHORT).show();
            } else if (!wallet.startsWith("0x") || wallet.length() != 42) {
                Toast.makeText(mContext, "Formato de wallet incorrecto (debe empezar por 0x...)", Toast.LENGTH_SHORT).show();
            } else {
                performSearchMovements(wallet);
            }
        });

        // Configuración para mostrar el autocompletado al interactuar con el campo de texto
        inputSearchWallet.setOnClickListener(v -> inputSearchWallet.showDropDown());
        inputSearchWallet.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) inputSearchWallet.showDropDown();
        });

        // Listener para el botón de volver atrás
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });
    }

    // Esta función se encarga de ocultar el teclado virtual de forma programática
    private void hideKeyboard() {
        if (getActivity() != null) {
            View view = getActivity().getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            }
        }
    }

    // Esta función se encarga de recuperar la clave privada del usuario (de MainActivity o SharedPreferences) y configurar la sugerencia de su propia wallet
    private void loadUserKeyAndSetupAutocomplete() {
        // 1. Cargar clave privada
        if (getActivity() instanceof MainActivity) {
            String key = ((MainActivity) getActivity()).getUsuarioClavePrivada();
            if (key != null && !key.isEmpty()) currentUserPrivateKey = key;
        } else {
            SharedPreferences prefs = mContext.getSharedPreferences("FairPayPrefs", Context.MODE_PRIVATE);
            String key = prefs.getString("CURRENT_USER_PRIVATE_KEY", "");
            if (!key.isEmpty()) currentUserPrivateKey = key;
        }

        // 2. Derivar dirección pública y configurar adaptador de autocompletado
        try {
            if (!currentUserPrivateKey.equals("0x01")) {
                Credentials credentials = Credentials.create(currentUserPrivateKey);
                String myAddress = credentials.getAddress();

                // Crear lista de sugerencias con la wallet del usuario actual
                String[] suggestions = new String[]{myAddress};

                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        mContext,
                        android.R.layout.simple_dropdown_item_1line,
                        suggestions
                );

                inputSearchWallet.setAdapter(adapter);
            }
        } catch (Exception e) {
            // Silenciamos errores de derivación para no interrumpir el flujo
        }
    }

    // Esta función se encarga de realizar un escaneo profundo en la Blockchain para encontrar transacciones asociadas a la wallet indicada
    private void performSearchMovements(String walletAddress) {
        boolean isPlatformSearch = walletAddress.equalsIgnoreCase(PLATFORM_WALLET);

        tvStatus.setText(isPlatformSearch ? "Buscando resoluciones de Plataforma..." : "Iniciando escaneo robusto...");
        btnSearchMovements.setEnabled(false);
        containerResults.removeAllViews();

        new Thread(() -> {
            FairPayService service = new FairPayService(currentUserPrivateKey);

            int foundCount = 0;
            int currentId = 0;
            int consecutiveFailures = 0;

            // Bucle de escaneo indefinido hasta encontrar múltiples fallos consecutivos (fin de datos)
            while (true) {
                if (consecutiveFailures >= 3) {
                    break;
                }

                BigInteger id = BigInteger.valueOf(currentId);

                // Actualización de estado en UI cada 5 intentos para feedback visual
                if (currentId % 5 == 0) {
                    final int displayId = currentId;
                    updateStatus("Escaneando ID: " + displayId + "...");
                }

                boolean success = false;
                int retries = 0;

                // Intentos con reintento simple ante fallos de red
                while (retries < 3 && !success) {
                    try {
                        List<Type> details = service.getEscrowDetails(id);

                        if (details != null && !details.isEmpty()) {
                            success = true;
                            consecutiveFailures = 0;

                            String buyer = details.get(0).getValue().toString();
                            String seller = details.get(1).getValue().toString();
                            BigInteger amountWei = (BigInteger) details.get(2).getValue();
                            boolean isFunded = (Boolean) details.get(3).getValue();
                            BigInteger approvals = (BigInteger) details.get(4).getValue();

                            String amountEth = Convert.fromWei(new BigDecimal(amountWei), Convert.Unit.ETHER).toPlainString();
                            boolean isFinalized = approvals.compareTo(BigInteger.ZERO) > 0;

                            // Lógica de filtrado de resultados
                            if (isPlatformSearch) {
                                // MODO PLATAFORMA: Solo mostrar si está finalizada (resolveDispute implica finalización)
                                if (isFinalized) {
                                    foundCount++;

                                    String info = "ID: " + id + " (RESOLUCIÓN)\n" +
                                            "Liberado a: " + (amountWei.equals(BigInteger.ZERO) ? "Vendedor (Liberado)" : "Comprador (Reembolsado)") + "\n" +
                                            "Comprador: " + formatAddress(buyer) + "\n" +
                                            "Vendedor: " + formatAddress(seller) + "\n" +
                                            "Monto Liberado: " + amountEth + " ETH";

                                    getActivity().runOnUiThread(() -> addPlatformResultCard(info));
                                }
                            } else {
                                // MODO USUARIO NORMAL: Mostrar si participa como comprador o vendedor
                                if (buyer.equalsIgnoreCase(walletAddress) || seller.equalsIgnoreCase(walletAddress)) {
                                    foundCount++;
                                    boolean isBuyer = buyer.equalsIgnoreCase(walletAddress);
                                    String counterpart = isBuyer ? seller : buyer;

                                    String statusStr;
                                    if (isFinalized) statusStr = "FINALIZADO (Aprobado)";
                                    else if (isFunded) statusStr = "PAGADO (Pendiente Aprobación)";
                                    else statusStr = "CREADO (Esperando Pago)";

                                    String info = "ID: " + id + "\n" +
                                            "Tu Rol: " + (isBuyer ? "COMPRADOR" : "VENDEDOR") + "\n" +
                                            "Contraparte: " + formatAddress(counterpart) + "\n" +
                                            "Monto: " + amountEth + " ETH\n" +
                                            "Estado: " + statusStr;

                                    getActivity().runOnUiThread(() -> addResultCard(info, isBuyer));
                                }
                            }
                        } else {
                            break;
                        }
                    } catch (Exception e) {
                        retries++;
                        try { Thread.sleep(200); } catch (InterruptedException ie) {}
                    }
                }

                if (!success) consecutiveFailures++;
                try { Thread.sleep(100); } catch (InterruptedException e) {}
                currentId++;
            }

            // Actualización final de la interfaz
            final int finalCount = foundCount;
            final int totalScanned = currentId - 3;
            getActivity().runOnUiThread(() -> {
                if (finalCount == 0) {
                    tvStatus.setText("Escaneo finalizado. Sin resultados.");
                } else {
                    tvStatus.setText("Escaneo completado. " + finalCount + " encontrados. (Total: " + totalScanned + ")");
                }
                btnSearchMovements.setEnabled(true);
            });

        }).start();
    }

    // --- MÉTODOS DE UTILIDAD UI ---

    // Esta función se encarga de actualizar el texto de estado en el hilo principal
    private void updateStatus(String msg) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> tvStatus.setText(msg));
        }
    }

    // Esta función se encarga de generar y añadir una tarjeta visual con los detalles de una transacción normal
    private void addResultCard(String info, boolean isBuyer) {
        TextView card = new TextView(mContext);
        card.setText(info);
        card.setPadding(40, 40, 40, 40);
        card.setTextSize(14);
        card.setTextColor(Color.BLACK);
        card.setTypeface(null, android.graphics.Typeface.BOLD);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 30);
        card.setLayoutParams(params);

        if (isBuyer) {
            card.setBackgroundColor(Color.parseColor("#E0F2F1")); // Fondo verdoso para comprador
            card.setTextColor(Color.parseColor("#00695C"));
        } else {
            card.setBackgroundColor(Color.parseColor("#FCE4EC")); // Fondo rojizo para vendedor
            card.setTextColor(Color.parseColor("#880E4F"));
        }

        containerResults.addView(card);
    }

    // Esta función se encarga de generar y añadir una tarjeta visual específica para resoluciones de plataforma
    private void addPlatformResultCard(String info) {
        TextView card = new TextView(mContext);
        card.setText(info);
        card.setPadding(40, 40, 40, 40);
        card.setTextSize(14);
        card.setTypeface(null, android.graphics.Typeface.BOLD);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 30);
        card.setLayoutParams(params);

        card.setBackgroundColor(Color.parseColor("#AEDDFF")); // Azul claro corporativo
        card.setTextColor(Color.parseColor("#103044"));     // Azul oscuro corporativo

        containerResults.addView(card);
    }

    // Esta función se encarga de formatear las direcciones largas para mostrarlas de forma compacta
    private String formatAddress(String addr) {
        if (addr != null && addr.length() > 10) {
            return addr.substring(0, 6) + "..." + addr.substring(addr.length() - 4);
        }
        return addr;
    }
}