package io.iotv.app.api

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import io.reactivex.*
import io.reactivex.functions.Cancellable
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

    fun getUserVideos(user: FirebaseUser): Single<DataSnapshot> = Single.create {
        object : SingleOnSubscribe<DataSnapshot>, ValueEventListener, Cancellable {
            lateinit var emitter: SingleEmitter<DataSnapshot>
            val query: Query by lazy {
                FirebaseDatabase
                        .getInstance()
                        .reference
                        .child("user-videos/${user.uid}")
            }

            override fun cancel() = query.removeEventListener(this)

            override fun onCancelled(error: DatabaseError) = emitter.onError(error.toException())

            override fun onDataChange(snapshot: DataSnapshot) = emitter.onSuccess(snapshot)

            override fun subscribe(emitter: SingleEmitter<DataSnapshot>) {
                this.emitter = emitter
                query.addListenerForSingleValueEvent(this)
            }
        }
    }

    fun watchUserVideos(user: FirebaseUser): Flowable<DataSnapshot> = Flowable.create(
        object : FlowableOnSubscribe<DataSnapshot>, ValueEventListener, Cancellable {
            lateinit var emitter: FlowableEmitter<DataSnapshot>
            val query: Query by lazy {
                FirebaseDatabase
                        .getInstance()
                        .reference
                        .child("user-videos/${user.uid}")
            }

            override fun cancel() = query.removeEventListener(this)

            override fun onCancelled(error: DatabaseError) = emitter.onError(error.toException())

            override fun onDataChange(snapshot: DataSnapshot) = emitter.onNext(snapshot)

            override fun subscribe(emitter: FlowableEmitter<DataSnapshot>) {
                this.emitter = emitter
                query.addValueEventListener(this)
            }
        },
        BackpressureStrategy.LATEST
    )

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