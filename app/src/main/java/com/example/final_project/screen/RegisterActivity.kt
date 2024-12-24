package com.example.final_project.screen

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.final_project.R
import com.example.final_project.databinding.SignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.random.Random

class RegisterActivity : AppCompatActivity() {
    private val binding: SignUpBinding by lazy {
        SignUpBinding.inflate(layoutInflater)
    }
    private lateinit var auth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()

        // Toggle Password Visibility
        binding.togglePasswordVisibility.setOnClickListener {
            togglePasswordVisibility(binding.txtInputPassword, binding.togglePasswordVisibility)
        }

        // Toggle Confirm Password Visibility
        binding.toggleConfirmPasswordVisibility.setOnClickListener {
            togglePasswordVisibility(binding.txtConfirmPassword, binding.toggleConfirmPasswordVisibility)
        }

        binding.btnSignUp.setOnClickListener {
            val progressDialog = ProgressDialog(this).apply {
                setTitle("SignUp")
                setMessage("Please wait...")
                setCanceledOnTouchOutside(false)
                show()
            }

            val email = binding.txtInputEmail.text.toString().trim()
            val password = binding.txtInputPassword.text.toString().trim()
            val confirmPassword = binding.txtConfirmPassword.text.toString()
            val userName = generateUniqueAnonymousName()

            // Validate inputs
            if (!validateInputs(email, password, confirmPassword, progressDialog)) return@setOnClickListener

            // Check if email exists in Firestore
            firestore.collection("users").whereEqualTo("email", email).get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        createUserWithEmailAndPassword(email, password, userName, progressDialog)
                    } else {
                        Toast.makeText(this, "Email already in use", Toast.LENGTH_SHORT).show()
                        progressDialog.dismiss()
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Error checking email: ${exception.message}", Toast.LENGTH_SHORT).show()
                    progressDialog.dismiss()
                }
        }

        binding.btnBackToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun togglePasswordVisibility(passwordField: EditText, visibilityToggle: ImageView) {
        val isVisible = passwordField.inputType == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        passwordField.inputType = if (isVisible) {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        } else {
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        }
        visibilityToggle.setImageResource(if (isVisible) R.drawable.ic_eye_closed else R.drawable.ic_eye_open)
        passwordField.setSelection(passwordField.text.length)
    }

    private fun validateInputs(email: String, password: String, confirmPassword: String, progressDialog: ProgressDialog): Boolean {
        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in all the details", Toast.LENGTH_SHORT).show()
            progressDialog.dismiss()
            return false
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords must match", Toast.LENGTH_SHORT).show()
            progressDialog.dismiss()
            return false
        }

        if (password.length < 8) {
            Toast.makeText(this, "Password must be at least 8 characters long", Toast.LENGTH_SHORT).show()
            progressDialog.dismiss()
            return false
        }

        val (isStrong, message) = isPasswordStrong(password)
        if (!isStrong) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            progressDialog.dismiss()
            return false
        }

        val emailPattern = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
        if (!email.matches(Regex(emailPattern))) {
            Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show()
            progressDialog.dismiss()
            return false
        }
        return true
    }

    private fun createUserWithEmailAndPassword(email: String, password: String, userName: String, progressDialog: ProgressDialog) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // User registration successful, now save user info to Firestore
                    saveUserInfo(userName,password, email, progressDialog)
                } else {
                    // Handle registration failure
                    Toast.makeText(this, "Registration Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    progressDialog.dismiss()
                }
            }
    }

    private fun saveUserInfo(userName: String,password :String, email: String, progressDialog: ProgressDialog) {
        val currentUserID = auth.currentUser!!.uid
        val userMap = hashMapOf(
            "userName" to userName,
            "uid" to currentUserID,
            "password" to password,
            "email" to email,
            "bio" to ""
        )

        // Save user info to Firestore
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("users").document(currentUserID).set(userMap)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    progressDialog.dismiss()
                    Toast.makeText(this, "Account created successfully.", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, LoginActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                        putExtra("fromRegistration", true) // Add this flag to indicate registration flow
                    }
                    startActivity(intent)
                    finish()
                } else {
                    // Handle Firestore error
                    Toast.makeText(this, "Error saving user info: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    progressDialog.dismiss()
                }
            }
    }

    private fun isPasswordStrong(password: String): Pair<Boolean, String> {
        val lengthRegex = ".{8,}".toRegex()
        val uppercaseRegex = ".*[A-Z].*".toRegex()
        val lowercaseRegex = ".*[a-z].*".toRegex()
        val digitRegex = ".*[0-9].*".toRegex()
        val specialCharRegex = ".*[!@#\$%^&*(),.?\":{}|<>].*".toRegex()

        return when {
            !password.matches(lengthRegex) -> Pair(false, "Password must be at least 8 characters long.")
            !password.matches(uppercaseRegex) -> Pair(false, "Password must contain at least one uppercase letter.")
            !password.matches(lowercaseRegex) -> Pair(false, "Password must contain at least one lowercase letter.")
            !password.matches(digitRegex) -> Pair(false, "Password must contain at least one digit.")
            !password.matches(specialCharRegex) -> Pair(false, "Password must contain at least one special character.")
            else -> Pair(true, "Password is strong.")
        }
    }

    private fun generateUniqueAnonymousName(): String {
        val randomNumber = Random.nextInt(10000, 99999)  // Generates a 5-digit number
        return "Anonymous $randomNumber"
    }
}
