
import android.app.Activity
import android.content.Intent
import android.graphics.PorterDuff
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.final_project.Model.Newsfeed
import com.example.final_project.R
import com.example.final_project.screen.CommentActivity
import com.example.final_project.screen.ProfileUserActivity
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class NewsfeedAdapterProfile(options: FirestoreRecyclerOptions<Newsfeed>) :
    FirestoreRecyclerAdapter<Newsfeed, NewsfeedAdapterProfile.NewsfeedViewHolder>(options) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsfeedViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_view_news_feed_profile, parent, false)
        return NewsfeedViewHolder(view)
    }
    override fun onBindViewHolder(holder: NewsfeedViewHolder, position: Int, model: Newsfeed) {
        Log.d("NewsfeedAdapter", "Binding item at position $position: $model")
        holder.bind(model) // Pass the model to the ViewHolder's bind method
    }


    class NewsfeedViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        private val contentTextView: TextView = view.findViewById(R.id.contentTextView)
        private val dateTextView: TextView = view.findViewById(R.id.dateTextView)
        private val userNameTextView: TextView = view.findViewById(R.id.UserName)
        private val cardViewId: CardView = view.findViewById(R.id.cardViewId)
        private val like: ImageView = view.findViewById(R.id.imageView5)
        private val likesTextView: TextView = view.findViewById(R.id.likesTextView)
        private val CommentTextView: TextView = view.findViewById(R.id.CommentTextView)
        private val delete :ImageView = view.findViewById(R.id.delete)
        private val profileView : LinearLayout = view.findViewById(R.id.profile)

        // Bind data to the views and handle click actions
        fun bind(newsfeed: Newsfeed) {
            contentTextView.text = newsfeed.content
            userNameTextView.text = newsfeed.userName

            // Format and display the time ago string
            val currentDate = System.currentTimeMillis()
            val timeDifference = currentDate - newsfeed.createdAt
            val timeAgo = when {
                timeDifference < 60 * 1000 -> "Just now"
                timeDifference < 60 * 60 * 1000 -> "${timeDifference / (60 * 1000)} minutes ago"
                timeDifference < 24 * 60 * 60 * 1000 -> "${timeDifference / (60 * 60 * 1000)} hours ago"
                else -> "${timeDifference / (24 * 60 * 60 * 1000)} days ago"
            }
            dateTextView.text = timeAgo
            likesTextView.text = "${newsfeed.likes} Likes"
            countComments(newsfeed.id) { count ->
                CommentTextView.text = "${count} Comments"
            }

            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

            val isLiked = newsfeed.likedBy.contains(currentUserId)

            if (isLiked) {
                like.setColorFilter(ContextCompat.getColor(view.context, R.color.red), PorterDuff.Mode.SRC_IN)
            } else {
                like.setColorFilter(ContextCompat.getColor(view.context, R.color.defaultGray), PorterDuff.Mode.SRC_IN)
            }

            like.setOnClickListener {
                if (currentUserId != null) {
                    val isLiked = newsfeed.likedBy.contains(currentUserId)

                    Log.d("NewsfeedAdapter", "isLiked before toggle: $isLiked")

                    if (isLiked) {
                        newsfeed.likes--
                        newsfeed.likedBy.remove(currentUserId)
                    } else {
                        newsfeed.likes++
                        newsfeed.likedBy.add(currentUserId)
                    }

                    val db = FirebaseFirestore.getInstance()
                    val postRef = db.collection("newsfeed").document(newsfeed.id.toString())

                    postRef.update(
                        "likes", newsfeed.likes,
                        "likedBy", ArrayList(newsfeed.likedBy)
                    )
                        .addOnSuccessListener {
                            Log.d("NewsfeedAdapter", "Successfully updated likes and liked users for post ${newsfeed.id}")
                        }
                        .addOnFailureListener { e ->
                            Log.e("NewsfeedAdapter", "Error updating likes and liked users", e)
                        }

                } else {
                    Log.e("NewsfeedAdapter", "User is not logged in.")
                }
            }

            cardViewId.setOnClickListener {
                val context = view.context
                val intent = Intent(context, CommentActivity::class.java).apply {
                    putExtra("id", newsfeed.id)
                }
                context.startActivity(intent)
            }
            delete.setOnClickListener {
                val db = FirebaseFirestore.getInstance()

                // Reference to the 'newsfeed' collection
                val postRef = db.collection("newsfeed").document(newsfeed.id.toString())  // Convert id to String

                // Delete the document
                postRef.delete()
                    .addOnSuccessListener {
                        // Successfully deleted the post
                        Toast.makeText(view.context, "Post deleted successfully", Toast.LENGTH_SHORT).show()
                        val activity = view.context as? Activity
                        activity?.recreate()
                    }
                    .addOnFailureListener { exception ->
                        // Error deleting the post
                        Log.e("NewsfeedAdapter", "Error deleting the post", exception)
                        Toast.makeText(view.context, "Failed to delete post", Toast.LENGTH_SHORT).show()
                    }
            }


            profileView.setOnClickListener{
                val context  = view.context
                val intent = Intent(context, ProfileUserActivity::class.java).apply {
                    putExtra("userId", newsfeed.userId)
                }
                context.startActivity(intent)
            }
        }

        private fun countComments(postId: Long, callback: (Int) -> Unit) {
            val firestore = FirebaseFirestore.getInstance()

            firestore.collection("comment")
                .whereEqualTo("postId", postId) // Filter comments by postId
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val commentCount = querySnapshot.size() // Number of comments
                    callback(commentCount) // Pass the count back via the callback
                }
                .addOnFailureListener { exception ->
                    Log.e("NewsfeedAdapter", "Error fetching comments: ", exception)
                    callback(0)
                }
        }

    }
}
