package tv.mg4.app

import android.util.Log
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import okio.Okio
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.IOException

class MP4ChunkedUploader(val fileDescriptor: FileDescriptor) {
    val inputStream = FileInputStream(fileDescriptor)
    val bufferedSource = Okio.buffer(Okio.source(inputStream))



}

class MP4ChunkRequestBody(val buffer: Buffer): RequestBody() {
    companion object {
        val TAG = "MP4ChunkRequestBody"
        // FIXME: Perhaps generate the boundary so it doesn't happen to interfere with a file?
        // Since a file containing \n--boundary would split the message if we don't parse this
        // properly. It'd probably be smarter to just migrate to protobuf tbh
        val MULTIPART_RELATED_TYPE = MediaType.parse("multipart/related; boundary=\"boundary\"")!!
    }

    override fun contentType(): MediaType {
        return MULTIPART_RELATED_TYPE
    }

    override fun writeTo(sink: BufferedSink?) {
        try {
            sink!!.writeUtf8("--boundary\nContent-Type: application/json; charset=utf8\n\n{}\n--boundary")
            sink.writeAll(buffer)
            sink.writeUtf8("\n--boundary--\n")
        } catch (e: IOException) {
            Log.e(TAG, "IOException in writeTo while writing request body")
            throw e
        }
    }

}