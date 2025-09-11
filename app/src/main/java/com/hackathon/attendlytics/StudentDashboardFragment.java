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

public class StudentDashboardFragment extends Fragment {

    private TextView textViewWelcome, textViewStudentInfo;
    private Button buttonSignOut, buttonViewAttendance, buttonMarkAttendance;
    
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    public StudentDashboardFragment() {
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
        return inflater.inflate(R.layout.fragment_student_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        textViewWelcome = view.findViewById(R.id.textViewWelcome);
        textViewStudentInfo = view.findViewById(R.id.textViewStudentInfo);
        buttonSignOut = view.findViewById(R.id.buttonSignOut);
        buttonViewAttendance = view.findViewById(R.id.buttonViewAttendance);
        buttonMarkAttendance = view.findViewById(R.id.buttonMarkAttendance);

        // Set click listeners
        buttonSignOut.setOnClickListener(v -> signOut());
        buttonViewAttendance.setOnClickListener(v -> viewAttendance());
        buttonMarkAttendance.setOnClickListener(v -> markAttendance());

        // Load user data
        loadUserData();
    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String studentName = documentSnapshot.getString("studentName");
                            String studentId = documentSnapshot.getString("studentId");
                            String department = documentSnapshot.getString("department");
                            String year = documentSnapshot.getString("year");
                            String section = documentSnapshot.getString("section");

                            textViewWelcome.setText("Welcome, " + (studentName != null ? studentName : "Student") + "!");
                            
                            StringBuilder info = new StringBuilder();
                            if (studentId != null) info.append("ID: ").append(studentId).append("\n");
                            if (department != null) info.append("Department: ").append(department).append("\n");
                            if (year != null) info.append("Year: ").append(year).append("\n");
                            if (section != null) info.append("Section: ").append(section);
                            
                            textViewStudentInfo.setText(info.toString());
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
        NavHostFragment.findNavController(StudentDashboardFragment.this)
                .navigate(R.id.action_studentDashboardFragment_to_roleSelectionFragment);
    }

    private void viewAttendance() {
        Toast.makeText(getContext(), "View Attendance feature coming soon!", Toast.LENGTH_SHORT).show();
    }

    private void markAttendance() {
        Toast.makeText(getContext(), "Mark Attendance feature coming soon!", Toast.LENGTH_SHORT).show();
    }
}
