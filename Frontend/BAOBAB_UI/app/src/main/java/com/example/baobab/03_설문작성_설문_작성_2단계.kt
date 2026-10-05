package com.example.baobab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class SurveyQuestionDraft(type: SurveyQuestionType, title: String = "", options: List<String> = emptyList()) {
    var type by mutableStateOf(type)
    var title by mutableStateOf(title)
    var required by mutableStateOf(true)
    var selectedOptionIndex by mutableIntStateOf(0)
    val options = mutableStateListOf<String>().apply { addAll(options) }
}

enum class SurveyQuestionType { MULTIPLE_CHOICE, SHORT_ANSWER }

@Composable
fun SurveyCreationStepTwoScreen(onBackClick: () -> Unit = {},
    onNextClick: (List<SurveyQuestionDraft>) -> Unit = {},
    state: SurveyCreationState = remember { SurveyCreationState() }, editing: Boolean = false) {
    val questions = state.questions
    val valid = questions.isNotEmpty() && questions.all { question ->
        question.title.isNotBlank() && (question.type == SurveyQuestionType.SHORT_ANSWER ||
            (question.options.size >= 2 && question.options.none { it.isBlank() } &&
                question.options.map { it.trim() }.distinct().size == question.options.size))
    }
    Column(Modifier.fillMaxSize().background(Color(0xFFFDF9F1)).safeDrawingPadding().imePadding()) {
        SurveyFormHeader(onBackClick, if (editing) "설문 수정하기" else "설문 작성하기")
        SurveyFormProgress(currentStep = 2)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("응답자에게 물어볼 내용을 작성해주세요", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("객관식은 선택지 2개 이상, 주관식은 자유 답변을 받습니다.", color = Color(0xFF697369), fontSize = 13.sp)
            questions.forEachIndexed { index, question ->
                key(question) {
                    Surface(shape = RoundedCornerShape(20.dp), color = Color.White) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("문항 ${index + 1}", color = Color(0xFF2F5539), fontWeight = FontWeight.Bold)
                                IconButton(onClick = { questions.remove(question) }, enabled = questions.size > 1) {
                                    Icon(Icons.Outlined.DeleteOutline, "문항 ${index + 1} 삭제")
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(SurveyQuestionType.MULTIPLE_CHOICE to "객관식", SurveyQuestionType.SHORT_ANSWER to "주관식")
                                    .forEach { (type, label) ->
                                        FilterChip(question.type == type, onClick = {
                                            question.type = type
                                            if (type == SurveyQuestionType.MULTIPLE_CHOICE && question.options.isEmpty())
                                                question.options.addAll(listOf("", ""))
                                        }, label = { Text(label) })
                                    }
                            }
                            OutlinedTextField(question.title, onValueChange = { question.title = it },
                                label = { Text("질문 내용") }, placeholder = { Text("예) 동네에 필요한 시설은 무엇인가요?") },
                                modifier = Modifier.fillMaxWidth(), minLines = 2, shape = RoundedCornerShape(12.dp))
                            if (question.type == SurveyQuestionType.MULTIPLE_CHOICE) {
                                question.options.forEachIndexed { optionIndex, option ->
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("${optionIndex + 1}", color = Color(0xFF697369))
                                        OutlinedTextField(option, onValueChange = { question.options[optionIndex] = it },
                                            placeholder = { Text("선택지 입력") }, singleLine = true,
                                            modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                                        IconButton(onClick = { question.options.removeAt(optionIndex) }, enabled = question.options.size > 2) {
                                            Icon(Icons.Outlined.Close, "선택지 ${optionIndex + 1} 삭제")
                                        }
                                    }
                                }
                                TextButton(onClick = { question.options.add("") }) {
                                    Icon(Icons.Outlined.Add, null)
                                    Text("선택지 추가")
                                }
                            } else Text("응답자가 직접 답변을 입력하는 문항입니다.", color = Color(0xFF697369), fontSize = 13.sp)
                            HorizontalDivider(color = Color(0xFFE8ECE4))
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("필수 응답", fontWeight = FontWeight.Medium)
                                Switch(question.required, onCheckedChange = { question.required = it })
                            }
                        }
                    }
                }
            }
            OutlinedButton(onClick = { questions.add(SurveyQuestionDraft(SurveyQuestionType.SHORT_ANSWER)) },
                modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Add, null); Text("문항 추가") }
            if (!valid) Text("질문 내용을 입력하고 객관식 선택지는 서로 다르게 작성해주세요.", color = Color(0xFF697369), fontSize = 13.sp)
        }
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onBackClick, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) { Text("이전 단계로") }
            Button(onClick = { if (valid) onNextClick(questions.toList()) }, enabled = valid,
                modifier = Modifier.weight(1f).heightIn(min = 52.dp)) { Text("다음 단계로") }
        }
    }
}
