package com.hackathon.attendlytics;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class TeacherRegistrationFragment extends Fragment {

    private static final String TAG = "TeacherRegistration";
    
    private EditText editTextPhoneNumber, editTextOtp, editTextEmail, editTextEmailOtp;
    private TextInputLayout textInputLayoutOtp, textInputLayoutEmail, textInputLayoutEmailOtp;
    private Button buttonSendOtp, buttonVerifyOtp, buttonSendEmailOtp, buttonVerifyEmailOtp, buttonCompleteRegistration;
    private ProgressBar progressBar;
    
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private String userRole;

    // Email OTP variables
    private String generatedEmailOtp;
    private boolean isPhoneVerified = false;
    private boolean isEmailVerified = false;

    public TeacherRegistrationFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get role from arguments
        if (getArguments() != null) {
            userRole = getArguments().getString("userRole", "teacher");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_teacher_registration, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        initializeViews(view);
        
        // Set click listeners
        buttonSendOtp.setOnClickListener(v -> sendPhoneOtp());
        buttonVerifyOtp.setOnClickListener(v -> verifyPhoneOtp());
        buttonSendEmailOtp.setOnClickListener(v -> sendEmailOtp());
        buttonVerifyEmailOtp.setOnClickListener(v -> verifyEmailOtp());
        buttonCompleteRegistration.setOnClickListener(v -> completeTeacherRegistration());
    }

    private void initializeViews(View view) {
        editTextPhoneNumber = view.findViewById(R.id.editTextTeacherPhoneNumber);
        editTextOtp = view.findViewById(R.id.editTextTeacherOtp);
        editTextEmail = view.findViewById(R.id.editTextTeacherEmail);
        editTextEmailOtp = view.findViewById(R.id.editTextTeacherEmailOtp);
        
        textInputLayoutOtp = view.findViewById(R.id.textInputLayoutTeacherOtp);
        textInputLayoutEmail = view.findViewById(R.id.textInputLayoutTeacherEmail);
        textInputLayoutEmailOtp = view.findViewById(R.id.textInputLayoutTeacherEmailOtp);
        
        buttonSendOtp = view.findViewById(R.id.buttonSendTeacherOtp);
        buttonVerifyOtp = view.findViewById(R.id.buttonVerifyTeacherOtp);
        buttonSendEmailOtp = view.findViewById(R.id.buttonSendTeacherEmailOtp);
        buttonVerifyEmailOtp = view.findViewById(R.id.buttonVerifyTeacherEmailOtp);
        buttonCompleteRegistration = view.findViewById(R.id.buttonCompleteTeacherRegistration);
        
        progressBar = view.findViewById(R.id.progressBarTeacherRegistration);
    }

    private void sendPhoneOtp() {
        String phoneNumber = editTextPhoneNumber.getText().toString().trim();
        
        if (phoneNumber.isEmpty()) {
            editTextPhoneNumber.setError("Please enter phone number");
            return;
        }

        if (!phoneNumber.startsWith("+")) {
            phoneNumber = "+91" + phoneNumber; // Default to India
        }

        progressBar.setVisibility(View.VISIBLE);
        buttonSendOtp.setEnabled(false);

        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber(phoneNumber)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(requireActivity())
                        .setCallbacks(mCallbacks)
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                @Override
                public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                    Log.d(TAG, "onVerificationCompleted:" + credential);
                    signInWithPhoneAuthCredential(credential);
                }

                @Override
                public void onVerificationFailed(@NonNull FirebaseException e) {
                    Log.w(TAG, "onVerificationFailed", e);
                    progressBar.setVisibility(View.GONE);
                    buttonSendOtp.setEnabled(true);
                    Toast.makeText(getContext(), "Verification failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }

                @Override
                public void onCodeSent(@NonNull String verificationId,
                                       @NonNull PhoneAuthProvider.ForceResendingToken token) {
                    Log.d(TAG, "onCodeSent:" + verificationId);
                    mVerificationId = verificationId;
                    mResendToken = token;
                    
                    progressBar.setVisibility(View.GONE);
                    textInputLayoutOtp.setVisibility(View.VISIBLE);
                    buttonVerifyOtp.setVisibility(View.VISIBLE);
                    
                    // Disable phone number editing and send button after OTP is sent
                    editTextPhoneNumber.setEnabled(false);
                    buttonSendOtp.setEnabled(false);
                    
                    Toast.makeText(getContext(), "OTP sent successfully", Toast.LENGTH_SHORT).show();
                }
            };

    private void verifyPhoneOtp() {
        String code = editTextOtp.getText().toString().trim();
        
        if (code.isEmpty()) {
            editTextOtp.setError("Please enter OTP");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, code);
        signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(requireActivity(), task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithCredential:success");
                        isPhoneVerified = true;
                        
                        // Show email verification section
                        textInputLayoutEmail.setVisibility(View.VISIBLE);
                        buttonSendEmailOtp.setVisibility(View.VISIBLE);
                        
                        // Disable phone fields
                        editTextPhoneNumber.setEnabled(false);
                        editTextOtp.setEnabled(false);
                        buttonSendOtp.setEnabled(false);
                        buttonVerifyOtp.setEnabled(false);
                        
                        Toast.makeText(getContext(), "Phone verified successfully!", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        if (task.getException() != null) {
                            Toast.makeText(getContext(), "Verification failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getContext(), "Verification failed", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void sendEmailOtp() {
        String email = editTextEmail.getText().toString().trim();
        
        if (email.isEmpty()) {
            editTextEmail.setError("Please enter email");
            return;
        }

        if (!email.endsWith("@vnrvjiet.in")) {
            editTextEmail.setError("Please use your institutional email (@vnrvjiet.in)");
            return;
        }

        // Generate simple 6-digit OTP
        Random random = new Random();
        generatedEmailOtp = String.valueOf(random.nextInt(900000) + 100000);
        
        // In real implementation, send email here
        // For demo purposes, we'll show the OTP in toast
        Toast.makeText(getContext(), "Email OTP: " + generatedEmailOtp + " (Demo mode)", Toast.LENGTH_LONG).show();
        
        textInputLayoutEmailOtp.setVisibility(View.VISIBLE);
        buttonVerifyEmailOtp.setVisibility(View.VISIBLE);
        buttonSendEmailOtp.setEnabled(false);
    }

    private void verifyEmailOtp() {
        String enteredOtp = editTextEmailOtp.getText().toString().trim();
        
        if (enteredOtp.isEmpty()) {
            editTextEmailOtp.setError("Please enter email OTP");
            return;
        }

        if (enteredOtp.equals(generatedEmailOtp)) {
            isEmailVerified = true;
            
            // Disable email fields
            editTextEmail.setEnabled(false);
            editTextEmailOtp.setEnabled(false);
            buttonSendEmailOtp.setEnabled(false);
            buttonVerifyEmailOtp.setEnabled(false);
            
            // Show complete registration button
            buttonCompleteRegistration.setVisibility(View.VISIBLE);
            
            Toast.makeText(getContext(), "Email verified successfully!", Toast.LENGTH_SHORT).show();
        } else {
            editTextEmailOtp.setError("Invalid OTP");
            Toast.makeText(getContext(), "Invalid email OTP", Toast.LENGTH_SHORT).show();
        }
    }

    private void completeTeacherRegistration() {
        if (!isPhoneVerified || !isEmailVerified) {
            Toast.makeText(getContext(), "Please complete phone and email verification", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Authentication error. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        
        String userId = currentUser.getUid();
        String phoneNumber = currentUser.getPhoneNumber();
        String email = editTextEmail.getText().toString().trim();

        // Create teacher data
        Map<String, Object> teacherData = new HashMap<>();
        teacherData.put("phoneNumber", phoneNumber != null ? phoneNumber : "");
        teacherData.put("email", email);
        teacherData.put("role", "teacher");
        teacherData.put("registrationCompleted", true);
        teacherData.put("profileCompleted", false); // Will be completed in profile setup
        teacherData.put("createdAt", System.currentTimeMillis());

        // Save to Firestore teachers collection
        db.collection("teachers").document(userId)
                .set(teacherData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Teacher registration successful! Please go back and sign in.", Toast.LENGTH_LONG).show();
                    
                    // Simple solution: just pop back to previous screen
                    // User can then manually click "Sign In"
                    NavHostFragment.findNavController(TeacherRegistrationFragment.this).popBackStack();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.w(TAG, "Error adding teacher document", e);
                    Toast.makeText(getContext(), "Registration failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}