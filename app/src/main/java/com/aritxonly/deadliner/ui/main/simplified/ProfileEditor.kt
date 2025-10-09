package com.aritxonly.deadliner.ui.main.simplified

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.data.UserProfileRepository
import com.aritxonly.deadliner.model.UserProfile
import com.image.cropview.CropType
import com.image.cropview.EdgeType
import com.image.cropview.ImageCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.min

@Composable
fun ProfileEditor(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profile by UserProfileRepository.profile.collectAsState(initial = UserProfile())
    var nickname by remember(profile.nickname) { mutableStateOf(TextFieldValue(profile.nickname)) }

    // 裁剪状态
    var showCropper by remember { mutableStateOf(false) }
    var cropBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // 图片选择器
    val picker = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val bmp = BitmapFactory.decodeStream(input)
                    withContext(Dispatchers.Main) {
                        cropBitmap = bmp
                        showCropper = bmp != null
                    }
                }
            }
        }
    }

    // 当前头像
    val avatarFile = remember(profile.avatarFileName) {
        profile.avatarFileName?.let { File(context.filesDir, "avatars/$it") }
    }
    var imageBitmap by remember(avatarFile?.absolutePath) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(profile.avatarFileName) {
        val f = avatarFile
        imageBitmap = if (f?.exists() == true) withContext(Dispatchers.IO) {
            decodeImageBitmapSized(f, 512)
        } else null
    }

    // --- UI ---
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 🟢 头像 + 编辑按钮
        Box(
            modifier = Modifier.size(100.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable {
                        picker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                    },
                contentAlignment = Alignment.Center
            ) {
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap!!,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_person),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            FilledIconButton(
                onClick = { picker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) },
                shape = CircleShape,
                modifier = Modifier
                    .offset(x = 6.dp, y = 6.dp)
                    .size(32.dp)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_camera),
                    contentDescription = "更换头像"
                )
            }
        }

        // 🟣 昵称输入
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("昵称", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("输入昵称") }
            )
        }

        // 🔘 保存按钮
        Button(
            onClick = {
                scope.launch {
                    UserProfileRepository.setNickname(nickname.text.trim())
                }
            },
            enabled = nickname.text.trim() != profile.nickname,
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("保存更改")
        }

        // 🗑️ 移除头像
        if (profile.hasAvatar()) {
            TextButton(onClick = { scope.launch { UserProfileRepository.removeAvatar(context) } }) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_delete),
                    contentDescription = null
                )
                Spacer(Modifier.width(6.dp))
                Text("移除头像")
            }
        }
    }

    // 🟡 裁剪对话框
    if (showCropper && cropBitmap != null) {
        AvatarCropperDialog(
            src = cropBitmap!!,
            onCancel = { showCropper = false },
            onDone = { cropped ->
                showCropper = false
                scope.launch(Dispatchers.IO) {
                    UserProfileRepository.setAvatarFromBitmap(context, cropped)
                }
            }
        )
    }
}

@Composable
fun AvatarCropperDialog(
    src: Bitmap,
    onCancel: () -> Unit,
    onDone: (Bitmap) -> Unit
) {
    val imageCrop = remember(src) { ImageCrop(src) }
    val aspect = src.width.toFloat() / src.height.toFloat()

    Dialog(onDismissRequest = onCancel) {
        Surface(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
            shadowElevation = 6.dp,
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("裁剪头像", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_close),
                            contentDescription = "关闭"
                        )
                    }
                }

                // 预览区域：保持原图宽高比 + 圆角
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(aspect)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    imageCrop.ImageCropView(
                        modifier = Modifier.matchParentSize(),
                        guideLineColor = Color.LightGray,
                        guideLineWidth = 1.dp,
                        edgeCircleSize = 8.dp,
                        showGuideLines = true,
                        cropType = CropType.PROFILE_CIRCLE,
                        edgeType = EdgeType.CIRCULAR
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onCancel) { Text("取消") }
                    Spacer(Modifier.width(12.dp))
                    Button(onClick = {
                        val cropped = imageCrop.onCrop() // 库已按视图裁好
                        // 规范成 512x512 正方形（不失真；若本身是圆形内容，背景为黑/透明见库实现）
                        val size = min(cropped.width, cropped.height)
                        val x = (cropped.width - size) / 2
                        val y = (cropped.height - size) / 2
                        val square = Bitmap.createBitmap(cropped, x, y, size, size)
                        val final = Bitmap.createScaledBitmap(square, 512, 512, true)
                        onDone(final)
                    }) { Text("确定") }
                }
            }
        }
    }
}

private fun decodeImageBitmapSized(file: File, reqSize: Int = 512): ImageBitmap? {
    if (!file.exists()) return null

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    val maxDim = max(bounds.outWidth, bounds.outHeight).toFloat()
    var sample = 1
    if (maxDim > reqSize) {
        var half = maxDim / 2f
        while ((half / sample) > reqSize) sample *= 2
    }

    val opts = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    val bmp = BitmapFactory.decodeFile(file.absolutePath, opts) ?: return null
    return bmp.asImageBitmap()
}