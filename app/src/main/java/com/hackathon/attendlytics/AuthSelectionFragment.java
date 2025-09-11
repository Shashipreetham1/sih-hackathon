package com.hackathon.attendlytics;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

public class AuthSelectionFragment extends Fragment {

    private Button buttonSignIn, buttonRegister, buttonBack;
    private TextView textViewRoleInfo;
    private String userRole;

    public AuthSelectionFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get role from arguments
        if (getArguments() != null) {
            userRole = getArguments().getString("userRole", "student");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_auth_selection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        buttonSignIn = view.findViewById(R.id.buttonSignIn);
        buttonRegister = view.findViewById(R.id.buttonRegister);
        buttonBack = view.findViewById(R.id.buttonBack);
        textViewRoleInfo = view.findViewById(R.id.textViewRoleInfo);

        // Update UI based on role
        updateUIForRole();

        // Set click listeners
        buttonSignIn.setOnClickListener(v -> goToSignIn());
        buttonRegister.setOnClickListener(v -> goToRegister());
        buttonBack.setOnClickListener(v -> goBack());
    }

    private void updateUIForRole() {
        if ("teacher".equals(userRole)) {
            textViewRoleInfo.setText("Teacher Authentication");
            buttonRegister.setText("Register as Teacher");
        } else {
            textViewRoleInfo.setText("Student Authentication");
            buttonRegister.setText("Register as Student");
        }
    }

    private void goToSignIn() {
        // Pass role to sign-in fragment
        Bundle bundle = new Bundle();
        bundle.putString("userRole", userRole);
        
        if ("teacher".equals(userRole)) {
            NavHostFragment.findNavController(AuthSelectionFragment.this)
                    .navigate(R.id.action_authSelectionFragment_to_teacherSignInFragment, bundle);
        } else {
            NavHostFragment.findNavController(AuthSelectionFragment.this)
                    .navigate(R.id.action_authSelectionFragment_to_signInFragment, bundle);
        }
    }

    private void goToRegister() {
        // Pass role to registration fragment
        Bundle bundle = new Bundle();
        bundle.putString("userRole", userRole);
        
        if ("teacher".equals(userRole)) {
            // Try direct navigation first, with fallback to intermediate navigation
            try {
                NavHostFragment.findNavController(AuthSelectionFragment.this)
                        .navigate(R.id.action_authSelectionFragment_to_teacherRegistrationFragment, bundle);
            } catch (Exception e) {
                // Fallback: Use teacher sign-in as intermediate step
                bundle.putBoolean("goToRegistration", true);
                NavHostFragment.findNavController(AuthSelectionFragment.this)
                        .navigate(R.id.action_authSelectionFragment_to_teacherSignInFragment, bundle);
            }
        } else {
            NavHostFragment.findNavController(AuthSelectionFragment.this)
                    .navigate(R.id.action_authSelectionFragment_to_loginFragment, bundle);
        }
    }

    private void goBack() {
        NavHostFragment.findNavController(AuthSelectionFragment.this)
                .navigate(R.id.action_authSelectionFragment_to_roleSelectionFragment);
    }
}