package com.example.taller3

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.taller3.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = Firebase.auth  // Cambiado para que sea consistente con las otras actividades

        setupListeners()
    }

    override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly
        val currentUser = auth.currentUser
        if (currentUser != null && currentUser.isEmailVerified) {
            // Navigate to main activity
            navigateToMain()
        }
    }

    private fun setupListeners() {
        // Login button click listener
        binding.btnLogin.setOnClickListener {
            validateAndLogin()
        }

        // Register text click listener
        binding.txtRegister.setOnClickListener {
            // Navigate to RegisterActivity
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun validateAndLogin() {
        val email = binding.edtEmail.text.toString().trim()
        val password = binding.edtPassword.text.toString().trim()

        // Validate inputs
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_empty_fields), Toast.LENGTH_SHORT).show()
            return
        }

        // Validate email format
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, getString(R.string.error_invalid_email), Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading indicator
        showLoading(true)

        // Attempt login with Firebase
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                showLoading(false)
                if (task.isSuccessful) {
                    // Sign in success
                    val user = auth.currentUser
                    if (user != null) {
                        Toast.makeText(this, getString(R.string.success_login), Toast.LENGTH_SHORT).show()
                        navigateToMain()
                    }

                } else {
                    // If sign in fails, display a message to the user
                    Toast.makeText(this, getString(R.string.error_login), Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !isLoading
        binding.edtEmail.isEnabled = !isLoading
        binding.edtPassword.isEnabled = !isLoading
        binding.txtRegister.isEnabled = !isLoading
    }
}