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

public class RoleSelectionFragment extends Fragment {

    private Button buttonStudent, buttonTeacher;

    public RoleSelectionFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_role_selection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        buttonStudent = view.findViewById(R.id.buttonStudent);
        buttonTeacher = view.findViewById(R.id.buttonTeacher);

        // Set click listeners
        buttonStudent.setOnClickListener(v -> selectStudentRole());
        buttonTeacher.setOnClickListener(v -> selectTeacherRole());
    }

    private void selectStudentRole() {
        // Pass role to the next fragment using Bundle
        Bundle bundle = new Bundle();
        bundle.putString("userRole", "student");
        NavHostFragment.findNavController(RoleSelectionFragment.this)
                .navigate(R.id.action_roleSelectionFragment_to_authSelectionFragment, bundle);
    }

    private void selectTeacherRole() {
        // Pass role to the next fragment using Bundle
        Bundle bundle = new Bundle();
        bundle.putString("userRole", "teacher");
        NavHostFragment.findNavController(RoleSelectionFragment.this)
                .navigate(R.id.action_roleSelectionFragment_to_authSelectionFragment, bundle);
    }
}