package com.hackathon.attendlytics;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.activity.EdgeToEdge;

public class MainActivity extends AppCompatActivity {

    private NavController navController;
    private AppBarConfiguration appBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Setup NavController
        navController = Navigation.findNavController(this, R.id.nav_host_fragment);

        // Setup AppBarConfiguration.
        // Pass the IDs of top-level destinations. For now, let's assume LoginFragment is the only one.
        // You can add other top-level destinations to the set if needed.
        appBarConfiguration = new AppBarConfiguration.Builder(R.id.loginFragment).build();
                               // If you had a DrawerLayout:
                               // .setOpenableLayout(drawerLayout)
                               // .build();


        // Set up the ActionBar to work with the NavController
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        // Handle Up button navigation
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}
