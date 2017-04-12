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
    fun createVideo(localVideoPath: String, localThumbnailPath: String): UploadTask? {
        val user = FirebaseAuth.getInstance().currentUser
        user?.let {
            val videoFile = File(localVideoPath)
            val videoStream = FileInputStream(videoFile)
            val thumbFile = File(localThumbnailPath)
            val thumbStream = FileInputStream(thumbFile)

            val uploadTask = writeThumbnailToStorage(thumbFile.name, user.uid, thumbStream)

            uploadTask.continueWithTask({
                writeVideoToStorage(videoFile.name, user.uid, videoStream)
            })
            uploadTask.continueWithTask({
                writeVideoToDatabase(it.result.downloadUrl.toString(), user)
            })
            return uploadTask
        }
        // TODO: investigate a sealed class
        return null
    }

//    fun listUserVideos() {
//        val user = FirebaseAuth.getInstance().currentUser
//        user?.let {
//            val databaseRef = FirebaseDatabase
//                    .getInstance()
//                    .reference
//                    .child()
//        }
//    }

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

    private fun writeVideoToDatabase(downloadUrl: String, user: FirebaseUser): Task<Void> {
        val databaseRef = FirebaseDatabase
                .getInstance()
                .reference
        val videoId = databaseRef
                .child("videos")
                .push()
                .key
        val updates = mapOf(
                "/user-videos/${user.uid}/$videoId" to true,
                "/videos/$videoId" to mapOf(
                    "owner_id" to user.uid,
                    "url" to downloadUrl
                )
        )
        return databaseRef.updateChildren(updates)
    }
}