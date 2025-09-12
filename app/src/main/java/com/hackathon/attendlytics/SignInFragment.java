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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.TimeUnit;

public class SignInFragment extends Fragment {

    private static final String TAG = "SignInFragment";

    private EditText editTextPhoneNumber, editTextOtp;
    private Button buttonSendOtp, buttonVerifyOtp, buttonGoToRegister;
    private TextView textViewSignInTitle, textViewOr;
    private ProgressBar progressBarSignIn;
    private com.google.android.material.textfield.TextInputLayout textInputLayoutOtp;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private String mVerificationId;
    private String validatedPhoneNumber;
    private String userRole;

    public SignInFragment() {
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
        return inflater.inflate(R.layout.fragment_sign_in, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        editTextPhoneNumber = view.findViewById(R.id.editTextPhoneNumberSignIn);
        editTextOtp = view.findViewById(R.id.editTextOtpSignIn);
        textInputLayoutOtp = view.findViewById(R.id.textInputLayoutOtpSignIn);
        buttonSendOtp = view.findViewById(R.id.buttonSendOtpSignIn);
        buttonVerifyOtp = view.findViewById(R.id.buttonVerifyOtpSignIn);
        buttonGoToRegister = view.findViewById(R.id.buttonGoToRegister);
        textViewSignInTitle = view.findViewById(R.id.textViewSignInTitle);
        textViewOr = view.findViewById(R.id.textViewOr);
        progressBarSignIn = view.findViewById(R.id.progressBarSignIn);

        // Set click listeners
        buttonSendOtp.setOnClickListener(v -> sendOtp());
        buttonVerifyOtp.setOnClickListener(v -> verifyOtp());
        buttonGoToRegister.setOnClickListener(v -> goToRegistration());

        // Initially hide OTP fields
        textInputLayoutOtp.setVisibility(View.GONE);
        buttonVerifyOtp.setVisibility(View.GONE);
    }

    private void sendOtp() {
        String phoneNumber = editTextPhoneNumber.getText().toString().trim();
        if (TextUtils.isEmpty(phoneNumber) || phoneNumber.length() < 10) {
            Toast.makeText(getContext(), "Enter a valid phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        // Add country code if not present
        if (!phoneNumber.startsWith("+")) {
            phoneNumber = "+91" + phoneNumber; // Default to India
        }

        validatedPhoneNumber = phoneNumber;
        progressBarSignIn.setVisibility(View.VISIBLE);
        editTextPhoneNumber.setEnabled(false);
        buttonSendOtp.setEnabled(false);

        Log.d(TAG, "Sending OTP for sign-in to: " + phoneNumber);

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(requireActivity())
                .setCallbacks(mCallbacks)
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private final PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                @Override
                public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                    Log.d(TAG, "onVerificationCompleted - Auto verification successful");
                    progressBarSignIn.setVisibility(View.GONE);
                    signInWithPhoneAuthCredential(credential);
                }

                @Override
                public void onVerificationFailed(@NonNull FirebaseException e) {
                    Log.w(TAG, "onVerificationFailed", e);
                    progressBarSignIn.setVisibility(View.GONE);
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
                    Log.d(TAG, "onCodeSent: OTP sent successfully for sign-in");
                    progressBarSignIn.setVisibility(View.GONE);
                    mVerificationId = verificationId;
                    mResendToken = token;

                    textInputLayoutOtp.setVisibility(View.VISIBLE);
                    buttonVerifyOtp.setVisibility(View.VISIBLE);
                    editTextPhoneNumber.setEnabled(false);
                    buttonSendOtp.setEnabled(false);
                    Toast.makeText(getContext(), "OTP sent for sign-in", Toast.LENGTH_SHORT).show();
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

        progressBarSignIn.setVisibility(View.VISIBLE);
        Log.d(TAG, "Verifying OTP for sign-in: " + code);

        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, code);
        signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(requireActivity(), task -> {
                    progressBarSignIn.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithCredential success");
                        FirebaseUser user = task.getResult().getUser();
                        if (user != null) {
                            checkUserProfileAndNavigate(user.getUid());
                        }
                    } else {
                        Log.w(TAG, "signInWithCredential failed", task.getException());
                        Toast.makeText(getContext(), "Sign in failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                        editTextPhoneNumber.setEnabled(true);
                        buttonSendOtp.setEnabled(true);
                        editTextOtp.setText("");
                    }
                });
    }

    private void checkUserProfileAndNavigate(String uid) {
        Log.d(TAG, "Checking user profile for UID: " + uid);
        
        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d(TAG, "User profile found, checking completion status");
                        
                        // Check if profile is completed
                        Boolean profileCompleted = documentSnapshot.getBoolean("profileCompleted");
                        Boolean faceEnrolled = documentSnapshot.getBoolean("faceEnrolled");
                        
                        String studentName = documentSnapshot.getString("studentName");
                        String studentId = documentSnapshot.getString("studentId");
                        
                        if (profileCompleted != null && profileCompleted) {
                            if (faceEnrolled != null && faceEnrolled) {
                                // Fully registered user - go to dashboard
                                Toast.makeText(getContext(), "Welcome back, " + 
                                    (studentName != null ? studentName : "Student") + "!", Toast.LENGTH_SHORT).show();
                                
                                // Check current destination before navigating
                                NavController navController = NavHostFragment.findNavController(SignInFragment.this);
                                if (navController.getCurrentDestination() != null && 
                                    navController.getCurrentDestination().getId() != R.id.studentDashboardFragment) {
                                    navController.navigate(R.id.action_signInFragment_to_studentDashboardFragment);
                                }
                            } else {
                                // Profile complete but face not enrolled
                                Toast.makeText(getContext(), "Welcome back! Please complete face enrollment.", Toast.LENGTH_SHORT).show();
                                
                                // Check current destination before navigating
                                NavController navController = NavHostFragment.findNavController(SignInFragment.this);
                                if (navController.getCurrentDestination() != null && 
                                    navController.getCurrentDestination().getId() != R.id.faceEnrollFragment) {
                                    navController.navigate(R.id.action_signInFragment_to_faceEnrollFragment);
                                }
                            }
                        } else {
                            // Profile not completed - go to profile setup
                            Toast.makeText(getContext(), "Welcome back! Please complete your profile.", Toast.LENGTH_SHORT).show();
                            
                            // Check current destination before navigating
                            NavController navController = NavHostFragment.findNavController(SignInFragment.this);
                            if (navController.getCurrentDestination() != null && 
                                navController.getCurrentDestination().getId() != R.id.studentProfileSetupFragment) {
                                navController.navigate(R.id.action_signInFragment_to_studentProfileSetupFragment);
                            }
                        }
                    } else {
                        // User not found - this phone number is not registered
                        Log.w(TAG, "User profile not found for UID: " + uid);
                        Toast.makeText(getContext(), 
                            "This phone number is not registered. Please register first.", 
                            Toast.LENGTH_LONG).show();
                        
                        // Sign out the user since they're not properly registered
                        mAuth.signOut();
                        
                        // Reset UI
                        editTextPhoneNumber.setEnabled(true);
                        buttonSendOtp.setEnabled(true);
                        editTextOtp.setText("");
                        textInputLayoutOtp.setVisibility(View.GONE);
                        buttonVerifyOtp.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking user profile", e);
                    Toast.makeText(getContext(), "Error checking profile. Please try again.", Toast.LENGTH_LONG).show();
                    
                    // Reset UI
                    editTextPhoneNumber.setEnabled(true);
                    buttonSendOtp.setEnabled(true);
                });
    }

    private void goToRegistration() {
        NavHostFragment.findNavController(SignInFragment.this)
            .navigate(R.id.action_signInFragment_to_loginFragment);
    }
}