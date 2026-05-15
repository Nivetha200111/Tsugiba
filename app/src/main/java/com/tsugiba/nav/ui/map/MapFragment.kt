package com.tsugiba.nav.ui.map

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
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
import com.tsugiba.nav.util.PolylineDecoder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import android.Manifest

@AndroidEntryPoint
class MapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MapViewModel by viewModels()
    private var googleMap: GoogleMap? = null
    private val drawnPolylines = mutableListOf<Polyline>()
    private var currentMarker: Marker? = null

    private val locationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val lat = intent.getDoubleExtra(LocationTrackingService.EXTRA_LAT, 0.0)
            val lng = intent.getDoubleExtra(LocationTrackingService.EXTRA_LNG, 0.0)
            viewModel.updateLocation(lat, lng)
            moveCamera(lat, lng)
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
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
        setupMap()
        setupActivityModeChips()
        setupSuggestionsList()
        setupSearchBar()
        observeUiState()
        checkAndRequestPermissions()
    }

    private fun setupMap() {
        val mapFrag = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFrag.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.uiSettings.apply {
            isZoomControlsEnabled = true
            isCompassEnabled = true
            isMyLocationButtonEnabled = true
        }
        map.setOnMapLongClickListener { latLng ->
            viewModel.searchRoute(latLng.latitude, latLng.longitude)
        }
    }

    private fun setupActivityModeChips() {
        binding.chipGroupMode.setOnCheckedStateChangeListener { _, checkedIds ->
            val mode = when (checkedIds.firstOrNull()) {
                R.id.chipJog -> ActivityMode.JOG
                R.id.chipRun -> ActivityMode.RUN
                else -> ActivityMode.WALK
            }
            viewModel.setActivityMode(mode)
        }
    }

    private fun setupSuggestionsList() {
        val adapter = SuggestionsAdapter { suggestion ->
            suggestion.altRoute?.let { viewModel.selectRoute(it) }
        }
        binding.rvSuggestions.adapter = adapter
    }

    private fun setupSearchBar() {
        binding.btnStopNav.setOnClickListener { viewModel.stopNavigation() }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is MapUiState.Idle -> showIdleState()
                        is MapUiState.Loading -> showLoading()
                        is MapUiState.RoutesLoaded -> showRoutes(state)
                        is MapUiState.Navigating -> showNavigation(state)
                        is MapUiState.Error -> showError(state.message)
                    }
                }
            }
        }
    }

    private fun showIdleState() {
        binding.progressBar.visibility = View.GONE
        binding.bottomSheet.visibility = View.GONE
        binding.btnStopNav.visibility = View.GONE
        clearPolylines()
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun showRoutes(state: MapUiState.RoutesLoaded) {
        binding.progressBar.visibility = View.GONE
        binding.bottomSheet.visibility = View.VISIBLE
        clearPolylines()
        state.routes.forEachIndexed { index, route ->
            val color = if (index == 0) R.color.route_primary else R.color.route_alternate
            drawPolyline(route.overviewPolyline, ContextCompat.getColor(requireContext(), color), index == 0)
        }
        updateSuggestions(state.suggestions)
        state.routes.firstOrNull()?.let { showRouteInfo(it) }
    }

    private fun showNavigation(state: MapUiState.Navigating) {
        binding.progressBar.visibility = View.GONE
        binding.btnStopNav.visibility = View.VISIBLE
        clearPolylines()
        drawPolyline(
            state.activeRoute.overviewPolyline,
            ContextCompat.getColor(requireContext(), R.color.route_active),
            true
        )
        updateSuggestions(state.suggestions)
        showRouteInfo(state.activeRoute)
    }

    private fun showError(message: String) {
        binding.progressBar.visibility = View.GONE
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun drawPolyline(encoded: String, color: Int, primary: Boolean) {
        val points = PolylineDecoder.decode(encoded)
        googleMap?.addPolyline(
            PolylineOptions()
                .addAll(points)
                .color(color)
                .width(if (primary) 12f else 7f)
                .zIndex(if (primary) 1f else 0f)
        )?.also { drawnPolylines.add(it) }
    }

    private fun clearPolylines() {
        drawnPolylines.forEach { it.remove() }
        drawnPolylines.clear()
    }

    private fun showRouteInfo(route: Route) {
        binding.tvDistance.text = com.tsugiba.nav.util.FormatUtils.formatDistance(route.distanceMeters)
        binding.tvDuration.text = com.tsugiba.nav.util.FormatUtils.formatDuration(
            route.durationInTrafficSeconds ?: route.durationSeconds
        )
        binding.tvTrafficLevel.text = route.trafficLevel.displayName
    }

    private fun updateSuggestions(suggestions: List<TrafficSuggestion>) {
        (binding.rvSuggestions.adapter as? SuggestionsAdapter)?.submitList(suggestions)
    }

    private fun moveCamera(lat: Double, lng: Double) {
        currentMarker?.remove()
        googleMap?.let { map ->
            currentMarker = map.addMarker(MarkerOptions().position(com.google.android.gms.maps.model.LatLng(lat, lng)))
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(com.google.android.gms.maps.model.LatLng(lat, lng), 16f))
        }
    }

    private fun checkAndRequestPermissions() {
        val fine = Manifest.permission.ACCESS_FINE_LOCATION
        val coarse = Manifest.permission.ACCESS_COARSE_LOCATION
        if (ContextCompat.checkSelfPermission(requireContext(), fine) == PackageManager.PERMISSION_GRANTED) {
            startLocationService()
        } else {
            permissionLauncher.launch(arrayOf(fine, coarse))
        }
    }

    private fun startLocationService() {
        val intent = Intent(requireContext(), LocationTrackingService::class.java)
        requireContext().startForegroundService(intent)
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(
            requireContext(),
            locationReceiver,
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
