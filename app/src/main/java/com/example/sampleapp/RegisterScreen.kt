package com.example.sampleapp


import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun RegisterScreen(navController: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Register User", modifier = Modifier.padding(bottom = 48.dp))

//        Button(
//            onClick = { /* navigate to new user screen (we'll make next) */ },
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(bottom = 24.dp)
//        ) {
//            Text("New User")
//        }
        Button(
            onClick = { navController.navigate("new_user") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Text("New User")
        }


        Button(
            onClick = { navController.navigate("existing_user") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Existing Users")
        }

    }
}
