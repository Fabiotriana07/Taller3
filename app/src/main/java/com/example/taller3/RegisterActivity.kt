package com.example.taller3

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.taller3.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = Firebase.auth

        setupListeners()
    }

    private fun setupListeners() {
        // Back arrow click listener
        binding.imgBackArrow.setOnClickListener {
            onBackPressed()
        }

        // Register button click listener
        binding.btnRegister.setOnClickListener {
            validateAndRegister()
        }
    }

    private fun validateAndRegister() {
        val name = binding.edtName.text.toString().trim()
        val email = binding.edtEmail.text.toString().trim()
        val password = binding.edtPassword.text.toString().trim()
        val confirmPassword = binding.edtConfirmPassword.text.toString().trim()

        // Validate inputs
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_empty_fields), Toast.LENGTH_SHORT).show()
            return
        }

        // Validate email format
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, getString(R.string.error_invalid_email), Toast.LENGTH_SHORT).show()
            return
        }

        // Check if passwords match
        if (password != confirmPassword) {
            Toast.makeText(this, getString(R.string.error_password_match), Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading indicator
        showLoading(true)

        // Create user with Firebase
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign up success, update UI with the signed-in user's information
                    val user = auth.currentUser

                    // Send email verification
                    user?.sendEmailVerification()
                        ?.addOnCompleteListener { verificationTask ->
                            if (verificationTask.isSuccessful) {
                                // Save additional user information to Firestore
                                saveUserData(user.uid, name, email)
                            } else {
                                showLoading(false)
                                Toast.makeText(this, "Error al enviar email de verificación", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    // If sign up fails, display a message to the user
                    showLoading(false)
                    Toast.makeText(this, getString(R.string.error_register), Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveUserData(userId: String, name: String, email: String) {
        val db = Firebase.firestore
        val user = hashMapOf(
            "name" to name,
            "email" to email,
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("users").document(userId)
            .set(user)
            .addOnSuccessListener {
                showLoading(false)
                Toast.makeText(this, getString(R.string.success_register) + " Por favor verifica tu correo electrónico", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !isLoading
        binding.edtName.isEnabled = !isLoading
        binding.edtEmail.isEnabled = !isLoading
        binding.edtPassword.isEnabled = !isLoading
        binding.edtConfirmPassword.isEnabled = !isLoading
        binding.imgBackArrow.isEnabled = !isLoading
    }
}