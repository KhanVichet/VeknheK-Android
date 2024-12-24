package com.example.final_project.screen

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import com.example.final_project.R
import com.example.final_project.databinding.EditProfileBinding
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore


class UserInformation : AppCompatActivity() {
    private val binding: EditProfileBinding by lazy { EditProfileBinding.inflate(layoutInflater) }


    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        UserInfo() { userName ,email,bio ->
            if (userName != null && email != null || bio != null) {
                binding.txtUserName.text = userName
                binding.txtEmail.text = email
                binding.txtBio.text = Editable.Factory.getInstance().newEditable(bio)
            }
        }

        binding.btnSignUp.setOnClickListener{
            updateProfile()
        }
        binding.btnBackToHomePage.setOnClickListener{
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        val toolbar : Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (menu is MenuBuilder) {
            menu.setOptionalIconsVisible(true)
        }
        menuInflater.inflate(R.menu.option_menu_item, menu)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId){
            R.id.item1 -> startActivity(Intent(this,ProfileActivity::class.java))
            R.id.item2 -> startActivity(Intent(this,UserInformation::class.java))
            R.id.item3 ->logout()
        }
        return super.onOptionsItemSelected(item)
    }
    private fun logout(){
        FirebaseAuth.getInstance().signOut()

        val loginIntent = Intent(this, LoginActivity::class.java)
        loginIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(loginIntent)

        finish()
    }
    private fun UserInfo(callback: (String?,String?,String?) -> Unit) {
        val currentUser = auth.currentUser
        val currentUserId = currentUser?.uid

        if (currentUserId != null) {
            val userDocRef = firestore.collection("users").document(currentUserId)

            userDocRef.get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val userName = document.getString("userName")
                        val email = document.getString("email")
                        val bio = document.getString("bio")
                        callback(userName,email,bio)
                    }
                }

        } else {
            callback(null,null,null)
        }
    }

    private fun updateProfile() {
        val currentUser = auth.currentUser
        val currentUserId = currentUser?.uid

        if (currentUserId == null) {
            Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show()
            return
        }

        val userRef = firestore.collection("users").document(currentUserId)

        val newBio = binding.txtBio.text.toString().trim()
        val currentPassword = binding.txtCurrentPassword.text.toString()
        val newPassword = binding.txtPasswordChange.text.toString()
        val confirmPassword = binding.txtConfirmPasswordChange.text.toString()

        // Update bio
        updateBio(userRef, newBio)


        // Update password
        if (newPassword.isNotEmpty() && confirmPassword.isNotEmpty()) {
            updatePassword(userRef, currentPassword, newPassword, confirmPassword)
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    // Function to update the bio
    private fun updateBio(userRef: DocumentReference, newBio: String) {

        userRef.get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val currentBio = document.getString("bio")

                if (newBio != currentBio) {
                    userRef.update("bio", newBio).addOnSuccessListener {
                        Toast.makeText(this, "Bio updated successfully.", Toast.LENGTH_SHORT).show()
                    }.addOnFailureListener {
                        Toast.makeText(this, "Failed to update bio.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to fetch current bio.", Toast.LENGTH_SHORT).show()
        }
    }
    private fun updatePassword(
        userRef: DocumentReference,
        currentPassword: String,
        newPassword: String,
        confirmPassword: String
    ) {
        if (newPassword != confirmPassword) {
            Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword.length < 8) {
            Toast.makeText(this, "Password must be at least 8 characters long.", Toast.LENGTH_SHORT).show()
            return
        }

        val (isStrong, message) = isPasswordStrong(newPassword)
        if (!isStrong) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            return
        }
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)
            user.reauthenticate(credential).addOnCompleteListener { reauthTask ->
                if (reauthTask.isSuccessful) {
                    user.updatePassword(newPassword).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            userRef.update("password", newPassword).addOnSuccessListener {
                                Toast.makeText(this, "Password updated successfully.", Toast.LENGTH_SHORT).show()
                            }.addOnFailureListener {
                                Toast.makeText(this, "Failed to update password in Firestore.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(
                                this,
                                "Failed to update password: ${task.exception?.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Reauthentication failed.", Toast.LENGTH_SHORT).show()
                }
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

}