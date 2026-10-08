package com.example.baobab

import com.sun.net.httpserver.HttpServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.net.InetSocketAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MyPageRecruitmentTest {
    private lateinit var server: HttpServer
    private lateinit var model: MyPageViewModel
    private val worker = Executors.newSingleThreadExecutor()
    private var applicationsStatus = 200
    private var applicationState = "APPLIED"
    private var blockOwned = false
    private val ownedStarted = CountDownLatch(1)
    private val releaseOwned = CountDownLatch(1)
    private val recruitment = """{"id":42,"authorId":1,"author":{"id":1,"name":"Author"},
        "title":"Research","organization":"Lab","activityType":"EXPERIMENT","participationMode":"OFFLINE",
        "description":"About","eligibility":"Students","location":"Lab","durationMinutes":60,
        "targetCount":5,"acceptedCount":0,"rewardPoint":1000,"applicationDeadline":"2099-12-01T00:00:00Z",
        "status":"OPEN","createdAt":"2026-10-08T00:00:00Z","slots":[]}"""

    @Before fun setUp() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/") { exchange ->
            val path = exchange.requestURI.path
            var status = 200
            val body = when (path) {
                "/api/recruitments/mine" -> {
                    if (blockOwned) { ownedStarted.countDown(); releaseOwned.await(5, TimeUnit.SECONDS) }
                    "[$recruitment]"
                }
                "/api/recruitment-applications/me" -> {
                    status = applicationsStatus
                    if (status == 200) """[{"id":7,"status":"$applicationState","createdAt":"2026-10-08T00:00:00Z",
                        "slot":{"id":9,"recruitmentId":42,"startsAt":"2099-12-02T00:00:00Z","endsAt":"2099-12-02T01:00:00Z"},
                        "recruitment":$recruitment}]""" else """{"error":"Unavailable"}"""
                }
                "/api/recruitment-applications/7/cancel" -> {
                    applicationState = "CANCELLED"
                    """{"id":7,"status":"CANCELLED"}"""
                }
                else -> "[]"
            }
            val bytes = body.toByteArray()
            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        model = MyPageViewModel(HttpSurveyRepository("http://127.0.0.1:${server.address.port}/api/"), worker, Executor { it.run() })
    }

    @After fun tearDown() { releaseOwned.countDown(); worker.shutdownNow(); server.stop(0) }
    private fun awaitIdle() { worker.submit {}.get(10, TimeUnit.SECONDS) }

    @Test fun ownedAndAppliedRecruitmentsLoadTogetherAndCancelRefreshesHistory() {
        model.loadRecruitmentActivity("test-token"); awaitIdle()
        assertEquals(42, model.recruitments.single().id)
        assertEquals("APPLIED", model.recruitmentApplications.single().status)
        var cancelled = false
        model.cancelRecruitmentApplication("test-token", 7) { cancelled = true }
        awaitIdle(); awaitIdle()
        assertTrue(cancelled)
        assertEquals("CANCELLED", model.recruitmentApplications.single().status)
        assertFalse(model.recruitmentBusy)
    }

    @Test fun failedApplicationEndpointKeepsOwnedRecruitmentsVisible() {
        applicationsStatus = 503
        model.loadRecruitmentActivity("test-token"); awaitIdle()
        assertEquals(42, model.recruitments.single().id)
        assertNotNull(model.recruitmentError)
        assertFalse(model.recruitmentLoading)
    }

    @Test fun logoutDoesNotAllowAnOldAccountRequestToRepopulateHistory() {
        blockOwned = true
        model.loadRecruitmentActivity("test-token")
        assertTrue(ownedStarted.await(5, TimeUnit.SECONDS))
        model.load(null)
        releaseOwned.countDown(); awaitIdle()
        assertTrue(model.recruitments.isEmpty())
        assertTrue(model.recruitmentApplications.isEmpty())
        assertFalse(model.recruitmentLoading)
    }
}
