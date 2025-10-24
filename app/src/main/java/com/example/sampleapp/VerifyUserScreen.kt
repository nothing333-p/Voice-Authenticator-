package com.example.sampleapp

import android.Manifest
import android.media.MediaRecorder
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.sampleapp.ui.UserViewModel
import java.io.File

@Composable
fun VerifyUserScreen(
    navController: NavHostController,
    viewModel: UserViewModel
) {
    val context = LocalContext.current

    var mediaRecorder: MediaRecorder? by remember { mutableStateOf(null) }
    var isRecording by remember { mutableStateOf(false) }
    var recordedFilePath by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Microphone permission required", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Verify User", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(24.dp))

        // Record voice
        Button(
            onClick = {
                if (isRecording) {
                    try {
                        mediaRecorder?.stop()
                        mediaRecorder?.release()
                        mediaRecorder = null
                        isRecording = false
                        Toast.makeText(context, "Recording stopped", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                    val fileName = "verify_sample_${System.currentTimeMillis()}.m4a"
                    val file = File(outputDir, fileName)

                    try {
                        mediaRecorder = MediaRecorder().apply {
                            setAudioSource(MediaRecorder.AudioSource.MIC)
                            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                            setOutputFile(file.absolutePath)
                            prepare()
                            start()
                        }

                        recordedFilePath = file.absolutePath
                        isRecording = true
                        Toast.makeText(context, "Recording started...", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, "Recording failed", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isRecording) "Stop Recording" else "Record Sample")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (recordedFilePath != null) {
                    // TODO: Add verification logic here
                    Toast.makeText(context, "Verifying user...", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Please record your voice first", Toast.LENGTH_SHORT).show()
                }
            },
            enabled = recordedFilePath != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Verify")
        }
    }
}
