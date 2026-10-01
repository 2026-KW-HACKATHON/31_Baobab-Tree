package com.example.baobab

import com.google.gson.Gson
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
    private val gson = Gson()

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

    private fun get(path: String): String = request(path)

    private fun request(path: String, body: Any? = null, token: String? = null): String {
        val connection = base.resolve(path).toURL().openConnection() as HttpURLConnection
        try {
            connection.requestMethod = if (body == null) "GET" else "POST"
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "application/json")
            if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(gson.toJson(body).toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            if (status !in 200..299) {
                throw SurveyApiException(when (status) {
                    401 -> if (path == "auth/login") "아이디와 비밀번호를 확인해주세요." else "로그인이 만료되었습니다. 다시 로그인해주세요."
                    409 -> if (path.endsWith("/responses")) "이미 참여한 설문입니다." else "이미 사용 중인 아이디 또는 이메일입니다."
                    400 -> if (path.endsWith("/responses")) "설문이 마감되었거나 모집 인원이 찼을 수 있습니다. 설문 정보와 답변을 확인해주세요." else "입력 내용을 확인해주세요."
                    404 -> "설문을 찾을 수 없습니다."
                    else -> "요청에 실패했습니다. 잠시 후 다시 시도해주세요."
                }, status)
            }
            return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (error: SurveyApiException) {
            throw error
        } catch (error: IOException) {
            throw SurveyApiException("서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.")
        } finally {
            connection.disconnect()
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
    val questionType: String? = null, val options: List<OptionDto>? = null
) {
    fun toQuestion(): SurveyQuestion {
        require(id != null && id > 0 && !question.isNullOrBlank() && !questionType.isNullOrBlank())
        return SurveyQuestion(id, question, questionType, requireNotNull(options).map { it.toOption() })
    }
}
private data class SurveyDto(
    val id: Int? = null, val title: String? = null, val category: String? = null,
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
            questionCount = questionItems.size, questions = questionItems
        )
    }
}
