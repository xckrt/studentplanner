package com.xckrt.studentplanner.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xckrt.studentplanner.data.ApiService
import com.xckrt.studentplanner.viewmodels.GroupSelectionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupSelectionScreen(
    viewModel: GroupSelectionViewModel,
    onGroupSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.uploadExcel(it) }
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Настройка группы", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("Выберите группу из списка или загрузите Excel файл учебного заведения",
            textAlign = TextAlign.Center, color = Color.Gray)
        Spacer(modifier = Modifier.height(32.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = viewModel.selectedGroup?.name ?: "Выберите группу...",
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                label = { Text("Список групп") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                viewModel.groups.forEach { group ->
                    DropdownMenuItem(
                        text = { Text(group.name) },
                        onClick = {
                            viewModel.selectedGroup = group
                            expanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                viewModel.selectedGroup?.let { group ->
                    viewModel.selectGroup(group.id) {
                        onGroupSelected(group.id)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = viewModel.selectedGroup != null && !viewModel.isUploading
        ) {
            Text("ПОДТВЕРДИТЬ И ВОЙТИ")
        }
        Spacer(modifier = Modifier.height(40.dp))
        Divider()
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(
            onClick = { filePickerLauncher.launch("application/vnd.ms-excel") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !viewModel.isUploading
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("ЗАГРУЗИТЬ НОВОЕ РАСПИСАНИЕ")
        }
        if (viewModel.isUploading) {
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        viewModel.errorMessage?.let { msg ->
            Text(
                text = msg,
                color = if (msg.contains("обработан")) Color.Green else Color.Red,
                modifier = Modifier.padding(top = 16.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}