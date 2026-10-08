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
import android.net.Uri
import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

class MainActivity : ComponentActivity() {
    private val navigation by lazy { ViewModelProvider(this)[BaobabViewModel::class.java] }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openSharedSurvey(intent)

        if (isPaymentReturn(intent)) {
            val account =
                ViewModelProvider(this)[AccountViewModel::class.java]

            val myPage =
                ViewModelProvider(this)[MyPageViewModel::class.java]

            if (account.token != null) {
                myPage.load(account.token)
            } else {
                account.restoreSession {
                    myPage.load(account.token)
                }
            }
        }
    }

    private fun isPaymentReturn(intent: Intent): Boolean {
        val uri = intent.data ?: return false

        return intent.action == Intent.ACTION_VIEW &&
                uri.scheme == "baobab" &&
                uri.host == "payments" &&
                uri.path == "/return"
    }

    private fun openSharedSurvey(intent: Intent) {
        if (isPaymentReturn(intent)) {
            navigation.navigate(BaobabScreen.POINTS)
            return
        }

        if (intent.action == Intent.ACTION_VIEW) {
            sharedSurvey(intent.dataString)?.let {
                navigation.openSurvey(SurveyItem(id = it.id), it.referralToken)
            }
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
        if (savedInstanceState == null || viewModel.selectedSurvey == null) {
            openSharedSurvey(intent)

            if (isPaymentReturn(intent)) {
                account.restoreSession {
                    myPage.load(account.token)
                }
            }
        }


        setContent {
            BAOBABTheme {
                val onShare: (SurveyItem) -> Unit = { survey ->
                    if (account.token == null) {
                        Toast.makeText(this@MainActivity, "공유 포인트를 받으려면 로그인해주세요.", Toast.LENGTH_SHORT).show()
                        viewModel.navigate(BaobabScreen.LOGIN)
                    } else account.createShareLink(survey, { reward ->
                        val link = surveyWebShareLink(survey.id, reward.shareToken, BuildConfig.SURVEY_API_BASE_URL)
                        shareSurvey(this@MainActivity, survey, link)
                        Toast.makeText(this@MainActivity, "링크를 받은 사람이 설문을 완료하면 ${reward.rewardPoint}P를 받아요.", Toast.LENGTH_LONG).show()
                    }, { message -> Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show() })
                }
                LaunchedEffect(myPage.sessionExpired) {
                    if (myPage.sessionExpired) {
                        account.invalidateSession()
                        myPage.load(null)
                        if (viewModel.currentScreen == BaobabScreen.MY) viewModel.navigate(BaobabScreen.LOGIN)
                    }
                }
                var choosingParticipation by remember { mutableStateOf(false) }
                var pendingDeletion by remember { mutableStateOf<SurveyItem?>(null) }
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

                SurveyDraftSupport(
                    model = viewModel,
                    token = account.token,
                    submitting = account.busy
                ) { onDraftCreate, onDraftBack, onDraftCreated ->

                    if (choosingParticipation) {
                        ParticipationCreationDialog(
                            onDismiss = { choosingParticipation = false },
                            onSurvey = {
                                choosingParticipation = false
                                if (account.token != null) onDraftCreate() else {
                                    viewModel.resumeCreationAfterLogin = true
                                    account.clearError()
                                    viewModel.navigate(BaobabScreen.LOGIN)
                                }
                            },
                            onRecruitment = {
                                choosingParticipation = false
                                viewModel.openRecruitment(page = "CREATE")
                            }
                        )
                    }
                    BackHandler(enabled = viewModel.canGoBack || account.busy) {
                        if (!account.busy) onDraftBack()
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
                                AccountScreen(
                                    false, account,
                                    onSuccess = {
                                        if (viewModel.resumeParticipationAfterLogin) {
                                            viewModel.resumeParticipationAfterLogin = false
                                            viewModel.navigate(BaobabScreen.DETAIL)
                                            account.checkParticipation(requireNotNull(participation.survey)) {
                                                viewModel.navigate(BaobabScreen.PARTICIPATE)
                                            }
                                        } else if (viewModel.resumeRecruitmentAfterLogin) {
                                            viewModel.resumeRecruitmentAfterLogin = false
                                            viewModel.goBack()
                                        } else if (viewModel.resumeCreationAfterLogin) {
                                            viewModel.resumeCreationAfterLogin = false
                                            onDraftCreate()
                                        } else viewModel.goHome()
                                    },
                                    onSwitch = {
                                        account.clearError(); viewModel.navigate(
                                        BaobabScreen.START
                                    )
                                    },
                                    onGuest = {
                                        viewModel.resumeRecruitmentAfterLogin = false
                                        viewModel.resumeParticipationAfterLogin =
                                            false; viewModel.resumeCreationAfterLogin =
                                        false; viewModel.goHome()
                                    })
                            }
                        }

                        // 시작 화면
                        BaobabScreen.START -> {
                            LocalNetworkPermissionGate {
                                AccountScreen(
                                    true,
                                    account,
                                    onSuccess = { account.clearError(); viewModel.goBack() },
                                    onSwitch = { account.clearError(); viewModel.goBack() },
                                    onGuest = {})
                            }
                        }

                        // 홈
                        BaobabScreen.HOME -> {
                            LocalNetworkPermissionGate {
                                LaunchedEffect(Unit) { surveyData.loadSurveys(); surveyData.loadRecruitments(force = true) }
                                LaunchedEffect(account.token) { myPage.load(account.token) }
                                HomeScreen(
                                    currentPoint = myPage.profile?.point ?: account.point,
                                    loggedIn = account.token != null,
                                    onPointClick = { viewModel.navigate(BaobabScreen.POINTS) },
                                    surveys = surveyData.feedItems,
                                    loading = surveyData.listState.loading || surveyData.recruitmentState.loading,
                                    error = listOfNotNull(surveyData.listState.error, surveyData.recruitmentState.error).joinToString("\n").ifBlank { null },
                                    onRetry = {
                                        surveyData.loadSurveys(force = true); surveyData.loadRecruitments(force = true); myPage.load(
                                        account.token
                                    )
                                    },
                                    searchTerm = viewModel.homeSearchTerm,
                                    onSearchTermChange = { viewModel.homeSearchTerm = it },
                                    onSurveyClick = { survey ->
                                        survey.recruitmentId?.let { viewModel.openRecruitment(it) }
                                            ?: viewModel.openSurvey(survey)
                                    },
                                    onSearchClick = { query -> viewModel.openSearch(query) },
                                    onCreateSurveyClick = { choosingParticipation = true },
                                    onMyClick = {
                                        viewModel.navigate(BaobabScreen.MY)
                                    }
                                )
                            }
                        }

                        // 검색 결과
                        BaobabScreen.SEARCH -> {
                            LocalNetworkPermissionGate {
                                LaunchedEffect(Unit) { surveyData.loadSurveys(); surveyData.loadRecruitments(force = true) }
                                SearchResultsScreen(
                                    surveys = surveyData.feedItems,
                                    loading = surveyData.listState.loading || surveyData.recruitmentState.loading,
                                    error = listOfNotNull(surveyData.listState.error, surveyData.recruitmentState.error).joinToString("\n").ifBlank { null },
                                    onRetry = { surveyData.loadSurveys(force = true); surveyData.loadRecruitments(force = true) },
                                    searchTerm = viewModel.searchTerm,
                                    category = viewModel.searchCategory,
                                    onCategoryChange = { viewModel.searchCategory = it },
                                    onSurveyClick = { survey ->
                                        survey.recruitmentId?.let { viewModel.openRecruitment(it) }
                                            ?: viewModel.openSurvey(survey)
                                    },
                                    onCreateSurveyClick = { choosingParticipation = true },
                                    onMyClick = {
                                        viewModel.navigate(BaobabScreen.MY)
                                    },
                                    onSearchTermChange = { viewModel.searchTerm = it },
                                    onBack = { viewModel.goBack() }
                                )
                            }
                        }

                        // 설문 상세
                        BaobabScreen.DETAIL -> {
                            LocalNetworkPermissionGate {
                                LaunchedEffect(Unit) { account.restoreSession { } }
                                val selected = requireNotNull(viewModel.selectedSurvey)
                                LaunchedEffect(account.token) {
                                    if (account.token != null && myPage.profile == null) myPage.load(
                                        account.token
                                    )
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
                                            onShare = onShare,
                                            shareBusy = account.busy,
                                            onBack = { viewModel.goBack() },
                                            survey = survey,
                                            canDelete = account.token != null && myPage.profile?.id != null &&
                                                    survey.userId == myPage.profile?.id,
                                            onDelete = {
                                                account.clearError(); pendingDeletion = survey
                                            },
                                            onEdit = {
                                                account.clearError(); viewModel.beginEditing(
                                                survey
                                            )
                                            },
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
                                ParticipationScreen(
                                    survey, participation.answers, account.busy, account.error,
                                    onAnswer = { id, value -> participation.answers[id] = value },
                                    onSubmit = {
                                        account.participate(survey, participation.answers, viewModel.referralFor(survey.id)) {
                                            viewModel.clearReferral(survey.id)
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
                                onBackClick = { onDraftBack() },
                                onNextClick = { _ ->
                                    viewModel.navigate(BaobabScreen.CREATE_TWO)
                                }
                            )
                        }

                        // 설문 작성 2단계
                        BaobabScreen.CREATE_TWO -> {
                            SurveyCreationStepTwoScreen(
                                state = viewModel.creationState,
                                onBackClick = { onDraftBack() },
                                onNextClick = { _ ->
                                    viewModel.navigate(BaobabScreen.CREATE_THREE)
                                }
                            )
                        }

                        // 설문 작성 3단계
                        BaobabScreen.CREATE_THREE -> {
                            LocalNetworkPermissionGate {
                                SurveyCreationStepThreeScreen(
                                    currentPoint = myPage.profile?.point ?: account.point,
                                    submitting = account.busy,
                                    error = account.error,
                                    state = viewModel.creationState,
                                    onBackClick = { if (!account.busy) onDraftBack() },
                                    onCompleteClick = { settings ->
                                        if (account.token == null) {
                                            viewModel.resumeCreationAfterLogin = true
                                            viewModel.navigate(BaobabScreen.LOGIN)
                                        } else {
                                            val draft = viewModel.snapshotDraft(settings)
                                            account.create(draft) {
                                                onDraftCreated()
                                                viewModel.completeCreation(draft)
                                                myPage.load(account.token)
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
                                LaunchedEffect(account.token) {
                                    myPage.load(account.token)
                                }

                                // 결제 브라우저에서 앱으로 돌아오면 잔액 갱신
                                DisposableEffect(account.token) {
                                    val activityLifecycle =
                                        this@MainActivity.lifecycle

                                    val observer = LifecycleEventObserver { _, event ->
                                        if (event == Lifecycle.Event.ON_RESUME) {
                                            myPage.load(account.token)
                                        }
                                    }

                                    activityLifecycle.addObserver(observer)

                                    onDispose {
                                        activityLifecycle.removeObserver(observer)
                                    }
                                }

                                key(account.token) {
                                    PointWalletScreen(
                                        point = myPage.profile?.point,
                                        coupons = myPage.coupons,
                                        exchangeBusy = myPage.exchangingCoupon,
                                        exchangeError = myPage.couponError,
                                        onExchange = { itemId, onSuccess ->
                                            account.token?.let { token ->
                                                myPage.exchangeCoupon(
                                                    token = token,
                                                    itemId = itemId,
                                                    success = onSuccess
                                                )
                                            }
                                        },
                                        loggedIn = account.token != null,
                                        loading = myPage.loading,
                                        error = myPage.error,
                                        paymentBusy = account.busy ||
                                                myPage.exchangingCoupon,
                                        paymentError = account.error,
                                        onCharge = { provider, amount ->
                                            account.startPointPayment(
                                                provider = provider,
                                                amount = amount
                                            ) { checkoutUrl ->
                                                try {
                                                    startActivity(
                                                        Intent(
                                                            Intent.ACTION_VIEW,
                                                            Uri.parse(checkoutUrl)
                                                        )
                                                    )
                                                } catch (_: ActivityNotFoundException) {
                                                    Toast.makeText(
                                                        this@MainActivity,
                                                        "결제창을 열 브라우저가 없습니다.",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            }
                                        },
                                        onHistoryClick = {
                                            viewModel.navigate(BaobabScreen.POINT_HISTORY)
                                        },
                                        onRetry = {
                                            myPage.load(account.token)
                                        },
                                        onBack = {
                                            viewModel.goBack()
                                        },
                                        onLogin = {
                                            account.clearError()
                                            viewModel.navigate(BaobabScreen.LOGIN)
                                        }
                                    )
                                }
                            }
                        }

                        BaobabScreen.POINT_HISTORY -> {
                            LocalNetworkPermissionGate {
                                PointHistoryScreen(
                                    token = account.token,
                                    onBack = {
                                        viewModel.goBack()
                                    }
                                )
                            }
                        }
                        BaobabScreen.RECRUITMENTS -> {
                            LocalNetworkPermissionGate {
                                key(account.token) {
                                    RecruitmentHubScreen(
                                        token = account.token,
                                        initialPage = viewModel.recruitmentEntry,
                                        initialDetailId = viewModel.selectedRecruitmentId,
                                        onBack = { viewModel.goBack() },
                                        onLogin = {
                                            viewModel.resumeRecruitmentAfterLogin = true
                                            account.clearError()
                                            viewModel.navigate(BaobabScreen.LOGIN)
                                        },
                                        onHome = {
                                            viewModel.navigate(BaobabScreen.HOME)
                                        },
                                        onSearch = {
                                            viewModel.openSearch("")
                                        },
                                        onMy = {
                                            viewModel.navigate(BaobabScreen.MY)
                                        },
                                        onPointHistory = {
                                            viewModel.navigate(BaobabScreen.POINT_HISTORY)
                                        }
                                    )
                                }
                            }
                        }
                        BaobabScreen.EDIT -> {
                            val survey = requireNotNull(viewModel.editingSurvey)
                            SurveyEditScreen(
                                survey, viewModel.editState, account.busy, account.error,
                                onBack = { viewModel.goBack() },
                                onSave = { draft, status ->
                                    account.editSurvey(survey, draft, status) {
                                        viewModel.goBack()
                                        surveyData.loadSurveys(force = true)
                                        surveyData.loadDetail(survey.id)
                                        myPage.load(account.token)
                                    }
                                })
                        }

                        // MY
                        BaobabScreen.MY -> {
                            LocalNetworkPermissionGate {
                                MyPageScreen(
                                    onShare = onShare,
                                    shareBusy = account.busy,
                                    model = myPage, token = account.token,
                                    onOpenRecruitment = { viewModel.openRecruitment(it) },
                                    onManageRecruitment = { viewModel.openRecruitment(it, page = "MANAGE") },
                                    onPointHistory = { viewModel.navigate(BaobabScreen.POINT_HISTORY) },
                                    onBack = { viewModel.goHome() },
                                    onLogin = {
                                        account.clearError(); viewModel.navigate(
                                        BaobabScreen.LOGIN
                                    )
                                    },
                                    onLogout = { account.logout(); myPage.load(null); viewModel.goHome() },
                                    onCreate = { choosingParticipation = true },
                                    onProfileUpdated = { surveyData.loadSurveys(force = true) },
                                    onEdit = { account.clearError(); viewModel.beginEditing(it) },
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
}
