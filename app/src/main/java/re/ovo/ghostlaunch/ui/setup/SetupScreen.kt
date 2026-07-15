package re.ovo.ghostlaunch.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
fun SetupScreen(onSetPassword: (String) -> Unit) {
    var pwd by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var pendingPassword by remember { mutableStateOf<String?>(null) }

    val title = stringResource(R.string.setup_title)
    val desc = stringResource(R.string.setup_desc)
    val pwdLabel = stringResource(R.string.setup_password_label)
    val errorMinLength = stringResource(R.string.pwd_error_min_length)
    val ok = stringResource(R.string.ok)
    val hint = stringResource(R.string.setup_hint)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(Modifier.height(8.dp))
        Text(desc, fontSize = 13.sp, color = Color(0xFF555555), textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = pwd,
            onValueChange = { pwd = it.filter { c -> c.isDigit() } },
            label = { Text(pwdLabel) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))
        Text(hint, fontSize = 12.sp, color = Color(0xFF888888), modifier = Modifier.fillMaxWidth())

        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = Color(0xFF8B0000), fontSize = 13.sp)
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                if (pwd.length < 4) {
                    error = errorMinLength
                } else {
                    error = null
                    pendingPassword = pwd
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(6.dp)
        ) { Text(ok) }
    }

    pendingPassword?.let { setPwd ->
        val reminderTitle = stringResource(R.string.pwd_reminder_title)
        val reminderSetTo = stringResource(R.string.pwd_reminder_set_to)
        val reminderWarn = stringResource(R.string.pwd_reminder_warn)
        val reminderConfirm = stringResource(R.string.pwd_reminder_confirm)

        AlertDialog(
            onDismissRequest = { pendingPassword = null },
            title = { Text(reminderTitle) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(reminderSetTo, fontSize = 14.sp, color = Color(0xFF555555))
                    Spacer(Modifier.height(16.dp))
                    Text(
                        setPwd,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(reminderWarn, fontSize = 13.sp, color = Color(0xFF8B0000))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingPassword = null
                    onSetPassword(setPwd)
                }) { Text(reminderConfirm) }
            }
        )
    }
}
