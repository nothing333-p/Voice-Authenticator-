

package com.example.sampleapp

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.sampleapp.ui.UserViewModel
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

@Composable
fun NewUserScreen(
    navController: NavHostController,
    viewModel: UserViewModel
) {
    val context = LocalContext.current

    var userName by remember { mutableStateOf(TextFieldValue("")) }
    var selectedGender by remember { mutableStateOf("Male") }

    // AudioRecord and job refs
    var audioRecordState by remember { mutableStateOf<AudioRecord?>(null) }
    var recordingJob by remember { mutableStateOf<Job?>(null) }
    var isRecording by remember { mutableStateOf(false) }

    // List to store audio file paths (immutable list for Compose)
    var recordedFiles by remember { mutableStateOf(listOf<String>()) }

    val coroutineScope = rememberCoroutineScope()

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Microphone permission required", Toast.LENGTH_SHORT).show()
        }
    }

    // Ask permission on launch
    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    // WAV recording parameters
    val sampleRate = 16000 // adjust if desired
    val channelConfig = AudioFormat.CHANNEL_IN_MONO
    val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    val bitsPerSample = 16
    val channels = 1

    fun startRecording(outputFile: File) {
        val minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        if (minBufSize == AudioRecord.ERROR || minBufSize == AudioRecord.ERROR_BAD_VALUE) {
            Toast.makeText(context, "AudioRecord not supported on device", Toast.LENGTH_SHORT).show()
            return
        }

        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            minBufSize * 2
        )

        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            Toast.makeText(context, "Failed to initialize recorder", Toast.LENGTH_SHORT).show()
            return
        }

        // Write placeholder header first
        try {
            FileOutputStream(outputFile).use { fos ->
                val header = ByteArray(44)
                fos.write(header)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Unable to create file", Toast.LENGTH_SHORT).show()
            return
        }

        audioRecord.startRecording()
        audioRecordState = audioRecord

        val job = coroutineScope.launch(Dispatchers.IO) {
            val buffer = ByteArray(minBufSize)
            var totalBytesWritten = 0L

            try {
                FileOutputStream(outputFile, true).use { dataOut ->
                    while (isActive && audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                        val read = audioRecord.read(buffer, 0, buffer.size)
                        if (read > 0) {
                            dataOut.write(buffer, 0, read)
                            totalBytesWritten += read
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    audioRecord.stop()
                } catch (ignored: Exception) { }
                audioRecord.release()
                audioRecordState = null

                // Write final WAV header now that we know length
                try {
                    writeWavHeader(
                        file = outputFile,
                        totalAudioLen = totalBytesWritten,
                        sampleRate = sampleRate.toLong(),
                        channels = channels,
                        byteRate = (sampleRate * channels * bitsPerSample / 8).toLong(),
                        bitsPerSample = bitsPerSample
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        recordingJob = job
    }

    fun stopRecording() {
        recordingJob?.cancel()
        recordingJob = null
        isRecording = false
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background image
        Image(
            painter = painterResource(R.drawable.newuserbackground),
            contentDescription = "New User Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        
        // Content overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "New User Registration", 
                style = MaterialTheme.typography.headlineSmall,
                color = androidx.compose.ui.graphics.Color(0xFFF5DEB3) // Beige color for dark background
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Name input
            OutlinedTextField(
                value = userName,
                onValueChange = { userName = it },
                label = { Text("Enter your name", color = androidx.compose.ui.graphics.Color.White) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = androidx.compose.ui.text.TextStyle(color = androidx.compose.ui.graphics.Color.White)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Gender selection
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("Male", "Female", "Other").forEach { gender ->
                    Button(
                        onClick = { selectedGender = gender },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedGender == gender)
                                androidx.compose.ui.graphics.Color(0xFF2E7D32) // Dark green when selected
                            else
                                androidx.compose.ui.graphics.Color(0xFF4A90E2), // Light blue when not selected
                            contentColor = androidx.compose.ui.graphics.Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = if (selectedGender == gender) 8.dp else 2.dp,
                            pressedElevation = if (selectedGender == gender) 12.dp else 6.dp
                        ),
                        border = if (selectedGender == gender)
                            BorderStroke(2.dp, androidx.compose.ui.graphics.Color.White)
                        else
                            null
                    ) {
                        Text(
                            text = gender,
                            fontWeight = if (selectedGender == gender) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Record / Stop button
            Button(
                onClick = {
                    if (isRecording) {
                        stopRecording()
                        Toast.makeText(context, "Recording stopped", Toast.LENGTH_SHORT).show()
                    } else {
                        if (recordedFiles.size >= 5) {
                            Toast.makeText(context, "You can only record 5 samples", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (userName.text.isEmpty()) {
                            Toast.makeText(context, "Please enter a name first", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                        if (outputDir == null) {
                            Toast.makeText(context, "Cannot access external files dir", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val fileName = "${userName.text}_${recordedFiles.size + 1}.wav"
                        val file = File(outputDir, fileName)

                        isRecording = true
                        startRecording(file)

                        // Add to list immediately (file will be finalized when recording stops)
                        recordedFiles = recordedFiles + file.absolutePath
                        Toast.makeText(context, "Recording started...", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = userName.text.isNotEmpty() && recordedFiles.size < 5,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) 
                        androidx.compose.ui.graphics.Color(0xFFE74C3C) // Red when recording
                    else 
                        androidx.compose.ui.graphics.Color(0xFF27AE60), // Green when ready to record
                    contentColor = androidx.compose.ui.graphics.Color.White
                )
            ) {
                Text(if (isRecording) "Stop Recording" else "Record Audio (${recordedFiles.size}/5)")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Minimal display: show filenames (no play buttons)
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Recorded files (${recordedFiles.size}/5):", 
                    style = MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.ui.graphics.Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                recordedFiles.forEachIndexed { idx, path ->
                    // Display just the filename — no playback action
                    Text(
                        text = File(path).name, 
                        modifier = Modifier.padding(vertical = 2.dp),
                        color = androidx.compose.ui.graphics.Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save User Button
            Button(
                onClick = {
                    if (recordedFiles.size == 5 && userName.text.isNotEmpty()) {
                        viewModel.addUser(
                            name = userName.text,
                            gender = selectedGender,
                            audioPaths = recordedFiles.toList()
                        )
                        Toast.makeText(context, "User saved successfully!", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    } else {
                        Toast.makeText(
                            context,
                            "Please complete all 5 recordings before saving.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                enabled = recordedFiles.size == 5,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = androidx.compose.ui.graphics.Color(0xFF8E44AD), // Purple for save button
                    contentColor = androidx.compose.ui.graphics.Color.White
                )
            ) {
                Text("Save User")
            }
        }
    }
}

/**
 * Writes the WAV header into the given file by updating the first 44 bytes.
 * totalAudioLen is the number of raw PCM bytes written after the header.
 */
@Throws(Exception::class)
private fun writeWavHeader(
    file: File,
    totalAudioLen: Long,
    sampleRate: Long,
    channels: Int,
    byteRate: Long,
    bitsPerSample: Int
) {
    val totalDataLen = totalAudioLen + 36
    val raf = RandomAccessFile(file, "rw")
    try {
        raf.seek(0)
        val header = ByteBuffer.allocate(44)
        header.order(ByteOrder.LITTLE_ENDIAN)

        header.put("RIFF".toByteArray(Charsets.US_ASCII))
        header.putInt((totalDataLen and 0xffffffffL).toInt())
        header.put("WAVE".toByteArray(Charsets.US_ASCII))

        header.put("fmt ".toByteArray(Charsets.US_ASCII))
        header.putInt(16)
        header.putShort(1.toShort())
        header.putShort(channels.toShort())
        header.putInt(sampleRate.toInt())
        header.putInt(byteRate.toInt())
        header.putShort((channels * bitsPerSample / 8).toShort())
        header.putShort(bitsPerSample.toShort())

        header.put("data".toByteArray(Charsets.US_ASCII))
        header.putInt((totalAudioLen and 0xffffffffL).toInt())

        raf.write(header.array())
    } finally {
        raf.close()
    }
}
