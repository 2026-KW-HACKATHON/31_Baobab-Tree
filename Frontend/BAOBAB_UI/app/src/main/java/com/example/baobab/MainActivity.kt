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

                    // 로그인
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

                    // 시작 화면
                    "start" -> {
                        SplashScreen()
                    }

                    // 홈
                    "home" -> {
                        HomeScreen(
                            onSurveyClick = { survey ->
                                currentScreen = "detail"
                            },
                            onSearchClick = {
                                currentScreen = "search"
                            },
                            onCreateSurveyClick = {
                                currentScreen = "create1"
                            },
                            onMyClick = {
                                currentScreen = "my"
                            }
                        )
                    }

                    // 검색 결과
                    "search" -> {
                        SearchResultsScreen(
                            onSurveyClick = { survey ->
                                currentScreen = "detail"
                            },
                            onCreateSurveyClick = {
                                currentScreen = "create1"
                            },
                            onMyClick = {
                                currentScreen = "my"
                            },
                            onSearchTermChange = {
                                // 검색어 변경 처리 필요하면 나중에 추가
                            }
                        )
                    }

                    // 설문 상세
                    "detail" -> {
                        SurveyDetailScreen(
                            onParticipateClick = {
                                currentScreen = "surveyComplete"
                            }
                        )
                    }

                    // 설문 참여 완료
                    "surveyComplete" -> {
                        SurveyCompletionScreen(
                            onHomeClick = {
                                currentScreen = "home"
                            }
                        )
                    }

                    // 설문 작성 1단계
                    "create1" -> {
                        SurveyCreationStepOneScreen(
                            onBackClick = {
                                currentScreen = "home"
                            },
                            onNextClick = { stepOneData ->
                                currentScreen = "create2"
                            }
                        )
                    }

                    // 설문 작성 2단계
                    "create2" -> {
                        SurveyCreationStepTwoScreen(
                            onBackClick = {
                                currentScreen = "create1"
                            },
                            onCreatePageClick = {
                                // 현재는 별도 동작 없음
                            },
                            onNextClick = { questions ->
                                currentScreen = "create3"
                            }
                        )
                    }

                    // 설문 작성 3단계
                    "create3" -> {
                        SurveyCreationStepThreeScreen(
                            onBackClick = {
                                currentScreen = "create2"
                            },
                            onImageAttachClick = {
                                // 이미지 첨부 기능은 나중에 연결
                            },
                            onCompleteClick = { settings ->
                                currentScreen = "createComplete"
                            }
                        )
                    }

                    // 설문 작성 완료
                    "createComplete" -> {
                        SurveyCreationCompleteScreen(
                            onHomeClick = {
                                currentScreen = "home"
                            }
                        )
                    }

                    // MY
                    "my" -> {
                        Text("MY 화면")
                    }
                }
            }
        }
    }
}