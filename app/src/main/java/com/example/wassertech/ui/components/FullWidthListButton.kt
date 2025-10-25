package com.example.wassertech.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FullWidthListButton(
    text: String,
    onClick: () -> Unit,
    showSearchIcon: Boolean = false
) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text, style = MaterialTheme.typography.titleMedium)
            if (showSearchIcon) {
                Icon(imageVector = Icons.Default.Search, contentDescription = null)
            }
        }
    }
}
