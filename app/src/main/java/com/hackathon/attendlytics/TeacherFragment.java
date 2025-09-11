package com.hackathon.attendlytics;

import android.graphics.Bitmap;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class TeacherFragment extends Fragment {

    private static final String TAG = "TeacherFragment";
    private ImageView imageViewQrCode;
    private Button buttonStartAttendance;
    private boolean isAttendanceStarted = false;
    private String currentSessionId = null;

    public TeacherFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_teacher, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        imageViewQrCode = view.findViewById(R.id.imageViewQrCode);
        buttonStartAttendance = view.findViewById(R.id.buttonStartAttendance);

        buttonStartAttendance.setOnClickListener(v -> {
            if (!isAttendanceStarted) {
                startAttendanceSession();
            } else {
                stopAttendanceSession();
            }
        });
    }

    private void startAttendanceSession() {
        // Task 2: Generate a random session ID
        currentSessionId = "ATTEND-SESSION-" + System.currentTimeMillis();
        Log.d(TAG, "Generated Session ID: " + currentSessionId);

        // Task 3: Generate a QR code and display it
        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            // You can adjust QR code size here if needed, e.g., barcodeEncoder.encodeBitmap(currentSessionId, BarcodeFormat.QR_CODE, 400, 400);
            Bitmap bitmap = barcodeEncoder.encodeBitmap(currentSessionId, BarcodeFormat.QR_CODE, 250, 250);
            imageViewQrCode.setImageBitmap(bitmap);
            Log.d(TAG, "QR Code generated and displayed.");

            // Update UI
            buttonStartAttendance.setText("Stop Attendance");
            isAttendanceStarted = true;

            // TODO: Task 4: Start BLE advertising will be added here in the next step

        } catch (Exception e) {
            Log.e(TAG, "Error generating QR Code", e);
            // Optionally, show an error to the user
            imageViewQrCode.setImageBitmap(null); // Clear any previous QR code
        }
    }

    private void stopAttendanceSession() {
        // Clear QR Code
        imageViewQrCode.setImageBitmap(null); // Or set a placeholder
        Log.d(TAG, "Attendance session stopped.");

        // Update UI
        buttonStartAttendance.setText("Start Attendance");
        isAttendanceStarted = false;
        currentSessionId = null;

        // TODO: Task 5: Stop BLE advertising will be added here
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (isAttendanceStarted) {
            // Ensure resources are cleaned up if fragment is destroyed while session is active
            stopAttendanceSession();
        }
    }
}
