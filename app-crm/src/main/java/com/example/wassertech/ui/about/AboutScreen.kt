package ru.wassertech.ui.about

import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.wassertech.crm.R

@Composable
fun AboutScreen() {
    val context = LocalContext.current
    val packageInfo = try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
    } catch (e: Exception) {
        null
    }
    
    val versionName = packageInfo?.versionName ?: "Неизвестно"
    val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
        packageInfo?.longVersionCode?.toInt() ?: (packageInfo?.versionCode ?: 0)
    } else {
        @Suppress("DEPRECATION")
        packageInfo?.versionCode ?: 0
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Логотип над надписью
            Image(
                painter = painterResource(R.drawable.logo_wassertech),
                contentDescription = "Wassertech Logo",
                modifier = Modifier
                    .size(120.dp)
                    .padding(bottom = 24.dp),
                contentScale = ContentScale.Fit
            )
            
            Text(
                text = "Wassertech CRM",
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center
            )
            
            Spacer(Modifier.height(32.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Версия приложения",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Text(
                        text = versionName,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        // Автор внизу страницы
        Text(
            text = "Автор: Дмитрий Шульски / Wassertech Digital",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}

