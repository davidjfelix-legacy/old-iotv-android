package io.iotv.app.fragments

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.app.Fragment
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.MediaRecorder
import android.media.ThumbnailUtils
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.support.v13.app.ActivityCompat
import android.support.v13.app.FragmentCompat
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.*
import android.widget.Button
import android.widget.Toast
import io.iotv.app.AutoFitTextureView
import io.iotv.app.R
import io.iotv.app.api.IotvAPIClient
import java.io.FileOutputStream
import java.io.IOException
import java.lang.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit


class CameraRecordFragment : Fragment(), View.OnClickListener, FragmentCompat.OnRequestPermissionsResultCallback {
    companion object {
        private val TAG = "CameraRecordFragment"

        private enum class SensorOrientation(val degrees: Int) {
            default(90),
            inverse(270)
        }

        private val DEFAULT_ORIENTATIONS = SparseIntArray()
        private val INVERSE_ORIENTATIONS = SparseIntArray()
        private val REQUEST_VIDEO_PERMISSIONS = 1
        private val FRAGMENT_DIALOG = "dialog"

        private val VIDEO_PERMISSIONS = arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
        )

        init {
            DEFAULT_ORIENTATIONS.append(Surface.ROTATION_0, 90)
            DEFAULT_ORIENTATIONS.append(Surface.ROTATION_90, 0)
            DEFAULT_ORIENTATIONS.append(Surface.ROTATION_180, 270)
            DEFAULT_ORIENTATIONS.append(Surface.ROTATION_270, 180)

            INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270)
            INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 180)
            INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 90)
            INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 0)
        }

        private fun chooseVideoSize(choices: Array<Size>): Size {
            choices
                    .filter { it.width == it.height * 4 / 3 }
                    .filter { it.width <= 1080 }
                    .forEach { return it }
            Log.e(TAG, "Couldn't find any suitable video size")
            return choices[choices.size - 1]
        }

        private fun chooseOptimalSize(choices: Array<Size>, width: Int, height: Int, aspectRatio: Size): Size {
            val bigEnough = ArrayList<Size>()
            choices
                    .filter { it.height == it.width * aspectRatio.height / aspectRatio.width }
                    .filter { it.width >= width }
                    .filterTo(bigEnough) { it.height >= height }
            if (bigEnough.size > 0) {
                return bigEnough.minBy { it.height.toLong() * it.width.toLong() }!!
            } else {
                Log.e(TAG, "Couldn't find any suitable preview size")
                return choices[0]
            }
        }

        class ErrorDialog : DialogFragment() {
            companion object {
                private val ARG_MESSAGE = "message"

                fun newInstance(message: String): ErrorDialog {
                    val dialog = ErrorDialog()
                    val args = Bundle()
                    args.putString(ARG_MESSAGE, message)
                    dialog.arguments = args
                    return dialog
                }
            }

            override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
                return AlertDialog.Builder(activity)
                        .setMessage(arguments.getString(ARG_MESSAGE))
                        .setPositiveButton(android.R.string.ok) { _, _ -> activity.finish() }
                        .create()
            }
        }

        class ConfirmationDialog : DialogFragment() {
            override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
                return AlertDialog.Builder(activity)
                        .setMessage(R.string.permission_request)
                        .setPositiveButton(android.R.string.ok,
                                { _, _ -> FragmentCompat.requestPermissions(parentFragment, VIDEO_PERMISSIONS, REQUEST_VIDEO_PERMISSIONS) })
                        .setNegativeButton(android.R.string.cancel,
                                { _, _ -> parentFragment.activity.finish() })
                        .create()
            }
        }

    }

    private lateinit var mTextureView: AutoFitTextureView
    private lateinit var mButtonVideo: Button
    private var mCameraDevice: CameraDevice? = null
    private var mPreviewSession: CameraCaptureSession? = null

    private val mSurfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture?, width: Int, height: Int) {
            openCamera(width, height)
        }

        override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture?, width: Int, height: Int) {
            configureTransform(width, height)
        }

        override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture?): Boolean {
            return true
        }

        override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture?) {
        }
    }

    private var mPreviewSize: Size? = null
    private lateinit var mVideoSize: Size
    private var mMediaRecorder: MediaRecorder? = null
    private var mIsRecordingVideo = false
    private lateinit var mBackgroundThread: HandlerThread
    private lateinit var mBackgroundHandler: Handler
    private val mCameraOpenCloseLock = Semaphore(1)

    private val mStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(cameraDevice: CameraDevice) {
            mCameraDevice = cameraDevice
            startPreview()
            mCameraOpenCloseLock.release()
            configureTransform(mTextureView.width, mTextureView.height)
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            mCameraOpenCloseLock.release()
            cameraDevice.close()

        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
            mCameraOpenCloseLock.release()
            cameraDevice.close()
            activity.finish()
        }
    }

    private var mSensorOrientation: Int? = null
    private var mNextVideoAbsolutePath: String = ""
    private lateinit var mPreviewBuilder: CaptureRequest.Builder
    private var mRecorderSurface: Surface? = null
    private lateinit var mCameraManager: CameraManager


    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater?.inflate(io.iotv.app.R.layout.fragment_camera_record, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        view?.let {
            mTextureView = view.findViewById(io.iotv.app.R.id.texture) as AutoFitTextureView
            mButtonVideo = view.findViewById(io.iotv.app.R.id.video) as Button
            mButtonVideo.z = -10.toFloat()
            mButtonVideo.setOnClickListener(this)
        }
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()
        if (mTextureView.isAvailable) {
            openCamera(mTextureView.width, mTextureView.height)
        } else {
            mTextureView.surfaceTextureListener = mSurfaceTextureListener
        }
    }

    override fun onPause() {
        closeCamera()
        stopBackgroundThread()
        super.onPause()
    }

    override fun onClick(view: View) {
        when (view.id) {
            io.iotv.app.R.id.video -> {
                if (mIsRecordingVideo) {
                    stopRecordingVideo()
                } else {
                    startRecordingVideo()
                }
            }
        }
    }

    private fun startBackgroundThread() {
        mBackgroundThread = HandlerThread("CameraBackground")
        mBackgroundThread.start()
        mBackgroundHandler = Handler(mBackgroundThread.looper)
    }

    private fun stopBackgroundThread() {
        mBackgroundThread.quitSafely()
        try {
            mBackgroundThread.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    private fun requestVideoPermissions() {
        if (VIDEO_PERMISSIONS.any { shouldShowRequestPermissionRationale(it) }) {
            ConfirmationDialog().show(childFragmentManager, FRAGMENT_DIALOG)
        } else {
            requestPermissions(VIDEO_PERMISSIONS, REQUEST_VIDEO_PERMISSIONS)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        Log.d(TAG, "onRequestPermissionsResult")
        if (requestCode == REQUEST_VIDEO_PERMISSIONS) {
            if (grantResults.size == VIDEO_PERMISSIONS.size) {
                if (grantResults.any { it != PackageManager.PERMISSION_GRANTED }) {
                    ErrorDialog.newInstance(getString(io.iotv.app.R.string.permission_request))
                            .show(childFragmentManager, FRAGMENT_DIALOG)
                }
            } else {
                ErrorDialog.newInstance(getString(io.iotv.app.R.string.permission_request))
                        .show(childFragmentManager, FRAGMENT_DIALOG)
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun openCamera(width: Int, height: Int) {
        if (VIDEO_PERMISSIONS.any { ActivityCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED }) {
            requestVideoPermissions()
            return
        }
        if (activity.isFinishing) {
            return
        }
        try {
            Log.d(TAG, "tryAcquire")
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }
            mCameraManager = activity.getSystemService(CameraManager::class.java)
            val cameraId = mCameraManager.cameraIdList[0] // FIXME do this smarter
            val characteristics = mCameraManager.getCameraCharacteristics(cameraId)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)
            mVideoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder::class.java))
            mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture::class.java), width, height, mVideoSize)
            val orientation = resources.configuration.orientation
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mTextureView.setAspectRatio(mPreviewSize!!.width, mPreviewSize!!.height)
            } else {
                mTextureView.setAspectRatio(mPreviewSize!!.height, mPreviewSize!!.width)
            }
            configureTransform(width, height)
            mMediaRecorder = MediaRecorder()
            mCameraManager.openCamera(cameraId, mStateCallback, null)
        } catch (e: CameraAccessException) {
            Toast.makeText(activity, "Cannot access the camera.", Toast.LENGTH_SHORT).show()
            activity.finish()
        } catch (e: NullPointerException) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            ErrorDialog.newInstance(getString(io.iotv.app.R.string.camera_error))
                    .show(childFragmentManager, FRAGMENT_DIALOG)
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera opening.")
        }
    }

    private fun closeCamera() {
        try {
            mCameraOpenCloseLock.acquire()
            closePreviewSession()
            mCameraDevice?.close()
            mMediaRecorder?.release()
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.")
        } finally {
            mCameraOpenCloseLock.release()
        }
    }

    private fun startPreview() {
        if (null == mCameraDevice || !mTextureView.isAvailable || null == mPreviewSize) {
            return
        }

        try {
            closePreviewSession()
            val texture = mTextureView.surfaceTexture
            texture.setDefaultBufferSize(mPreviewSize!!.width, mPreviewSize!!.height)
            mPreviewBuilder = mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

            val previewSurface = Surface(texture)
            mPreviewBuilder.addTarget(previewSurface)
            mCameraDevice?.createCaptureSession(listOf(previewSurface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                    mPreviewSession = cameraCaptureSession
                    updatePreview()
                }

                override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                    Toast.makeText(activity, "Failed", Toast.LENGTH_SHORT).show()
                }
            }, mBackgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun updatePreview() {
        try {
            setUpCaptureRequestBuilder(mPreviewBuilder)
            val thread = HandlerThread("CameraPreview")
            thread.start()
            mPreviewSession?.setRepeatingRequest(mPreviewBuilder.build(), null, mBackgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun setUpCaptureRequestBuilder(builder: CaptureRequest.Builder) {
        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
    }

    private fun configureTransform(viewWidth: Int, viewHeight: Int) {
        if (null == mTextureView || null == mPreviewSize || null == activity) {
            return
        }
        val rotation = activity.windowManager.defaultDisplay.rotation
        val matrix = Matrix()
        val viewRect = RectF(0.toFloat(), 0.toFloat(), viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect = RectF(0.toFloat(), 0.toFloat(), mPreviewSize!!.height.toFloat(), mPreviewSize!!.width.toFloat())
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset((centerX - bufferRect.centerX()), (centerY - bufferRect.centerY()))
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
            val scale = Math.max(
                    (viewHeight.toFloat() / mPreviewSize!!.height.toFloat()),
                    (viewWidth.toFloat() / mPreviewSize!!.width.toFloat()))
            matrix.postScale(scale, scale, centerX, centerY)
            matrix.postRotate((90.toFloat() * (rotation - 2).toFloat()), centerX, centerY)
        }
        mTextureView.setTransform(matrix)
    }

    private fun setUpMediaRecorder() {

        mMediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
        mMediaRecorder?.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        mMediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        if (mNextVideoAbsolutePath.isEmpty()) {
            mNextVideoAbsolutePath = getVideoFilePath(activity)
        }
        mMediaRecorder?.setOutputFile(mNextVideoAbsolutePath)
        mMediaRecorder?.setVideoEncodingBitRate(10000000)
        mMediaRecorder?.setVideoFrameRate(30)
        mMediaRecorder?.setVideoSize(mVideoSize.width, mVideoSize.height)
        mMediaRecorder?.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        mMediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        val rotation = activity.windowManager.defaultDisplay.rotation
        when (mSensorOrientation) {
            SensorOrientation.default.degrees -> mMediaRecorder?.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation))
            SensorOrientation.inverse.degrees -> mMediaRecorder?.setOrientationHint(INVERSE_ORIENTATIONS.get(rotation))
        }
        mMediaRecorder?.prepare()
    }

    private fun getVideoFilePath(context: Context): String {
        return context.getExternalFilesDir(null).absolutePath + "/" + System.currentTimeMillis() + ".mp4"
    }

    private fun startRecordingVideo() {
        if (null == mCameraDevice || !mTextureView.isAvailable || null == mPreviewSize) {
            return
        }
        try {
            closePreviewSession()
            setUpMediaRecorder()
            val texture = mTextureView.surfaceTexture
            texture.setDefaultBufferSize(mPreviewSize!!.width, mPreviewSize!!.height)
            mPreviewBuilder = mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
            val surfaces = ArrayList<Surface?>()

            // Set up surface for camera preview
            val previewSurface = Surface(texture)
            surfaces.add(previewSurface)
            mPreviewBuilder.addTarget(previewSurface)

            // Set up surface for the mediarecorder
            mRecorderSurface = mMediaRecorder?.surface
            surfaces.add(mRecorderSurface)
            mPreviewBuilder.addTarget(mRecorderSurface)

            // Start a capture session
            mCameraDevice?.createCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                    mPreviewSession = cameraCaptureSession
                    updatePreview()
                    activity.runOnUiThread {
                        // UI
                        mButtonVideo.setText(io.iotv.app.R.string.stop)
                        mIsRecordingVideo = true

                        // Start recording
                        mMediaRecorder?.start()
                    }
                }

                override fun onConfigureFailed(p0: CameraCaptureSession?) {
                    Toast.makeText(activity, "Failed", Toast.LENGTH_SHORT).show()
                }
            }, mBackgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


    private fun closePreviewSession() {
        mPreviewSession?.close()
    }

    private fun stopRecordingVideo() {
        // UI
        mIsRecordingVideo = false
        mButtonVideo.setText(io.iotv.app.R.string.record)

        // Stop recording
        mMediaRecorder?.stop()
        mMediaRecorder?.reset()

        Toast.makeText(activity, "Videosaved: " + mNextVideoAbsolutePath, Toast.LENGTH_SHORT).show()
        // TODO: extract me into a service
        val thumbnail = ThumbnailUtils.createVideoThumbnail(mNextVideoAbsolutePath, MediaStore.Video.Thumbnails.MINI_KIND)
        val thumbnailStream = FileOutputStream("$mNextVideoAbsolutePath.png")
        thumbnail.compress(Bitmap.CompressFormat.PNG, 100, thumbnailStream)
        val client = IotvAPIClient()
        val uploadTask = client.createVideo(mNextVideoAbsolutePath, "$mNextVideoAbsolutePath.png")
        uploadTask?.let {

            uploadTask.addOnFailureListener {
                Toast.makeText(activity, "Failed to upload", Toast.LENGTH_SHORT).show()
            }
            uploadTask.addOnSuccessListener {
                Toast.makeText(activity, "Succeeded uploading", Toast.LENGTH_SHORT).show()
            }
        }
        Log.d(TAG, "Video saved: " + mNextVideoAbsolutePath)
        mNextVideoAbsolutePath = ""
        startPreview()
    }
}
