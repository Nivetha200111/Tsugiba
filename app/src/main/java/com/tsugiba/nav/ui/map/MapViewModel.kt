package com.tsugiba.nav.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tsugiba.nav.data.model.*
import com.tsugiba.nav.data.repository.NavigationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val repository: NavigationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MapUiState>(MapUiState.Idle)
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val _activityMode = MutableStateFlow(ActivityMode.WALK)
    val activityMode: StateFlow<ActivityMode> = _activityMode.asStateFlow()

    private val _currentLocation = MutableStateFlow<LatLng?>(null)
    val currentLocation: StateFlow<LatLng?> = _currentLocation.asStateFlow()

    private val _suggestions = MutableStateFlow<List<TrafficSuggestion>>(emptyList())
    val suggestions: StateFlow<List<TrafficSuggestion>> = _suggestions.asStateFlow()

    fun setActivityMode(mode: ActivityMode) {
        _activityMode.value = mode
    }

    fun updateLocation(lat: Double, lng: Double) {
        _currentLocation.value = LatLng(lat, lng)
    }

    fun searchRoute(destinationLat: Double, destinationLng: Double) {
        val origin = _currentLocation.value ?: return
        viewModelScope.launch {
            _uiState.value = MapUiState.Loading
            repository.getRoutes(
                originLat = origin.latitude,
                originLng = origin.longitude,
                destinationLat = destinationLat,
                destinationLng = destinationLng,
                mode = _activityMode.value
            ).collect { result ->
                result.fold(
                    onSuccess = { routes ->
                        val suggestions = repository.buildSuggestions(routes)
                        _suggestions.value = suggestions
                        _uiState.value = MapUiState.RoutesLoaded(routes, suggestions)
                    },
                    onFailure = {
                        _uiState.value = MapUiState.Error(it.message ?: "Unknown error")
                    }
                )
            }
        }
    }

    fun selectRoute(route: Route) {
        val current = _uiState.value
        if (current is MapUiState.RoutesLoaded) {
            _uiState.value = MapUiState.Navigating(route, current.suggestions)
        }
    }

    fun stopNavigation() {
        _uiState.value = MapUiState.Idle
        _suggestions.value = emptyList()
    }
}

sealed class MapUiState {
    object Idle : MapUiState()
    object Loading : MapUiState()
    data class RoutesLoaded(val routes: List<Route>, val suggestions: List<TrafficSuggestion>) : MapUiState()
    data class Navigating(val activeRoute: Route, val suggestions: List<TrafficSuggestion>) : MapUiState()
    data class Error(val message: String) : MapUiState()
}
