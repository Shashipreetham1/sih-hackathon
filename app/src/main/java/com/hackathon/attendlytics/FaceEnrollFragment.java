package com.hackathon.attendlytics;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.graphics.Rect;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FaceEnrollFragment extends Fragment {

    private static final String TAG = "FaceEnrollFragment";
    private PreviewView previewView;
    private Button buttonCaptureFace;
    private TextView textViewStatus;
    private ProgressBar progressBar;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ExecutorService cameraExecutor;
    private FaceDetector faceDetector;
    private ImageAnalysis imageAnalysis; // Member variable, can be nullified
    private boolean isProcessingFace = false;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startCamera();
                } else {
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), "Camera permission is required for face enrollment.", Toast.LENGTH_LONG).show();
                    }
                }
            });

    public FaceEnrollFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        if (cameraExecutor == null || cameraExecutor.isShutdown()) {
            cameraExecutor = Executors.newSingleThreadExecutor();
        }

        FaceDetectorOptions highAccuracyOpts =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
                        .setMinFaceSize(0.15f)
                        .build();
        faceDetector = FaceDetection.getClient(highAccuracyOpts);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_face_enroll, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        previewView = view.findViewById(R.id.previewViewFaceEnroll);
        buttonCaptureFace = view.findViewById(R.id.buttonCaptureFace);
        textViewStatus = view.findViewById(R.id.textViewFaceEnrollStatus);
        progressBar = view.findViewById(R.id.progressBarFaceEnroll);

        if (cameraExecutor == null || cameraExecutor.isShutdown()) {
            cameraExecutor = Executors.newSingleThreadExecutor();
        }

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }

        if (buttonCaptureFace != null) {
            buttonCaptureFace.setOnClickListener(v -> {
                if (!isProcessingFace) {
                    isProcessingFace = true;
                    if (textViewStatus != null) {
                        textViewStatus.setVisibility(View.VISIBLE);
                        textViewStatus.setText("Processing...");
                    }
                    if (progressBar != null) {
                        progressBar.setVisibility(View.VISIBLE);
                    }
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), "Attempting to capture face...", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private boolean allPermissionsGranted() {
        if (getContext() == null) return false;
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void startCamera() {
        if (getContext() == null) {
            Log.w(TAG, "startCamera: Context is null, cannot get ProcessCameraProvider.");
            return;
        }
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            try {
                if (!isAdded() || getView() == null || !getViewLifecycleOwner().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
                    Log.w(TAG, "startCamera listener: Fragment view not available or lifecycle not STARTED.");
                    // Removed getNow() and unbindAll() from here
                    return;
                }

                ProcessCameraProvider cameraProvider = cameraProviderFuture.get(); // This might block if not ready, but previous check helps
                if (cameraProvider == null) {
                    Log.e(TAG, "startCamera listener: CameraProvider is null.");
                    if (isAdded() && getContext() != null) {
                         Toast.makeText(getContext(), "Failed to initialize camera.", Toast.LENGTH_LONG).show();
                    }
                    return;
                }

                if (previewView == null) {
                    Log.e(TAG, "startCamera listener: PreviewView is null. Aborting camera setup.");
                     if (isAdded() && getContext() != null) {
                         Toast.makeText(getContext(), "Camera preview unavailable. Please try again.", Toast.LENGTH_LONG).show();
                    }
                    cameraProvider.unbindAll();
                    return;
                }

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build();

                if (cameraExecutor == null || cameraExecutor.isShutdown()) {
                     Log.w(TAG, "startCamera listener: cameraExecutor is null or shutdown. Re-initializing.");
                     cameraExecutor = Executors.newSingleThreadExecutor();
                }

                ImageAnalysis localImageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                if (localImageAnalysis == null) {
                    Log.e(TAG, "startCamera listener: ImageAnalysis.Builder().build() returned null. Cannot set analyzer.");
                    cameraProvider.unbindAll();
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), "Failed to setup image analysis component.", Toast.LENGTH_LONG).show();
                    }
                    return;
                }

                localImageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);
                this.imageAnalysis = localImageAnalysis; 
                
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(getViewLifecycleOwner(), cameraSelector, preview, localImageAnalysis);

                if (textViewStatus != null) {
                    textViewStatus.setVisibility(View.VISIBLE);
                    textViewStatus.setText("Position face in camera view");
                } else {
                    Log.w(TAG, "startCamera listener: textViewStatus is null when trying to update text.");
                }

            } catch (Exception e) {
                Log.e(TAG, "Use case binding failed in startCamera listener", e);
                 if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "Failed to start camera: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void analyzeImage(@NonNull ImageProxy imageProxy) {
        if (!isAdded() || faceDetector == null) {
            Log.w(TAG, "analyzeImage: Fragment not added or FaceDetector is null. Closing image proxy.");
            imageProxy.close();
            return;
        }

        @androidx.camera.core.ExperimentalGetImage
        Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());

            if (image == null) {
                Log.e(TAG, "analyzeImage: InputImage is null. Skipping face detection.");
                if (isProcessingFace) {
                    if (textViewStatus != null) {
                        textViewStatus.setText("Error processing image. Try again.");
                    }
                    isProcessingFace = false;
                    if (progressBar != null) {
                         progressBar.setVisibility(View.GONE);
                    }
                }
                imageProxy.close();
                return;
            }

            if (isProcessingFace) {
                Task<List<Face>> result = faceDetector.process(image)
                    .addOnSuccessListener(faces -> {
                        if (!isAdded()) return;
                        if (faces.isEmpty()) {
                            if (textViewStatus != null) textViewStatus.setText("No face detected. Try again.");
                        } else if (faces.size() > 1) {
                            if (textViewStatus != null) textViewStatus.setText("Multiple faces detected. Ensure only one face is visible.");
                        } else {
                            if (textViewStatus != null) textViewStatus.setText("Face detected!");
                            captureFaceAndStore(faces.get(0));
                        }
                        isProcessingFace = false; 
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                    })
                    .addOnFailureListener(e -> {
                        if (!isAdded()) return;
                        Log.e(TAG, "Face detection failed", e);
                        if (textViewStatus != null) textViewStatus.setText("Face detection error. Try again.");
                        isProcessingFace = false;
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                    })
                    .addOnCompleteListener(task -> imageProxy.close());
            } else {
                imageProxy.close();
            }
        } else {
            Log.w(TAG, "analyzeImage: mediaImage is null.");
            if (isProcessingFace) {
                 if (isAdded() && textViewStatus != null) textViewStatus.setText("Error capturing image. Try again.");
                isProcessingFace = false;
                if (isAdded() && progressBar != null) progressBar.setVisibility(View.GONE);
            }
            imageProxy.close();
        }
    }

    private void captureFaceAndStore(Face face) {
        if (!isAdded() || getContext() == null) return;
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "User not authenticated. Please login again.", Toast.LENGTH_LONG).show();
            return;
        }
        String uid = currentUser.getUid();
        
        // Generate face embeddings from detected face features
        List<Float> faceEmbedding = generateFaceEmbedding(face);
        
        if (faceEmbedding.isEmpty()) {
            Toast.makeText(getContext(), "Failed to generate face features. Please try again.", Toast.LENGTH_LONG).show();
            return;
        }
        
        Map<String, Object> faceDataUpdate = new HashMap<>();
        faceDataUpdate.put("faceData", faceEmbedding);
        faceDataUpdate.put("faceEnrollmentTimestamp", System.currentTimeMillis());
        faceDataUpdate.put("faceEnrollmentDate", new java.util.Date());
        faceDataUpdate.put("faceEnrolled", true);
        faceDataUpdate.put("faceEmbeddingSize", faceEmbedding.size());
        faceDataUpdate.put("faceEnrollmentMethod", "ml_kit_landmarks");
        faceDataUpdate.put("lastFaceUpdateTimestamp", System.currentTimeMillis());
        
        Log.d(TAG, "Storing face enrollment data for user: " + uid);
        Log.d(TAG, "Face embedding contains " + faceEmbedding.size() + " features");

        // Use set with merge option instead of update for new users
        db.collection("users").document(uid)
                .set(faceDataUpdate, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    if (!isAdded() || getContext() == null) return;
                    Log.d(TAG, "Face enrollment successful for user: " + uid + " with " + faceEmbedding.size() + " features");
                    Toast.makeText(getContext(), "Face enrollment successful! " + faceEmbedding.size() + " features captured.", Toast.LENGTH_LONG).show();
                    NavHostFragment.findNavController(FaceEnrollFragment.this)
                        .navigate(R.id.action_faceEnrollFragment_to_captchaFragment);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || getContext() == null) return;
                    Log.w(TAG, "Error writing face data to Firestore", e);
                    
                    String errorMessage = e.getMessage();
                    if (errorMessage != null && errorMessage.contains("PERMISSION_DENIED")) {
                        if (errorMessage.contains("Cloud Firestore API has not been used")) {
                            Toast.makeText(getContext(), "⚠️ Firestore API not enabled. Please enable it in Firebase Console.\n\nFor now, continuing without cloud storage...", Toast.LENGTH_LONG).show();
                            // Continue to next step even without cloud storage
                            Log.d(TAG, "Face embedding generated locally with " + faceEmbedding.size() + " features");
                            NavHostFragment.findNavController(FaceEnrollFragment.this)
                                .navigate(R.id.action_faceEnrollFragment_to_captchaFragment);
                        } else {
                            Toast.makeText(getContext(), "⚠️ Database permission denied. Check Firestore rules.\n\nContinuing locally...", Toast.LENGTH_LONG).show();
                            NavHostFragment.findNavController(FaceEnrollFragment.this)
                                .navigate(R.id.action_faceEnrollFragment_to_captchaFragment);
                        }
                    } else {
                        Toast.makeText(getContext(), "Failed to save face data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private List<Float> generateFaceEmbedding(Face face) {
        List<Float> embedding = new ArrayList<>();
        
        try {
            // Get face bounding box coordinates
            Rect bounds = face.getBoundingBox();
            embedding.add((float) bounds.left);
            embedding.add((float) bounds.top);
            embedding.add((float) bounds.right);
            embedding.add((float) bounds.bottom);
            
            // Add face rotation angles
            embedding.add(face.getHeadEulerAngleX()); // Pitch
            embedding.add(face.getHeadEulerAngleY()); // Yaw
            embedding.add(face.getHeadEulerAngleZ()); // Roll
            
            // Add landmark positions if available
            FaceLandmark leftEye = face.getLandmark(FaceLandmark.LEFT_EYE);
            if (leftEye != null) {
                PointF leftEyePos = leftEye.getPosition();
                embedding.add(leftEyePos.x);
                embedding.add(leftEyePos.y);
            } else {
                embedding.add(0f);
                embedding.add(0f);
            }
            
            FaceLandmark rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE);
            if (rightEye != null) {
                PointF rightEyePos = rightEye.getPosition();
                embedding.add(rightEyePos.x);
                embedding.add(rightEyePos.y);
            } else {
                embedding.add(0f);
                embedding.add(0f);
            }
            
            FaceLandmark nose = face.getLandmark(FaceLandmark.NOSE_BASE);
            if (nose != null) {
                PointF nosePos = nose.getPosition();
                embedding.add(nosePos.x);
                embedding.add(nosePos.y);
            } else {
                embedding.add(0f);
                embedding.add(0f);
            }
            
            FaceLandmark mouth = face.getLandmark(FaceLandmark.MOUTH_BOTTOM);
            if (mouth != null) {
                PointF mouthPos = mouth.getPosition();
                embedding.add(mouthPos.x);
                embedding.add(mouthPos.y);
            } else {
                embedding.add(0f);
                embedding.add(0f);
            }
            
            FaceLandmark leftEar = face.getLandmark(FaceLandmark.LEFT_EAR);
            if (leftEar != null) {
                PointF leftEarPos = leftEar.getPosition();
                embedding.add(leftEarPos.x);
                embedding.add(leftEarPos.y);
            } else {
                embedding.add(0f);
                embedding.add(0f);
            }
            
            FaceLandmark rightEar = face.getLandmark(FaceLandmark.RIGHT_EAR);
            if (rightEar != null) {
                PointF rightEarPos = rightEar.getPosition();
                embedding.add(rightEarPos.x);
                embedding.add(rightEarPos.y);
            } else {
                embedding.add(0f);
                embedding.add(0f);
            }
            
            FaceLandmark leftCheek = face.getLandmark(FaceLandmark.LEFT_CHEEK);
            if (leftCheek != null) {
                PointF leftCheekPos = leftCheek.getPosition();
                embedding.add(leftCheekPos.x);
                embedding.add(leftCheekPos.y);
            } else {
                embedding.add(0f);
                embedding.add(0f);
            }
            
            FaceLandmark rightCheek = face.getLandmark(FaceLandmark.RIGHT_CHEEK);
            if (rightCheek != null) {
                PointF rightCheekPos = rightCheek.getPosition();
                embedding.add(rightCheekPos.x);
                embedding.add(rightCheekPos.y);
            } else {
                embedding.add(0f);
                embedding.add(0f);
            }
            
            // Add face dimensions as ratios for better comparison
            float faceWidth = bounds.width();
            float faceHeight = bounds.height();
            embedding.add(faceWidth);
            embedding.add(faceHeight);
            embedding.add(faceWidth / faceHeight); // Aspect ratio
            
            // Add confidence score if available
            if (face.getTrackingId() != null) {
                embedding.add((float) face.getTrackingId());
            } else {
                embedding.add(1.0f); // Default confidence
            }
            
            // Add classification features (smile, eye open probabilities)
            float leftEyeOpenProb = face.getLeftEyeOpenProbability() != null ? 
                face.getLeftEyeOpenProbability() : 0.5f;
            float rightEyeOpenProb = face.getRightEyeOpenProbability() != null ? 
                face.getRightEyeOpenProbability() : 0.5f;
            float smilingProb = face.getSmilingProbability() != null ? 
                face.getSmilingProbability() : 0.5f;
                
            embedding.add(leftEyeOpenProb);
            embedding.add(rightEyeOpenProb);
            embedding.add(smilingProb);
            
            Log.d(TAG, "Generated face embedding with " + embedding.size() + " features");
            return embedding;
            
        } catch (Exception e) {
            Log.e(TAG, "Error generating face embedding", e);
            return new ArrayList<>();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        try {
            if (cameraProviderFuture != null && cameraProviderFuture.isDone()) {
                // Use get() as isDone() is true; handle potential exceptions.
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get(); 
                if (cameraProvider != null) {
                    cameraProvider.unbindAll();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error unbinding camera provider in onDestroyView", e);
        }

        if (cameraExecutor != null) {
            if (!cameraExecutor.isShutdown()) {
                cameraExecutor.shutdown();
            }
            cameraExecutor = null;
        }
        faceDetector = null;
        
        previewView = null;
        buttonCaptureFace = null;
        textViewStatus = null;
        progressBar = null;
        imageAnalysis = null;
        cameraProviderFuture = null;
    }
}
