package com.tsugiba.nav.ui.map

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.tsugiba.nav.R
import com.tsugiba.nav.data.model.*
import com.tsugiba.nav.databinding.FragmentMapBinding
import com.tsugiba.nav.service.LocationTrackingService
import com.tsugiba.nav.ui.suggestions.SuggestionsAdapter
import com.tsugiba.nav.util.FormatUtils
import com.tsugiba.nav.util.PolylineDecoder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MapViewModel by viewModels()
    private var googleMap: GoogleMap? = null
    private val drawnPolylines = mutableListOf<Polyline>()
    private var trackPolyline: Polyline? = null
    private val trackPoints = mutableListOf<LatLng>()
    private var isTracking = false
    private var startTimeMs = 0L
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val locationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val lat = intent.getDoubleExtra(LocationTrackingService.EXTRA_LAT, 0.0)
            val lng = intent.getDoubleExtra(LocationTrackingService.EXTRA_LNG, 0.0)
            val pos = LatLng(lat, lng)
            viewModel.updateLocation(lat, lng)
            googleMap?.animateCamera(CameraUpdateFactory.newLatLng(pos))
            if (isTracking) {
                trackPoints.add(pos)
                trackPolyline?.points = trackPoints.toList()
                updateLiveStats()
            }
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            enableMyLocation()
            startLocationService()
        } else {
            Snackbar.make(binding.root, R.string.location_permission_denied, Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        setupMap()
        setupChips()
        setupButtons()
        setupSuggestions()
        observeState()
        checkPermissions()
    }

    private fun setupMap() {
        val mapFrag = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFrag.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.uiSettings.isCompassEnabled = true
        map.uiSettings.isZoomControlsEnabled = true
        map.setOnMapLongClickListener { latLng ->
            viewModel.searchRoute(latLng.latitude, latLng.longitude)
        }
        if (hasLocationPermission()) enableMyLocation()
    }

    private fun enableMyLocation() {
        if (!hasLocationPermission()) return
        try {
            googleMap?.isMyLocationEnabled = true
            googleMap?.uiSettings?.isMyLocationButtonEnabled = true
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                loc?.let {
                    val pos = LatLng(it.latitude, it.longitude)
                    googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 16f))
                    viewModel.updateLocation(it.latitude, it.longitude)
                }
            }
        } catch (e: SecurityException) { /* permission revoked */ }
    }

    private fun setupChips() {
        binding.chipGroupMode.setOnCheckedStateChangeListener { _, checkedIds ->
            val mode = when (checkedIds.firstOrNull()) {
                R.id.chipJog -> ActivityMode.JOG
                R.id.chipRun -> ActivityMode.RUN
                else -> ActivityMode.WALK
            }
            viewModel.setActivityMode(mode)
        }
    }

    private fun setupButtons() {
        binding.btnStart.setOnClickListener {
            if (!isTracking) startTracking() else stopTracking()
        }
        binding.btnStopNav.setOnClickListener {
            viewModel.stopNavigation()
        }
    }

    private fun setupSuggestions() {
        val adapter = SuggestionsAdapter { suggestion ->
            suggestion.altRoute?.let { viewModel.selectRoute(it) }
        }
        binding.rvSuggestions.adapter = adapter
    }

    private fun startTracking() {
        isTracking = true
        startTimeMs = SystemClock.elapsedRealtime()
        trackPoints.clear()
        trackPolyline?.remove()
        trackPolyline = googleMap?.addPolyline(
            PolylineOptions()
                .color(ContextCompat.getColor(requireContext(), R.color.route_active))
                .width(10f)
                .zIndex(2f)
        )
        binding.btnStart.text = "Stop"
        binding.btnStart.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.traffic_heavy))
        binding.statsCard.visibility = View.VISIBLE
        binding.bottomSheet.visibility = View.GONE
    }

    private fun stopTracking() {
        isTracking = false
        binding.btnStart.text = "Start"
        binding.btnStart.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.secondary))
        binding.statsCard.visibility = View.GONE
    }

    private fun updateLiveStats() {
        val elapsedMs = SystemClock.elapsedRealtime() - startTimeMs
        val elapsedSec = (elapsedMs / 1000).toInt()
        val distanceM = computeTrackDistance()
        val mode = viewModel.activityMode.value
        binding.tvLiveTime.text = FormatUtils.formatDuration(elapsedSec)
        binding.tvLiveDistance.text = FormatUtils.formatDistance(distanceM)
        binding.tvLivePace.text = if (distanceM > 0 && elapsedSec > 0) {
            val paceSecPerKm = (elapsedSec * 1000f / distanceM).toInt()
            val pm = paceSecPerKm / 60
            val ps = paceSecPerKm % 60
            "$pm'${ps.toString().padStart(2,'0')}\" /km"
        } else "--'-- /km"
        binding.tvLiveCalories.text = "${FormatUtils.estimatedCalories(distanceM, mode)} kcal"
    }

    private fun computeTrackDistance(): Int {
        var dist = 0f
        for (i in 1 until trackPoints.size) {
            val r = FloatArray(1)
            android.location.Location.distanceBetween(
                trackPoints[i-1].latitude, trackPoints[i-1].longitude,
                trackPoints[i].latitude, trackPoints[i].longitude, r
            )
            dist += r[0]
        }
        return dist.toInt()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is MapUiState.Idle -> {
                            binding.progressBar.visibility = View.GONE
                            binding.bottomSheet.visibility = View.GONE
                            binding.btnStopNav.visibility = View.GONE
                            clearPolylines()
                        }
                        is MapUiState.Loading -> binding.progressBar.visibility = View.VISIBLE
                        is MapUiState.RoutesLoaded -> showRoutes(state)
                        is MapUiState.Navigating -> showNavigating(state)
                        is MapUiState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun showRoutes(state: MapUiState.RoutesLoaded) {
        binding.progressBar.visibility = View.GONE
        binding.bottomSheet.visibility = View.VISIBLE
        clearPolylines()
        state.routes.forEachIndexed { i, route ->
            val color = if (i == 0) R.color.route_primary else R.color.route_alternate
            drawPolyline(route.overviewPolyline, ContextCompat.getColor(requireContext(), color), i == 0)
        }
        (binding.rvSuggestions.adapter as? SuggestionsAdapter)?.submitList(state.suggestions)
        state.routes.firstOrNull()?.let { showRouteInfo(it) }
    }

    private fun showNavigating(state: MapUiState.Navigating) {
        binding.progressBar.visibility = View.GONE
        binding.btnStopNav.visibility = View.VISIBLE
        clearPolylines()
        drawPolyline(state.activeRoute.overviewPolyline,
            ContextCompat.getColor(requireContext(), R.color.route_active), true)
        (binding.rvSuggestions.adapter as? SuggestionsAdapter)?.submitList(state.suggestions)
        showRouteInfo(state.activeRoute)
    }

    private fun showRouteInfo(route: Route) {
        binding.tvDistance.text = FormatUtils.formatDistance(route.distanceMeters)
        binding.tvDuration.text = FormatUtils.formatDuration(route.durationInTrafficSeconds ?: route.durationSeconds)
        binding.tvTrafficLevel.text = route.trafficLevel.displayName
        val trafficColor = when (route.trafficLevel) {
            TrafficLevel.CLEAR -> R.color.traffic_clear
            TrafficLevel.LIGHT -> R.color.traffic_light
            TrafficLevel.MODERATE -> R.color.traffic_moderate
            TrafficLevel.HEAVY -> R.color.traffic_heavy
            TrafficLevel.UNKNOWN -> R.color.on_surface
        }
        binding.tvTrafficLevel.setTextColor(ContextCompat.getColor(requireContext(), trafficColor))
    }

    private fun drawPolyline(encoded: String, color: Int, primary: Boolean) {
        val points = PolylineDecoder.decode(encoded)
        if (points.isEmpty()) return
        googleMap?.addPolyline(
            PolylineOptions().addAll(points).color(color)
                .width(if (primary) 14f else 8f).zIndex(if (primary) 1f else 0f)
        )?.also { drawnPolylines.add(it) }
    }

    private fun clearPolylines() { drawnPolylines.forEach { it.remove() }; drawnPolylines.clear() }

    private fun checkPermissions() {
        if (hasLocationPermission()) {
            startLocationService()
            enableMyLocation()
        } else {
            permissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    private fun hasLocationPermission() =
        ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    private fun startLocationService() {
        requireContext().startForegroundService(
            Intent(requireContext(), LocationTrackingService::class.java)
        )
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(
            requireContext(), locationReceiver,
            IntentFilter(LocationTrackingService.ACTION_LOCATION_UPDATE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStop() {
        super.onStop()
        requireContext().unregisterReceiver(locationReceiver)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
