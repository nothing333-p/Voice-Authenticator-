

package com.example.sampleapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.sampleapp.data.AppDatabase
import com.example.sampleapp.data.UserRepository
import com.example.sampleapp.ui.UserViewModel
import com.example.sampleapp.ui.UserViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApp()
        }
    }
}

@Composable
fun MyApp() {
    val context = LocalContext.current

    // ✅ Initialize Room database + repository + ViewModel
    val db = remember { AppDatabase.getDatabase(context) }
    val repository = remember { UserRepository(db.userDao()) }
    val userViewModel: UserViewModel = viewModel(
        factory = UserViewModelFactory(repository)
    )

    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomeScreen(navController) }

        // ✅ New user registration screen
        composable("new_user") {
            NewUserScreen(navController, userViewModel)
        }

        // ✅ Existing users (playback, delete, verify)
        composable("existing_user") {
            ExistingUserScreen(navController, userViewModel)
        }

        // ✅ Optional verify user screen (if needed)
        composable("verify_user") {
            VerifyUserScreen(navController, userViewModel)
        }
    }
}

@Composable
fun HomeScreen(navController: NavHostController) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background image
        Image(
            painter = painterResource(R.drawable.background),
            contentDescription = "Background Image",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        
        // Content overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Voice Authenticator",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = androidx.compose.ui.graphics.Color(0xFFF5DEB3), // Beige color
                modifier = Modifier.padding(bottom = 48.dp)
            )

            // 🧍‍♂️ Register New User
            Button(
                onClick = { navController.navigate("new_user") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Text("New User Registration")
            }

            // 📁 Existing Users (play, delete, verify)
            Button(
                onClick = { navController.navigate("existing_user") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Existing Users / Verify")
            }
        }
    }
}
