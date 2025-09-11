package com.hackathon.attendlytics;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

public class WelcomeFragment extends Fragment {

    private Button buttonSignIn, buttonRegister;

    public WelcomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_welcome, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        buttonSignIn = view.findViewById(R.id.buttonSignIn);
        buttonRegister = view.findViewById(R.id.buttonRegister);

        // Set click listeners
        buttonSignIn.setOnClickListener(v -> goToSignIn());
        buttonRegister.setOnClickListener(v -> goToRegister());
    }

    private void goToSignIn() {
        NavHostFragment.findNavController(WelcomeFragment.this)
                .navigate(R.id.action_welcomeFragment_to_signInFragment);
    }

    private void goToRegister() {
        NavHostFragment.findNavController(WelcomeFragment.this)
                .navigate(R.id.action_welcomeFragment_to_loginFragment);
    }
}