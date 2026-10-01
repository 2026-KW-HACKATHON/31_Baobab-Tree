import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BaobabGreen = Color(0xFF2D4F37)
private val BaobabBrown = Color(0xFF563C28)
private val LoginBackground = Color(0xFFFDF9F1)
private val InputBackground = Color(0xFFF4F1E9)
private val PlaceholderColor = Color(0x80000000)

@Composable
fun LoginScreen(
    onSignUpClick: () -> Unit = {},
    onGuestLoginClick: () -> Unit = {}
) {
    val logo = buildAnnotatedString {
        withStyle(SpanStyle(color = BaobabGreen)) {
            append("BA")
        }
        withStyle(SpanStyle(color = BaobabBrown)) {
            append("OB")
        }
        withStyle(SpanStyle(color = BaobabGreen)) {
            append("AB")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LoginBackground),
        contentAlignment = Alignment.Center
    ) {
        LoginContent(
            logo = logo,
            onSignUpClick = onSignUpClick,
            onGuestLoginClick = onGuestLoginClick,
            modifier = Modifier.offset(y = 15.dp)
        )
    }
}

@Composable
private fun LoginContent(
    logo: AnnotatedString,
    onSignUpClick: () -> Unit,
    onGuestLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var userId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .width(270.dp)
            .height(312.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(44.5.dp))

        Text(
            text = logo,
            modifier = Modifier.width(150.dp),
            fontFamily = FontFamily(Font(R.font.jaro_regular)),
            fontSize = 35.sp,
            lineHeight = 35.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(25.dp))

        LoginInput(
            value = userId,
            onValueChange = { userId = it },
            placeholder = "아이디"
        )

        Spacer(modifier = Modifier.height(30.dp))

        LoginInput(
            value = password,
            onValueChange = { password = it },
            placeholder = "패스워드",
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LoginLink(
                text = "회원가입",
                onClick = onSignUpClick
            )
            Spacer(modifier = Modifier.width(10.dp))
            LoginLink(
                text = "Guest로 로그인",
                onClick = onGuestLoginClick
            )
        }
    }
}

@Composable
private fun LoginInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        singleLine = true,
        textStyle = TextStyle(
            color = Color.Black,
            fontFamily = FontFamily(Font(R.font.jaro_regular)),
            fontSize = 15.sp,
            lineHeight = 20.sp
        ),
        visualTransformation = visualTransformation,
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(InputBackground, RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = PlaceholderColor,
                        fontFamily = FontFamily(Font(R.font.jaro_regular)),
                        fontSize = 15.sp,
                        lineHeight = 20.sp
                    )
                }
                innerTextField()
            }
        }
    )
}

@Composable
private fun LoginLink(
    text: String,
    onClick: () -> Unit
) {
    androidx.compose.foundation.text.ClickableText(
        text = AnnotatedString(text),
        onClick = { onClick() },
        style = TextStyle(
            color = BaobabGreen,
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp
        )
    )
}
