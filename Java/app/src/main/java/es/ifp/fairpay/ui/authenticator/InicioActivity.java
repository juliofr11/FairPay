package es.ifp.fairpay.ui.authenticator;

import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import es.ifp.fairpay.R;


public class InicioActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_inicio);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.nav_host_fragment), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // Contenedor de los Fragments, todos se cargan aquí
        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        // Controlador de navegación, para gestionar la carga de Fragments, transiciones y argumentos
        NavController navController;
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        } else {
            navController = null;
        }
        // Barra de navegación inferior,
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        if (navController != null) {
            NavigationUI.setupWithNavController(bottomNav, navController);
        }
        // Para limpiar el stack de pantallas cada vez que se pulsa el botón de la barra inferior
        bottomNav.setOnItemSelectedListener(item -> {
            int destinationId = item.getItemId();
            // Limpia el stack anterior y navega al nuevo destino
            NavOptions navOptions = new NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setPopUpTo(navController.getGraph().getStartDestinationId(), false)
                    .build();
            try {
                navController.navigate(destinationId, null, navOptions);
            } catch (IllegalArgumentException e) {
                // Si ya estás en ese destino, evita error de navegación
            }
            return true;
        });
    }
}