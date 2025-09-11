package com.hackathon.attendlytics;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment; // Added
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull; // Added
import androidx.annotation.Nullable; // Added
import com.hackathon.attendlytics.databinding.FragmentLoginBinding; // Added

public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding; // Added

    public LoginFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment using ViewBinding
        binding = FragmentLoginBinding.inflate(inflater, container, false); // Modified
        return binding.getRoot(); // Modified
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) { // Added method
        super.onViewCreated(view, savedInstanceState);

        binding.buttonToStudent.setOnClickListener(v -> {
            NavHostFragment.findNavController(LoginFragment.this)
                    .navigate(R.id.action_loginFragment_to_studentFragment);
        });
    }

    @Override
    public void onDestroyView() { // Added method
        super.onDestroyView();
        binding = null; // Clean up binding
    }
}
