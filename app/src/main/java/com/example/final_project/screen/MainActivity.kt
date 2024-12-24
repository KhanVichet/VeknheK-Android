package com.example.final_project.screen

import NewsfeedAdapter
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder

import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.final_project.Model.Newsfeed
import com.example.final_project.R
import com.example.final_project.databinding.ActivityMainBinding
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query


class MainActivity : AppCompatActivity() {

    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }


    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var adapter: NewsfeedAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Initialize Firebase instances
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()


        loadUserName()
        setupRecyclerView()

        binding.btnPost.setOnClickListener { handlePostButtonClick() }

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


    private fun loadUserName() {
        fetchUserName { userName ->
            binding.userIdTextView.text = userName ?: "Unknown User"
        }
    }

    private fun setupRecyclerView() {
        val query = firestore.collection("newsfeed")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(20)

        val options = FirestoreRecyclerOptions.Builder<Newsfeed>()
            .setQuery(query, Newsfeed::class.java)
            .build()

        adapter = NewsfeedAdapter(options)
        binding.newsfeedRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }
        adapter.startListening()
    }

    private fun handlePostButtonClick() {
        val content = binding.txtTypingPost.text.toString().trim()
        if (content.isEmpty()) {
            showToast("Please write content before posting.")
        } else {
            postToDatabase(content)

        }
    }


    private fun postToDatabase(content: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            showToast("Please log in first.")
            return
        }

        val currentUserId = currentUser.uid
        val timestamp = System.currentTimeMillis()

        val forbiddenWords = listOf("មីឆ្កែ", "អាឆ្កែ", "មីថោក","អាថោក")

        // Check if the content contains any forbidden words
        val cleanedContent = replaceForbiddenWords(content, forbiddenWords)

        fetchUserName { userName ->
            if (userName != null) {
                val post = Newsfeed(
                    id = timestamp,
                    content = cleanedContent,
                    userId = currentUserId,
                    userName = userName,
                    createdAt = timestamp,
                    updatedAt = timestamp,
                    likes = 0 // Initialize likes count to 0
                ).toHashMap()

                firestore.collection("newsfeed").document(timestamp.toString())
                    .set(post)
                    .addOnSuccessListener {
                        showToast("Post successful!")
                        binding.txtTypingPost.text.clear()
                        recreate()
                    }
                    .addOnFailureListener { exception ->
                        Log.e("MainActivity", "Failed to post: ${exception.message}")
                        showToast("Failed to post. Try again later.")
                    }
            } else {
                showToast("Unable to fetch username. Post failed.")
            }
        }
    }

    private fun replaceForbiddenWords(content: String, forbiddenWords: List<String>): String {
        var cleanedContent = content
        forbiddenWords.forEach { word ->
            // Replace each forbidden word with asterisks of the same length
            cleanedContent = cleanedContent.replace(word, "*".repeat(word.length), ignoreCase = true)
        }
        return cleanedContent
    }

    private fun fetchUserName(callback: (String?) -> Unit) {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            Log.e("MainActivity", "User not authenticated.")
            callback(null)
            return
        }

        firestore.collection("users").document(currentUserId).get()
            .addOnSuccessListener { document ->
                val userName = document.getString("userName")
                if (userName != null) {
                    callback(userName)
                } else {
                    Log.e("MainActivity", "User document not found.")
                    callback(null)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("MainActivity", "Error fetching user profile: ${exception.message}")
                callback(null)
            }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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
