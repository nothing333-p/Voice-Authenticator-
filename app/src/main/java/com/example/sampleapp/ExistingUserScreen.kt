package com.example.sampleapp

import android.Manifest
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.sampleapp.data.User
import com.example.sampleapp.ui.UserViewModel
import java.io.File

@Composable
fun ExistingUserScreen(
    navController: NavHostController,
    viewModel: UserViewModel
) {
    val context = LocalContext.current
    val users by viewModel.users.collectAsState(initial = emptyList<User>())

    // Permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) Toast.makeText(context, "Microphone permission required", Toast.LENGTH_SHORT).show()
    }
    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        viewModel.loadUsers()
    }

    // Media player
    var mediaPlayer: MediaPlayer? by remember { mutableStateOf(null) }
    var currentlyPlaying by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Existing Users", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(users) { user ->
                var showDeleteDialog by remember { mutableStateOf(false) }

                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        title = { Text("Delete User") },
                        text = { Text("Are you sure you want to delete ${user.name}? This will also remove their recordings.") },
                        confirmButton = {
                            TextButton(onClick = {
                                user.audioPaths.forEach { path ->
                                    val file = File(path)
                                    if (file.exists()) file.delete()
                                }
                                viewModel.deleteUser(user)
                                Toast.makeText(context, "User deleted", Toast.LENGTH_SHORT).show()
                                showDeleteDialog = false
                            }) {
                                Text("Delete", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                UserCard(
                    user = user,
                    isPlaying = { currentlyPlaying == it },
                    onPlayAudio = { filePath ->
                        try {
                            mediaPlayer?.release()
                            mediaPlayer = MediaPlayer().apply {
                                setDataSource(filePath)
                                prepare()
                                start()
                                setOnCompletionListener { currentlyPlaying = null }
                            }
                            currentlyPlaying = filePath
                            Toast.makeText(context, "Playing ${filePath}", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, "Playback failed", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onStopAudio = {
                        mediaPlayer?.stop()
                        mediaPlayer?.release()
                        mediaPlayer = null
                        currentlyPlaying = null
                    },
                    onDeleteClick = { showDeleteDialog = true }
                )
            }
        }
    }
}

@Composable
fun UserCard(
    user: User,
    isPlaying: (String) -> Boolean,
    onPlayAudio: (String) -> Unit,
    onStopAudio: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current
    var mediaRecorder: MediaRecorder? by remember { mutableStateOf(null) }
    var isRecording by remember { mutableStateOf(false) }
    var recordedFilePath by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Header row with delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${user.name} (${user.gender})", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete User",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Audio samples playback
            user.audioPaths.forEachIndexed { index, path ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sample ${index + 1}")
                    Button(
                        onClick = { if (isPlaying(path)) onStopAudio() else onPlayAudio(path) },
                        modifier = Modifier.width(120.dp)
                    ) {
                        Text(if (isPlaying(path)) "Stop" else "Play")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Verification Record Button
            Button(
                onClick = {
                    if (isRecording) {
                        try {
                            mediaRecorder?.stop()
                            mediaRecorder?.release()
                            mediaRecorder = null
                            isRecording = false
                            Toast.makeText(context, "Recording stopped", Toast.LENGTH_SHORT).show()

                            // 🔍 Placeholder verification logic
                            recordedFilePath?.let {
                                val matchFound = verifyUser(it, user.audioPaths)
                                val message = if (matchFound)
                                    "✅ Verified as ${user.name}"
                                else
                                    "❌ Verification failed for ${user.name}"
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            }

                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                        val fileName = "${user.name}_verify_${System.currentTimeMillis()}.m4a"
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
                            Toast.makeText(context, "Recording started for ${user.name}...", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, "Recording failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isRecording) "Stop & Verify" else "Start Verification")
            }
        }
    }
}

/**
 * 🔍 Simple placeholder verification method.
 * Later this will be replaced by actual audio similarity logic.
 */
fun verifyUser(testFilePath: String, savedPaths: List<String>): Boolean {
    // TODO: Implement real verification logic (e.g., MFCC similarity)
    // For now, randomly simulate success
    return Math.random() > 0.5
}
