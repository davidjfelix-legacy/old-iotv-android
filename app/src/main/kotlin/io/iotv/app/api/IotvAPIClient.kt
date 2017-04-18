package io.iotv.app.api

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import java.io.File
import java.io.FileInputStream

class IotvAPIClient {
    fun createVideo(localVideoPath: String, localThumbnailPath: String): UploadTask? {
        val user = FirebaseAuth.getInstance().currentUser
        user?.let {
            val videoFile = File(localVideoPath)
            val videoStream = FileInputStream(videoFile)
            val thumbFile = File(localThumbnailPath)
            val thumbStream = FileInputStream(thumbFile)

            val uploadTask = writeThumbnailToStorage(thumbFile.name, user.uid, thumbStream)

            uploadTask.continueWithTask({
                val thumbUrl = it.result.downloadUrl.toString()
                writeVideoToStorage(videoFile.name, user.uid, videoStream)
                        .continueWithTask({
                            writeVideoToDatabase(it.result.downloadUrl.toString(), thumbUrl, user.uid)
                        })
            })
            return uploadTask
        }
        // TODO: investigate a sealed class
        return null
    }

    fun listUserVideos() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.let {
            val databaseRef = FirebaseDatabase
                    .getInstance()
                    .reference
                    .child("user-videos")
            databaseRef.addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onCancelled(p0: DatabaseError?) {
                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }
                override fun onDataChange(p0: DataSnapshot?) {
                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }
            })
        }
    }

    private fun writeThumbnailToStorage(fileName: String, userId: String, inputStream: FileInputStream): UploadTask {
        return FirebaseStorage
                .getInstance()
                .reference
                .child("videoThumbnails/$userId/$fileName")
                .putStream(inputStream)
    }

    private fun writeVideoToStorage(fileName: String, userId: String, inputStream: FileInputStream): UploadTask {
        return FirebaseStorage
                .getInstance()
                .reference
                .child("rawVideos/$userId/$fileName")
                .putStream(inputStream)
    }

    private fun writeVideoToDatabase(videoUrl: String, thumbUrl: String, userId: String): Task<Void> {
        val databaseRef = FirebaseDatabase
                .getInstance()
                .reference
        val videoId = databaseRef
                .child("videos")
                .push()
                .key
        val updates = mapOf(
                "/user-videos/$userId/$videoId" to true,
                "/videos/$videoId" to mapOf(
                        "owner_id" to userId,
                        "url" to videoUrl,
                        "thumbnail_url" to thumbUrl
                )
        )
        return databaseRef.updateChildren(updates)
    }
}