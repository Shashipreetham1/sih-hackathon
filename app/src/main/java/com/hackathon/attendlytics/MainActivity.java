package com.hackathon.attendlytics; // Make sure this matches your package name

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.hackathon.attendlytics.databinding.ActivityMainBinding; // Adjust if your module name is different

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private AppBarConfiguration appBarConfiguration;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set the Toolbar
        Toolbar toolbar = binding.toolbar; // Using ViewBinding
        setSupportActionBar(toolbar);

        // Get the NavHostFragment and NavController
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();

            // Define AppBarConfiguration: Pass the set of Top-level destinations.
            // These are destinations where the Up button should not be displayed.
            // For now, let's assume LoginFragment is a top-level destination.
            // You can add more as needed (e.g., your main screen after login).
            appBarConfiguration = new AppBarConfiguration.Builder(R.id.loginFragment)
                    // .setOpenableLayout(binding.drawerLayout) // If you were using a DrawerLayout
                    .build();

            // Set up the ActionBar with NavController
            NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        } else {
            // Handle the case where NavHostFragment is not found, though it shouldn't happen with correct setup
            // You could log an error or throw an exception
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        // Handle the Up button action (back navigation)
        // Ensure navController is not null
        return navController != null && (NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp());
    }
}