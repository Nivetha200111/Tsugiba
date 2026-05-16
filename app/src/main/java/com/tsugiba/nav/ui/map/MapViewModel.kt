package com.tsugiba.nav.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tsugiba.nav.data.api.LandmarkHintEngine
import com.tsugiba.nav.data.health.HealthInsightEngine
import com.tsugiba.nav.data.health.HealthRepository
import com.tsugiba.nav.data.model.ActivityMode
import com.tsugiba.nav.data.model.LatLng
import com.tsugiba.nav.data.model.Route
import com.tsugiba.nav.data.model.RouteHint
import com.tsugiba.nav.data.model.TrafficSuggestion
import com.tsugiba.nav.data.repository.NavigationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val repository: NavigationRepository,
    private val healthRepository: HealthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MapUiState>(MapUiState.Idle)
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val _activityMode = MutableStateFlow(ActivityMode.WALK)
    val activityMode: StateFlow<ActivityMode> = _activityMode.asStateFlow()

    private val _currentLocation = MutableStateFlow<LatLng?>(null)
    val currentLocation: StateFlow<LatLng?> = _currentLocation.asStateFlow()

    private val _hints = MutableStateFlow<List<RouteHint>>(emptyList())
    val hints: StateFlow<List<RouteHint>> = _hints.asStateFlow()

    private val _healthState = MutableStateFlow<HealthState>(HealthState.Loading)
    val healthState: StateFlow<HealthState> = _healthState.asStateFlow()

    fun setActivityMode(mode: ActivityMode) { _activityMode.value = mode }

    fun updateLocation(lat: Double, lng: Double) { _currentLocation.value = LatLng(lat, lng) }

    fun loadHealth() {
        viewModelScope.launch {
            if (!healthRepository.isAvailable()) {
                _healthState.value = HealthState.Unavailable; return@launch
            }
            if (!healthRepository.hasPermissions()) {
                _healthState.value = HealthState.NeedsPermission; return@launch
            }
            _healthState.value = HealthState.Loading
            healthRepository.getSnapshot().fold(
                onSuccess = { _healthState.value = HealthState.Available(HealthInsightEngine.buildInsight(it)) },
                onFailure = { _healthState.value = HealthState.Unavailable }
            )
        }
    }

    fun searchRoute(destinationLat: Double, destinationLng: Double) {
        val origin = _currentLocation.value ?: return
        viewModelScope.launch {
            _uiState.value = MapUiState.Loading
            repository.getRoutes(
                originLat = origin.latitude, originLng = origin.longitude,
                destinationLat = destinationLat, destinationLng = destinationLng,
                mode = _activityMode.value
            ).collect { result ->
                result.fold(
                    onSuccess = { routes ->
                        _hints.value = LandmarkHintEngine.buildHints(routes.first())
                        _uiState.value = MapUiState.RoutesLoaded(routes, repository.buildSuggestions(routes))
                    },
                    onFailure = { _uiState.value = MapUiState.Error(it.message ?: "Unknown error") }
                )
            }
        }
    }

    fun selectRoute(route: Route) {
        val suggestions = when (val s = _uiState.value) {
            is MapUiState.RoutesLoaded -> s.suggestions
            is MapUiState.Navigating -> s.suggestions
            else -> emptyList()
        }
        _hints.value = LandmarkHintEngine.buildHints(route)
        _uiState.value = MapUiState.Navigating(route, suggestions)
    }

    fun stopNavigation() {
        _uiState.value = MapUiState.Idle
        _hints.value = emptyList()
    }
}

sealed class MapUiState {
    object Idle : MapUiState()
    object Loading : MapUiState()
    data class RoutesLoaded(val routes: List<Route>, val suggestions: List<TrafficSuggestion>) : MapUiState()
    data class Navigating(val activeRoute: Route, val suggestions: List<TrafficSuggestion>) : MapUiState()
    data class Error(val message: String) : MapUiState()
}

sealed class HealthState {
    object Unavailable : HealthState()
    object NeedsPermission : HealthState()
    object Loading : HealthState()
    data class Available(val insight: HealthInsightEngine.Insight) : HealthState()
}
