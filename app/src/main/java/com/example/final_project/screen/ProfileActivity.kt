package com.example.final_project.screen

import NewsfeedAdapterProfile
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.final_project.Model.Newsfeed
import com.example.final_project.R
import com.example.final_project.databinding.ProfileBinding
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ProfileActivity : AppCompatActivity() {

    private val binding: ProfileBinding by lazy {
        ProfileBinding.inflate(layoutInflater)
    }
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var adapter: NewsfeedAdapterProfile


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
//        recyclerView = binding.newsfeedRecyclerView
//        recyclerView.layoutManager = LinearLayoutManager(this)
        setupRecyclerView()

        loadUserInfo()


        binding.btnBackToHomePage.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        binding.btnUpdateProfile.setOnClickListener{
            startActivity(Intent(this,UserInformation::class.java))
            finish()
        }
        val toolbar : Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)


    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Enable icons in the overflow menu
        if (menu is MenuBuilder) {
            menu.setOptionalIconsVisible(true) // Pass 'true' to enable icons
        }

        // Inflate the menu resource
        menuInflater.inflate(R.menu.option_menu_item, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId){
            R.id.item1 -> startActivity(Intent(this,ProfileActivity::class.java))
            R.id.item2 -> startActivity(Intent(this,UserInformation::class.java))
            R.id.item3 -> logout()
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

    private fun loadUserInfo() {
        getUserInformation { userName, bio ->
            binding.txtUserName.text = userName ?: "Username not available"
            binding.txtBio.text = bio ?: "Bio not available"
        }
        loadUserPostCount()
        loadUserLikes()
    }

    private fun loadUserPostCount() {
        countUserPosts { count ->
            binding.txtCountPost.text = if (count != 0) count.toString() else "0"
        }
    }
    private fun loadUserLikes(){
        countLikesOnUserPosts{ txtlikes ->
            binding.txtTotalLike.text = if (txtlikes != 0 ) txtlikes.toString() else  "0"
        }
    }

    private fun getUserInformation(callback: (String?, String?) -> Unit) {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            Log.e("ProfileActivity", "User not authenticated.")
            callback(null, null)
            return
        }

        firestore.collection("users").document(currentUserId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val userName = document.getString("userName")
                    val bio = document.getString("bio")
                    callback(userName, bio)
                } else {
                    Log.e("ProfileActivity", "User document does not exist.")
                    callback(null, null)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("ProfileActivity", "Error fetching user information: ", exception)
                callback(null, null)
            }
    }

    private fun countUserPosts(callback: (Int) -> Unit) {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            Log.e("ProfileActivity", "User not authenticated.")
            callback(0)
            return
        }

        firestore.collection("newsfeed")
            .whereEqualTo("userId", currentUserId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                callback(querySnapshot.size())
            }
            .addOnFailureListener { exception ->
                Log.e("ProfileActivity", "Error fetching user's posts: ", exception)
                callback(0)
            }
    }
    private fun countLikesOnUserPosts(callback: (Int) -> Unit) {
        val currentUserId = auth.currentUser?.uid

        if (currentUserId == null) {
            callback(0)
            return
        }

        firestore.collection("newsfeed")
            .whereEqualTo("userId", currentUserId) // Filter posts created by the current user
            .get()
            .addOnSuccessListener { querySnapshot ->
                var totalLikes = 0
                for (document in querySnapshot) {
                    val likes = document.getLong("likes") ?: 0 // Assuming 'likes' is a field storing the count
                    totalLikes += likes.toInt()
                }
                callback(totalLikes)
            }
            .addOnFailureListener { exception ->
                Log.e("NewsfeedAdapter", "Error fetching likes: ", exception)
                callback(0)
            }
    }


    private fun setupRecyclerView() {
        val currentUserId = auth.currentUser?.uid
        val query = firestore.collection("newsfeed")
            .whereEqualTo("userId",currentUserId)
            .orderBy("createdAt", Query.Direction.DESCENDING)

        val options = FirestoreRecyclerOptions.Builder<Newsfeed>()
            .setQuery(query, Newsfeed::class.java)
            .build()

        adapter = NewsfeedAdapterProfile(options)
        binding.newsfeedRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ProfileActivity)
            adapter = this@ProfileActivity.adapter
        }
        adapter.startListening()
        loadUserInfo()

    }

    override fun onStart() {
        super.onStart()
        adapter.startListening()
    }

    override fun onStop() {
        super.onStop()
        adapter.stopListening()
    }
}
