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
import androidx.navigation.fragment.NavHostFragment;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FaceVerificationFragment extends Fragment {

    private static final String TAG = "FaceVerificationFragment";
    private static final double SIMILARITY_THRESHOLD = 0.85; // Adjust based on testing
    
    private PreviewView previewView;
    private Button buttonVerifyFace;
    private Button buttonSkipVerification;
    private TextView textViewStatus;
    private ProgressBar progressBar;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ExecutorService cameraExecutor;
    private FaceDetector faceDetector;
    private ImageAnalysis imageAnalysis;
    private boolean isProcessingFace = false;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Data passed from StudentFragment
    private String sessionId;
    private String detectionMethod;
    private List<Float> storedFaceEmbedding;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startCamera();
                } else {
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), "Camera permission is required for face verification.", Toast.LENGTH_LONG).show();
                    }
                }
            });

    public FaceVerificationFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        // Get arguments passed from StudentFragment
        if (getArguments() != null) {
            sessionId = getArguments().getString("sessionId");
            detectionMethod = getArguments().getString("detectionMethod");
        }
        
        if (cameraExecutor == null || cameraExecutor.isShutdown()) {
            cameraExecutor = Executors.newSingleThreadExecutor();
        }

        FaceDetectorOptions highAccuracyOpts =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
                        .build();

        faceDetector = FaceDetection.getClient(highAccuracyOpts);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_face_verification, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        previewView = view.findViewById(R.id.previewViewVerification);
        buttonVerifyFace = view.findViewById(R.id.buttonVerifyFace);
        buttonSkipVerification = view.findViewById(R.id.buttonSkipVerification);
        textViewStatus = view.findViewById(R.id.textViewVerificationStatus);
        progressBar = view.findViewById(R.id.progressBarVerification);

        buttonVerifyFace.setOnClickListener(v -> captureFaceForVerification());
        buttonSkipVerification.setOnClickListener(v -> skipVerification());

        textViewStatus.setText("📷 Position your face in the camera to verify your identity");

        // Load stored face embedding
        loadStoredFaceEmbedding();
    }

    private void loadStoredFaceEmbedding() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Authentication error", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        textViewStatus.setText("Loading your stored face data...");

        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    progressBar.setVisibility(View.GONE);
                    
                    if (documentSnapshot.exists()) {
                        List<Double> faceData = (List<Double>) documentSnapshot.get("faceData");
                        
                        if (faceData != null && !faceData.isEmpty()) {
                            // Convert Double list to Float list
                            storedFaceEmbedding = new ArrayList<>();
                            for (Double value : faceData) {
                                storedFaceEmbedding.add(value.floatValue());
                            }
                            
                            Log.d(TAG, "Loaded stored face embedding with " + storedFaceEmbedding.size() + " features");
                            textViewStatus.setText("✅ Face data loaded. Ready for verification!");
                            
                            // Check camera permission and start camera
                            if (checkCameraPermission()) {
                                startCamera();
                            } else {
                                requestCameraPermission();
                            }
                        } else {
                            // No stored face data
                            textViewStatus.setText("⚠️ No face data found. You can skip verification or enroll your face first.");
                            buttonSkipVerification.setVisibility(View.VISIBLE);
                        }
                    } else {
                        textViewStatus.setText("⚠️ Profile not found. You can skip verification.");
                        buttonSkipVerification.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Error loading face data", e);
                    textViewStatus.setText("❌ Error loading face data. You can skip verification.");
                    buttonSkipVerification.setVisibility(View.VISIBLE);
                });
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) 
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        requestPermissionLauncher.launch(Manifest.permission.CAMERA);
    }

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (Exception e) {
                Log.e(TAG, "Error starting camera", e);
                Toast.makeText(getContext(), "Error starting camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, new FaceAnalyzer());

        try {
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
        } catch (Exception exc) {
            Log.e(TAG, "Use case binding failed", exc);
        }
    }

    private void captureFaceForVerification() {
        if (storedFaceEmbedding == null || storedFaceEmbedding.isEmpty()) {
            Toast.makeText(getContext(), "No stored face data available for verification", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isProcessingFace) {
            return;
        }

        buttonVerifyFace.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        textViewStatus.setText("📸 Capturing face for verification...");
        isProcessingFace = true;
    }

    private void skipVerification() {
        // Proceed to attendance marking without face verification
        proceedToAttendanceMarking(false);
    }

    private void proceedToAttendanceMarking(boolean faceVerified) {
        Bundle result = new Bundle();
        result.putString("sessionId", sessionId);
        result.putString("detectionMethod", detectionMethod);
        result.putBoolean("faceVerified", faceVerified);
        
        // Navigate back to StudentFragment with results
        getParentFragmentManager().setFragmentResult("faceVerificationResult", result);
        NavHostFragment.findNavController(this).popBackStack();
    }

    private class FaceAnalyzer implements ImageAnalysis.Analyzer {
        @Override
        public void analyze(@NonNull ImageProxy imageProxy) {
            if (!isProcessingFace) {
                imageProxy.close();
                return;
            }

            @androidx.camera.core.ExperimentalGetImage
            Image mediaImage = imageProxy.getImage();
            if (mediaImage != null) {
                InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
                
                faceDetector.process(image)
                        .addOnSuccessListener(faces -> {
                            if (faces.size() > 0) {
                                Face face = faces.get(0); // Use the first detected face
                                
                                requireActivity().runOnUiThread(() -> {
                                    if (isAdded() && getContext() != null) {
                                        verifyFace(face);
                                    }
                                });
                            } else {
                                requireActivity().runOnUiThread(() -> {
                                    if (isAdded() && getContext() != null) {
                                        textViewStatus.setText("❌ No face detected. Please position your face in the camera.");
                                        progressBar.setVisibility(View.GONE);
                                        buttonVerifyFace.setEnabled(true);
                                        isProcessingFace = false;
                                    }
                                });
                            }
                            imageProxy.close();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Face detection failed", e);
                            requireActivity().runOnUiThread(() -> {
                                if (isAdded() && getContext() != null) {
                                    textViewStatus.setText("❌ Face detection failed. Please try again.");
                                    progressBar.setVisibility(View.GONE);
                                    buttonVerifyFace.setEnabled(true);
                                    isProcessingFace = false;
                                }
                            });
                            imageProxy.close();
                        });
            } else {
                imageProxy.close();
            }
        }
    }

    private void verifyFace(Face detectedFace) {
        progressBar.setVisibility(View.VISIBLE);
        textViewStatus.setText("🔍 Comparing face features...");

        // Generate embedding for detected face
        List<Float> currentEmbedding = generateFaceEmbedding(detectedFace);
        
        if (currentEmbedding.isEmpty()) {
            textViewStatus.setText("❌ Failed to analyze face features. Please try again.");
            progressBar.setVisibility(View.GONE);
            buttonVerifyFace.setEnabled(true);
            isProcessingFace = false;
            return;
        }

        // Calculate similarity between stored and current embedding
        double similarity = calculateSimilarity(storedFaceEmbedding, currentEmbedding);
        
        Log.d(TAG, "Face similarity score: " + similarity);
        
        progressBar.setVisibility(View.GONE);
        isProcessingFace = false;
        
        if (similarity >= SIMILARITY_THRESHOLD) {
            textViewStatus.setText("✅ Face verified successfully! (Similarity: " + String.format("%.2f", similarity * 100) + "%)");
            Toast.makeText(getContext(), "Face verification successful!", Toast.LENGTH_SHORT).show();
            
            // Proceed to attendance marking with verification
            proceedToAttendanceMarking(true);
        } else {
            textViewStatus.setText("❌ Face verification failed. (Similarity: " + String.format("%.2f", similarity * 100) + "%)");
            Toast.makeText(getContext(), "Face verification failed. You can try again or skip verification.", Toast.LENGTH_LONG).show();
            buttonVerifyFace.setEnabled(true);
            buttonSkipVerification.setVisibility(View.VISIBLE);
        }
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

    private double calculateSimilarity(List<Float> stored, List<Float> current) {
        if (stored.size() != current.size()) {
            Log.w(TAG, "Embedding size mismatch: stored=" + stored.size() + ", current=" + current.size());
            return 0.0;
        }

        // Calculate cosine similarity
        double dotProduct = 0.0;
        double normStored = 0.0;
        double normCurrent = 0.0;

        for (int i = 0; i < stored.size(); i++) {
            dotProduct += stored.get(i) * current.get(i);
            normStored += Math.pow(stored.get(i), 2);
            normCurrent += Math.pow(current.get(i), 2);
        }

        if (normStored == 0.0 || normCurrent == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normStored) * Math.sqrt(normCurrent));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        try {
            if (cameraProviderFuture != null && cameraProviderFuture.isDone()) {
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
        
        if (faceDetector != null) {
            faceDetector = null;
        }
        
        previewView = null;
        buttonVerifyFace = null;
        buttonSkipVerification = null;
        textViewStatus = null;
        progressBar = null;
        imageAnalysis = null;
        cameraProviderFuture = null;
    }
}