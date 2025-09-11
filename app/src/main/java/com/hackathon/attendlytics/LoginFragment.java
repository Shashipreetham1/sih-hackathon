package com.hackathon.attendlytics;

import android.os.Bundle;
import android.text.TextUtils;
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

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class LoginFragment extends Fragment {

    private static final String TAG = "LoginFragment";
    private static final String VALID_EMAIL_DOMAIN = "@vnrvjiet.in"; // Updated domain

    private EditText editTextPhoneNumber, editTextOtp, editTextEmail, editTextEmailOtp;
    private Button buttonSendOtp, buttonVerifyOtp, buttonValidateEmail, buttonSendEmailOtp, buttonVerifyEmailOtp;
    private ProgressBar progressBarLogin;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private String validatedPhoneNumber;
    private String validatedEmail;
    private String emailOtpCode; // Store generated email OTP
    private String userRole;

    public LoginFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        // Get role from arguments
        if (getArguments() != null) {
            userRole = getArguments().getString("userRole", "student");
        } else {
            userRole = "student"; // Default to student
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        editTextPhoneNumber = view.findViewById(R.id.editTextPhoneNumber);
        editTextOtp = view.findViewById(R.id.editTextOtp);
        editTextEmail = view.findViewById(R.id.editTextEmail);
        editTextEmailOtp = view.findViewById(R.id.editTextEmailOtp);
        buttonSendOtp = view.findViewById(R.id.buttonSendOtp);
        buttonVerifyOtp = view.findViewById(R.id.buttonVerifyOtp);
        buttonSendEmailOtp = view.findViewById(R.id.buttonSendEmailOtp);
        buttonVerifyEmailOtp = view.findViewById(R.id.buttonVerifyEmailOtp);
        buttonValidateEmail = view.findViewById(R.id.buttonValidateEmail);
        progressBarLogin = view.findViewById(R.id.progressBarLogin);

        buttonSendOtp.setOnClickListener(v -> sendOtp());
        buttonVerifyOtp.setOnClickListener(v -> verifyOtp());
        buttonSendEmailOtp.setOnClickListener(v -> sendEmailOtp());
        buttonVerifyEmailOtp.setOnClickListener(v -> verifyEmailOtp());
        buttonValidateEmail.setOnClickListener(v -> validateEmailAndProceed());
    }

    private void sendOtp() {
        String phoneNumber = editTextPhoneNumber.getText().toString().trim();
        if (TextUtils.isEmpty(phoneNumber) || phoneNumber.length() < 10) { // Basic validation
            Toast.makeText(getContext(), "Enter a valid phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        // Add country code if not present
        if (!phoneNumber.startsWith("+")) {
            phoneNumber = "+91" + phoneNumber; // Default to India
        }

        validatedPhoneNumber = phoneNumber; // Store for later use
        progressBarLogin.setVisibility(View.VISIBLE);
        editTextPhoneNumber.setEnabled(false);
        buttonSendOtp.setEnabled(false);

        Log.d(TAG, "Sending OTP to: " + phoneNumber);

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phoneNumber)       // Phone number to verify
                .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                .setActivity(requireActivity())    // Activity for callback binding
                .setCallbacks(mCallbacks)          // OnVerificationStateChangedCallbacks
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private final PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                @Override
                public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                    Log.d(TAG, "onVerificationCompleted - Auto verification successful");
                    progressBarLogin.setVisibility(View.GONE);
                    signInWithPhoneAuthCredential(credential);
                }

                @Override
                public void onVerificationFailed(@NonNull FirebaseException e) {
                    Log.w(TAG, "onVerificationFailed", e);
                    progressBarLogin.setVisibility(View.GONE);
                    editTextPhoneNumber.setEnabled(true);
                    buttonSendOtp.setEnabled(true);
                    
                    String errorMessage = "Verification failed. Please try again.";
                    if (e.getMessage() != null) {
                        errorMessage = e.getMessage();
                    }
                    Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                }

                @Override
                public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                    Log.d(TAG, "onCodeSent: OTP sent successfully. VerificationId: " + verificationId);
                    progressBarLogin.setVisibility(View.GONE);
                    mVerificationId = verificationId;
                    mResendToken = token;

                    editTextOtp.setVisibility(View.VISIBLE);
                    buttonVerifyOtp.setVisibility(View.VISIBLE);
                    editTextPhoneNumber.setEnabled(false);
                    buttonSendOtp.setEnabled(false);
                    Toast.makeText(getContext(), "OTP sent to your phone", Toast.LENGTH_SHORT).show();
                }
            };

    private void verifyOtp() {
        String code = editTextOtp.getText().toString().trim();
        if (TextUtils.isEmpty(code) || code.length() != 6) {
            Toast.makeText(getContext(), "Enter the 6-digit OTP", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mVerificationId == null) {
            Toast.makeText(getContext(), "Verification ID not received. Please try sending OTP again.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBarLogin.setVisibility(View.VISIBLE);
        Log.d(TAG, "Verifying OTP: " + code);

        // Firebase OTP verification for all numbers
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, code);
        signInWithPhoneAuthCredential(credential);
    }

    private void proceedToEmailValidation() {
        editTextEmail.setVisibility(View.VISIBLE);
        buttonSendEmailOtp.setVisibility(View.VISIBLE);
        editTextOtp.setEnabled(false);
        buttonVerifyOtp.setEnabled(false);
        Toast.makeText(getContext(), "Phone verified! Now enter your college email.", Toast.LENGTH_SHORT).show();
    }

    private void sendEmailOtp() {
        String email = editTextEmail.getText().toString().trim().toLowerCase();
        if (TextUtils.isEmpty(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(getContext(), "Enter a valid email address", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!email.endsWith(VALID_EMAIL_DOMAIN)) {
            Toast.makeText(getContext(), "Email must be from " + VALID_EMAIL_DOMAIN + " domain", Toast.LENGTH_LONG).show();
            return;
        }

        // Store validated email
        validatedEmail = email;
        
        // Generate a random 6-digit OTP
        emailOtpCode = String.format("%06d", (int)(Math.random() * 1000000));
        
        Log.d(TAG, "Generated Email OTP: " + emailOtpCode + " for email: " + email);
        
        // Show OTP input fields
        editTextEmailOtp.setVisibility(View.VISIBLE);
        buttonVerifyEmailOtp.setVisibility(View.VISIBLE);
        editTextEmail.setEnabled(false);
        buttonSendEmailOtp.setEnabled(false);
        
        // For testing, show the OTP in a toast (remove this in production)
        Toast.makeText(getContext(), "Test Email OTP: " + emailOtpCode + " (Check your email in production)", Toast.LENGTH_LONG).show();
        
        Log.i(TAG, "Email OTP sent to: " + email);
    }

    private void verifyEmailOtp() {
        String enteredOtp = editTextEmailOtp.getText().toString().trim();
        if (TextUtils.isEmpty(enteredOtp) || enteredOtp.length() != 6) {
            Toast.makeText(getContext(), "Enter the 6-digit email OTP", Toast.LENGTH_SHORT).show();
            return;
        }

        if (emailOtpCode != null && emailOtpCode.equals(enteredOtp)) {
            // OTP is correct
            Toast.makeText(getContext(), "Email OTP verified successfully!", Toast.LENGTH_SHORT).show();
            
            // Show final validation button
            buttonValidateEmail.setVisibility(View.VISIBLE);
            editTextEmailOtp.setEnabled(false);
            buttonVerifyEmailOtp.setEnabled(false);
            
            Log.d(TAG, "Email OTP verified for: " + validatedEmail);
        } else {
            Toast.makeText(getContext(), "Invalid email OTP. Please try again.", Toast.LENGTH_SHORT).show();
            editTextEmailOtp.setText("");
        }
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(requireActivity(), task -> {
                    progressBarLogin.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithCredential success");
                        // FirebaseUser user = task.getResult().getUser(); // You can get user details here
                        Toast.makeText(getContext(), "Phone verification successful!", Toast.LENGTH_SHORT).show();
                        // Use common method for proceeding to email validation
                        proceedToEmailValidation();
                    } else {
                        Log.w(TAG, "signInWithCredential failed", task.getException());
                        Toast.makeText(getContext(), "Sign in failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                         editTextPhoneNumber.setEnabled(true); // Allow re-try phone number
                         buttonSendOtp.setEnabled(true);
                         editTextOtp.setText(""); // Clear OTP field
                    }
                });
    }

    private void validateEmailAndProceed() {
        // Check if email OTP was verified
        if (validatedEmail == null) {
            Toast.makeText(getContext(), "Please verify your email first", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBarLogin.setVisibility(View.VISIBLE);
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && validatedPhoneNumber != null) {
            String uid = currentUser.getUid();
            
            // Create comprehensive student registration data
            Map<String, Object> userData = new HashMap<>();
            
            // Basic authentication details
            userData.put("uid", uid);
            userData.put("phone", validatedPhoneNumber);
            userData.put("email", validatedEmail);
            
            // Registration details
            userData.put("registrationTimestamp", System.currentTimeMillis());
            userData.put("registrationDate", new java.util.Date());
            userData.put("accountStatus", "active");
            userData.put("registrationMethod", "phone_email_verification");
            
            // Student profile data (to be filled in profile setup)
            userData.put("profileCompleted", false);
            userData.put("studentName", "");
            userData.put("studentId", "");
            userData.put("department", "");
            userData.put("year", "");
            userData.put("section", "");
            
            // Face enrollment status
            userData.put("faceEnrolled", false);
            userData.put("faceEnrollmentTimestamp", null);
            
            // Attendance tracking
            userData.put("totalAttendance", 0);
            userData.put("lastAttendanceDate", null);
            
            // Device and session info
            userData.put("lastLoginTimestamp", System.currentTimeMillis());
            userData.put("deviceInfo", android.os.Build.MODEL + " " + android.os.Build.VERSION.RELEASE);
            
            Log.d(TAG, "Storing comprehensive user data for UID: " + uid);
            Log.d(TAG, "Phone: " + validatedPhoneNumber + ", Email: " + validatedEmail);

            db.collection("users").document(uid)
                .set(userData) // set() will create or overwrite
                .addOnSuccessListener(aVoid -> {
                    progressBarLogin.setVisibility(View.GONE);
                    Log.d(TAG, "User data successfully written to Firestore for UID: " + uid);
                    Toast.makeText(getContext(), "Email validated. Please complete your profile.", Toast.LENGTH_SHORT).show();
                    // Navigate to Student Profile Setup instead of face enrollment
                    NavHostFragment.findNavController(LoginFragment.this)
                        .navigate(R.id.action_loginFragment_to_studentProfileSetupFragment);
                })
                .addOnFailureListener(e -> {
                    progressBarLogin.setVisibility(View.GONE);
                    Log.w(TAG, "Error writing user data to Firestore", e);
                    
                    // Check if it's a Firestore permission error
                    if (e.getMessage() != null && e.getMessage().contains("PERMISSION_DENIED")) {
                        Toast.makeText(getContext(), "Firestore not configured. Proceeding without saving to database.", Toast.LENGTH_LONG).show();
                        Log.i(TAG, "Proceeding to profile setup despite Firestore error");
                        // Proceed anyway for development
                        NavHostFragment.findNavController(LoginFragment.this)
                            .navigate(R.id.action_loginFragment_to_studentProfileSetupFragment);
                    } else {
                        Toast.makeText(getContext(), "Failed to save data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
        } else {
            progressBarLogin.setVisibility(View.GONE);
            Toast.makeText(getContext(), "User not authenticated or phone number not validated. Please restart login.", Toast.LENGTH_LONG).show();
            // Optionally, sign out and restart flow
            // mAuth.signOut();
            // restart_login_flow_ui_logic();
        }
    }
}