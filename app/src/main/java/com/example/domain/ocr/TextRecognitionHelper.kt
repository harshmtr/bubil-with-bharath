package com.example.domain.ocr

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * TextRecognitionHelper integrates Google ML Kit Text Recognition for:
 * 1. Real-time CameraX ImageAnalysis camera stream processing with automatic frame lifecycle management.
 * 2. Throttled stream analysis to avoid CPU/battery starvation on high-frame-rate feeds.
 * 3. Asynchronous one-shot text recognition from Bitmaps and MediaStore Uris.
 */
class TextRecognitionHelper(
    private val options: TextRecognizerOptions = TextRecognizerOptions.DEFAULT_OPTIONS
) {

    companion object {
        private const val TAG = "TextRecognitionHelper"
        const val DEFAULT_THROTTLE_INTERVAL_MS = 400L
    }

    private val recognizer = TextRecognition.getClient(options)

    /**
     * Creates a CameraX ImageAnalysis.Analyzer for processing real-time camera frames.
     *
     * @param throttleIntervalMs Minimum delay between frame processing runs (default: 400ms).
     * @param onTextRecognized Callback invoked with the recognized ML Kit [Text] object.
     * @param onError Optional callback invoked on ML Kit recognition failures.
     */
    fun createAnalyzer(
        throttleIntervalMs: Long = DEFAULT_THROTTLE_INTERVAL_MS,
        onTextRecognized: (Text) -> Unit,
        onError: ((Exception) -> Unit)? = null
    ): ImageAnalysis.Analyzer {
        return object : ImageAnalysis.Analyzer {
            private var lastAnalyzedTimestamp = 0L

            @OptIn(ExperimentalGetImage::class)
            override fun analyze(imageProxy: ImageProxy) {
                val currentTimestamp = SystemClock.uptimeMillis()
                if (currentTimestamp - lastAnalyzedTimestamp < throttleIntervalMs) {
                    imageProxy.close()
                    return
                }

                val mediaImage = imageProxy.image
                if (mediaImage == null) {
                    imageProxy.close()
                    return
                }

                lastAnalyzedTimestamp = currentTimestamp

                try {
                    val inputImage = InputImage.fromMediaImage(
                        mediaImage,
                        imageProxy.imageInfo.rotationDegrees
                    )

                    recognizer.process(inputImage)
                        .addOnSuccessListener { visionText ->
                            onTextRecognized(visionText)
                        }
                        .addOnFailureListener { exception ->
                            Log.e(TAG, "ML Kit text recognition error in stream", exception)
                            onError?.invoke(exception)
                        }
                        .addOnCompleteListener {
                            imageProxy.close()
                        }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to create InputImage from ImageProxy", e)
                    imageProxy.close()
                    onError?.invoke(e)
                }
            }
        }
    }

    /**
     * Processes an individual ImageProxy frame manually from CameraX.
     * Guaranteed to close the imageProxy when recognition finishes.
     */
    @OptIn(ExperimentalGetImage::class)
    fun processImageProxy(
        imageProxy: ImageProxy,
        onSuccess: (Text) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            onError(IllegalArgumentException("ImageProxy contains null media image"))
            return
        }

        try {
            val inputImage = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    onSuccess(visionText)
                }
                .addOnFailureListener { e ->
                    onError(e)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } catch (e: Exception) {
            imageProxy.close()
            onError(e)
        }
    }

    /**
     * Suspended function to recognize text from a Bitmap.
     */
    suspend fun recognizeTextFromBitmap(bitmap: Bitmap, rotationDegrees: Int = 0): Text =
        suspendCancellableCoroutine { cont ->
            try {
                val inputImage = InputImage.fromBitmap(bitmap, rotationDegrees)
                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        cont.resume(visionText)
                    }
                    .addOnFailureListener { e ->
                        cont.resumeWithException(e)
                    }
            } catch (e: Exception) {
                cont.resumeWithException(e)
            }
        }

    /**
     * Suspended function to recognize text from a content Uri.
     */
    suspend fun recognizeTextFromUri(context: Context, uri: Uri): Text =
        suspendCancellableCoroutine { cont ->
            try {
                val inputImage = InputImage.fromFilePath(context, uri)
                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        cont.resume(visionText)
                    }
                    .addOnFailureListener { e ->
                        cont.resumeWithException(e)
                    }
            } catch (e: Exception) {
                cont.resumeWithException(e)
            }
        }

    /**
     * Closes and cleans up the underlying ML Kit TextRecognizer.
     */
    fun close() {
        try {
            recognizer.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing TextRecognizer", e)
        }
    }
}
