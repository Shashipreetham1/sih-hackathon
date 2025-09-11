package com.hackathon.attendlytics;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private NavController navController;
    private AppBarConfiguration appBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Starting.");

        // Initialize Firebase App Check (using debug provider for development)
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        );

        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate: setContentView completed.");

        // Setup Toolbar
        setupToolbar();
        
        // Setup Navigation
        setupNavigation();

        // Setup Window Insets
        setupWindowInsets();
        
        Log.d(TAG, "onCreate: finished.");
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar == null) {
            Log.e(TAG, "setupToolbar: Toolbar not found!");
            Toast.makeText(this, "Error: Toolbar not found", Toast.LENGTH_SHORT).show();
            return;
        }
        
        setSupportActionBar(toolbar);
        Log.d(TAG, "setupToolbar: Toolbar set as ActionBar");
        
        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) {
            Log.e(TAG, "setupToolbar: ActionBar is null after setSupportActionBar!");
            Toast.makeText(this, "Error: ActionBar not set", Toast.LENGTH_SHORT).show();
        } else {
            Log.d(TAG, "setupToolbar: ActionBar successfully set");
        }
    }

    private void setupNavigation() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment == null) {
            Log.e(TAG, "setupNavigation: NavHostFragment not found!");
            Toast.makeText(this, "Error: Navigation host not found", Toast.LENGTH_SHORT).show();
            return;
        }

        navController = navHostFragment.getNavController();
        Log.d(TAG, "setupNavigation: NavController obtained");

        // Create AppBarConfiguration
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        Log.d(TAG, "setupNavigation: AppBarConfiguration built");

        // Setup ActionBar with NavController only if ActionBar exists
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            try {
                NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
                Log.d(TAG, "setupNavigation: Navigation UI setup completed");
            } catch (Exception e) {
                Log.e(TAG, "setupNavigation: Error setting up navigation UI", e);
                Toast.makeText(this, "Error setting up navigation", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e(TAG, "setupNavigation: Cannot setup navigation - ActionBar is null");
        }
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Don't apply top padding since we have a toolbar
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });
        Log.d(TAG, "setupWindowInsets: Window insets listener set");
    }

    @Override
    public boolean onSupportNavigateUp() {
        Log.d(TAG, "onSupportNavigateUp called");
        if (navController == null) {
            Log.e(TAG, "onSupportNavigateUp: NavController is null");
            return super.onSupportNavigateUp();
        }
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}
