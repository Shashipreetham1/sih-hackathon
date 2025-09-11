package com.hackathon.attendlytics;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class StudentProfileSetupFragment extends Fragment {

    private static final String TAG = "StudentProfileSetup";

    private EditText editTextStudentName, editTextStudentId, editTextSection;
    private Spinner spinnerDepartment, spinnerYear;
    private Button buttonSaveProfile;
    private ProgressBar progressBarProfile;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    public StudentProfileSetupFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_student_profile_setup, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        editTextStudentName = view.findViewById(R.id.editTextStudentName);
        editTextStudentId = view.findViewById(R.id.editTextStudentId);
        editTextSection = view.findViewById(R.id.editTextSection);
        spinnerDepartment = view.findViewById(R.id.spinnerDepartment);
        spinnerYear = view.findViewById(R.id.spinnerYear);
        buttonSaveProfile = view.findViewById(R.id.buttonSaveProfile);
        progressBarProfile = view.findViewById(R.id.progressBarProfile);

        setupSpinners();
        
        buttonSaveProfile.setOnClickListener(v -> saveStudentProfile());
    }

    private void setupSpinners() {
        // Department spinner
        String[] departments = {
            "Select Department",
            "Computer Science and Engineering (CSE)",
            "Electronics and Communication Engineering (ECE)", 
            "Electrical and Electronics Engineering (EEE)",
            "Mechanical Engineering (MECH)",
            "Civil Engineering (CIVIL)",
            "Chemical Engineering (CHEM)",
            "Information Technology (IT)"
        };
        
        ArrayAdapter<String> departmentAdapter = new ArrayAdapter<>(
            requireContext(), 
            android.R.layout.simple_spinner_item, 
            departments
        );
        departmentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDepartment.setAdapter(departmentAdapter);

        // Year spinner
        String[] years = {
            "Select Year",
            "1st Year",
            "2nd Year", 
            "3rd Year",
            "4th Year"
        };
        
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            years
        );
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerYear.setAdapter(yearAdapter);
    }

    private void saveStudentProfile() {
        // Validate inputs
        String studentName = editTextStudentName.getText().toString().trim();
        String studentId = editTextStudentId.getText().toString().trim().toUpperCase();
        String section = editTextSection.getText().toString().trim().toUpperCase();
        String department = spinnerDepartment.getSelectedItem().toString();
        String year = spinnerYear.getSelectedItem().toString();

        // Validation
        if (TextUtils.isEmpty(studentName)) {
            editTextStudentName.setError("Enter your full name");
            return;
        }

        if (TextUtils.isEmpty(studentId) || studentId.length() < 10) {
            editTextStudentId.setError("Enter valid student ID (e.g., 23071A12A6)");
            return;
        }

        if (TextUtils.isEmpty(section)) {
            editTextSection.setError("Enter section (e.g., A, B, C)");
            return;
        }

        if (department.equals("Select Department")) {
            Toast.makeText(getContext(), "Please select your department", Toast.LENGTH_SHORT).show();
            return;
        }

        if (year.equals("Select Year")) {
            Toast.makeText(getContext(), "Please select your year", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get current user
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Authentication error. Please login again.", Toast.LENGTH_LONG).show();
            return;
        }

        progressBarProfile.setVisibility(View.VISIBLE);
        buttonSaveProfile.setEnabled(false);

        // Extract department code
        String departmentCode = extractDepartmentCode(department);

        // Create profile data
        Map<String, Object> profileData = new HashMap<>();
        profileData.put("studentName", studentName);
        profileData.put("studentId", studentId);
        profileData.put("department", departmentCode);
        profileData.put("departmentFull", department);
        profileData.put("year", year);
        profileData.put("section", section);
        profileData.put("profileCompleted", true);
        profileData.put("profileCompletionTimestamp", System.currentTimeMillis());
        profileData.put("profileCompletionDate", new java.util.Date());

        // Save to Firestore
        String uid = currentUser.getUid();
        Log.d(TAG, "Saving student profile for UID: " + uid);
        
        db.collection("users").document(uid)
                .set(profileData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    progressBarProfile.setVisibility(View.GONE);
                    Log.d(TAG, "Student profile saved successfully");
                    Toast.makeText(getContext(), "Profile saved! Proceeding to face enrollment.", Toast.LENGTH_SHORT).show();
                    
                    // Navigate to face enrollment
                    NavHostFragment.findNavController(StudentProfileSetupFragment.this)
                        .navigate(R.id.action_studentProfileSetupFragment_to_faceEnrollFragment);
                })
                .addOnFailureListener(e -> {
                    progressBarProfile.setVisibility(View.GONE);
                    buttonSaveProfile.setEnabled(true);
                    Log.e(TAG, "Failed to save student profile", e);
                    Toast.makeText(getContext(), "Failed to save profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private String extractDepartmentCode(String fullDepartment) {
        if (fullDepartment.contains("CSE")) return "CSE";
        if (fullDepartment.contains("ECE")) return "ECE";
        if (fullDepartment.contains("EEE")) return "EEE";
        if (fullDepartment.contains("MECH")) return "MECH";
        if (fullDepartment.contains("CIVIL")) return "CIVIL";
        if (fullDepartment.contains("CHEM")) return "CHEM";
        if (fullDepartment.contains("IT")) return "IT";
        return "UNKNOWN";
    }
}