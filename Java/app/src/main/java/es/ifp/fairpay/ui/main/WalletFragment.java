package es.ifp.fairpay.ui.main;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Convert;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import es.ifp.fairpay.R;

public class WalletFragment extends Fragment {

    // Elementos de la interfaz de usuario
    protected ListView listaMovimientos;
    protected TextView tvUsuario, tvSaldo, tvWalletAddress;
    protected Button verMovimientos;

    // Estructuras de datos para la lista
    protected ArrayList<String> listaTransacciones = new ArrayList<>();
    protected ArrayAdapter<String> adaptador;

    // Configuración de Blockchain y API externa
    private final String RPC_URL = "https://sepolia.drpc.org";

    // Clave API para consultar el explorador de bloques (Etherscan)
    private final String ETHERSCAN_API_KEY = "E9FX35CSBQ19TRG846I36K51T5M7A3NCKY";

    private Web3j web3j;
    private String myWalletAddress = "";

    // Esta función se encarga de inflar el diseño visual del fragmento
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_wallet, container, false);
    }

    // Esta función se encarga de inicializar la lógica, vincular las vistas y cargar los datos al crear la vista
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Vinculación de los elementos visuales del XML
        listaMovimientos = view.findViewById(R.id.listView_movimientos_wallet);
        verMovimientos = view.findViewById(R.id.boton_vertodo_wallet);

        try {
            tvUsuario = view.findViewById(R.id.text_nombre_wallet);
            tvSaldo = view.findViewById(R.id.text_saldo_wallet);
            tvWalletAddress = view.findViewById(R.id.text_wallet_address);
        } catch (Exception e) {
            Log.e("WalletFragment", "Error vinculando vistas: " + e.getMessage());
        }

        // 2. Configuración del adaptador para mostrar la lista de transacciones
        adaptador = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, listaTransacciones);
        if (listaMovimientos != null) {
            listaMovimientos.setAdapter(adaptador);
        }

        // 3. Carga de los datos del usuario almacenados localmente
        loadUserData();

        // 4. Inicio de la carga de datos desde la red (Blockchain y API)
        if (!myWalletAddress.isEmpty()) {
            fetchBalanceWeb3();      // Consultar saldo en Blockchain
            fetchTransactionsApi();  // Consultar historial en API Etherscan
        } else {
            if (tvUsuario != null) tvUsuario.setText("Usuario no identificado");
        }

        // 5. Configuración del botón de navegación para ir a la sección completa de movimientos
        if (verMovimientos != null) {
            verMovimientos.setOnClickListener(v -> {
                if (getActivity() != null) {
                    BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_nav);
                    if (bottomNav != null) bottomNav.setSelectedItemId(R.id.movimientosFragment);
                }
            });
        }
    }

    // Esta función se encarga de recuperar el nombre y la dirección de la billetera del usuario desde las preferencias compartidas
    private void loadUserData() {
        if (getContext() == null) return;
        SharedPreferences prefs = getContext().getSharedPreferences("FairPayPrefs", Context.MODE_PRIVATE);
        String name = prefs.getString("CURRENT_USER_NAME", "Usuario");
        String privateKey = prefs.getString("CURRENT_USER_PRIVATE_KEY", "");

        if (tvUsuario != null) tvUsuario.setText(name);

        if (!privateKey.isEmpty()) {
            try {
                // Derivamos la dirección pública a partir de la clave privada guardada
                Credentials credentials = Credentials.create(privateKey);
                myWalletAddress = credentials.getAddress();
                if (tvWalletAddress != null) {
                    tvWalletAddress.setText(myWalletAddress);
                    tvWalletAddress.setVisibility(View.VISIBLE);
                }
            } catch (Exception e) {
                myWalletAddress = "Error en clave";
            }
        } else {
            myWalletAddress = "";
            if (tvWalletAddress != null) tvWalletAddress.setText("Sin wallet conectada");
        }
    }

    // Esta función se encarga de consultar el saldo actual de la billetera directamente en la Blockchain (red Sepolia) usando Web3j
    private void fetchBalanceWeb3() {
        if (tvSaldo != null) tvSaldo.setText("Cargando...");
        new Thread(() -> {
            try {
                web3j = Web3j.build(new HttpService(RPC_URL));
                EthGetBalance balanceResponse = web3j.ethGetBalance(myWalletAddress, DefaultBlockParameterName.LATEST).send();
                BigInteger balanceWei = balanceResponse.getBalance();
                BigDecimal balanceEth = Convert.fromWei(new BigDecimal(balanceWei), Convert.Unit.ETHER);

                // Actualizamos la interfaz en el hilo principal
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (tvSaldo != null) tvSaldo.setText(String.format(Locale.US, "%.4f ETH", balanceEth));
                });
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (tvSaldo != null) tvSaldo.setText("Error Red");
                });
            }
        }).start();
    }

    // Esta función se encarga de obtener el historial de transacciones utilizando la API V2 de Etherscan
    private void fetchTransactionsApi() {
        listaTransacciones.clear();
        listaTransacciones.add("Cargando movimientos...");
        adaptador.notifyDataSetChanged();

        new Thread(() -> {
            try {
                // Preparamos el parámetro de la API KEY si es válida
                String apiKeyParam = "";
                if (!"YourApiKeyToken".equals(ETHERSCAN_API_KEY)) {
                    apiKeyParam = "&apikey=" + ETHERSCAN_API_KEY;
                }

                // Construcción de la URL de consulta a la API de Etherscan para la red Sepolia
                String urlString = "https://api.etherscan.io/v2/api" +
                        "?chainid=11155111" + // ID de cadena para Sepolia
                        "&module=account" +
                        "&action=txlist" +
                        "&address=" + myWalletAddress +
                        "&startblock=0" +
                        "&endblock=99999999" +
                        "&page=1" +
                        "&offset=10" + // Limitamos a las últimas 10 transacciones
                        "&sort=desc" + // Ordenamos de más reciente a más antigua
                        apiKeyParam;

                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);

                // Lectura de la respuesta del servidor
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // Procesamiento del JSON recibido
                JSONObject jsonResponse = new JSONObject(response.toString());
                String status = jsonResponse.optString("status");

                List<String> formattedTxs = new ArrayList<>();

                if ("1".equals(status)) {
                    JSONArray result = jsonResponse.getJSONArray("result");

                    for (int i = 0; i < result.length(); i++) {
                        JSONObject tx = result.getJSONObject(i);

                        // 1. Formateo del Hash (mostramos solo el inicio y el final)
                        String hash = tx.getString("hash");
                        String shortHash = hash.length() > 14 ? hash.substring(0, 14) + "..." : hash;

                        // 2. Identificación del método o tipo de transacción
                        String functionName = tx.optString("functionName", "");
                        String methodDisplay;

                        if (!functionName.isEmpty()) {
                            int parenIndex = functionName.indexOf("(");
                            if (parenIndex > 0) {
                                methodDisplay = functionName.substring(0, parenIndex);
                                // Capitalizamos la primera letra
                                methodDisplay = methodDisplay.substring(0, 1).toUpperCase() + methodDisplay.substring(1);
                            } else {
                                methodDisplay = functionName;
                            }
                        } else {
                            String input = tx.optString("input", "0x");
                            if ("0x".equals(input)) {
                                methodDisplay = "Transferencia";
                            } else {
                                methodDisplay = "Smart Contract";
                            }
                        }

                        // 3. Cálculo del tiempo transcurrido (hace X minutos/horas)
                        long timeStamp = tx.getLong("timeStamp") * 1000;
                        CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(timeStamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);

                        // Composición del texto final para la lista
                        String item = String.format("%s | %s\n%s", shortHash, methodDisplay, timeAgo);
                        formattedTxs.add(item);
                    }
                } else {
                    String message = jsonResponse.optString("message", "Error");
                    String resultMsg = jsonResponse.optString("result", "");

                    if ("No transactions found".equalsIgnoreCase(message)) {
                        formattedTxs.add("Sin movimientos históricos.");
                    } else if (resultMsg.contains("Missing/invalid API key")) {
                        formattedTxs.add("Error: Falta API Key válida de Etherscan.");
                    } else {
                        formattedTxs.add("Info: " + message);
                    }
                }

                // Actualización de la lista en el hilo principal de la interfaz
                new Handler(Looper.getMainLooper()).post(() -> {
                    listaTransacciones.clear();
                    if (formattedTxs.isEmpty()) {
                        listaTransacciones.add("Sin movimientos.");
                    } else {
                        listaTransacciones.addAll(formattedTxs);
                    }
                    adaptador.notifyDataSetChanged();
                });

            } catch (Exception e) {
                Log.e("WalletFragment", "Error API: " + e.getMessage());
                new Handler(Looper.getMainLooper()).post(() -> {
                    listaTransacciones.clear();
                    listaTransacciones.add("Error de conexión (Etherscan).");
                    adaptador.notifyDataSetChanged();
                });
            }
        }).start();
    }
}