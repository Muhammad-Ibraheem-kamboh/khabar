package com.khabar.app.notifications

import android.content.ContentValues
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.khabar.app.config.AppConfig
import com.khabar.app.domain.model.MailCategory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

object ToneSynth {

    private const val SAMPLE_RATE = 44100

    /**
     * Synthesizes 16-bit 44.1kHz mono PCM audio with cosine ramp attack/decay.
     */
    fun generateToneWavBytes(
        frequenciesHz: List<Int>,
        durationsMs: List<Int>,
        decay: Boolean = false
    ): ByteArray {
        val sampleRate = SAMPLE_RATE
        val totalSamples = durationsMs.sumOf { (it * sampleRate) / 1000 }
        val pcm = ShortArray(totalSamples)

        var offset = 0
        for (i in frequenciesHz.indices) {
            val freq = frequenciesHz[i]
            val durationMs = durationsMs[i]
            val count = (durationMs * sampleRate) / 1000
            val ramp = (0.01 * sampleRate).toInt().coerceAtMost(count / 4) // 10ms smooth ramp

            for (s in 0 until count) {
                val t = s.toDouble() / sampleRate
                var amplitude = sin(2.0 * PI * freq * t)

                // Attack ramp
                if (s < ramp) {
                    amplitude *= (s.toDouble() / ramp)
                }
                // Decay ramp
                if (decay) {
                    val progress = s.toDouble() / count
                    amplitude *= (1.0 - progress)
                } else if (s > count - ramp) {
                    amplitude *= ((count - s).toDouble() / ramp)
                }

                pcm[offset + s] = (amplitude * 24000).toInt().toShort()
            }
            offset += count
        }

        return createWavContainer(pcm, sampleRate)
    }

    private fun createWavContainer(pcmData: ShortArray, sampleRate: Int): ByteArray {
        val byteDataLength = pcmData.size * 2
        val totalLength = 36 + byteDataLength

        val baos = ByteArrayOutputStream()
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)

        header.put("RIFF".toByteArray(Charsets.US_ASCII))
        header.putInt(totalLength)
        header.put("WAVE".toByteArray(Charsets.US_ASCII))
        header.put("fmt ".toByteArray(Charsets.US_ASCII))
        header.putInt(16) // Subchunk1Size for PCM
        header.putShort(1) // AudioFormat 1 = PCM
        header.putShort(1) // NumChannels 1 = Mono
        header.putInt(sampleRate) // SampleRate
        header.putInt(sampleRate * 2) // ByteRate (SampleRate * NumChannels * BitsPerSample/8)
        header.putShort(2) // BlockAlign (NumChannels * BitsPerSample/8)
        header.putShort(16) // BitsPerSample 16
        header.put("data".toByteArray(Charsets.US_ASCII))
        header.putInt(byteDataLength)

        baos.write(header.array())

        val pcmByteBuffer = ByteBuffer.allocate(byteDataLength).order(ByteOrder.LITTLE_ENDIAN)
        for (sample in pcmData) {
            pcmByteBuffer.putShort(sample)
        }
        baos.write(pcmByteBuffer.array())

        return baos.toByteArray()
    }

    fun playPreview(context: Context, category: MailCategory) = playPreview(category)

    fun playPreview(category: MailCategory) {
        val frequencies = category.defaultToneHz
        val durations = category.defaultToneDurationMs
        if (frequencies.isEmpty()) return

        val isDecay = category == MailCategory.DEADLINE || category == MailCategory.CAREER
        val wavBytes = generateToneWavBytes(frequencies, durations, isDecay)

        try {
            // Skip 44 bytes header and play PCM in AudioTrack
            val pcmBytes = wavBytes.copyOfRange(44, wavBytes.size)
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .build()
                )
                .setBufferSizeInBytes(pcmBytes.size)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(pcmBytes, 0, pcmBytes.size)
            audioTrack.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getOrCreateToneUri(context: Context, category: MailCategory): Uri {
        val fileName = "khabar_${category.idString.lowercase()}.wav"

        // On API 29+, use MediaStore Audio
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val projection = arrayOf(MediaStore.Audio.Media._ID)
            val selection = "${MediaStore.Audio.Media.DISPLAY_NAME} = ?"
            val selectionArgs = arrayOf(fileName)

            resolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                    return Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id.toString())
                }
            }

            // Create new
            val frequencies = category.defaultToneHz.ifEmpty { listOf(440) }
            val durations = category.defaultToneDurationMs.ifEmpty { listOf(150) }
            val isDecay = category == MailCategory.DEADLINE || category == MailCategory.CAREER
            val wavBytes = generateToneWavBytes(frequencies, durations, isDecay)

            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
                put(MediaStore.Audio.Media.RELATIVE_PATH, "Notifications/${AppConfig.APP_NAME}")
                put(MediaStore.Audio.Media.IS_NOTIFICATION, 1)
            }

            val newUri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
            if (newUri != null) {
                resolver.openOutputStream(newUri)?.use { it.write(wavBytes) }
                return newUri
            }
        }

        // On API 26-28 or fallback: write app-private file & return FileProvider Uri
        val soundsDir = File(context.filesDir, "sounds")
        if (!soundsDir.exists()) soundsDir.mkdirs()
        val file = File(soundsDir, fileName)

        if (!file.exists()) {
            val frequencies = category.defaultToneHz.ifEmpty { listOf(440) }
            val durations = category.defaultToneDurationMs.ifEmpty { listOf(150) }
            val isDecay = category == MailCategory.DEADLINE || category == MailCategory.CAREER
            val wavBytes = generateToneWavBytes(frequencies, durations, isDecay)
            FileOutputStream(file).use { it.write(wavBytes) }
        }

        return try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (_: Exception) {
            Uri.parse("android.resource://${context.packageName}/raw/$fileName")
        }
    }
}
