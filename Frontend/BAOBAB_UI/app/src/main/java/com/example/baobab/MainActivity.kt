package com.example.baobab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.baobab.ui.theme.BAOBABTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            BAOBABTheme {

                var currentScreen by remember {
                    mutableStateOf("login")
                }

                when (currentScreen) {

                    "login" -> {
                        LoginScreen(
                            onSignUpClick = {
                                currentScreen = "start"
                            },
                            onGuestLoginClick = {
                                currentScreen = "home"
                            }
                        )
                    }

                    "start" -> {
                        SplashScreen()
                    }

                    "home" -> {
                        HomeScreen(
                            onSurveyClick = { survey ->
                                currentScreen = "detail"
                            },
                            onSearchClick = {
                                currentScreen = "search"
                            },
                            onCreateSurveyClick = {
                                currentScreen = "create"
                            },
                            onMyClick = {
                                currentScreen = "my"
                            }
                        )
                    }

                    "search" -> {
                        Text("검색 화면")
                    }

                    "detail" -> {
                        Text("설문 상세 화면")
                    }

                    "create" -> {
                        Text("설문 작성 화면")
                    }

                    "my" -> {
                        Text("MY 화면")
                    }
                }
            }
        }
    }
}