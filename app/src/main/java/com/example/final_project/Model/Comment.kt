package com.example.final_project.Model

class Comment (
    var commentId : Long = 0,
    var postId : Long = 0,
    var userId : String = "",
    var userName : String = "",
    var commentContent : String = "",
    var commentAt : Long = System.currentTimeMillis()
){
    fun toHashMap(): HashMap<String, Any> {
        return hashMapOf(
            "commentId" to commentId,
            "postId" to postId,
            "userId" to userId,
            "userName" to userName,
            "commentContent" to commentContent,
            "commentAt" to commentAt
        )
    }
}