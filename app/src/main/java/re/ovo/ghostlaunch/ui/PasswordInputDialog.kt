package re.ovo.ghostlaunch.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import re.ovo.ghostlaunch.R

@Composable
fun PasswordInputDialog(
    title: String,
    confirmText: String = stringResource(R.string.ok),
    label: String = stringResource(R.string.pwd_label_optional),
    allowEmpty: Boolean = false,
    initialPassword: String = "",
    header: (@Composable () -> Unit)? = null,
    hint: String? = null,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var pwd by remember { mutableStateOf(initialPassword) }
    var error by remember { mutableStateOf<String?>(null) }
    val errorMinLength = stringResource(R.string.pwd_error_min_length)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                if (header != null) {
                    header()
                    Spacer(Modifier.size(12.dp))
                }
                OutlinedTextField(
                    value = pwd,
                    onValueChange = { pwd = it.filter { c -> c.isDigit() } },
                    label = { Text(label) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = error != null
                )
                error?.let {
                    Spacer(Modifier.size(4.dp))
                    Text(it, color = Color(0xFF8B0000), fontSize = 12.sp)
                }
                hint?.let {
                    Spacer(Modifier.size(8.dp))
                    Text(it, fontSize = 12.sp, color = Color(0xFF888888))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (!allowEmpty && pwd.length < 4) {
                    error = errorMinLength
                    return@TextButton
                }
                onConfirm(pwd)
            }) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
fun PasswordReminderDialog(password: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pwd_reminder_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.pwd_reminder_set_to),
                    fontSize = 14.sp,
                    color = Color(0xFF555555)
                )
                Spacer(Modifier.size(16.dp))
                Text(
                    password,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.size(16.dp))
                Text(
                    stringResource(R.string.pwd_reminder_warn),
                    fontSize = 13.sp,
                    color = Color(0xFF8B0000)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.pwd_reminder_confirm)) }
        }
    )
}
