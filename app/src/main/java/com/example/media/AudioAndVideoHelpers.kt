package com.example.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

class AudioRecorderHelper(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun startRecording(): File {
        stopSilently()
        val file = File(context.cacheDir, "tabi_audio_${System.currentTimeMillis()}.m4a")
        val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        rec.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44100)
            setAudioEncodingBitRate(96000)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        recorder = rec
        outputFile = file
        return file
    }

    fun stopAndReadBytes(): ByteArray? {
        val file = outputFile
        return try {
            recorder?.stop()
            recorder?.release()
            recorder = null
            file?.takeIf { it.exists() && it.length() > 0L }?.readBytes()
        } catch (e: Exception) {
            Log.e("AudioRecorderHelper", "Stop recording failed", e)
            stopSilently()
            null
        }
    }

    fun stopSilently() {
        runCatching {
            recorder?.stop()
        }
        runCatching {
            recorder?.release()
        }
        recorder = null
    }
}

object MotionVideoExporter {

    /**
     * Encodes an animated Japanese motion reel (Ken Burns zoom/pan + 3D drifting sakura petals +
     * poetic Japanese caption) from [sourceBitmap] into a real H.264 MP4 file in 16:9 or 9:16.
     */
    suspend fun exportPhotoMotionMp4(
        context: Context,
        sourceBitmap: Bitmap,
        isLandscape16x9: Boolean,
        japaneseTitle: String,
        englishSubtitle: String,
        motionPreset: String
    ): File = withContext(Dispatchers.Default) {
        val width = if (isLandscape16x9) 960 else 540
        val height = if (isLandscape16x9) 540 else 960
        val fps = 24
        val totalFrames = fps * 4 // 4-second crisp clip

        val outFile = File(context.cacheDir, "tabi_reel_${System.currentTimeMillis()}.mp4")
        if (outFile.exists()) outFile.delete()

        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, 2_400_000)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val inputSurface = codec.createInputSurface()
        codec.start()

        val muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var trackIndex = -1
        var muxerStarted = false
        val bufferInfo = MediaCodec.BufferInfo()

        val imgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val petalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.argb(220, 255, 192, 208)
            style = Paint.Style.FILL
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            textSize = if (isLandscape16x9) 34f else 30f
            isFakeBoldText = true
            setShadowLayer(8f, 0f, 3f, AndroidColor.BLACK)
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.argb(235, 240, 240, 245)
            textSize = if (isLandscape16x9) 22f else 20f
            setShadowLayer(6f, 0f, 2f, AndroidColor.BLACK)
        }
        val gradientPaint = Paint()

        fun drainEncoder(endOfStream: Boolean) {
            if (endOfStream) codec.signalEndOfInputStream()
            while (true) {
                val outIdx = codec.dequeueOutputBuffer(bufferInfo, 10_000L)
                if (outIdx == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (!endOfStream) break
                } else if (outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    trackIndex = muxer.addTrack(codec.outputFormat)
                    muxer.start()
                    muxerStarted = true
                } else if (outIdx >= 0) {
                    val encodedData = codec.getOutputBuffer(outIdx)
                    if (encodedData != null && bufferInfo.size > 0 && muxerStarted) {
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                    }
                    codec.releaseOutputBuffer(outIdx, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) break
                }
            }
        }

        try {
            for (frameIdx in 0 until totalFrames) {
                val progress = frameIdx.toFloat() / totalFrames.toFloat()
                val canvas: Canvas = inputSurface.lockCanvas(null)
                try {
                    canvas.drawColor(AndroidColor.BLACK)
                    val baseScale = maxOf(
                        width.toFloat() / sourceBitmap.width.toFloat(),
                        height.toFloat() / sourceBitmap.height.toFloat()
                    )
                    val zoom = when (motionPreset) {
                        "Shinkansen Dolly" -> 1.0f + progress * 0.24f
                        "Kyoto Tilt" -> 1.12f - progress * 0.08f
                        else -> 1.04f + progress * 0.16f
                    }
                    val panX = when (motionPreset) {
                        "Kyoto Tilt" -> sin(progress * Math.PI).toFloat() * 32f
                        else -> (progress - 0.5f) * 38f
                    }
                    val panY = (progress - 0.5f) * -22f
                    val drawW = sourceBitmap.width * baseScale * zoom
                    val drawH = sourceBitmap.height * baseScale * zoom
                    val left = (width - drawW) / 2f + panX
                    val top = (height - drawH) / 2f + panY
                    canvas.drawBitmap(sourceBitmap, null, RectF(left, top, left + drawW, top + drawH), imgPaint)

                    // Bottom cinematic scrim
                    gradientPaint.shader = LinearGradient(
                        0f, height * 0.55f, 0f, height.toFloat(),
                        AndroidColor.TRANSPARENT, AndroidColor.argb(215, 10, 12, 20),
                        Shader.TileMode.CLAMP
                    )
                    canvas.drawRect(0f, height * 0.55f, width.toFloat(), height.toFloat(), gradientPaint)

                    // Drifting sakura petals
                    for (p in 0 until 18) {
                        val px = ((p * 89 + progress * width * 0.85f + sin(progress * 6.28f + p) * 28f) % width)
                        val py = ((p * 67 + progress * height * 1.15f) % height)
                        val pr = 5f + (p % 4) * 2.5f
                        canvas.drawCircle(px, py, pr, petalPaint)
                    }

                    // Poetic Japanese overlay
                    if (japaneseTitle.isNotBlank()) {
                        canvas.drawText(japaneseTitle.take(28), 36f, height - 74f, titlePaint)
                    }
                    if (englishSubtitle.isNotBlank()) {
                        canvas.drawText(englishSubtitle.take(54), 36f, height - 38f, subPaint)
                    }
                } finally {
                    inputSurface.unlockCanvasAndPost(canvas)
                }
                drainEncoder(false)
            }
            drainEncoder(true)
        } finally {
            runCatching { codec.stop() }
            runCatching { codec.release() }
            runCatching { inputSurface.release() }
            if (muxerStarted) {
                runCatching { muxer.stop() }
            }
            runCatching { muxer.release() }
        }
        outFile
    }
}
