package com.example.baobab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.material3.Text
import androidx.lifecycle.ViewModelProvider
import com.example.baobab.ui.theme.BAOBABTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val viewModel = ViewModelProvider(this)[BaobabViewModel::class.java]
        val surveyData = ViewModelProvider(this)[SurveyDataViewModel::class.java]
        val account = ViewModelProvider(this)[AccountViewModel::class.java]
        val participation = ViewModelProvider(this)[ParticipationViewModel::class.java]


        setContent {
            BAOBABTheme {
                account.participationNotice?.let { notice ->
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { account.dismissParticipationNotice() },
                        title = { Text("설문 참여 안내") }, text = { Text(notice) },
                        confirmButton = {
                            androidx.compose.material3.TextButton(onClick = { account.dismissParticipationNotice() }) { Text("확인") }
                        })
                }

                BackHandler(enabled = viewModel.canGoBack || account.busy) {
                    if (!account.busy) viewModel.goBack()
                }

                when (viewModel.currentScreen) {

                    // 로그인
                    BaobabScreen.LOGIN -> {
                        LocalNetworkPermissionGate {
                            AccountScreen(false, account,
                                onSuccess = {
                                    if (viewModel.resumeParticipationAfterLogin) {
                                        viewModel.resumeParticipationAfterLogin = false
                                        viewModel.navigate(BaobabScreen.DETAIL)
                                        account.checkParticipation(requireNotNull(participation.survey)) {
                                            viewModel.navigate(BaobabScreen.PARTICIPATE)
                                        }
                                    } else if (viewModel.resumeCreationAfterLogin) {
                                        viewModel.resumeCreationAfterLogin = false
                                        viewModel.beginCreation()
                                    } else viewModel.goHome()
                                },
                                onSwitch = { account.clearError(); viewModel.navigate(BaobabScreen.START) },
                                onGuest = { viewModel.resumeParticipationAfterLogin = false; viewModel.resumeCreationAfterLogin = false; viewModel.goHome() })
                        }
                    }

                    // 시작 화면
                    BaobabScreen.START -> {
                        LocalNetworkPermissionGate {
                            AccountScreen(true, account,
                                onSuccess = { account.clearError(); viewModel.goBack() },
                                onSwitch = { account.clearError(); viewModel.goBack() }, onGuest = {})
                        }
                    }

                    // 홈
                    BaobabScreen.HOME -> {
                        LocalNetworkPermissionGate {
                            LaunchedEffect(Unit) { surveyData.loadSurveys() }
                            HomeScreen(
                                surveys = surveyData.listState.data.orEmpty(),
                                loading = surveyData.listState.loading,
                                error = surveyData.listState.error,
                                onRetry = { surveyData.loadSurveys(force = true) },
                                searchTerm = viewModel.homeSearchTerm,
                                onSearchTermChange = { viewModel.homeSearchTerm = it },
                                onSurveyClick = { survey ->
                                    viewModel.openSurvey(survey)
                                },
                                onSearchClick = { query -> viewModel.openSearch(query) },
                                onCreateSurveyClick = {
                                    if (account.token != null) viewModel.beginCreation()
                                    else {
                                        viewModel.resumeCreationAfterLogin = true
                                        account.clearError()
                                        viewModel.navigate(BaobabScreen.LOGIN)
                                    }
                                },
                                onMyClick = {
                                    viewModel.navigate(BaobabScreen.MY)
                                }
                            )
                        }
                    }

                    // 검색 결과
                    BaobabScreen.SEARCH -> {
                        LocalNetworkPermissionGate {
                            LaunchedEffect(Unit) { surveyData.loadSurveys() }
                            SearchResultsScreen(
                                surveys = surveyData.listState.data.orEmpty(),
                                loading = surveyData.listState.loading,
                                error = surveyData.listState.error,
                                onRetry = { surveyData.loadSurveys(force = true) },
                                searchTerm = viewModel.searchTerm,
                                category = viewModel.searchCategory,
                                onCategoryChange = { viewModel.searchCategory = it },
                                onSurveyClick = { survey ->
                                    viewModel.openSurvey(survey)
                                },
                                onCreateSurveyClick = {
                                    if (account.token != null) viewModel.beginCreation()
                                    else {
                                        viewModel.resumeCreationAfterLogin = true
                                        account.clearError()
                                        viewModel.navigate(BaobabScreen.LOGIN)
                                    }
                                },
                                onMyClick = {
                                    viewModel.navigate(BaobabScreen.MY)
                                },
                                onSearchTermChange = { viewModel.searchTerm = it }
                            )
                        }
                    }

                    // 설문 상세
                    BaobabScreen.DETAIL -> {
                        LocalNetworkPermissionGate {
                            val selected = requireNotNull(viewModel.selectedSurvey)
                            LaunchedEffect(selected.id) { surveyData.loadDetail(selected.id) }
                            SurveyRequestContent(
                                loading = surveyData.detailId != selected.id || surveyData.detailState.loading,
                                error = surveyData.detailState.error,
                                data = surveyData.detailState.data,
                                onRetry = { surveyData.loadDetail(selected.id) },
                                modifier = Modifier.fillMaxSize()
                            ) { survey ->
                                key(survey.id) {
                                    SurveyDetailScreen(
                                        survey = survey,
                                        relatedSurveys = surveyData.listState.data.orEmpty(),
                                        onRelatedSurveyClick = { viewModel.openSurvey(it) },
                                        onParticipateClick = {
                                            account.clearError()
                                            participation.begin(survey)
                                            if (account.token == null) {
                                                viewModel.resumeCreationAfterLogin = false
                                                viewModel.resumeParticipationAfterLogin = true
                                                viewModel.navigate(BaobabScreen.LOGIN)
                                            } else account.checkParticipation(survey) {
                                                viewModel.navigate(BaobabScreen.PARTICIPATE)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 설문 참여 완료
                    BaobabScreen.PARTICIPATE -> {
                        LocalNetworkPermissionGate {
                            val survey = requireNotNull(participation.survey)
                            ParticipationScreen(survey, participation.answers, account.busy, account.error,
                                onAnswer = { id, value -> participation.answers[id] = value },
                                onSubmit = {
                                    account.participate(survey, participation.answers) {
                                        viewModel.navigate(BaobabScreen.SURVEY_COMPLETE)
                                        surveyData.loadSurveys(force = true)
                                        surveyData.loadDetail(survey.id)
                                    }
                                },
                                onBack = { viewModel.goBack() },
                                needsLogin = account.token == null,
                                onLogin = {
                                    account.clearError()
                                    viewModel.resumeCreationAfterLogin = false
                                    viewModel.resumeParticipationAfterLogin = true
                                    viewModel.navigate(BaobabScreen.LOGIN)
                                })
                        }
                    }

                    BaobabScreen.SURVEY_COMPLETE -> {
                        SurveyCompletionScreen(
                            result = account.participationResult,
                            onHomeClick = {
                                viewModel.goHome()
                            }
                        )
                    }

                    // 설문 작성 1단계
                    BaobabScreen.CREATE_ONE -> {
                        SurveyCreationStepOneScreen(
                            state = viewModel.creationState,
                            onBackClick = { viewModel.goBack() },
                            onNextClick = { _ ->
                                viewModel.navigate(BaobabScreen.CREATE_TWO)
                            }
                        )
                    }

                    // 설문 작성 2단계
                    BaobabScreen.CREATE_TWO -> {
                        SurveyCreationStepTwoScreen(
                            state = viewModel.creationState,
                            onBackClick = { viewModel.goBack() },
                            onCreatePageClick = {
                                // 현재는 별도 동작 없음
                            },
                            onNextClick = { _ ->
                                viewModel.navigate(BaobabScreen.CREATE_THREE)
                            }
                        )
                    }

                    // 설문 작성 3단계
                    BaobabScreen.CREATE_THREE -> {
                        LocalNetworkPermissionGate {
                        SurveyCreationStepThreeScreen(
                            submitting = account.busy,
                            error = account.error,
                            state = viewModel.creationState,
                            onBackClick = { if (!account.busy) viewModel.goBack() },
                            onImageAttachClick = {
                                // 이미지 첨부 기능은 나중에 연결
                            },
                            onCompleteClick = { settings ->
                                if (account.token == null) {
                                    viewModel.resumeCreationAfterLogin = true
                                    viewModel.navigate(BaobabScreen.LOGIN)
                                } else {
                                    val draft = viewModel.snapshotDraft(settings)
                                    account.create(draft) {
                                        viewModel.completeCreation(draft)
                                        surveyData.loadSurveys(force = true)
                                    }
                                }
                            }
                        )
                    }

                    }

                    // 설문 작성 완료
                    BaobabScreen.CREATE_COMPLETE -> {
                        SurveyCreationCompleteScreen(
                            onHomeClick = {
                                viewModel.goHome()
                            }
                        )
                    }

                    // MY
                    BaobabScreen.MY -> {
                        Text("MY 화면")
                    }
                }
            }
        }
    }
}
