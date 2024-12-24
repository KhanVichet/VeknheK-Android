package com.example.final_project.screen

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.final_project.R
import com.example.final_project.databinding.LoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser


class LoginActivity : AppCompatActivity() {
    private val binding: LoginBinding by lazy {
        LoginBinding.inflate(layoutInflater)
    }

    private lateinit var auth: FirebaseAuth

    override fun onStart(){
        super.onStart()
        val fromRegistration = intent.getBooleanExtra("fromRegistration", false)

        val currentUser: FirebaseUser? = auth.currentUser
        if (currentUser != null && !fromRegistration) {
            // Redirect to MainActivity if the user is already logged in (except after registration)
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()


        binding.togglePasswordVisibility.setOnClickListener{
            togglePasswordVisibility(binding.txtPassword, binding.togglePasswordVisibility)
        }

        binding.CreateNewAccount.setOnClickListener{
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }
        binding.btnLogin.setOnClickListener{
            Login()

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
    private fun Login(){
        val email = binding.txtEmail.text.toString()
        val password = binding.txtPassword.text.toString()

        if(email.isEmpty() || password.isEmpty()){
            Toast.makeText(this,"Please Fill All The Detail", Toast.LENGTH_SHORT).show()
        }

        val emailPattern = "[a-zA-Z0-9._%+-]+@gmail\\.com"
        if (!email.matches(Regex(emailPattern))) {
            Toast.makeText(this, "Please enter a valid Gmail address", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email,password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    showLoginErrorDialog()
                    Toast.makeText(this, "Login Fail", Toast.LENGTH_SHORT).show()
                }
            }
    }
    private fun showLoginErrorDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.invalid_login_fail)
        val btnOk = dialog.findViewById<Button>(R.id.btnOk)
        btnOk.setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

}
