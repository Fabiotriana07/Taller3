package com.example.taller3

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.taller3.databinding.ActivityRegisterBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var storage: FirebaseStorage
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var imageUri: Uri? = null
    private var cameraPhotoUri: Uri? = null

    private val LOCATION_PERMISSION_REQUEST = 100
    private val CAMERA_PERMISSION_REQUEST = 101

    // Registrar resultado de selección de imagen de galería
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            imageUri = result.data?.data
            binding.imgProfile.setImageURI(imageUri)
        }
    }

    // Registrar resultado de captura de foto con cámara
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            cameraPhotoUri?.let { uri ->
                imageUri = uri
                binding.imgProfile.setImageURI(imageUri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase services
        auth = Firebase.auth
        storage = Firebase.storage
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

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

        // Get location button click listener
        binding.btnGetLocation.setOnClickListener {
            requestLocation()
        }

        // Add photo button click listener
        binding.btnAddPhoto.setOnClickListener {
            showImagePickerDialog()
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf(getString(R.string.camera), getString(R.string.gallery), getString(R.string.cancel))

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.picking_image))
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> checkCameraPermissionAndOpenCamera()
                    1 -> openGallery()
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun checkCameraPermissionAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST
            )
        } else {
            openCamera()
        }
    }

    private fun openCamera() {
        val photoFile = createImageFile()
        photoFile?.let { file ->
            cameraPhotoUri = FileProvider.getUriForFile(
                this,
                "${packageName}.provider",
                file
            )

            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraPhotoUri)

            takePictureIntent.resolveActivity(packageManager)?.let {
                cameraLauncher.launch(takePictureIntent)
            }
        }
    }

    private fun createImageFile(): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = getExternalFilesDir(null)

        return File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
        )
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private fun requestLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
        } else {
            getLastLocation()
        }
    }

    private fun getLastLocation() {
        // Verificar si el GPS está habilitado
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, R.string.location_service_disabled, Toast.LENGTH_SHORT).show()
            showEnableLocationDialog()
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        binding.edtLatitude.setText(location.latitude.toString())
                        binding.edtLongitude.setText(location.longitude.toString())
                    } else {
                        Toast.makeText(this, R.string.error_getting_location, Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, R.string.error_getting_location, Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showEnableLocationDialog() {
        AlertDialog.Builder(this)
            .setMessage(R.string.location_service_disabled)
            .setPositiveButton("Activar") { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getLastLocation()
                } else {
                    Toast.makeText(this, R.string.location_permission_denied, Toast.LENGTH_SHORT).show()
                }
            }
            CAMERA_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera()
                }
            }
        }
    }

    private fun validateAndRegister() {
        val firstName = binding.edtFirstName.text.toString().trim()
        val lastName = binding.edtLastName.text.toString().trim()
        val idNumber = binding.edtIdNumber.text.toString().trim()
        val email = binding.edtEmail.text.toString().trim()
        val password = binding.edtPassword.text.toString().trim()
        val confirmPassword = binding.edtConfirmPassword.text.toString().trim()
        val latitude = binding.edtLatitude.text.toString().trim()
        val longitude = binding.edtLongitude.text.toString().trim()

        // Validate inputs
        if (firstName.isEmpty() || lastName.isEmpty() || idNumber.isEmpty() ||
            email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
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

        // Validate location coordinates if provided
        if (latitude.isNotEmpty() || longitude.isNotEmpty()) {
            try {
                val lat = latitude.toDouble()
                val lng = longitude.toDouble()
                if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
                    Toast.makeText(this, getString(R.string.error_invalid_location), Toast.LENGTH_SHORT).show()
                    return
                }
            } catch (e: NumberFormatException) {
                Toast.makeText(this, getString(R.string.error_invalid_location), Toast.LENGTH_SHORT).show()
                return
            }
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
                                // Upload profile image if selected
                                if (imageUri != null) {
                                    uploadProfileImage(user.uid, firstName, lastName, idNumber, email, latitude, longitude)
                                } else {
                                    // Save user data without image
                                    saveUserData(user.uid, firstName, lastName, idNumber, email, latitude, longitude, null)
                                }
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

    private fun uploadProfileImage(userId: String, firstName: String, lastName: String,
                                   idNumber: String, email: String, latitude: String, longitude: String) {
        val storageRef = storage.reference
        val profileImagesRef = storageRef.child("profile_images/$userId.jpg")

        imageUri?.let { uri ->
            profileImagesRef.putFile(uri)
                .addOnSuccessListener {
                    // Get the download URL
                    profileImagesRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        saveUserData(userId, firstName, lastName, idNumber, email, latitude, longitude, downloadUri.toString())
                    }
                }
                .addOnFailureListener { e ->
                    showLoading(false)
                    Toast.makeText(this, getString(R.string.error_upload_image), Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun saveUserData(userId: String, firstName: String, lastName: String,
                             idNumber: String, email: String, latitude: String,
                             longitude: String, profileImageUrl: String?) {
        val db = Firebase.firestore
        val user = hashMapOf(
            "firstName" to firstName,
            "lastName" to lastName,
            "idNumber" to idNumber,
            "email" to email,
            "createdAt" to System.currentTimeMillis()
        )

        // Add location if provided
        if (latitude.isNotEmpty() && longitude.isNotEmpty()) {
            user["latitude"] = latitude.toDouble()
            user["longitude"] = longitude.toDouble()
        }

        // Add profile image URL if available
        if (profileImageUrl != null) {
            user["profileImageUrl"] = profileImageUrl
        }

        db.collection("users").document(userId)
            .set(user)
            .addOnSuccessListener {
                showLoading(false)
                Toast.makeText(
                    this,
                    getString(R.string.success_register) + " Por favor verifica tu correo electrónico",
                    Toast.LENGTH_LONG
                ).show()
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
        binding.edtFirstName.isEnabled = !isLoading
        binding.edtLastName.isEnabled = !isLoading
        binding.edtIdNumber.isEnabled = !isLoading
        binding.edtEmail.isEnabled = !isLoading
        binding.edtPassword.isEnabled = !isLoading
        binding.edtConfirmPassword.isEnabled = !isLoading
        binding.edtLatitude.isEnabled = !isLoading
        binding.edtLongitude.isEnabled = !isLoading
        binding.imgBackArrow.isEnabled = !isLoading
        binding.btnGetLocation.isEnabled = !isLoading
        binding.btnAddPhoto.isEnabled = !isLoading
    }
}