package com.poojapurohit.auth.compose.presentation.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ServicesList(
    services: List<String>,
    selectedServices: Set<String>,
    onSelectionChange: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .border(
                width = 3.dp,
                color = Color.White,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(10.dp)
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(services) { service ->
                ServiceItem(
                    service = service,
                    isSelected = selectedServices.contains(service),
                    onSelectionChange = { onSelectionChange(service, it) }
                )
            }
        }
    }
}