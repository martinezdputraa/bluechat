package com.martinezdputra.bluechat.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.martinezdputra.bluechat.data.chat.BluetoothMessage
import com.martinezdputra.bluechat.ui.theme.BluechatTheme
import com.martinezdputra.bluechat.ui.theme.OldRose
import com.martinezdputra.bluechat.ui.theme.Vanilla

@Composable
fun ChatMessage(
    message: BluetoothMessage,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(
                RoundedCornerShape(
                    topStart = if (message.isFromLocalUser) 15.dp else 0.dp,
                    topEnd = 15.dp,
                    bottomStart = 15.dp,
                    bottomEnd = if (message.isFromLocalUser) 0.dp else 15.dp,
                )
            )
            .background(
                if (message.isFromLocalUser) OldRose else Vanilla
            )
            .padding(16.dp)
    ) {
        Text(
            text = message.senderName,
            fontSize = 10.sp,
            color = Color.Black
        )
        Text(
            text = message.message,
            color = Color.Black,
            modifier = Modifier.widthIn(max = 250.dp)
        )
    }
}

@Preview
@Composable
fun ChatMessagePreview() {
    BluechatTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            ChatMessage(
                message = BluetoothMessage(
                    message = "Hello Friend!",
                    senderName = "Friend",
                    isFromLocalUser = true,
                ),
                modifier = Modifier.align(Alignment.End)
            )
            Spacer(modifier = Modifier.height(16.dp))
            ChatMessage(
                message = BluetoothMessage(
                    message = "Hi Friend!",
                    senderName = "Me",
                    isFromLocalUser = false,
                ),
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(16.dp))
            ChatMessage(
                message = BluetoothMessage(
                    message = "Have a great day!",
                    senderName = "Me",
                    isFromLocalUser = false,
                ),
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(16.dp))
            ChatMessage(
                message = BluetoothMessage(
                    message = "Thank you!",
                    senderName = "Friend",
                    isFromLocalUser = true,
                ),
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}
