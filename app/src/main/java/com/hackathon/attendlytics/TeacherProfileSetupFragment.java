package com.hackathon.attendlytics;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class TeacherProfileSetupFragment extends Fragment {

    private TextInputEditText editTextTeacherName, editTextTeacherEmail, editTextTeacherID;
    private Spinner spinnerDepartment, spinnerSubjectCode;
    private Button buttonCompleteTeacherProfile;
    
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    public TeacherProfileSetupFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_teacher_profile_setup, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Initialize views
        initializeViews(view);
        setupSpinners();
        
        buttonCompleteTeacherProfile.setOnClickListener(v -> completeTeacherProfile());
    }

    private void initializeViews(View view) {
        editTextTeacherName = view.findViewById(R.id.editTextTeacherName);
        editTextTeacherEmail = view.findViewById(R.id.editTextTeacherEmail);
        editTextTeacherID = view.findViewById(R.id.editTextTeacherID);
        spinnerDepartment = view.findViewById(R.id.spinnerTeacherDepartment);
        spinnerSubjectCode = view.findViewById(R.id.spinnerSubjectCode);
        buttonCompleteTeacherProfile = view.findViewById(R.id.buttonCompleteTeacherProfile);
    }

    private void setupSpinners() {
        // Department options
        String[] departments = {
                "Select Department",
                "Computer Science & Engineering",
                "Information Technology", 
                "Electronics & Communication Engineering",
                "Electrical & Electronics Engineering",
                "Mechanical Engineering",
                "Civil Engineering",
                "Biotechnology",
                "Chemical Engineering",
                "Mathematics",
                "Physics",
                "Chemistry",
                "English",
                "Management Studies"
        };

        ArrayAdapter<String> departmentAdapter = new ArrayAdapter<>(
                requireContext(), 
                android.R.layout.simple_spinner_item, 
                departments
        );
        departmentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDepartment.setAdapter(departmentAdapter);

        // Subject codes - comprehensive list for different departments
        String[] subjectCodes = {
                "Select Subject Code",
                // CSE Subjects
                "CS101 - Programming Fundamentals",
                "CS201 - Data Structures",
                "CS301 - Database Management Systems",
                "CS401 - Computer Networks",
                "CS501 - Software Engineering",
                "CS601 - Machine Learning",
                // IT Subjects  
                "IT101 - Web Technologies",
                "IT201 - System Administration",
                "IT301 - Cybersecurity",
                "IT401 - Cloud Computing",
                // ECE Subjects
                "EC101 - Circuit Analysis",
                "EC201 - Digital Electronics",
                "EC301 - Signal Processing",
                "EC401 - Communication Systems",
                // EEE Subjects
                "EE101 - Electrical Circuits",
                "EE201 - Power Systems",
                "EE301 - Control Systems",
                "EE401 - Power Electronics",
                // Mechanical Subjects
                "ME101 - Engineering Mechanics",
                "ME201 - Thermodynamics",
                "ME301 - Fluid Mechanics",
                "ME401 - Manufacturing Processes",
                // Civil Subjects
                "CE101 - Surveying",
                "CE201 - Structural Analysis",
                "CE301 - Geotechnical Engineering",
                "CE401 - Transportation Engineering",
                // Core Subjects
                "MA101 - Engineering Mathematics I",
                "MA201 - Engineering Mathematics II",
                "PH101 - Engineering Physics",
                "CH101 - Engineering Chemistry",
                "EN101 - Technical Communication",
                // Management
                "MG101 - Principles of Management",
                "MG201 - Financial Management",
                "MG301 - Marketing Management",
                "MG401 - Human Resource Management"
        };

        ArrayAdapter<String> subjectAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                subjectCodes
        );
        subjectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSubjectCode.setAdapter(subjectAdapter);
    }

    private void completeTeacherProfile() {
        String teacherName = editTextTeacherName.getText().toString().trim();
        String teacherEmail = editTextTeacherEmail.getText().toString().trim();
        String teacherID = editTextTeacherID.getText().toString().trim();
        String department = spinnerDepartment.getSelectedItem().toString();
        String subjectCode = spinnerSubjectCode.getSelectedItem().toString();

        // Validation
        if (teacherName.isEmpty()) {
            editTextTeacherName.setError("Please enter teacher name");
            return;
        }

        if (teacherEmail.isEmpty()) {
            editTextTeacherEmail.setError("Please enter email");
            return;
        }

        if (!teacherEmail.endsWith("@vnrvjiet.in")) {
            editTextTeacherEmail.setError("Please use your institutional email (@vnrvjiet.in)");
            return;
        }

        if (teacherID.isEmpty()) {
            editTextTeacherID.setError("Please enter teacher ID");
            return;
        }

        if (department.equals("Select Department")) {
            Toast.makeText(getContext(), "Please select a department", Toast.LENGTH_SHORT).show();
            return;
        }

        if (subjectCode.equals("Select Subject Code")) {
            Toast.makeText(getContext(), "Please select a subject code", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save teacher profile to Firestore
        saveTeacherProfileToFirestore(teacherName, teacherEmail, teacherID, department, subjectCode);
    }

    private void saveTeacherProfileToFirestore(String teacherName, String teacherEmail, 
                                              String teacherID, String department, String subjectCode) {
        String userId = firebaseAuth.getCurrentUser().getUid();

        Map<String, Object> teacherData = new HashMap<>();
        teacherData.put("teacherName", teacherName);
        teacherData.put("teacherEmail", teacherEmail);
        teacherData.put("teacherID", teacherID);
        teacherData.put("department", department);
        teacherData.put("subjectCode", subjectCode);
        teacherData.put("role", "teacher");
        teacherData.put("profileCompleted", true);
        teacherData.put("createdAt", System.currentTimeMillis());

        firestore.collection("teachers").document(userId)
                .set(teacherData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Teacher profile completed successfully!", Toast.LENGTH_SHORT).show();
                    // Navigate to teacher dashboard
                    NavHostFragment.findNavController(TeacherProfileSetupFragment.this)
                            .navigate(R.id.action_teacherProfileSetupFragment_to_teacherDashboardFragment);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to save profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}