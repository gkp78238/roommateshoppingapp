package edu.uga.cs.roommateshoppingapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

public class RegisterActivity extends AppCompatActivity {
    private TextInputLayout emailInputLayout, passwordInputLayout, confirmPasswordInputLayout;
    private EditText emailInput, passwordInput, confirmPasswordInput;
    private Button registerButton;
    private ProgressBar progressBar;
    private TextView errorText;
    private FirebaseAuth mAuth;
    private View rootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        rootView = findViewById(android.R.id.content);
        emailInputLayout = findViewById(R.id.emailInputLayout);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);
        confirmPasswordInputLayout = findViewById(R.id.confirmPasswordInputLayout);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        registerButton = findViewById(R.id.registerButton);
        progressBar = findViewById(R.id.progressBar);
        errorText = findViewById(R.id.errorText);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Create Account");
        }
    }

    private void setupClickListeners() {
        registerButton.setOnClickListener(v -> validateAndRegister());

        emailInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                emailInputLayout.setError(null);
                errorText.setVisibility(View.GONE);
            }
        });

        passwordInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                passwordInputLayout.setError(null);
                errorText.setVisibility(View.GONE);
            }
        });

        confirmPasswordInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                confirmPasswordInputLayout.setError(null);
                errorText.setVisibility(View.GONE);
            }
        });
    }

    private void validateAndRegister() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();
        boolean isValid = true;

        emailInputLayout.setError(null);
        passwordInputLayout.setError(null);
        confirmPasswordInputLayout.setError(null);
        errorText.setVisibility(View.GONE);

        if (email.isEmpty()) {
            emailInputLayout.setError("Email is required");
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.setError("Please enter a valid email address");
            isValid = false;
        }

        if (password.isEmpty()) {
            passwordInputLayout.setError("Password is required");
            isValid = false;
        } else if (password.length() < 6) {
            passwordInputLayout.setError("Password must be at least 6 characters");
            isValid = false;
        }

        if (confirmPassword.isEmpty()) {
            confirmPasswordInputLayout.setError("Please confirm your password");
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            confirmPasswordInputLayout.setError("Passwords do not match");
            isValid = false;
        }

        if (isValid) {
            performRegistration(email, password);
        }
    }

    private void performRegistration(String email, String password) {
        setLoading(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(this, authResult -> {
                    showSuccessMessage();
                    setLoading(false);
                    rootView.postDelayed(this::finish, 1500);
                })
                .addOnFailureListener(this, e -> {
                    setLoading(false);
                    handleRegistrationError(e);
                });
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        registerButton.setEnabled(!isLoading);
        emailInput.setEnabled(!isLoading);
        passwordInput.setEnabled(!isLoading);
        confirmPasswordInput.setEnabled(!isLoading);
        registerButton.setText(isLoading ? "Creating Account..." : "Create Account");
    }

    private void handleRegistrationError(Exception e) {
        String message;
        if (e instanceof FirebaseAuthUserCollisionException) {
            message = "This email is already in use. Please use a different email or try logging in.";
            emailInputLayout.setError("Email already in use");
        } else if (e instanceof FirebaseAuthWeakPasswordException) {
            message = "Password is too weak. Please choose a stronger password.";
            passwordInputLayout.setError("Password too weak");
        } else {
            message = "Registration failed. Please check your connection and try again.";
        }
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
    }

    private void showSuccessMessage() {
        Snackbar.make(rootView, "Account created successfully!", Snackbar.LENGTH_SHORT)
                .setBackgroundTint(getResources().getColor(R.color.success_green))
                .setTextColor(getResources().getColor(R.color.white))
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}