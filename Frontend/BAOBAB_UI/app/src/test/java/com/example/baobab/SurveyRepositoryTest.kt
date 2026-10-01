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
