
package com.example.wassertech.ui.maintenance

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wassertech.viewmodel.MaintenanceViewModel

@Composable
fun MaintenanceScreen(
    componentId: String,
    onDone: () -> Unit,
    vm: MaintenanceViewModel = viewModel()
) {
    var installationId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(componentId) {
        installationId = vm.getInstallationIdByComponent(componentId)
    }

    if (installationId == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        MaintenanceAllScreen(installationId = installationId!!, onDone = onDone)
    }
}
