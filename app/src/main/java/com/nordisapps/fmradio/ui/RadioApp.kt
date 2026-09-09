package com.nordisapps.fmradio.ui

import androidx.compose.runtime.Composable
import com.nordisapps.fmradio.RadioViewModel

@Composable
fun RadioApp(viewModel: RadioViewModel) {
    AppNavHost(
        favoriteStations = viewModel.uiState.favoriteStations.sorted(),
        onStationSelected = viewModel::tuneToStation,
        onFavoriteToggle = viewModel::toggleFavoriteAndPersist,
        settingsViewModel = viewModel,
        homeContent = { snackbarHostState ->
            RadioHomeScreen(viewModel = viewModel, snackbarHostState = snackbarHostState)
        }
    )
}