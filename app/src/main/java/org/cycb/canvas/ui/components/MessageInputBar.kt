package org.cycb.canvas.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.Calendar
import org.cycb.canvas.data.model.Message

@Composable
fun MessageInputBar(
    message: String,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
    isSending: Boolean,
    replyToMessage: Message? = null,
    onClearReply: () -> Unit = {},
    onVoiceMessageSend: ((String, Int) -> Unit)? = null,
    onImageSelected: ((android.net.Uri) -> Unit)? = null,
    selectedImageUri: android.net.Uri? = null,
    onRemoveImage: () -> Unit = {},
    onGifClick: (() -> Unit)? = null,
    onScheduleMessage: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val focusRequester = remember { FocusRequester() }
    var isExpanded by remember { mutableStateOf(true) }

    var showScheduleDialog by remember { mutableStateOf(false) }

    if (showScheduleDialog) {
        ScheduleMessageDialog(
            onDismiss = { showScheduleDialog = false },
            onSchedule = { time ->
                onScheduleMessage?.invoke(time)
                showScheduleDialog = false
            }
        )
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Auto-collapse additional icons when user starts typing or in landscape
    LaunchedEffect(message, isLandscape) {
        if ((message.isNotEmpty() || isLandscape) && isExpanded) {
            isExpanded = false
        }
    }

    val imagePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let { onImageSelected?.invoke(it) }
    }

    val sendButtonScale by animateFloatAsState(
        targetValue = if (message.isNotEmpty() && !isSending) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = Spring.StiffnessHigh
        ),
        label = "send_button_scale"
    )

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = if (isLandscape) 0.dp else 0.dp) // Resetting or adjusting
                .navigationBarsPadding()
        ) {
            // Preview Sections (Image and Reply)
            AnimatedVisibility(
                visible = selectedImageUri != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                selectedImageUri?.let { uri ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = if (isLandscape) 4.dp else 8.dp)) {
                        ImagePreview(
                            imageUri = uri,
                            onRemove = onRemoveImage,
                            isCompact = isLandscape
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = replyToMessage != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                replyToMessage?.let { reply ->
                    ReplyPreview(
                        reply = reply,
                        onClearReply = onClearReply
                    )
                }
            }

            // Input Control Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 8.dp, top = if (isLandscape) 2.dp else 4.dp, bottom = if (isLandscape) 2.dp else 8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Actions Expand Button (The "+" button)
                IconButton(
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isExpanded = !isExpanded 
                    },
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    val rotation by animateFloatAsState(
                        targetValue = if (isExpanded) 45f else 0f,
                        label = "expand_rotation"
                    )
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = if (isExpanded) "Hide attachment options" else "Show attachment options",
                        modifier = Modifier.graphicsLayer { rotationZ = rotation },
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Additional Action Buttons (Gallery, GIF)
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandHorizontally() + fadeIn(),
                    exit = shrinkHorizontally() + fadeOut()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { 
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            imagePickerLauncher.launch("image/*") 
                        }) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "Attach image from gallery",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (onGifClick != null) {
                            IconButton(onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onGifClick()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Gif,
                                    contentDescription = "Attach GIF",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Text Field Area (Modern Pill-shaped design)
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shadowElevation = 0.dp
                ) {
                    TextField(
                        value = message,
                        onValueChange = onMessageChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = if (isLandscape) 40.dp else 48.dp, max = if (isLandscape) 100.dp else 150.dp)
                            .focusRequester(focusRequester),
                        placeholder = {
                            Text(
                                "Message...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        },
                        trailingIcon = {
                            if (!isLandscape) {
                                IconButton(onClick = { /* Emoji Action placeholder */ }) {
                                    Icon(
                                        imageVector = Icons.Default.SentimentSatisfiedAlt,
                                        contentDescription = "Pick emoji",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (message.isNotEmpty() && !isSending) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSend()
                                }
                            }
                        )
                    )
                }

                // Right-side Primary Action (Send or Voice Mic)
                Box(
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .graphicsLayer {
                            scaleX = sendButtonScale
                            scaleY = sendButtonScale
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (message.isNotEmpty() || isSending) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .combinedClickable(
                                    onClick = {
                                        if (!isSending) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onSend()
                                        }
                                    },
                                    onLongClick = {
                                        if (!isSending && onScheduleMessage != null) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            showScheduleDialog = true
                                        }
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSending) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    } else if (onVoiceMessageSend != null) {
                        IconButton(onClick = { /* Handle Voice Recording UI/Feature */ }) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice Message",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleMessageDialog(
    onDismiss: () -> Unit,
    onSchedule: (Long) -> Unit
) {
    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState()
    var showTimePicker by remember { mutableStateOf(false) }

    if (!showTimePicker) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = { showTimePicker = true }) { Text("Next") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Select Time") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    val date = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    val calendar = Calendar.getInstance().apply {
                        timeInMillis = date
                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(Calendar.MINUTE, timePickerState.minute)
                    }
                    onSchedule(calendar.timeInMillis)
                }) { Text("Schedule") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Back") }
            }
        )
    }
}

@Composable
private fun ReplyPreview(
    reply: Message,
    onClearReply: () -> Unit
) {
    val senderName = reply.senderId.displayName

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "Replying to $senderName: ${reply.content}"
            },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Reply Indicator Accent Line
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                val displayName = reply.senderId.displayName
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                
                val contentText = when (reply.messageType) {
                    "image" -> "📷 Image"
                    "voice" -> "🎤 Voice message"
                    "file" -> "📎 File"
                    "gif" -> "🎬 GIF"
                    else -> reply.content
                }
                
                Text(
                    text = contentText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            IconButton(
                onClick = onClearReply,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
