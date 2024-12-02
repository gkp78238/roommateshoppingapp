package edu.uga.cs.roommateshoppingapp;

import android.content.Intent;
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
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

public class MainActivity extends AppCompatActivity {
    private TextInputLayout emailInputLayout, passwordInputLayout;
    private EditText emailInput, passwordInput;
    private Button loginButton, registerButton;
    private ProgressBar progressBar;
    private TextView errorText;
    private FirebaseAuth mAuth;
    private View rootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            mAuth.signOut();
        }

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        rootView = findViewById(android.R.id.content);
        emailInputLayout = findViewById(R.id.emailInputLayout);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);
        progressBar = findViewById(R.id.progressBar);
        errorText = findViewById(R.id.errorText);
    }

    private void setupClickListeners() {
        loginButton.setOnClickListener(v -> validateAndLogin());
        registerButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, RegisterActivity.class)));

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
    }

    private void validateAndLogin() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        boolean isValid = true;

        emailInputLayout.setError(null);
        passwordInputLayout.setError(null);
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

        if (isValid) {
            performLogin(email, password);
        }
    }

    private void performLogin(String email, String password) {
        setLoading(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(this, authResult -> {
                    showSuccessMessage();
                    startActivity(new Intent(MainActivity.this, HomeActivity.class));
                    finish();
                })
                .addOnFailureListener(this, e -> {
                    setLoading(false);
                    handleLoginError(e);
                });
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        loginButton.setEnabled(!isLoading);
        registerButton.setEnabled(!isLoading);
        emailInput.setEnabled(!isLoading);
        passwordInput.setEnabled(!isLoading);
        loginButton.setText(isLoading ? "Signing in..." : "Login");
    }

    private void handleLoginError(Exception e) {
        String message;
        if (e instanceof FirebaseAuthInvalidUserException) {
            String errorCode = ((FirebaseAuthInvalidUserException) e).getErrorCode();
            if (errorCode.equals("ERROR_USER_NOT_FOUND")) {
                message = "No account found with this email.";
                emailInputLayout.setError("No account found");
                passwordInputLayout.setError(null);
            } else {
                message = "Account access error. Please try again.";
            }
        } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
            String errorCode = ((FirebaseAuthInvalidCredentialsException) e).getErrorCode();
            if (errorCode.equals("ERROR_WRONG_PASSWORD")) {
                message = "Invalid password. Please try again.";
                emailInputLayout.setError(null);
                passwordInputLayout.setError("Invalid password");
            } else {
                message = "Invalid credentials. Please check your email and password.";
            }
        } else {
            message = "Login failed. Please check your connection and try again.";
        }
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
    }

    private void showSuccessMessage() {
        Snackbar.make(rootView, "Login successful!", Snackbar.LENGTH_SHORT)
                .setBackgroundTint(getResources().getColor(R.color.success_green))
                .setTextColor(getResources().getColor(R.color.white))
                .show();
    }
}