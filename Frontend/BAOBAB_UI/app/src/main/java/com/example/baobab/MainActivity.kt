package com.example.baobab

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material3.Text
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModel
import com.example.baobab.ui.theme.BAOBABTheme

class MainActivity : ComponentActivity() {
    private val navigation by lazy { ViewModelProvider(this)[BaobabViewModel::class.java] }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openSharedSurvey(intent)
    }

    private fun openSharedSurvey(intent: Intent) {
        if (intent.action == Intent.ACTION_VIEW) sharedSurveyId(intent.dataString)?.let {
            navigation.openSurvey(SurveyItem(id = it))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val viewModel = ViewModelProvider(this)[BaobabViewModel::class.java]
        val surveyData = ViewModelProvider(this)[SurveyDataViewModel::class.java]
        val account = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = AccountViewModel(
                sessionStore = EncryptedSessionStore(applicationContext, BuildConfig.SURVEY_API_BASE_URL)
            ) as T
        })[AccountViewModel::class.java]
        val participation = ViewModelProvider(this)[ParticipationViewModel::class.java]
        val myPage = ViewModelProvider(this)[MyPageViewModel::class.java]
        if (savedInstanceState == null) openSharedSurvey(intent)


        setContent {
            BAOBABTheme {
                LaunchedEffect(myPage.sessionExpired) {
                    if (myPage.sessionExpired) {
                        account.invalidateSession()
                        myPage.load(null)
                        if (viewModel.currentScreen == BaobabScreen.MY) viewModel.navigate(BaobabScreen.LOGIN)
                    }
                }
                var pendingDeletion by remember { mutableStateOf<SurveyItem?>(null) }
                val demoWallet = remember(account.token) { DemoWalletState() }
                pendingDeletion?.let { survey ->
                    SurveyDeleteDialog(
                        survey = survey,
                        deleting = account.busy,
                        error = account.error,
                        onDismiss = { pendingDeletion = null; account.clearError() },
                        onConfirm = {
                            account.deleteSurvey(survey) {
                                pendingDeletion = null
                                surveyData.loadSurveys(force = true)
                                myPage.load(account.token)
                                if (viewModel.currentScreen == BaobabScreen.DETAIL) viewModel.goHome()
                            }
                        }
                    )
                }
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
                            LaunchedEffect(Unit) {
                                account.restoreSession { restored ->
                                    if (restored && viewModel.currentScreen == BaobabScreen.LOGIN) viewModel.goHome()
                                }
                            }
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
                            LaunchedEffect(account.token) { myPage.load(account.token) }
                            HomeScreen(
                                currentPoint = myPage.profile?.point ?: account.point,
                                loggedIn = account.token != null,
                                onPointClick = { viewModel.navigate(BaobabScreen.POINTS) },
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
                            LaunchedEffect(Unit) { account.restoreSession { } }
                            val selected = requireNotNull(viewModel.selectedSurvey)
                            LaunchedEffect(account.token) {
                                if (account.token != null && myPage.profile == null) myPage.load(account.token)
                            }
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
                                        canDelete = account.token != null && myPage.profile?.id != null &&
                                            survey.userId == myPage.profile?.id,
                                        onDelete = { account.clearError(); pendingDeletion = survey },
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

                    BaobabScreen.POINTS -> {
                        LocalNetworkPermissionGate {
                            LaunchedEffect(account.token) { myPage.load(account.token) }
                            key(account.token) {
                                PointWalletScreen(
                                    wallet = demoWallet,
                                    point = myPage.profile?.point ?: account.point,
                                    loggedIn = account.token != null,
                                    loading = myPage.loading,
                                    error = myPage.error,
                                    onRetry = { myPage.load(account.token) },
                                    onBack = { viewModel.goBack() },
                                    onLogin = { account.clearError(); viewModel.navigate(BaobabScreen.LOGIN) }
                                )
                            }
                        }
                    }

                    // MY
                    BaobabScreen.MY -> {
                        LocalNetworkPermissionGate {
                            MyPageScreen(myPage, account.token,
                                onBack = { viewModel.goHome() },
                                onLogin = { account.clearError(); viewModel.navigate(BaobabScreen.LOGIN) },
                                onLogout = { account.logout(); myPage.load(null); viewModel.goHome() },
                                onCreate = { viewModel.beginCreation() },
                                onProfileUpdated = { surveyData.loadSurveys(force = true) },
                                onOpenSurvey = { viewModel.openSurvey(SurveyItem(id = it)) },
                                onPointClick = { viewModel.navigate(BaobabScreen.POINTS) },
                                onDelete = { account.clearError(); pendingDeletion = it })
                        }
                    }
                }
            }
        }
    }
}
