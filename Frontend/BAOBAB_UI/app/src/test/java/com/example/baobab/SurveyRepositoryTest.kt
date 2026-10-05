package com.example.baobab

import com.sun.net.httpserver.HttpServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.net.InetSocketAddress

class SurveyRepositoryTest {
    private lateinit var server: HttpServer
    private lateinit var repository: HttpSurveyRepository
    private var body = "[]"
    private var status = 200
    private var requestedPath = ""
    private var requestedMethod = ""
    private var requestedBody = ""
    private var authorization: String? = null

    @Before
    fun setUp() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/") { exchange ->
            requestedPath = exchange.requestURI.path
            requestedMethod = exchange.requestMethod
            requestedBody = exchange.requestBody.bufferedReader(Charsets.UTF_8).use { it.readText() }
            authorization = exchange.requestHeaders.getFirst("Authorization")
            val bytes = body.toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        repository = HttpSurveyRepository("http://127.0.0.1:${server.address.port}/api")
    }

    @After
    fun tearDown() { server.stop(0) }

    private val surveyJson = """{
        "id": 7, "title": "Travel", "category": null,
        "author": {"id": 1, "name": "Author"}, "rewardPoint": 300,
        "currentCount": 2, "status": "OPEN", "endDate": "2099-01-01T00:00:00.000Z",
        "questions": [{"id": 9, "question": "Bus?", "questionType": "single",
          "options": [{"id": 11, "optionText": "Yes"}, {"id": 12, "optionText": "No"}]}]
    }"""

    @Test
    fun mapsSchemaFieldsAndKeepsRealQuestionAndOptionIds() {
        body = "[$surveyJson]"
        val survey = repository.getSurveys().single()
        assertEquals("/api/surveys", requestedPath)
        assertEquals("7", survey.id)
        assertEquals("Author", survey.author)
        assertEquals("300P", survey.points)
        assertEquals(2, survey.participantCount)
        assertEquals(1, survey.questionCount)
        assertEquals(9, survey.questions.single().id)
        assertEquals(11, survey.questions.single().options.first().id)
        assertNotNull(survey.deadline)
        assertNull(survey.description)
        body = surveyJson
        assertEquals(survey, repository.getSurvey("7"))
        assertEquals("/api/surveys/7", requestedPath)
    }

    @Test
    fun emptyListStaysEmptyWithoutSampleFallback() {
        assertTrue(repository.getSurveys().isEmpty())
    }

    @Test
    fun mapsSavedSurveyFieldsAndQuestionRequirementAndParticipationAnswers() {
        body = "[$surveyJson]".replace("\"category\": null", """"category": null,
            "description":"About","audience":"Students","duration":"5분","imageData":"data:image/jpeg;base64,/9j/"""")
            .replace("\"questionType\": \"single\"", "\"questionType\": \"single\", \"required\": false")
        val survey = repository.getSurveys().single()
        assertEquals("About", survey.description)
        assertEquals("Students", survey.audience)
        assertEquals("5분", survey.duration)
        assertFalse(survey.questions.single().required)
        assertNotNull(survey.imageData)
        body = """[{"id":3,"createdAt":"2026-10-03T00:00:00Z",
            "survey":{"id":7,"title":"Travel","category":"Life","rewardPoint":300},
            "answers":[{"answer":"Bus","question":{"question":"Bus or walk?"}}]}]"""
        val history = repository.getParticipations("session-token").single()
        assertEquals("7", history.surveyId)
        assertEquals(AnswerHistory("Bus or walk?", "Bus"), history.answers.single())
        assertEquals("Bearer session-token", authorization)
    }

    @Test
    fun optionalAnswersCanBeSkippedButRequiredAndInvalidAnswersAreRejected() {
        val required = SurveyQuestion(9, "Required", "short", emptyList())
        val optional = SurveyQuestion(10, "Optional", "single", listOf(SurveyOption(1, "Yes")), required = false)
        val survey = SurveyItem(id = "7", questions = listOf(required, optional))
        val payload = answerPayload(survey, mapOf(9 to "Answer", 10 to " "))
        val answers = com.google.gson.Gson().toJsonTree(payload).asJsonObject.getAsJsonArray("answers")
        assertEquals(1, answers.size())
        assertThrows(IllegalArgumentException::class.java) { answerPayload(survey, emptyMap()) }
        assertThrows(IllegalArgumentException::class.java) { answerPayload(survey, mapOf(9 to "Answer", 10 to "Invalid")) }
        assertThrows(IllegalArgumentException::class.java) { answerPayload(survey, mapOf(9 to "Answer", 99 to "Foreign")) }
        assertEquals(emptyList<Any>(), answerPayload(survey.copy(questions = listOf(optional)), emptyMap())["answers"])
    }

    @Test
    fun savedSessionRestoresInANewViewModelAndLogoutClearsIt() {
        val store = MemorySessionStore()
        val worker = QueuedWorker()
        val ui = java.util.ArrayDeque<Runnable>()
        val executor = java.util.concurrent.Executor { ui.add(it) }
        val first = AccountViewModel(repository, worker, executor, store)
        body = """{"accessToken":"saved-token"}"""
        first.login("author", "password") {}
        worker.runNext(); ui.removeFirst().run()
        assertEquals("saved-token", store.read())
        val restarted = AccountViewModel(repository, worker, executor, store)
        body = """{"id":1,"name":"Author","email":"a@example.com","loginId":"author","point":300}"""
        var restored = false
        restarted.restoreSession { restored = it }
        assertNull(restarted.token)
        worker.runNext(); ui.removeFirst().run()
        assertTrue(restored)
        assertEquals("saved-token", restarted.token)
        assertEquals(300, restarted.point)
        restarted.logout()
        assertNull(store.read())
        assertNull(restarted.token)
        worker.shutdownNow()
    }

    @Test
    fun serverFailureKeepsSavedSessionButExpiryClearsIt() {
        val store = MemorySessionStore().apply { write("saved-token") }
        val worker = QueuedWorker()
        val ui = java.util.ArrayDeque<Runnable>()
        val account = AccountViewModel(repository, worker, java.util.concurrent.Executor { ui.add(it) }, store)
        status = 500
        account.restoreSession { assertFalse(it) }
        worker.runNext(); ui.removeFirst().run()
        assertEquals("saved-token", store.read())
        assertTrue(account.hasSavedSession)
        status = 401
        account.restoreSession(force = true) { assertFalse(it) }
        worker.runNext(); ui.removeFirst().run()
        assertNull(store.read())
        assertFalse(account.hasSavedSession)
        worker.shutdownNow()
    }

    @Test
    fun patchesProfileWithAuthenticationAndOptionalFieldsCanBeCleared() {
        body = """{"id":42,"name":"New Name","email":"new@example.com","loginId":"author","point":300,"region":null,"ageGroup":null}"""
        val user = repository.updateProfile("profile-token", " New Name ", "new@example.com", "", "", "password")
        assertEquals("PATCH", requestedMethod)
        assertEquals("/api/users/me", requestedPath)
        assertEquals("Bearer profile-token", authorization)
        assertEquals("New Name", user.name)
        val payload = com.google.gson.Gson().fromJson(requestedBody, com.google.gson.JsonObject::class.java)
        assertEquals("New Name", payload.get("name").asString)
        assertEquals("password", payload.get("currentPassword").asString)
        assertTrue(payload.get("region").isJsonNull)
        assertTrue(payload.get("ageGroup").isJsonNull)
        assertFalse(payload.has("point"))
        assertThrows(SurveyApiException::class.java) { repository.updateProfile("profile-token", "", "new@example.com", "", "", "password") }
        assertThrows(SurveyApiException::class.java) { repository.updateProfile("profile-token", "Name", "invalid", "", "", "password") }
        status = 409
        assertThrows(SurveyApiException::class.java) { repository.updateProfile("profile-token", "Name", "taken@example.com", "", "", "password") }
    }

    @Test
    fun passwordVerificationUsesAuthenticatedRequestAndRejectsWrongPasswordWithoutLoggingOut() {
        body = """{"verified":true}"""
        repository.verifyPassword("profile-token", " password ")
        assertEquals("POST", requestedMethod)
        assertEquals("/api/auth/verify-password", requestedPath)
        assertEquals("Bearer profile-token", authorization)
        val payload = com.google.gson.Gson().fromJson(requestedBody, com.google.gson.JsonObject::class.java)
        assertEquals(" password ", payload.get("password").asString)
        status = 403
        val error = assertThrows(SurveyApiException::class.java) { repository.verifyPassword("profile-token", "wrong") }
        assertEquals(403, error.status)
        assertEquals("비밀번호가 일치하지 않습니다.", error.message)
    }

    @Test
    fun signupSendsChosenMembershipAndOtherDescription() {
        body = """{"message":"Signed up"}"""
        repository.signup("Name", "name@example.com", "login", "password", "OTHER", " 지역 상인 ")
        assertEquals("/api/auth/signup", requestedPath)
        val payload = com.google.gson.Gson().fromJson(requestedBody, com.google.gson.JsonObject::class.java)
        assertEquals("OTHER", payload.get("memberType").asString)
        assertEquals("지역 상인", payload.get("memberDetail").asString)
    }

    @Test
    fun deletesSurveyWithAuthorTokenAndPropagatesOwnershipFailure() {
        body = """{"message":"Survey deleted"}"""
        repository.deleteSurvey("7", "author-token")
        assertEquals("DELETE", requestedMethod)
        assertEquals("/api/surveys/7", requestedPath)
        assertEquals("Bearer author-token", authorization)
        assertEquals("", requestedBody)
        status = 403
        val error = assertThrows(SurveyApiException::class.java) {
            repository.deleteSurvey("7", "other-token")
        }
        assertEquals(403, error.status)
        assertThrows(IllegalArgumentException::class.java) { repository.deleteSurvey("invalid", "author-token") }
    }

    @Test
    fun deletionOnlyCompletesAfterServerSuccessAndBlocksDuplicateRequests() {
        val worker = QueuedWorker()
        val ui = java.util.ArrayDeque<Runnable>()
        val account = AccountViewModel(repository, worker, java.util.concurrent.Executor { ui.add(it) })
        body = """{"accessToken":"author-token"}"""
        account.login("author", "password") {}
        worker.runNext(); ui.removeFirst().run()
        var completed = 0
        status = 403
        account.deleteSurvey(SurveyItem(id = "7")) { completed++ }
        account.deleteSurvey(SurveyItem(id = "7")) { completed++ }
        assertTrue(account.busy)
        worker.runNext(); ui.removeFirst().run()
        assertEquals(0, completed)
        assertNotNull(account.error)
        assertFalse(account.busy)
        status = 200
        body = """{"message":"Survey deleted"}"""
        account.deleteSurvey(SurveyItem(id = "7")) { completed++ }
        worker.runNext(); ui.removeFirst().run()
        assertEquals(1, completed)
        assertNull(account.error)
        worker.shutdownNow()
    }

    @Test
    fun mapsOwnershipAndAuthenticatedProfileAndParticipationCount() {
        body = "[$surveyJson]".replace("\"id\": 7", "\"id\": 7, \"userId\": 42, \"targetCount\": 100")
        val survey = repository.getSurveys().single()
        assertEquals(42, survey.userId)
        assertEquals(100, survey.targetCount)
        body = """{"id":42,"name":"Author","email":"author@example.com","loginId":"author","point":300}"""
        val profile = repository.getProfile("my-token")
        assertEquals(42, profile.id)
        assertEquals(300, profile.point)
        assertEquals("/api/users/me", requestedPath)
        assertEquals("Bearer my-token", authorization)
        body = """[{"surveyId":7},{"surveyId":8}]"""
        assertEquals(2, repository.getParticipationCount("my-token"))
        assertEquals("/api/users/me/responses", requestedPath)
    }

    @Test
    fun loadsRealStatisticsWithAuthorTokenAndHandlesForbiddenAccess() {
        body = """{"surveyId":7,"totalResponses":4,"questions":[
            {"questionId":9,"results":[{"option":"Yes","count":3,"percentage":75},{"option":"No","count":1,"percentage":25}]},
            {"questionId":10,"results":[{"option":"Convenient","count":2,"percentage":50}]}]}"""
        val result = repository.getResults("7", "author-token")
        assertEquals("/api/surveys/7/results", requestedPath)
        assertEquals("Bearer author-token", authorization)
        assertEquals(4, result.totalResponses)
        assertEquals(AnswerResult("Yes", 3, 75), result.questions.first().results.first())
        assertEquals("Convenient", result.questions.last().results.single().option)
        status = 403
        val error = assertThrows(SurveyApiException::class.java) { repository.getResults("7", "other-token") }
        assertEquals(403, error.status)
    }

    @Test
    fun responseSubmissionUsesActualIdsAndOptionTextAndServerPointBalance() {
        body = surveyJson
        val survey = repository.getSurvey("7")
        body = """{"responseId":20,"rewardPoint":300,"point":750}"""
        val result = repository.submitAnswers(survey, mapOf(9 to "Yes"), "session-token")
        assertEquals(ParticipationResult(20, 300, 750), result)
        assertEquals("/api/surveys/7/responses", requestedPath)
        assertEquals("POST", requestedMethod)
        assertEquals("Bearer session-token", authorization)
        val answer = com.google.gson.Gson().fromJson(requestedBody, com.google.gson.JsonObject::class.java)
            .getAsJsonArray("answers").single().asJsonObject
        assertEquals(9, answer.get("questionId").asInt)
        assertEquals("Yes", answer.get("answer").asString)
        status = 409
        val duplicate = assertThrows(SurveyApiException::class.java) {
            repository.submitAnswers(survey, mapOf(9 to "Yes"), "session-token")
        }
        assertEquals(409, duplicate.status)
        assertEquals("이미 참여한 설문입니다.", duplicate.message)
        status = 400
        assertThrows(SurveyApiException::class.java) { repository.submitAnswers(survey, mapOf(9 to "Yes"), "session-token") }
    }

    @Test
    fun missingAndInvalidAnswersNeverReachServer() {
        val survey = SurveyItem(id = "7", questions = listOf(
            SurveyQuestion(9, "Choice", "single", listOf(SurveyOption(11, "Yes"))),
            SurveyQuestion(10, "Reason", "short", emptyList())))
        for (answers in listOf(emptyMap(), mapOf(9 to "Yes"), mapOf(9 to "11", 10 to "Reason"), mapOf(9 to "Yes", 10 to "  "))) {
            assertThrows(IllegalArgumentException::class.java) { repository.submitAnswers(survey, answers, "token") }
        }
        assertEquals("", requestedPath)
        val payload = answerPayload(survey, mapOf(9 to "Yes", 10 to " Reason "))
        val json = com.google.gson.Gson().toJsonTree(payload).asJsonObject.getAsJsonArray("answers")
        assertEquals("Reason", json[1].asJsonObject.get("answer").asString)
    }

    private fun draft() = CompletedSurveyDraft(
        SurveyDraftStepOne("Travel", "Life", "", "", "2099. 01. 01."),
        listOf(SurveyQuestionSnapshot(SurveyQuestionType.MULTIPLE_CHOICE, "Bus?", true, 0, listOf("Yes", "No"))),
        SurveySettingsDraft("300P", "10명", "5분 이하"))

    private class QueuedWorker : java.util.concurrent.AbstractExecutorService() {
        val tasks = java.util.ArrayDeque<Runnable>()
        override fun execute(command: Runnable) { tasks.add(command) }
        fun runNext() = tasks.removeFirst().run()
        override fun shutdown() {}
        override fun shutdownNow(): MutableList<Runnable> = mutableListOf()
        override fun isShutdown() = false
        override fun isTerminated() = false
        override fun awaitTermination(timeout: Long, unit: java.util.concurrent.TimeUnit) = false
    }

    @Test
    fun participationCheckUsesAuthenticatedHistoryAndBlocksEntryBeforeAnswers() {
        val worker = QueuedWorker()
        val ui = java.util.ArrayDeque<Runnable>()
        val account = AccountViewModel(repository, worker, java.util.concurrent.Executor { ui.add(it) })
        body = """{"accessToken":"session-token"}"""
        account.login("author", "password") {}
        worker.runNext(); ui.removeFirst().run()
        val survey = SurveyItem(id = "7")
        var entries = 0
        body = """[{"id":20,"surveyId":7}]"""
        account.checkParticipation(survey) { entries++ }
        account.checkParticipation(survey) { entries++ }
        assertEquals(1, worker.tasks.size)
        worker.runNext(); ui.removeFirst().run()
        assertEquals("/api/users/me/responses", requestedPath)
        assertEquals("GET", requestedMethod)
        assertEquals("Bearer session-token", authorization)
        assertEquals(0, entries)
        assertEquals("이미 참여한 설문입니다.", account.participationNotice)
        account.dismissParticipationNotice()
        body = "[]"
        account.checkParticipation(survey) { entries++ }
        worker.runNext(); ui.removeFirst().run()
        assertEquals(1, entries)
        status = 500
        account.checkParticipation(survey) { entries++ }
        worker.runNext(); ui.removeFirst().run()
        assertEquals(1, entries)
        assertNotNull(account.participationNotice)
    }

    @Test
    fun participationOnlyCompletesOnSuccessAndIgnoresDoubleTapAndExpiresSession() {
        val worker = QueuedWorker()
        val ui = java.util.ArrayDeque<Runnable>()
        val account = AccountViewModel(repository, worker, java.util.concurrent.Executor { ui.add(it) })
        body = """{"accessToken":"session-token"}"""
        account.login("author", "password") {}
        worker.runNext(); ui.removeFirst().run()
        val survey = SurveyItem(id = "7", questions = listOf(SurveyQuestion(9, "Reason", "short", emptyList())))
        val answers = mutableMapOf(9 to "Original")
        var completions = 0
        account.participate(survey, answers) { completions++ }
        answers[9] = "Changed after submit"
        account.participate(survey, answers) { completions++ }
        assertEquals(1, worker.tasks.size)
        assertTrue(account.busy)
        status = 409
        worker.runNext(); ui.removeFirst().run()
        assertEquals(0, completions)
        assertFalse(account.busy)
        assertNull(account.participationResult)
        assertEquals("Changed after submit", answers[9])
        assertTrue(requestedBody.contains("Original"))
        status = 200
        body = """{"responseId":20,"rewardPoint":300,"point":750}"""
        account.participate(survey, answers) { completions++ }
        worker.runNext(); ui.removeFirst().run()
        assertEquals(1, completions)
        assertEquals(750, account.point)
        assertEquals(300, account.participationResult?.rewardPoint)
        status = 401
        account.participate(survey, answers) { completions++ }
        worker.runNext(); ui.removeFirst().run()
        assertNull(account.token)
        assertEquals(1, completions)
    }

    @Test
    fun loginAndCreateUsePostAndBearerWithSchemaFields() {
        body = """{"accessToken":"session-token"}"""
        val token = repository.login(" author ", "password")
        assertEquals("/api/auth/login", requestedPath)
        assertEquals("POST", requestedMethod)
        val gson = com.google.gson.Gson()
        val login = gson.fromJson(requestedBody, com.google.gson.JsonObject::class.java)
        assertEquals("author", login.get("loginId").asString)
        assertNull(authorization)
        body = surveyJson
        assertEquals("7", repository.createSurvey(draft(), token).id)
        assertEquals("/api/surveys", requestedPath)
        assertEquals("POST", requestedMethod)
        assertEquals("Bearer session-token", authorization)
        val create = gson.fromJson(requestedBody, com.google.gson.JsonObject::class.java)
        assertEquals(300, create.get("rewardPoint").asInt)
        assertEquals(10, create.get("targetCount").asInt)
        assertFalse(create.has("userId"))
        assertEquals("single", create.getAsJsonArray("questions")[0].asJsonObject.get("questionType").asString)
        status = 401
        val failure = assertThrows(SurveyApiException::class.java) { repository.createSurvey(draft(), token) }
        assertEquals(401, failure.status)
    }

    @Test
    fun invalidDraftIsRejectedBeforeSendingAndShortAnswersHaveNoOptions() {
        val original = draft()
        assertThrows(IllegalArgumentException::class.java) {
            surveyPayload(original.copy(settings = original.settings.copy(rewardPerPerson = "-1P")))
        }
        assertThrows(IllegalArgumentException::class.java) {
            surveyPayload(original.copy(questions = original.questions.map { it.copy(options = listOf("Yes", "Yes")) }))
        }
        assertThrows(IllegalArgumentException::class.java) {
            surveyPayload(original.copy(basicInfo = original.basicInfo.copy(deadline = "2099. 02. 30.")))
        }
        val payload = surveyPayload(original.copy(questions = original.questions.map { it.copy(type = SurveyQuestionType.SHORT_ANSWER) }))
        val json = com.google.gson.Gson().toJsonTree(payload).asJsonObject
        assertEquals(0, json.getAsJsonArray("questions")[0].asJsonObject.getAsJsonArray("options").size())
    }

    @Test
    fun paymentConfigurationErrorsRemainDistinctAndOldServersHavePaymentFallback() {
        status = 503
        for ((code, message) in listOf(
            "TOSS_NOT_CONFIGURED" to "토스 결제 연동 설정이 완료되지 않았습니다. 관리자에게 문의해주세요.",
            "PAYMENT_URL_NOT_CONFIGURED" to "결제창 연결 주소가 설정되지 않았습니다. 관리자에게 문의해주세요."
        )) {
            body = """{"code":"$code","error":"configuration unavailable"}"""
            val failure = assertThrows(SurveyApiException::class.java) {
                repository.preparePointPayment("session-token", "TOSS", 1000)
            }
            assertEquals(message, failure.message)
            assertEquals(503, failure.status)
        }
        for (legacyBody in listOf("""{"error":"Internal server error"}""", "<html>Unavailable</html>")) {
            body = legacyBody
            val failure = assertThrows(SurveyApiException::class.java) {
                repository.preparePointPayment("session-token", "TOSS", 1000)
            }
            assertEquals("결제 서비스를 사용할 수 없습니다. 결제 연동 설정을 확인해주세요.", failure.message)
        }
        body = """{"code":"TOSS_NOT_CONFIGURED"}"""
        assertEquals("서버가 일시적으로 응답하지 않습니다.",
            assertThrows(SurveyApiException::class.java) { repository.getSurveys() }.message)
    }

    @Test
    fun httpErrorsAndMalformedPayloadsAreFailuresNotEmptyLists() {
        status = 404
        assertThrows(SurveyApiException::class.java) { repository.getSurvey("7") }
        status = 200
        for (invalid in listOf("not json", "null", "{}", "[{\"id\":7}]", "[$surveyJson,$surveyJson]")) {
            body = invalid
            assertThrows(SurveyApiException::class.java) { repository.getSurveys() }
        }
        body = surveyJson
        assertThrows(SurveyApiException::class.java) { repository.getSurvey("8") }
        assertThrows(IllegalArgumentException::class.java) { repository.getSurvey("../7") }
    }
}
