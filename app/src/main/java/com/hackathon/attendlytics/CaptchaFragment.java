package com.hackathon.attendlytics;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

// TODO: If you plan to use Google's SafetyNet reCAPTCHA, add the dependency:
// implementation 'com.google.android.gms:play-services-safetynet:18.0.1' // Or latest

public class CaptchaFragment extends Fragment {

    private static final String TAG = "CaptchaFragment";
    private TextView textViewCaptchaStatus;
    private Button buttonProceedToDashboard;

    public CaptchaFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_captcha, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        textViewCaptchaStatus = view.findViewById(R.id.textViewCaptchaStatus);
        buttonProceedToDashboard = view.findViewById(R.id.buttonProceedToDashboard);

        buttonProceedToDashboard.setOnClickListener(v -> {
            // Navigate to StudentDashboardFragment (stub)
            NavHostFragment.findNavController(CaptchaFragment.this)
                    .navigate(R.id.action_captchaFragment_to_studentDashboardFragment);
        });

        // Simulate CAPTCHA process
        simulateCaptcha();
    }

    private void simulateCaptcha() {
        // In a real scenario, you would integrate with reCAPTCHA API here.
        // For this stub, we just log and update the UI.
        Log.d(TAG, "CAPTCHA check started (stub).");
        textViewCaptchaStatus.setText("Verifying CAPTCHA (stub)...");
        buttonProceedToDashboard.setVisibility(View.GONE); // Hide button initially

        // Simulate a delay for CAPTCHA verification
        new android.os.Handler().postDelayed(() -> {
            if (isAdded() && getActivity() != null) { // Check if fragment is still valid
                Log.d(TAG, "CAPTCHA passed (stub).");
                Toast.makeText(getContext(), "CAPTCHA Passed (Stub)", Toast.LENGTH_SHORT).show();
                textViewCaptchaStatus.setText("CAPTCHA Passed (Stub)!");
                buttonProceedToDashboard.setVisibility(View.VISIBLE); // Show button on success
            }
        }, 2000); // 2-second delay
    }
}
