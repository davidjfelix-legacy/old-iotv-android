package io.iotv.app.api

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import java.io.File
import java.io.FileInputStream

class IotvAPIClient {
    fun createVideo(localVideoPath: String): UploadTask? {
        val user = FirebaseAuth.getInstance().currentUser
        user?.let {
            val file = File(localVideoPath)
            val stream = FileInputStream(file)
            val uploadTask = FirebaseStorage
                    .getInstance()
                    .reference
                    .child("rawVideos/" + user.uid + "/" + file.name)
                    .putStream(stream)
            uploadTask.continueWithTask({
                writeVideoToDatabase(it.result.downloadUrl.toString(), user)
            })
            return uploadTask
        }
        // TODO: investigate a sealed class
        return null
    }

    private fun writeVideoToDatabase(downloadUrl: String, user: FirebaseUser): Task<Void> {
        val databaseRef = FirebaseDatabase
                .getInstance()
                .reference
        val videoId = databaseRef
                .child("videos")
                .push()
                .key
        val updates = mapOf(
                "/videos/" + videoId to mapOf(
                        "owner_id" to user.uid,
                        "url" to downloadUrl
                ))
        return databaseRef.updateChildren(updates)
    }
}