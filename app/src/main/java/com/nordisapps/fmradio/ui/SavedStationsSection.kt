package com.nordisapps.fmradio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun SavedStationsSection(
    stations: List<Double>,
    favoriteStations: Set<Double>,
    onStationSelected: (Double) -> Unit,
    onFavoriteToggle: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text(
            text = "Сохранённые станции",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (stations.isEmpty()) {
            Text(
                text = "Пока здесь пусто. Запустите сканирование, чтобы найти станции",
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(stations) { freq ->
                    val isFavorite = favoriteStations.contains(freq)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { onStationSelected(freq) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "$freq MHz",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start
                            )
                        }
                        IconButton(onClick = { onFavoriteToggle(freq) }) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                contentDescription = if (isFavorite) "Убрать из избранного" else "Добавить в избранное",
                                tint = if (isFavorite) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}