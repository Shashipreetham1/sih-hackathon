package com.hackathon.attendlytics;

import android.os.Bundle;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class TeacherDashboardFragment extends Fragment {

    private TextView textViewWelcome, textViewTeacherInfo;
    private Button buttonSignOut, buttonManageClasses, buttonViewReports, buttonMarkAttendance;
    
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    public TeacherDashboardFragment() {
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
        return inflater.inflate(R.layout.fragment_teacher_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        textViewWelcome = view.findViewById(R.id.textViewWelcome);
        textViewTeacherInfo = view.findViewById(R.id.textViewTeacherInfo);
        buttonSignOut = view.findViewById(R.id.buttonSignOut);
        buttonManageClasses = view.findViewById(R.id.buttonManageClasses);
        buttonViewReports = view.findViewById(R.id.buttonViewReports);
        buttonMarkAttendance = view.findViewById(R.id.buttonMarkAttendance);

        // Set click listeners
        buttonSignOut.setOnClickListener(v -> signOut());
        buttonManageClasses.setOnClickListener(v -> manageClasses());
        buttonViewReports.setOnClickListener(v -> viewReports());
        buttonMarkAttendance.setOnClickListener(v -> markAttendance());

        // Load teacher data
        loadTeacherData();
    }

    private void loadTeacherData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection("teachers").document(user.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String teacherName = documentSnapshot.getString("teacherName");
                            String employeeId = documentSnapshot.getString("employeeId");
                            String department = documentSnapshot.getString("department");
                            String subject = documentSnapshot.getString("subject");
                            String phoneNumber = documentSnapshot.getString("phoneNumber");

                            textViewWelcome.setText("Welcome, " + (teacherName != null ? teacherName : "Teacher") + "!");
                            
                            StringBuilder info = new StringBuilder();
                            if (employeeId != null) info.append("Employee ID: ").append(employeeId).append("\n");
                            if (department != null) info.append("Department: ").append(department).append("\n");
                            if (subject != null) info.append("Subject: ").append(subject).append("\n");
                            if (phoneNumber != null) info.append("Phone: ").append(phoneNumber);
                            
                            textViewTeacherInfo.setText(info.toString());
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Error loading profile", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void signOut() {
        mAuth.signOut();
        Toast.makeText(getContext(), "Signed out successfully", Toast.LENGTH_SHORT).show();
        NavHostFragment.findNavController(TeacherDashboardFragment.this)
                .navigate(R.id.action_teacherDashboardFragment_to_roleSelectionFragment);
    }

    private void manageClasses() {
        Toast.makeText(getContext(), "Manage Classes feature coming soon!", Toast.LENGTH_SHORT).show();
    }

    private void viewReports() {
        Toast.makeText(getContext(), "View Reports feature coming soon!", Toast.LENGTH_SHORT).show();
    }

    private void markAttendance() {
        Toast.makeText(getContext(), "Mark Attendance feature coming soon!", Toast.LENGTH_SHORT).show();
    }
}