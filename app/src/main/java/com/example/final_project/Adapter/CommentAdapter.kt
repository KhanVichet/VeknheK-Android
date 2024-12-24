package com.example.final_project.Adapter

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.final_project.Model.Comment
import com.example.final_project.R
import com.example.final_project.screen.ProfileUserActivity
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions

class CommentAdapter(options: FirestoreRecyclerOptions<Comment>) :
    FirestoreRecyclerAdapter<Comment, CommentAdapter.CommandViewHolder>(options) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommandViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.comment, parent, false)
        return CommandViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommandViewHolder, position: Int, model: Comment) {
        Log.d("CommentAdapter", "Binding item at position $position: $model")
        holder.bind(model)
    }

    class CommandViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        private val userName: TextView = view.findViewById(R.id.txtUsernameComment)
        private val commentContent: TextView = view.findViewById(R.id.txtComment)
        private val commentAt: TextView = view.findViewById(R.id.commandAt)
        private val profile : RelativeLayout = view.findViewById(R.id.profile)

        fun bind(comment: Comment) {
            userName.text = comment.userName
            commentContent.text = comment.commentContent

            val currentDate = System.currentTimeMillis()
            val timeDifference = currentDate - comment.commentAt

            // Display a relative time string
            val timeAgo = when {
                timeDifference < 60 * 1000 -> "Just now"
                timeDifference < 60 * 60 * 1000 -> "${timeDifference / (60 * 1000)} minutes ago"
                timeDifference < 24 * 60 * 60 * 1000 -> "${timeDifference / (60 * 60 * 1000)} hours ago"
                timeDifference < 7 * 24 * 60 * 60 * 1000 -> "${timeDifference / (24 * 60 * 60 * 1000)} days ago"
                else -> {
                    val date = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                    date.format(comment.commentAt)
                }
            }

            profile.setOnClickListener{
                val context  = view.context
                val intent = Intent(context, ProfileUserActivity::class.java).apply {
                    putExtra("userId", comment.userId)
                }
                context.startActivity(intent)
            }

            commentAt.text = timeAgo
        }
    }
}
