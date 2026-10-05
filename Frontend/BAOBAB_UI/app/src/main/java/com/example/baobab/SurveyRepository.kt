package com.example.baobab

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import java.io.IOException
import java.net.URI
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class WalletCoupon(
    val id: String,
    val itemId: String,
    val shop: String,
    val title: String,
    val cost: Int,
    val status: String,
    val createdAt: String,
    val usedAt: String? = null
)

data class CouponExchangeResult(
    val coupon: WalletCoupon,
    val point: Int,
    val alreadyProcessed: Boolean
)

interface SurveyRepository {
    fun getSurveys(): List<SurveyItem>
    fun getSurvey(id: String): SurveyItem
}

class SurveyApiException(message: String, val status: Int? = null) : IOException(message)

class HttpSurveyRepository(baseUrl: String) : SurveyRepository {
    private val base = URI(baseUrl.trimEnd('/') + "/").also {
        require(it.scheme in listOf("http", "https") && it.host != null && it.query == null && it.fragment == null) {
            "surveyApiBaseUrl must be an HTTP(S) API URL"
        }
    }
    private val gson = GsonBuilder().serializeNulls().create()
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .retryOnConnectionFailure(false)
        .build()

    fun deleteSurvey(id: String, token: String) {
        require(id.toIntOrNull()?.let { it > 0 } == true)
        request("surveys/$id", token = token, method = "DELETE")
    }

    fun getProfile(token: String): UserProfile = parse {
        gson.fromJson(request("users/me", token = token), UserProfile::class.java)
            .also { require(it.id > 0 && it.name.isNotBlank() && it.point >= 0) }
    }

    fun updateProfile(token: String, name: String, email: String, region: String, ageGroup: String): UserProfile = parse {
        if (name.isBlank()) throw SurveyApiException("이름을 입력해주세요.")
        if (!Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(email.trim()))
            throw SurveyApiException("이메일 형식을 확인해주세요.")
        val payload = mapOf("name" to name.trim(), "email" to email.trim(),
            "region" to region.trim().ifEmpty { null }, "ageGroup" to ageGroup.trim().ifEmpty { null })
        gson.fromJson(request("users/me", payload, token, method = "PATCH"), UserProfile::class.java)
            .also { require(it.id > 0 && it.name.isNotBlank()) }
    }

    fun getParticipationCount(token: String): Int = parse {
        gson.fromJson(request("users/me/responses", token = token), com.google.gson.JsonArray::class.java).size()
    }

    fun getParticipations(token: String): List<ParticipationHistory> = parse {
        gson.fromJson(request("users/me/responses", token = token), Array<ParticipationDto>::class.java)
            .map { record ->
                ParticipationHistory(record.id, record.survey.id.toString(), record.survey.title,
                    record.survey.category ?: "미분류", record.survey.rewardPoint, record.createdAt,
                    record.answers.map { AnswerHistory(it.question.question, it.answer) })
            }
    }

    fun getResults(id: String, token: String): SurveyResults = parse {
        require(id.toIntOrNull()?.let { it > 0 } == true)
        gson.fromJson(request("surveys/$id/results", token = token), SurveyResults::class.java)
            .also { require(it.surveyId.toString() == id && it.totalResponses >= 0) }
    }

    fun login(loginId: String, password: String): String = parse {
        val response = gson.fromJson(request("auth/login", mapOf("loginId" to loginId.trim(), "password" to password)), com.google.gson.JsonObject::class.java)
        response.get("accessToken").asString.also { require(it.isNotBlank()) }
    }

    fun signup(name: String, email: String, loginId: String, password: String) {
        request("auth/signup", mapOf("name" to name.trim(), "email" to email.trim(), "loginId" to loginId.trim(), "password" to password))
    }

    fun hasParticipated(surveyId: String, token: String): Boolean = parse {
        require(surveyId.toIntOrNull()?.let { it > 0 } == true)
        val responses = gson.fromJson(request("users/me/responses", token = token), com.google.gson.JsonArray::class.java)
        responses.any { it.asJsonObject.get("surveyId").asInt == surveyId.toInt() }
    }

    fun createSurvey(draft: CompletedSurveyDraft, token: String): SurveyItem = parse {
        gson.fromJson(request("surveys", surveyPayload(draft), token), SurveyDto::class.java).toItem()
    }

    fun submitAnswers(survey: SurveyItem, answers: Map<Int, String>, token: String): ParticipationResult {
        val payload = answerPayload(survey, answers)
        return parse {
            val json = gson.fromJson(request("surveys/${survey.id}/responses", payload, token), com.google.gson.JsonObject::class.java)
            ParticipationResult(json.get("responseId").asInt, json.get("rewardPoint").asInt, json.get("point").asInt)
                .also { require(it.responseId > 0 && it.rewardPoint >= 0 && it.point >= 0) }
        }
    }

    override fun getSurveys(): List<SurveyItem> = parse {
        val surveys = gson.fromJson(get("surveys"), Array<SurveyDto>::class.java)
            ?: throw IllegalArgumentException("Missing list")
        surveys.map { it.toItem() }.also { list ->
            require(list.map { it.id }.distinct().size == list.size)
        }
    }

    override fun getSurvey(id: String): SurveyItem {
        require(id.toIntOrNull()?.let { it > 0 } == true) { "Invalid survey ID" }
        return parse {
            val survey = gson.fromJson(get("surveys/$id"), SurveyDto::class.java)
                ?: throw IllegalArgumentException("Missing survey")
            survey.toItem().also { require(it.id == id) }
        }
    }

    fun preparePointPayment(
        token: String,
        provider: String,
        amount: Int
    ): String {
        if (provider !in listOf("KAKAOPAY", "TOSS")) {
            throw SurveyApiException("결제 수단을 확인해주세요.")
        }

        if (amount !in listOf(1000, 3000, 5000)) {
            throw SurveyApiException("충전 금액을 확인해주세요.")
        }

        val order = gson.fromJson(
            request(
                "payments/orders",
                body = mapOf(
                    "provider" to provider,
                    "amount" to amount
                ),
                token = token
            ),
            com.google.gson.JsonObject::class.java
        )

        val orderId = order.get("id")?.asString
            ?: throw SurveyApiException("충전 주문을 확인하지 못했습니다.")

        val callbackToken = order.get("callbackToken")?.asString
            ?: throw SurveyApiException("주문 확인 정보를 받지 못했습니다.")

        if (
            !Regex(
                "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-" +
                        "[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$"
            ).matches(orderId)
        ) {
            throw SurveyApiException("충전 주문 ID가 올바르지 않습니다.")
        }

        val paymentPath = when (provider) {
            "KAKAOPAY" -> "kakao"
            else -> "toss"
        }

        val ready = gson.fromJson(
            request(
                "payments/orders/$orderId/$paymentPath/ready",
                body = mapOf("callbackToken" to callbackToken),
                token = token
            ),
            com.google.gson.JsonObject::class.java
        )

        val checkoutUrl = ready.get("checkoutUrl")?.asString
            ?: throw SurveyApiException("결제창 주소를 받지 못했습니다.")

        val uri = try {
            URI(checkoutUrl)
        } catch (_: Exception) {
            throw SurveyApiException("결제창 주소가 올바르지 않습니다.")
        }

        if (
            uri.scheme !in listOf("http", "https") ||
            uri.host.isNullOrBlank() ||
            uri.userInfo != null
        ) {
            throw SurveyApiException("결제창 주소가 올바르지 않습니다.")
        }

        return checkoutUrl
    }
    fun getCoupons(token: String): List<WalletCoupon> = parse {
        val coupons = gson.fromJson(
            request("users/me/coupons", token = token),
            Array<WalletCoupon>::class.java
        ) ?: throw SurveyApiException("쿠폰 목록을 받지 못했습니다.")

        coupons.toList().also { list ->
            require(list.all {
                it.id.isNotBlank() &&
                        it.itemId.isNotBlank() &&
                        it.title.isNotBlank() &&
                        it.cost > 0
            })
        }
    }

    fun exchangeCoupon(
        token: String,
        itemId: String,
        requestKey: String
    ): CouponExchangeResult = parse {
        gson.fromJson(
            request(
                "coupons/exchange",
                body = mapOf(
                    "itemId" to itemId,
                    "requestKey" to requestKey
                ),
                token = token
            ),
            CouponExchangeResult::class.java
        ).also {
            require(
                it.point >= 0 &&
                        it.coupon.id.isNotBlank() &&
                        it.coupon.itemId == itemId
            )
        }
    }
    private fun get(path: String): String = request(path)

    private fun request(path: String, body: Any? = null, token: String? = null,
        method: String = if (body == null) "GET" else "POST"): String {
        try {
            val payload = body?.let { gson.toJson(it).toRequestBody("application/json; charset=utf-8".toMediaType()) }
            val builder = Request.Builder().url(base.resolve(path).toString()).header("Accept", "application/json")
                .method(method, payload)
            if (token != null) builder.header("Authorization", "Bearer $token")
            client.newCall(builder.build()).execute().use { response ->
                val status = response.code
                if (status !in 200..299) {
                    throw SurveyApiException(when (status) {
                        401 -> if (path == "auth/login") "아이디와 비밀번호를 확인해주세요." else "로그인이 만료되었습니다. 다시 로그인해주세요."
                        409 -> when {
                            path == "coupons/exchange" ->
                                "포인트가 부족하거나 교환 요청이 충돌했습니다. 잔액을 새로고침해주세요."
                            path.endsWith("/responses") -> "이미 참여한 설문입니다."
                            path == "users/me" -> "이미 사용 중인 이메일입니다."
                            else -> "이미 사용 중인 아이디 또는 이메일입니다."
                        }
                        400 -> if (path.endsWith("/responses")) "설문이 마감되었거나 모집 인원이 찼을 수 있습니다. 설문 정보와 답변을 확인해주세요." else "입력 내용을 확인해주세요."
                        404 -> if (path.contains("coupons")) {
                            "쿠폰 또는 교환 상품을 찾을 수 없습니다."
                        } else {
                            "설문을 찾을 수 없습니다."
                        }
                        403 -> "내가 만든 설문에만 접근할 수 있습니다."
                        503 -> if (path == "coupons/exchange") {
                            "교환 처리가 지연되고 있습니다. 같은 상품으로 다시 시도해주세요."
                        } else {
                            "서버가 일시적으로 응답하지 않습니다."
                        }
                        else -> "요청에 실패했습니다. 잠시 후 다시 시도해주세요."
                    }, status)
                }
                return response.body.string()
                }
        } catch (error: SurveyApiException) {
            throw error
        } catch (error: IOException) {
            throw SurveyApiException("서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.")
        }
    }

    private fun <T> parse(action: () -> T): T = try {
        action()
    } catch (error: SurveyApiException) {
        throw error
    } catch (error: Exception) {
        throw SurveyApiException("설문 정보를 확인할 수 없습니다. 다시 시도해주세요.")
    }
}

private data class AuthorDto(val name: String? = null)
private data class OptionDto(val id: Int? = null, val optionText: String? = null) {
    fun toOption(): SurveyOption {
        require(id != null && id > 0 && !optionText.isNullOrBlank())
        return SurveyOption(id, optionText)
    }
}
private data class QuestionDto(
    val id: Int? = null, val question: String? = null,
    val questionType: String? = null, val options: List<OptionDto>? = null, val required: Boolean? = null
) {
    fun toQuestion(): SurveyQuestion {
        require(id != null && id > 0 && !question.isNullOrBlank() && !questionType.isNullOrBlank())
        return SurveyQuestion(id, question, questionType, requireNotNull(options).map { it.toOption() }, required ?: true)
    }
}
private data class SurveyDto(
    val id: Int? = null, val title: String? = null, val category: String? = null,
    val userId: Int? = null, val targetCount: Int? = null,
    val description: String? = null, val audience: String? = null, val duration: String? = null, val imageData: String? = null,
    val author: AuthorDto? = null, val rewardPoint: Int? = null,
    val currentCount: Int? = null, val endDate: String? = null,
    val status: String? = null, val questions: List<QuestionDto>? = null
) {
    fun toItem(): SurveyItem {
        require(id != null && id > 0 && !title.isNullOrBlank())
        require(rewardPoint != null && rewardPoint >= 0 && currentCount != null && currentCount >= 0)
        require(!author?.name.isNullOrBlank() && status in listOf("OPEN", "CLOSED"))
        val questionItems = requireNotNull(questions).map { it.toQuestion() }
        return SurveyItem(
            id = id.toString(), title = title, category = category ?: "미분류",
            author = requireNotNull(author?.name), points = "${rewardPoint}P",
            participantCount = currentCount, status = requireNotNull(status), endDate = endDate,
            deadline = endDate?.let {
                OffsetDateTime.parse(it).atZoneSameInstant(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy. MM. dd."))
            },
            questionCount = questionItems.size, questions = questionItems,
            userId = userId, targetCount = targetCount,
            description = description, audience = audience, duration = duration, imageData = imageData
        )
    }
}

private data class ParticipationDto(val id: Int, val createdAt: String, val survey: HistorySurveyDto,
    val answers: List<HistoryAnswerDto>)
private data class HistorySurveyDto(val id: Int, val title: String, val category: String?, val rewardPoint: Int)
private data class HistoryAnswerDto(val answer: String, val question: HistoryQuestionDto)
private data class HistoryQuestionDto(val question: String)
