package com.aritxonly.deadliner.composable.agent

import android.content.Intent
import com.aritxonly.deadliner.R

import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle.Companion.Stroke
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.aritxonly.deadliner.AddDDLActivity
import com.aritxonly.deadliner.model.DeadlineType
import com.aritxonly.deadliner.model.GeneratedDDL
import com.aritxonly.deadliner.web.DeepSeekUtils.generateDeadline
import com.aritxonly.deadliner.web.DeepSeekUtils.parseGeneratedDDL
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DeepseekOverlay(
    initialText: String = "",
    onDismiss: () -> Unit,
    onAddDDL: (Intent) -> Unit,
    modifier: Modifier = Modifier.fillMaxSize(),
    borderThickness: Dp = 4.dp,
    glowColors: List<Color> = listOf(Color(0xFFA766E8), Color(0xFF4D6BFE)),
    hintText: String = "输入你的问题…"
) {
    // UI 状态
    var textState by remember { mutableStateOf(TextFieldValue(initialText)) }
    var isLoading by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<GeneratedDDL>>(emptyList()) }
    var failed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val context = LocalContext.current

    Box(modifier = modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.6f))
        .imePadding()
        .clickable {
            focusManager.clearFocus()
            onDismiss()
        }
    ) {
        // 顶部提示气泡
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = "针对屏幕所示建议提问",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        val itemCorner = dimensionResource(R.dimen.item_corner_radius)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 40.dp)
                .drawBehind {
                    val stroke = borderThickness.toPx()
                    // Load your dimen as Dp
                    val cornerDp = itemCorner
                    // Convert to pixels
                    val cornerPx = cornerDp.toPx()
                    drawRoundRect(
                        brush = Brush.horizontalGradient(glowColors),
                        size = size,
                        cornerRadius = CornerRadius(cornerPx, cornerPx),
                        style = Stroke(width = stroke)
                    )
                }
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(dimensionResource(R.dimen.item_corner_radius))
                )
                .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    }
            ) {
                val lineHeightDp = 24.dp
                val maxHeight = lineHeightDp * 2

                val scrollState = rememberScrollState()
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(max = maxHeight)
                        .verticalScroll(scrollState)
                ) {
                    BasicTextField(
                        value = textState,
                        onValueChange = { textState = it },
                        textStyle = LocalTextStyle.current.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = MaterialTheme.typography.bodyMedium.fontSize
                        ),
                        modifier = Modifier
                            .focusRequester(focusRequester),
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                focusManager.clearFocus()
                                if (textState.text.isNotBlank()) {
                                    results = emptyList()
                                    failed = false
                                    scope.launch {
                                        isLoading = true
                                        try {
                                            val raw = generateDeadline(textState.text)
                                            val ddl = parseGeneratedDDL(raw)
                                            results = listOf(ddl)
                                        } catch (e: Exception) {
                                            results = listOf(
                                                GeneratedDDL(
                                                    name = "解析失败",
                                                    dueTime = LocalDateTime.now(),
                                                    note = e.message ?: "未知错误"
                                                )
                                            )
                                            failed = true
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                }
                            }
                        ),
                        decorationBox = { innerTextField ->
                            if (textState.text.isEmpty()) {
                                Text(
                                    text = hintText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                            innerTextField()
                        }
                    )
                }

                IconButton(onClick = {
                    focusManager.clearFocus()
                    if (textState.text.isNotBlank()) {
                        results = emptyList()
                        failed = false
                        scope.launch {
                            isLoading = true
                            try {
                                val raw = generateDeadline(textState.text)
                                val ddl = parseGeneratedDDL(raw)
                                results = listOf(ddl)
                            } catch (e: Exception) {
                                results = listOf(
                                    GeneratedDDL(
                                        name = "解析失败",
                                        dueTime = LocalDateTime.now(),
                                        note = e.message ?: "未知错误"
                                    )
                                )
                                failed = true
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "发送",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // 加载指示条
        if (isLoading) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    )
            ) {
                LoadingIndicator(
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // 结果展示
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp)
        ) {
            results.forEach { ddl ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            val view = LayoutInflater.from(ctx)
                                .inflate(R.layout.item_layout, null, false)
                            view.findViewById<TextView>(R.id.titleText)
                                .text = ddl.name
                            view.findViewById<TextView>(R.id.remainingTimeTextAlt)
                                .text =
                                ddl.dueTime.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"))
                            view.findViewById<TextView>(R.id.noteText)
                                .text = ddl.note
                            view.findViewById<ImageView>(R.id.starIcon)
                                .visibility = View.GONE
                            view.findViewById<LinearProgressIndicator>(R.id.progressBar)
                                .visibility = View.GONE
                            view.findViewById<TextView>(R.id.remainingTimeText)
                                .visibility = View.GONE
                            view
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )

                    if (!failed) {
                        Button(
                            onClick = {
                                val intent = Intent(context, AddDDLActivity::class.java).apply {
                                    putExtra("EXTRA_CURRENT_TYPE", 0)
                                    putExtra("EXTRA_GENERATE_DDL", ddl)
                                }
                                onAddDDL(intent)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(24.dp)  // 圆角按钮
                        ) {
                            Text(text = "新建任务", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}