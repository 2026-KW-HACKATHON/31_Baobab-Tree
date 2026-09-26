@Composable
fun SplashScreen() {
    val logo = buildAnnotatedString {
        withStyle(
            SpanStyle(color = Color(0xFF2D4F37))
        ) {
            append("BA")
        }

        withStyle(
            SpanStyle(color = Color(0xFF563C28))
        ) {
            append("OB")
        }

        withStyle(
            SpanStyle(color = Color(0xFF2D4F37))
        ) {
            append("AB")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFDF9F1)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = logo,
            fontFamily = FontFamily(Font(R.font.jaro_regular)),
            fontSize = 35.sp,
            textAlign = TextAlign.Center,
            lineHeight = 35.sp
        )
    }
}
