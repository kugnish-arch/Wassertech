package com.example.wassertech.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.wassertech.core.ui.theme.Dimens

/**
 * Section header (subNavBar) - darker strip showing current location.
 * This is used on every screen to show the current section title.
 */
@Composable
fun SectionHeader(title: String) {
    Surface(tonalElevation = 1.dp) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
    // ЕДИНЫЙ зазор под сабнавбаром
    Spacer(Modifier.height(Dimens.SubNavbarGap))
}
