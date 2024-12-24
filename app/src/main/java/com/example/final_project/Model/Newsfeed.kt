package com.example.final_project.Model
class Newsfeed(
    val id: Long = 0,
    val content: String = "",
    val userId: String = "",
    val userName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    var isLiked: Boolean = false,
    var likes: Int = 0,
    var likedBy: MutableList<String> = mutableListOf() // Ensure this is a MutableList
) {
    fun toHashMap(): HashMap<String, Any> {
        return hashMapOf(
            "id" to id,
            "content" to content,
            "userId" to userId,
            "userName" to userName,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
            "likes" to likes,
            "likedBy" to likedBy // Ensure this field is stored as a MutableList
        )
    }
}

