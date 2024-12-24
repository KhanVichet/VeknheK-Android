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
import com.example.final_project.Adapter.CommentAdapter
import com.example.final_project.Model.Comment
import com.example.final_project.Model.Newsfeed
import com.example.final_project.R
import com.example.final_project.databinding.PostCommentBinding
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class CommentActivity : AppCompatActivity() {
    private val binding : PostCommentBinding by lazy {
        PostCommentBinding.inflate(layoutInflater)
    }
    private var id: Long? = null

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var adapter: NewsfeedAdapter
    private lateinit var adapterCommentAdapter: CommentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setupRecyclerView()
        setupRecyclerViewComment()

        binding.btnPost.setOnClickListener{
            val content = binding.txtCommentPost.text.toString().trim()
            if (content.isNotEmpty()) {
                postToDatabase(content)
                recreate()
            } else {
                showToast("Comment cannot be empty!")
            }
        }
        binding.btnBackToHomePage.setOnClickListener{
            startActivity(Intent(this,MainActivity::class.java))
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
    private fun setupRecyclerView() {
        id = intent.getLongExtra("id", 0L)
        val query = firestore.collection("newsfeed")
            .whereEqualTo("id", id!!)
            .orderBy("createdAt", Query.Direction.DESCENDING)

        val options = FirestoreRecyclerOptions.Builder<Newsfeed>()
            .setQuery(query, Newsfeed::class.java)
            .build()

        adapter = NewsfeedAdapter(options)
        binding.newsfeedRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@CommentActivity)
            adapter = this@CommentActivity.adapter
        }
        adapter.startListening()
    }

    private fun setupRecyclerViewComment() {
        val postId = intent.getLongExtra("id", 0L)
        val query = firestore.collection("comment")
            .whereEqualTo("postId", postId)
            .orderBy("commentAt", Query.Direction.DESCENDING)

        val options = FirestoreRecyclerOptions.Builder<Comment>()
            .setQuery(query, Comment::class.java)
            .build()

        adapterCommentAdapter = CommentAdapter(options)
        binding.commentRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@CommentActivity)
            adapter = this@CommentActivity.adapterCommentAdapter
        }
        adapterCommentAdapter.startListening()
    }

    private fun postToDatabase(content: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            showToast("Please log in first.")
            return
        }

        val postId = intent.getLongExtra("id", 0L)
        val currentUserId = currentUser.uid
        val timestamp = System.currentTimeMillis()

        val forbiddenWords = listOf("មីឆ្កែ", "អាឆ្កែ", "មីថោក","អាថោក")

        // Check if the content contains any forbidden words
        val cleanedContent = replaceForbiddenWords(content, forbiddenWords)

        fetchUserName { userName ->
            if (userName != null) {
                val comment = Comment(
                    commentId = timestamp,
                    postId = postId,
                    userId = currentUserId,
                    userName = userName,
                    commentContent = cleanedContent,
                    commentAt = timestamp
                )

                firestore.collection("comment").document(timestamp.toString()).set(comment.toHashMap())
                    .addOnCompleteListener {
                        showToast("Comment posted")
                        binding.txtCommentPost.text.clear() // Clear input after posting
                        updateRecyclerView()
                    }
                    .addOnFailureListener { exc ->
                        Log.e("CommentActivity", "Failed to post: ${exc.message}")
                        showToast("Failed to post comment")
                    }
            } else {
                showToast("Failed to fetch username. Cannot post.")
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
        val currentUserId = auth.currentUser?.uid ?: return callback(null)

        firestore.collection("users").document(currentUserId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    callback(document.getString("userName"))
                } else {
                    Log.e("CommentActivity", "User document not found.")
                    callback(null)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("CommentActivity", "Error fetching user profile: ${exception.message}")
                callback(null)
            }
    }

    private fun showToast(content: String) {
        Toast.makeText(this, content, Toast.LENGTH_SHORT).show()
    }

    // Update the RecyclerView after posting a new comment, without recreating the activity
    private fun updateRecyclerView() {
        adapterCommentAdapter.notifyDataSetChanged()  // Notify the adapter of the new item
    }

    override fun onStart() {
        super.onStart()
        adapter.startListening()
        adapterCommentAdapter.startListening()
    }

    override fun onStop() {
        super.onStop()
        adapter.stopListening()
        adapterCommentAdapter.stopListening()
    }
}
