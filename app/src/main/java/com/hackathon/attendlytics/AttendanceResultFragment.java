package com.hackathon.attendlytics;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AttendanceResultFragment extends Fragment {

    private TextView textViewResultTitle;
    private TextView textViewResultMessage;
    private TextView textViewSessionInfo;
    private TextView textViewTimestamp;
    private TextView textViewMethod;
    private Button buttonBackToStudent;
    private ImageView imageViewResult;

    private String sessionId;
    private String method;
    private boolean success;

    public AttendanceResultFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Get arguments passed from previous fragment
        if (getArguments() != null) {
            sessionId = getArguments().getString("sessionId", "Unknown");
            method = getArguments().getString("method", "Unknown");
            success = getArguments().getBoolean("success", false);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_attendance_result, container, false);
        
        initializeViews(view);
        setupContent();
        setupClickListeners();
        
        return view;
    }

    private void initializeViews(View view) {
        textViewResultTitle = view.findViewById(R.id.textViewResultTitle);
        textViewResultMessage = view.findViewById(R.id.textViewResultMessage);
        textViewSessionInfo = view.findViewById(R.id.textViewSessionInfo);
        textViewTimestamp = view.findViewById(R.id.textViewTimestamp);
        textViewMethod = view.findViewById(R.id.textViewMethod);
        buttonBackToStudent = view.findViewById(R.id.buttonBackToStudent);
        imageViewResult = view.findViewById(R.id.imageViewResult);
    }

    private void setupContent() {
        if (success) {
            // Success state
            textViewResultTitle.setText("Attendance Marked!");
            textViewResultMessage.setText("Your attendance has been successfully recorded.");
            imageViewResult.setImageResource(android.R.drawable.ic_dialog_info);
        } else {
            // Error state
            textViewResultTitle.setText("Attendance Failed");
            textViewResultMessage.setText("Could not mark your attendance. Please try again.");
            imageViewResult.setImageResource(android.R.drawable.ic_dialog_alert);
        }

        // Set session info
        textViewSessionInfo.setText("Session: " + sessionId);
        
        // Set current timestamp
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
        String timestamp = dateFormat.format(new Date());
        textViewTimestamp.setText("Time: " + timestamp);
        
        // Set method
        textViewMethod.setText("Method: " + method);
    }

    private void setupClickListeners() {
        buttonBackToStudent.setOnClickListener(v -> {
            // Navigate back to student fragment
            try {
                NavHostFragment.findNavController(this)
                    .navigate(R.id.action_attendanceResultFragment_to_studentFragment);
            } catch (Exception e) {
                // Fallback: pop back stack
                NavHostFragment.findNavController(this).popBackStack();
            }
        });
    }
}