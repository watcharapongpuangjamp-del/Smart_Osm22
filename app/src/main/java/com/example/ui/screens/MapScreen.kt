package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.preference.PreferenceManager
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.HouseSummary
import com.example.data.Household
import com.example.data.DataStatus
import com.example.data.PopulationEvent
import com.example.data.PopulationEventType
import com.example.data.sync.SyncState
import com.example.ui.theme.*
import com.example.ui.components.IdCardScannerDialog
import com.example.viewmodel.PersonViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

enum class MapLayerType(val title: String, val subtitle: String) {
    STANDARD_2D("แผนที่มาตรฐาน (2D)", "เส้นทาง คลอง และชื่อสถานที่คมชัด"),
    SATELLITE("ภาพถ่ายดาวเทียม (เห็นหลังคาบ้าน)", "Esri World Imagery เห็นตัวบ้านและหลังคาจริง"),
    TERRAIN_3D("ภูมิประเทศ 3D / Relief", "OpenTopoMap ระดับความสูง ภูเขา แม่น้ำ ลาดชัน"),
    HYBRID_SATELLITE("ดาวเทียม + เส้นทาง (Hybrid)", "Google Hybrid มองเห็นสิ่งปลูกสร้างพร้อมถนน")
}

enum class MarkerStyle(val title: String) {
    PIN_3D_HOUSE("หมุด 3D ทรงบ้านเรือน"),
    BADGE_2D("หมุด 2D สัญลักษณ์ประชากร")
}

enum class MapDisplayMode(val title: String) {
    HOUSEHOLDS("ครัวเรือน"),
    EVENTS("เหตุการณ์ประชากร")
}

enum class PopulationFilter(val label: String) {
    ALL("ทั้งหมด"),
    HIGH_DENSITY("หนาแน่น (4+ คน)"),
    ELDERLY("มีผู้สูงอายุ (60+)"),
    CHILDREN("มีเด็กเล็ก (0-12)"),
    LOW_DENSITY("1-2 คน")
}

// Custom Tile Sources for Satellite, Terrain 3D, and Google Hybrid
val ESRI_SATELLITE_TILE_SOURCE: ITileSource = object : OnlineTileSourceBase(
    "EsriSatellite",
    0,
    19,
    256,
    ".jpg",
    arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/")
) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        val zoom = MapTileIndex.getZoom(pMapTileIndex)
        val x = MapTileIndex.getX(pMapTileIndex)
        val y = MapTileIndex.getY(pMapTileIndex)
        return "$baseUrl$zoom/$y/$x$mImageFilenameEnding"
    }
}

val OPENTOPO_TERRAIN_TILE_SOURCE: ITileSource = XYTileSource(
    "OpenTopoMap",
    0,
    17,
    256,
    ".png",
    arrayOf(
        "https://a.tile.opentopomap.org/",
        "https://b.tile.opentopomap.org/",
        "https://c.tile.opentopomap.org/"
    ),
    "© OpenTopoMap, © OpenStreetMap contributors"
)

val GOOGLE_HYBRID_TILE_SOURCE: ITileSource = object : OnlineTileSourceBase(
    "GoogleHybrid",
    0,
    20,
    256,
    "",
    arrayOf("https://mt1.google.com/vt/lyrs=y&x={x}&y={y}&z={z}")
) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        val zoom = MapTileIndex.getZoom(pMapTileIndex)
        val x = MapTileIndex.getX(pMapTileIndex)
        val y = MapTileIndex.getY(pMapTileIndex)
        return "https://mt1.google.com/vt/lyrs=y&x=$x&y=$y&z=$zoom"
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    viewModel: PersonViewModel,
    targetHouseholdId: Long = -1L,
    onHouseClick: (Long) -> Unit,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val houseSummary by viewModel.houseSummary.collectAsStateWithLifecycle()
    val allEvents by viewModel.allEvents.collectAsStateWithLifecycle()
    val allHouseholdsWithPersons by viewModel.allHouseholdsWithPersons.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()

    val locationPermissionState = rememberPermissionState(permission = Manifest.permission.ACCESS_FINE_LOCATION)
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Village Baseline GIS Data State
    val villageBaselineRepository = remember { com.example.data.village.VillageBaselineRepository(context) }
    var villageBaselineList by remember { mutableStateOf<List<com.example.data.village.VillageBaseline>>(emptyList()) }
    var showVillageMarkers by remember { mutableStateOf(true) }
    var selectedVillageBaseline by remember { mutableStateOf<com.example.data.village.VillageBaseline?>(null) }

    LaunchedEffect(Unit) {
        villageBaselineList = villageBaselineRepository.getAllVillages()
    }

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var selectedHouse by remember { mutableStateOf<HouseSummary?>(null) }
    var selectedEvent by remember { mutableStateOf<PopulationEvent?>(null) }
    var showHouseholdSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    var displayMode by remember { mutableStateOf(MapDisplayMode.HOUSEHOLDS) }
    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf(PopulationFilter.ALL) }
    var showStatsPanel by remember { mutableStateOf(false) }

    // Map Layer and House Marker Style State
    var selectedMapLayer by remember { mutableStateOf(MapLayerType.STANDARD_2D) }
    var selectedMarkerStyle by remember { mutableStateOf(MarkerStyle.PIN_3D_HOUSE) }
    var showLayerMenu by remember { mutableStateOf(false) }
    var isClusteringEnabled by remember { mutableStateOf(true) }

    // Pinning state
    var isPinningMode by remember { mutableStateOf(false) }
    var pinningTargetHousehold by remember { mutableStateOf<Household?>(null) }
    var pendingPinLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var showHouseholdPickerDialog by remember { mutableStateOf(false) }
    var showUnpinnedHousesSheet by remember { mutableStateOf(false) }
    var houseToClearLocation by remember { mutableStateOf<HouseSummary?>(null) }
    var currentUserLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var showAddHouseholdDialog by remember { mutableStateOf(false) }

    val selectedHousePersons = remember(selectedHouse, allHouseholdsWithPersons) {
        allHouseholdsWithPersons.find { it.household.id == selectedHouse?.householdId }?.persons ?: emptyList()
    }

    val mappedHouses = remember(houseSummary) { houseSummary.filter { it.latitude != null && it.longitude != null } }
    val unmappedHouses = remember(houseSummary) { houseSummary.filter { it.latitude == null || it.longitude == null } }

    val filteredHouses = remember(mappedHouses, activeFilter, searchQuery) {
        val byFilter = when (activeFilter) {
            PopulationFilter.ALL -> mappedHouses
            PopulationFilter.HIGH_DENSITY -> mappedHouses.filter { it.totalMembers >= 4 }
            PopulationFilter.ELDERLY -> mappedHouses.filter { it.elderly > 0 }
            PopulationFilter.CHILDREN -> mappedHouses.filter { it.children > 0 }
            PopulationFilter.LOW_DENSITY -> mappedHouses.filter { it.totalMembers in 1..2 }
        }
        if (searchQuery.isBlank()) byFilter
        else byFilter.filter { it.houseNo.contains(searchQuery.trim(), ignoreCase = true) }
    }

    val currentZoom = mapViewRef?.zoomLevelDouble ?: 15.0

    val displayedClusters = remember(filteredHouses, isClusteringEnabled, currentZoom) {
        if (!isClusteringEnabled || currentZoom >= 16.5) {
            filteredHouses.map { HouseCluster(it.latitude ?: 0.0, it.longitude ?: 0.0, listOf(it)) }
        } else {
            val gridFactor = Math.pow(2.0, (16.5 - currentZoom).coerceAtLeast(0.0)) * 0.008
            class MutableCluster(var latSum: Double, var lonSum: Double, val houses: MutableList<HouseSummary>, var count: Int)
            val clusters = mutableListOf<MutableCluster>()
            
            for (house in filteredHouses) {
                val lat = house.latitude ?: continue
                val lon = house.longitude ?: continue
                
                var added = false
                for (cluster in clusters) {
                    val avgLat = cluster.latSum / cluster.count
                    val avgLon = cluster.lonSum / cluster.count
                    if (Math.abs(avgLat - lat) < gridFactor && Math.abs(avgLon - lon) < gridFactor) {
                        cluster.houses.add(house)
                        cluster.latSum += lat
                        cluster.lonSum += lon
                        cluster.count++
                        added = true
                        break
                    }
                }
                if (!added) {
                    clusters.add(MutableCluster(lat, lon, mutableListOf(house), 1))
                }
            }
            
            clusters.map { c ->
                HouseCluster(
                    centerLat = c.latSum / c.count,
                    centerLon = c.lonSum / c.count,
                    houses = c.houses
                )
            }
        }
    }

    // Population statistics for distribution analysis
    val totalMappedPopulation = remember(mappedHouses) { mappedHouses.sumOf { it.totalMembers } }
    val totalVillagePopulation = remember(houseSummary) { houseSummary.sumOf { it.totalMembers } }
    val mappedElderly = remember(mappedHouses) { mappedHouses.sumOf { it.elderly } }
    val mappedChildren = remember(mappedHouses) { mappedHouses.sumOf { it.children } }
    val mappedMales = remember(mappedHouses) { mappedHouses.sumOf { it.males } }
    val mappedFemales = remember(mappedHouses) { mappedHouses.sumOf { it.females } }
    val avgPerHouse = remember(mappedHouses) {
        if (mappedHouses.isNotEmpty()) totalMappedPopulation.toDouble() / mappedHouses.size else 0.0
    }
    val coveragePercent = remember(mappedHouses, houseSummary) {
        if (houseSummary.isNotEmpty()) (mappedHouses.size * 100) / houseSummary.size else 0
    }

    val firstLocation = mappedHouses.firstOrNull()
    val initialLat = firstLocation?.latitude ?: 14.2155
    val initialLon = firstLocation?.longitude ?: 101.0723

    // Handle incoming targetHouseholdId (e.g. from HouseDetailScreen or form)
    LaunchedEffect(targetHouseholdId, houseSummary) {
        if (targetHouseholdId != -1L && houseSummary.isNotEmpty()) {
            val target = houseSummary.find { it.householdId == targetHouseholdId }
            if (target != null) {
                if (target.latitude != null && target.longitude != null) {
                    selectedHouse = target
                    mapViewRef?.controller?.animateTo(GeoPoint(target.latitude, target.longitude))
                    mapViewRef?.controller?.setZoom(17.0)
                } else {
                    val hh = viewModel.getHouseholdById(targetHouseholdId)
                    if (hh != null) {
                        pinningTargetHousehold = hh
                        isPinningMode = true
                        val center = mapViewRef?.mapCenter?.let { GeoPoint(it.latitude, it.longitude) } ?: GeoPoint(initialLat, initialLon)
                        pendingPinLocation = center
                        Toast.makeText(context, "แตะบนแผนที่เพื่อระบุพิกัดบ้านเลขที่ ${hh.houseNo}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "ย้อนกลับ",
                            tint = Color.White
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            if (displayMode == MapDisplayMode.HOUSEHOLDS) "แผนที่พิกัดครัวเรือน (GIS)" else "แผนที่พิกัดเหตุการณ์ประชากร",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            if (isPinningMode) "โหมดปักหมุดตำแหน่งครัวเรือน" 
                            else if (displayMode == MapDisplayMode.HOUSEHOLDS) "กระจายตัวประชากร: $totalMappedPopulation/$totalVillagePopulation คน"
                            else "แสดงเหตุการณ์ประชากรทั้งหมด ${allEvents.size} รายการ",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                },
                actions = {
                    // Toggle Display Mode (Households vs Events)
                    IconButton(onClick = { 
                        displayMode = if (displayMode == MapDisplayMode.HOUSEHOLDS) MapDisplayMode.EVENTS else MapDisplayMode.HOUSEHOLDS
                        selectedHouse = null
                        selectedEvent = null
                    }) {
                        Icon(
                            if (displayMode == MapDisplayMode.HOUSEHOLDS) Icons.Filled.Map else Icons.Filled.Home,
                            contentDescription = "สลับโหมดการแสดงผล",
                            tint = MintAccent
                        )
                    }
                    // Toggle map layer & visual perspective dialog (2D, 3D Terrain, Satellite, House markers)
                    IconButton(onClick = { showLayerMenu = true }) {
                        Icon(
                            Icons.Filled.Layers,
                            contentDescription = "รูปแบบแผนที่ 3D/2D",
                            tint = Color.White
                        )
                    }
                    // Toggle population distribution statistics card
                    IconButton(onClick = { showStatsPanel = !showStatsPanel }) {
                        Icon(
                            Icons.Filled.Analytics,
                            contentDescription = "สถิติการกระจายตัว",
                            tint = if (showStatsPanel) MintAccent else Color.White
                        )
                    }
                    // Pin Mode button
                    IconButton(
                        onClick = {
                            if (isPinningMode) {
                                isPinningMode = false
                                pinningTargetHousehold = null
                                pendingPinLocation = null
                            } else {
                                isPinningMode = true
                                pinningTargetHousehold = null
                                val center = mapViewRef?.mapCenter?.let { GeoPoint(it.latitude, it.longitude) } ?: GeoPoint(initialLat, initialLon)
                                pendingPinLocation = center
                                Toast.makeText(context, "แตะตำแหน่งบนแผนที่ หรือลากหมุดสีแดง แล้วกดบันทึกพิกัด", Toast.LENGTH_LONG).show()
                            }
                        }
                    ) {
                        Icon(
                            if (isPinningMode) Icons.Filled.Close else Icons.Filled.AddLocationAlt,
                            contentDescription = if (isPinningMode) "ยกเลิกปักหมุด" else "ปักหมุดใหม่",
                            tint = if (isPinningMode) Color(0xFFFF6B6B) else Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isPinningMode) Color(0xFF065F46) else EmeraldPrimary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // OpenStreetMap View
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val config = Configuration.getInstance()
                    config.load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
                    config.userAgentValue = ctx.packageName
                    MapView(ctx).apply {
                        val tileSource = when (selectedMapLayer) {
                            MapLayerType.STANDARD_2D -> TileSourceFactory.MAPNIK
                            MapLayerType.SATELLITE -> ESRI_SATELLITE_TILE_SOURCE
                            MapLayerType.TERRAIN_3D -> OPENTOPO_TERRAIN_TILE_SOURCE
                            MapLayerType.HYBRID_SATELLITE -> GOOGLE_HYBRID_TILE_SOURCE
                        }
                        setTileSource(tileSource)
                        setMultiTouchControls(true)
                        controller.setZoom(16.0)
                        controller.setCenter(GeoPoint(initialLat, initialLon))
                        mapViewRef = this
                    }
                },
                update = { mapView ->
                    mapViewRef = mapView
                    
                    val desiredTileSource = when (selectedMapLayer) {
                        MapLayerType.STANDARD_2D -> TileSourceFactory.MAPNIK
                        MapLayerType.SATELLITE -> ESRI_SATELLITE_TILE_SOURCE
                        MapLayerType.TERRAIN_3D -> OPENTOPO_TERRAIN_TILE_SOURCE
                        MapLayerType.HYBRID_SATELLITE -> GOOGLE_HYBRID_TILE_SOURCE
                    }
                    if (mapView.tileProvider.tileSource.name() != desiredTileSource.name()) {
                        mapView.setTileSource(desiredTileSource)
                    }

                    mapView.overlays.removeAll { it is Marker || it is MapEventsOverlay }

                    // Add Touch Events Overlay for interactive map tapping and long press
                    val mapEventsReceiver = object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                            if (isPinningMode) {
                                pendingPinLocation = p
                                return true
                            }
                            selectedHouse = null
                            selectedEvent = null
                            return false
                        }

                        override fun longPressHelper(p: GeoPoint): Boolean {
                            pendingPinLocation = p
                            isPinningMode = true
                            Toast.makeText(context, "เลือกพิกัดแล้ว กดบันทึกเพื่อกำหนดครัวเรือน", Toast.LENGTH_SHORT).show()
                            return true
                        }
                    }
                    mapView.overlays.add(0, MapEventsOverlay(mapEventsReceiver))

                    // Add markers based on displayMode
                    if (displayMode == MapDisplayMode.HOUSEHOLDS) {
                        displayedClusters.forEach { cluster ->
                            if (cluster.houses.size == 1) {
                                val house = cluster.houses.first()
                                val lat = house.latitude ?: return@forEach
                                val lon = house.longitude ?: return@forEach
                                val marker = Marker(mapView).apply {
                                    position = GeoPoint(lat, lon)
                                    title = "บ้านเลขที่ ${house.houseNo}"
                                    snippet = "ประชากร ${house.totalMembers} คน (ชาย ${house.males}, หญิง ${house.females})"
                                    subDescription = if (house.elderly > 0) "ผู้สูงอายุ: ${house.elderly} คน" else null
                                    icon = createHouseholdMarkerDrawable(
                                        context = context,
                                        totalMembers = house.totalMembers,
                                        hasElderly = house.elderly > 0,
                                        hasChildren = house.children > 0,
                                        dataStatus = house.dataStatus,
                                        isSelected = selectedHouse?.householdId == house.householdId,
                                        markerStyle = selectedMarkerStyle
                                    )
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                }
                                marker.setOnMarkerClickListener { _, _ ->
                                    if (!isPinningMode) {
                                        selectedHouse = house
                                        showHouseholdSheet = true
                                        mapView.controller.animateTo(GeoPoint(lat, lon))
                                    }
                                    true
                                }
                                mapView.overlays.add(marker)
                            } else {
                                val marker = Marker(mapView).apply {
                                    position = GeoPoint(cluster.centerLat, cluster.centerLon)
                                    title = "กลุ่มครัวเรือน (${cluster.houses.size} หลัง)"
                                    snippet = "แตะเพื่อซูมเข้าหรือดูรายการครัวเรือนในกลุ่มนี้"
                                    icon = createClusterMarkerDrawable(
                                        context = context,
                                        count = cluster.houses.size,
                                        isSelected = cluster.houses.any { it.householdId == selectedHouse?.householdId }
                                    )
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                }
                                marker.setOnMarkerClickListener { _, _ ->
                                    if (!isPinningMode) {
                                        mapView.controller.animateTo(GeoPoint(cluster.centerLat, cluster.centerLon))
                                        mapView.controller.setZoom(mapView.zoomLevelDouble + 2.0)
                                        Toast.makeText(context, "กลุ่มครัวเรือน: ${cluster.houses.size} หลังคาเรือน (ซูมเข้าเพื่อขยาย)", Toast.LENGTH_SHORT).show()
                                    }
                                    true
                                }
                                mapView.overlays.add(marker)
                            }
                        }
                    } else {
                        allEvents.forEach { event ->
                            val marker = Marker(mapView).apply {
                                position = GeoPoint(event.latitude, event.longitude)
                                title = event.type.value
                                snippet = event.title
                                icon = createEventMarkerDrawable(context, event.type, selectedEvent?.id == event.id)
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            }
                            marker.setOnMarkerClickListener { _, _ ->
                                if (!isPinningMode) {
                                    selectedEvent = event
                                    mapView.controller.animateTo(GeoPoint(event.latitude, event.longitude))
                                }
                                true
                            }
                            mapView.overlays.add(marker)
                        }
                    }

                    // Add Village Baseline GIS Markers when enabled
                    if (showVillageMarkers && villageBaselineList.isNotEmpty()) {
                        villageBaselineList.forEach { v ->
                            val vLat = v.latitude
                            val vLon = v.longitude
                            if (vLat != 0.0 && vLon != 0.0) {
                                val vMarker = Marker(mapView).apply {
                                    position = GeoPoint(vLat, vLon)
                                    title = "หมู่บ้าน${v.villageName}"
                                    snippet = "ต.${v.subdistrictName} อ.${v.districtName} • เป้าหมาย: ${v.totalHouseCount} หลัง (${v.totalPopulation} คน)"
                                    icon = createVillageMarkerDrawable(
                                        context = context,
                                        villageName = v.villageName,
                                        houseCount = v.totalHouseCount,
                                        isSelected = selectedVillageBaseline?.villageCode == v.villageCode
                                    )
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                }
                                vMarker.setOnMarkerClickListener { _, _ ->
                                    if (!isPinningMode) {
                                        selectedVillageBaseline = v
                                        mapView.controller.animateTo(GeoPoint(vLat, vLon))
                                    }
                                    true
                                }
                                mapView.overlays.add(vMarker)
                            }
                        }
                    }

                    // Add pending pin marker when in pinning mode
                    if (isPinningMode && pendingPinLocation != null) {
                        val pendingMarker = Marker(mapView).apply {
                            position = pendingPinLocation
                            title = if (pinningTargetHousehold != null) "ปักหมุดบ้านเลขที่ ${pinningTargetHousehold?.houseNo}" else "หมุดตำแหน่งใหม่"
                            snippet = "แตะบนแผนที่เพื่อย้ายตำแหน่ง หรือลากหมุดนี้"
                            icon = createPendingPinMarkerDrawable(context)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            isDraggable = true
                            setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                                override fun onMarkerDrag(m: Marker) {}
                                override fun onMarkerDragEnd(m: Marker) {
                                    pendingPinLocation = m.position
                                }
                                override fun onMarkerDragStart(m: Marker) {}
                            })
                        }
                        mapView.overlays.add(pendingMarker)
                    }

                    // Add current user location blue marker if available
                    currentUserLocation?.let { loc ->
                        val userMarker = Marker(mapView).apply {
                            position = loc
                            title = "ตำแหน่งของคุณ"
                            snippet = "พิกัดปัจจุบันจาก GPS"
                            icon = createUserLocationMarkerDrawable(context)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        }
                        mapView.overlays.add(userMarker)
                    }

                    mapView.invalidate()
                }
            )

            // Top Search & Filter Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Cloud Sync / Offline Status Indicator Pill
                val syncIndicatorColors = when (syncState) {
                    is SyncState.Syncing -> Pair(Color(0xFF2563EB), Color(0xFFDBEAFE))
                    is SyncState.Success -> Pair(Color(0xFF059669), Color(0xFFD1FAE5))
                    is SyncState.Error -> Pair(Color(0xFFDC2626), Color(0xFFFEE2E2))
                    else -> Pair(Color(0xFF4B5563), Color(0xFFF3F4F6))
                }

                val syncIndicatorText = when (val state = syncState) {
                    is SyncState.Syncing -> state.message.ifBlank { "กำลังซิงค์ข้อมูล..." }
                    is SyncState.Success -> "ซิงค์คลาวด์แล้ว (Online)"
                    is SyncState.Error -> "ซิงค์ไม่สำเร็จ (แตะเพื่อลองใหม่)"
                    else -> "โหมดออฟไลน์ (Room Local)"
                }

                val syncIndicatorIcon = when (syncState) {
                    is SyncState.Syncing -> Icons.Filled.Sync
                    is SyncState.Success -> Icons.Filled.CloudDone
                    is SyncState.Error -> Icons.Filled.CloudOff
                    else -> Icons.Filled.Storage
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = syncIndicatorColors.second,
                    border = BorderStroke(1.dp, syncIndicatorColors.first.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .wrapContentWidth()
                        .shadow(2.dp, RoundedCornerShape(20.dp))
                        .clickable {
                            if (syncState !is SyncState.Syncing) {
                                viewModel.bidirectionalSync { result ->
                                    result.onSuccess {
                                        Toast.makeText(context, "ซิงค์ข้อมูลสำเร็จ", Toast.LENGTH_SHORT).show()
                                    }.onFailure {
                                        Toast.makeText(context, "ซิงค์ไม่สำเร็จ: ${it.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (syncState is SyncState.Syncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = syncIndicatorColors.first
                            )
                        } else {
                            Icon(
                                syncIndicatorIcon,
                                contentDescription = null,
                                tint = syncIndicatorColors.first,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = syncIndicatorText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = syncIndicatorColors.first
                        )
                    }
                }

                // Pinning mode status banner
                AnimatedVisibility(
                    visible = isPinningMode,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(6.dp, RoundedCornerShape(14.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF047857)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Filled.PinDrop, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (pinningTargetHousehold != null) "กำลังปักหมุด: บ้านเลขที่ ${pinningTargetHousehold?.houseNo}" else "โหมดปักหมุดพิกัดครัวเรือน",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    pendingPinLocation?.let { "Lat: ${String.format("%.5f", it.latitude)}, Lon: ${String.format("%.5f", it.longitude)}" }
                                        ?: "แตะตำแหน่งบนแผนที่เพื่อวางหมุด",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                            IconButton(
                                onClick = {
                                    isPinningMode = false
                                    pinningTargetHousehold = null
                                    pendingPinLocation = null
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "ยกเลิก", tint = Color.White)
                            }
                        }
                    }
                }

                // Search Bar
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(6.dp, RoundedCornerShape(16.dp), spotColor = CardShadowTint),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("ค้นหาบ้านเลขที่บนแผนที่...") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "ล้างค้นหา", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // Population Distribution Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(PopulationFilter.values()) { filter ->
                        val count = when (filter) {
                            PopulationFilter.ALL -> mappedHouses.size
                            PopulationFilter.HIGH_DENSITY -> mappedHouses.count { it.totalMembers >= 4 }
                            PopulationFilter.ELDERLY -> mappedHouses.count { it.elderly > 0 }
                            PopulationFilter.CHILDREN -> mappedHouses.count { it.children > 0 }
                            PopulationFilter.LOW_DENSITY -> mappedHouses.count { it.totalMembers in 1..2 }
                        }
                        FilterChip(
                            selected = activeFilter == filter,
                            onClick = { activeFilter = filter },
                            label = { Text("${filter.label} ($count)") },
                            leadingIcon = if (activeFilter == filter) {
                                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            )
                        )
                    }

                    // Village Baseline GIS chip
                    item {
                        FilterChip(
                            selected = showVillageMarkers,
                            onClick = { showVillageMarkers = !showVillageMarkers },
                            label = { Text("หมุดศูนย์กลางหมู่บ้าน (${villageBaselineList.size})") },
                            leadingIcon = {
                                Icon(Icons.Filled.LocationCity, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF1E3A8A),
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            )
                        )
                    }

                    // Unpinned houses shortcut chip
                    item {
                        ActionChip(
                            label = "ยังไม่ระบุพิกัด (${unmappedHouses.size})",
                            icon = Icons.Filled.LocationOff,
                            tint = if (unmappedHouses.isNotEmpty()) Color(0xFFE11D48) else Color.Gray,
                            onClick = { showUnpinnedHousesSheet = true }
                        )
                    }
                }

                // Collapsible Population Distribution Overview Card
                AnimatedVisibility(
                    visible = showStatsPanel,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(8.dp, RoundedCornerShape(18.dp), spotColor = CardShadowTint),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Filled.Analytics, contentDescription = null, tint = EmeraldPrimary)
                                    Text(
                                        "สรุปการกระจายตัวของประชากร",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    "ครอบคลุม $coveragePercent%",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                            }

                            // Progress Bar
                            LinearProgressIndicator(
                                progress = { if (houseSummary.isNotEmpty()) mappedHouses.size.toFloat() / houseSummary.size.toFloat() else 0f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = EmeraldPrimary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            // Population Breakdown Grid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StatMiniCard(
                                    modifier = Modifier.weight(1f),
                                    title = "ประชากรบนแผนที่",
                                    value = "$totalMappedPopulation คน",
                                    subtext = "ชาย $mappedMales | หญิง $mappedFemales",
                                    color = EmeraldPrimary
                                )
                                StatMiniCard(
                                    modifier = Modifier.weight(1f),
                                    title = "ผู้สูงอายุ (60+)",
                                    value = "$mappedElderly คน",
                                    subtext = "กลุ่มเปราะบาง",
                                    color = Color(0xFF7C3AED)
                                )
                                StatMiniCard(
                                    modifier = Modifier.weight(1f),
                                    title = "เด็กเล็ก (0-12)",
                                    value = "$mappedChildren คน",
                                    subtext = "วัยเจริญเติบโต",
                                    color = Color(0xFF0284C7)
                                )
                            }

                            // Legend and Unpinned Action
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    LegendDot(color = Color(0xFF059669), label = "1-3 คน")
                                    LegendDot(color = Color(0xFFEA580C), label = "4+ คน")
                                    LegendDot(color = Color(0xFF7C3AED), label = "ผู้สูงอายุ")
                                }
                                if (unmappedHouses.isNotEmpty()) {
                                    TextButton(
                                        onClick = { showUnpinnedHousesSheet = true },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("ปักหมุดบ้านที่เหลือ (${unmappedHouses.size})", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Right-side Floating Control Buttons
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Map Layers and 3D Terrain Switcher FAB
                FloatingActionButton(
                    onClick = { showLayerMenu = true },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = EmeraldPrimary,
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp).shadow(4.dp, CircleShape)
                ) {
                    Icon(Icons.Filled.Layers, contentDescription = "เปลี่ยนรูปแบบแผนที่ 3D/2D", modifier = Modifier.size(20.dp))
                }

                // Clustering Toggle FAB
                FloatingActionButton(
                    onClick = { isClusteringEnabled = !isClusteringEnabled },
                    containerColor = if (isClusteringEnabled) EmeraldPrimary else MaterialTheme.colorScheme.surface,
                    contentColor = if (isClusteringEnabled) Color.White else MaterialTheme.colorScheme.onSurface,
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp).shadow(4.dp, CircleShape)
                ) {
                    Icon(
                        Icons.Filled.GroupWork,
                        contentDescription = "สลับการรวมกลุ่มหมุด (Cluster)",
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Add New Household FAB
                FloatingActionButton(
                    onClick = { showAddHouseholdDialog = true },
                    containerColor = EmeraldPrimary,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp).shadow(4.dp, CircleShape)
                ) {
                    Icon(
                        Icons.Filled.AddHome,
                        contentDescription = "เพิ่มครัวเรือนใหม่",
                        modifier = Modifier.size(20.dp)
                    )
                }

                // My GPS Location FAB
                FloatingActionButton(
                    onClick = {
                        if (locationPermissionState.status.isGranted) {
                            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                if (location != null) {
                                    val userPoint = GeoPoint(location.latitude, location.longitude)
                                    currentUserLocation = userPoint
                                    mapViewRef?.controller?.animateTo(userPoint)
                                    mapViewRef?.controller?.setZoom(17.0)
                                    Toast.makeText(context, "ย้ายไปยังตำแหน่งปัจจุบันของคุณ", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "ไม่พบตำแหน่ง GPS ปัจจุบัน", Toast.LENGTH_SHORT).show()
                                }
                            }.addOnFailureListener {
                                Toast.makeText(context, "เกิดข้อผิดพลาดในการดึง GPS", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            locationPermissionState.launchPermissionRequest()
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = Color(0xFF2563EB),
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp).shadow(4.dp, CircleShape)
                ) {
                    Icon(
                        Icons.Filled.MyLocation,
                        contentDescription = "ตำแหน่งปัจจุบันของฉัน",
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Zoom In
                FloatingActionButton(
                    onClick = { mapViewRef?.controller?.zoomIn() },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp).shadow(4.dp, CircleShape)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "ซูมเข้า", modifier = Modifier.size(20.dp))
                }

                // Zoom Out
                FloatingActionButton(
                    onClick = { mapViewRef?.controller?.zoomOut() },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp).shadow(4.dp, CircleShape)
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "ซูมออก", modifier = Modifier.size(20.dp))
                }

                // Pin Here via GPS (When in pinning mode)
                if (isPinningMode) {
                    FloatingActionButton(
                        onClick = {
                            if (locationPermissionState.status.isGranted) {
                                coroutineScope.launch {
                                    try {
                                        @SuppressLint("MissingPermission")
                                        val req = com.google.android.gms.location.CurrentLocationRequest.Builder()
                                            .setPriority(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY)
                                            .build()
                                        @SuppressLint("MissingPermission")
                                        val loc = fusedLocationClient.getCurrentLocation(req, null).await()
                                        if (loc != null) {
                                            val pt = GeoPoint(loc.latitude, loc.longitude)
                                            pendingPinLocation = pt
                                            mapViewRef?.controller?.animateTo(pt)
                                            mapViewRef?.controller?.setZoom(17.0)
                                            Toast.makeText(context, "วางหมุดที่ตำแหน่ง GPS ปัจจุบันแล้ว", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "เกิดข้อผิดพลาด: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                locationPermissionState.launchPermissionRequest()
                            }
                        },
                        containerColor = Color(0xFFEA580C),
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(50.dp).shadow(6.dp, CircleShape)
                    ) {
                        Icon(Icons.Filled.GpsFixed, contentDescription = "วางหมุดตาม GPS ปัจจุบัน")
                    }
                }

                // My Location
                FloatingActionButton(
                    onClick = {
                        if (locationPermissionState.status.isGranted) {
                            coroutineScope.launch {
                                try {
                                    Toast.makeText(context, "กำลังค้นหาตำแหน่งของคุณ...", Toast.LENGTH_SHORT).show()
                                    @SuppressLint("MissingPermission")
                                    val locationRequest = com.google.android.gms.location.CurrentLocationRequest.Builder()
                                        .setPriority(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY)
                                        .build()
                                    @SuppressLint("MissingPermission")
                                    val location = fusedLocationClient.getCurrentLocation(locationRequest, null).await()
                                    if (location != null) {
                                        val geoPoint = GeoPoint(location.latitude, location.longitude)
                                        mapViewRef?.controller?.animateTo(geoPoint)
                                        mapViewRef?.controller?.setZoom(17.0)
                                        Toast.makeText(context, "ย้ายไปยังตำแหน่งปัจจุบันแล้ว", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "ไม่พบตำแหน่งปัจจุบัน", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "เกิดข้อผิดพลาดในการดึงพิกัด: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            locationPermissionState.launchPermissionRequest()
                        }
                    },
                    containerColor = EmeraldPrimary,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(52.dp).shadow(6.dp, CircleShape)
                ) {
                    Icon(Icons.Filled.MyLocation, contentDescription = "ตำแหน่งของฉัน")
                }
            }

            // Bottom Floating Card 1: Pinning Mode Confirmation Bar
            AnimatedVisibility(
                visible = isPinningMode,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(20.dp), spotColor = CardShadowTint),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, EmeraldPrimary)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFFEF2F2)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.PinDrop, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(24.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (pinningTargetHousehold != null) "ปักหมุดบ้านเลขที่ ${pinningTargetHousehold?.houseNo}" else "พิกัดที่เลือกบนแผนที่",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    pendingPinLocation?.let { "Lat: ${String.format("%.5f", it.latitude)}, Lon: ${String.format("%.5f", it.longitude)}" } ?: "กรุณาแตะบนแผนที่",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    isPinningMode = false
                                    pinningTargetHousehold = null
                                    pendingPinLocation = null
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("ยกเลิก")
                            }

                            Button(
                                onClick = {
                                    val loc = pendingPinLocation
                                    if (loc == null) {
                                        Toast.makeText(context, "กรุณาแตะบนแผนที่เพื่อวางหมุดก่อน", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (pinningTargetHousehold != null) {
                                        val hh = pinningTargetHousehold!!
                                        viewModel.updateHouseholdLocation(
                                            householdId = hh.id,
                                            latitude = loc.latitude,
                                            longitude = loc.longitude,
                                            provider = "MANUAL_PIN"
                                        ) { success, msg ->
                                            if (success) {
                                                Toast.makeText(context, "บันทึกพิกัดบ้านเลขที่ ${hh.houseNo} สำเร็จ", Toast.LENGTH_SHORT).show()
                                                isPinningMode = false
                                                pinningTargetHousehold = null
                                                pendingPinLocation = null
                                            } else {
                                                Toast.makeText(context, msg ?: "เกิดข้อผิดพลาด", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } else {
                                        showHouseholdPickerDialog = true
                                    }
                                },
                                modifier = Modifier.weight(1.5f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                            ) {
                                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (pinningTargetHousehold != null) "บันทึกพิกัดนี้" else "เลือกครัวเรือน...")
                            }
                        }
                    }
                }
            }

            // Bottom Floating Card 2: Selected House Preview Card (Normal Mode)
            // Removed in favor of ModalBottomSheet for richer summary details

            // Bottom Floating Card 3: Selected Event Preview Card
            AnimatedVisibility(
                visible = selectedEvent != null && !isPinningMode,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                selectedEvent?.let { event ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(12.dp, RoundedCornerShape(22.dp), spotColor = CardShadowTint),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.5.dp, MintAccent.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    val eventColor = when (event.type) {
                                        PopulationEventType.BIRTH -> Color(0xFF10B981)
                                        PopulationEventType.DEATH -> Color(0xFF6B7280)
                                        PopulationEventType.MOVE_IN -> Color(0xFF3B82F6)
                                        PopulationEventType.MOVE_OUT -> Color(0xFFF59E0B)
                                        PopulationEventType.HEALTH_CHECK -> Color(0xFF8B5CF6)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(eventColor.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            when(event.type) {
                                                PopulationEventType.BIRTH -> Icons.Filled.ChildCare
                                                PopulationEventType.DEATH -> Icons.Filled.PersonRemove
                                                PopulationEventType.MOVE_IN -> Icons.Filled.Login
                                                PopulationEventType.MOVE_OUT -> Icons.Filled.Logout
                                                PopulationEventType.HEALTH_CHECK -> Icons.Filled.MedicalInformation
                                            },
                                            contentDescription = null, 
                                            tint = eventColor, 
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = event.type.value,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = event.title,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(onClick = { selectedEvent = null }) {
                                    Icon(Icons.Filled.Close, contentDescription = "ปิด", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            if (!event.description.isNullOrBlank()) {
                                Text(
                                    text = event.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Filled.AccessTime, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                                    val dateStr = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(event.timestamp))
                                    Text(
                                        dateStr,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (event.personName != null) {
                                    Text(
                                        event.personName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            
                            Button(
                                onClick = { viewModel.deleteEvent(event); selectedEvent = null },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.1f), contentColor = Color.Red)
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("ลบเหตุการณ์นี้")
                            }
                        }
                    }
                }
            }
        }

        // Village Baseline GIS Information Dialog
        if (selectedVillageBaseline != null) {
            val village = selectedVillageBaseline!!
            AlertDialog(
                onDismissRequest = { selectedVillageBaseline = null },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.LocationCity,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = "ข้อมูล GIS หมู่บ้าน${village.villageName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = EmeraldPrimary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "ที่ตั้ง: หมู่บ้าน${village.villageName} (หมู่ ${village.villageNo})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "ตำบล${village.subdistrictName} อำเภอ${village.districtName} จังหวัด${village.provinceName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Target Statistics
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("หลังคาเรือนเป้าหมาย", style = MaterialTheme.typography.labelSmall)
                                    Text("${village.totalHouseCount} หลัง", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                                }
                            }
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("ประชากรรวมเป้าหมาย", style = MaterialTheme.typography.labelSmall)
                                    Text("${village.totalPopulation} คน", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                                    Text("(ชาย ${village.menCount} / หญิง ${village.womenCount})", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        // Infrastructure Details
                        if (village.mainRoadName.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Filled.AddRoad, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                                Text("ถนนสายหลัก: ${village.mainRoadName}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (village.riverName.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Filled.Water, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(16.dp))
                                Text("แหล่งน้ำ/สายน้ำ: ${village.riverName}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (village.damName.isNotBlank() || village.reservoirName.isNotBlank() || village.weirName.isNotBlank()) {
                            val waterAsset = listOf(village.damName, village.reservoirName, village.weirName).filter { it.isNotBlank() }.joinToString(", ")
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Filled.Pool, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(16.dp))
                                Text("ชลประทาน/ฝาย: $waterAsset", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (village.localGovName.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Filled.AccountBalance, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                                Text("อปท. ในพื้นที่: ${village.localGovName}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            mapViewRef?.controller?.animateTo(GeoPoint(village.latitude, village.longitude))
                            mapViewRef?.controller?.setZoom(16.0)
                            selectedVillageBaseline = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ซูมไปยังจุดนี้")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedVillageBaseline = null }) {
                        Text("ปิด")
                    }
                }
            )
        }
        }

        // Household Summary Bottom Sheet
        if (showHouseholdSheet && selectedHouse != null) {
            ModalBottomSheet(
                onDismissRequest = { showHouseholdSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                dragHandle = { BottomSheetDefaults.DragHandle() },
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                selectedHouse?.let { house ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Header Section
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(EmeraldPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Home, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(32.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "บ้านเลขที่ ${house.houseNo}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (!house.headName.isNullOrBlank()) {
                                    Text(
                                        text = "เจ้าบ้าน: ${house.headName}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = EmeraldPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (house.dataStatus == DataStatus.VERIFIED) EmeraldPrimary.copy(alpha = 0.12f) else Color.Red.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, if (house.dataStatus == DataStatus.VERIFIED) EmeraldPrimary.copy(alpha = 0.3f) else Color.Red.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = house.dataStatus.value,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (house.dataStatus == DataStatus.VERIFIED) EmeraldPrimary else Color.Red
                                )
                            }
                        }

                        // Demographic Summary Cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatMiniCard(
                                modifier = Modifier.weight(1f),
                                title = "ทั้งหมด",
                                value = "${house.totalMembers}",
                                subtext = "คนในบ้าน",
                                color = MaterialTheme.colorScheme.primary
                            )
                            StatMiniCard(
                                modifier = Modifier.weight(1f),
                                title = "ชาย / หญิง",
                                value = "${house.males} / ${house.females}",
                                subtext = "เพศสภาพ",
                                color = Color(0xFF0EA5E9)
                            )
                            StatMiniCard(
                                modifier = Modifier.weight(1f),
                                title = "สูงอายุ / เด็ก",
                                value = "${house.elderly} / ${house.children}",
                                subtext = "กลุ่มเปราะบาง",
                                color = Color(0xFF8B5CF6)
                            )
                        }

                        // GPS Coordinates Section
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = EmeraldPrimary)
                                    Column {
                                        Text("พิกัดแผนที่ (GIS)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                        Text("Lat: ${house.latitude}, Lon: ${house.longitude}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                TextButton(onClick = { houseToClearLocation = house; showHouseholdSheet = false }) {
                                    Text("ลบพิกัด", color = Color.Red, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Household Members List Section
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("รายชื่อผู้อยู่อาศัย (${selectedHousePersons.size} คน)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            if (selectedHousePersons.isEmpty()) {
                                Text("ไม่พบข้อมูลรายชื่อ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(bottom = 4.dp)
                                ) {
                                    items(selectedHousePersons) { person ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    if (person.gender == com.example.data.Gender.MALE) Icons.Filled.Man else Icons.Filled.Woman,
                                                    contentDescription = null,
                                                    tint = if (person.gender == com.example.data.Gender.MALE) Color.Blue else Color.Magenta,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Column {
                                                    Text(person.fullName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                                    Text(person.houseStatus.value, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Main Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        val hh = viewModel.getHouseholdById(house.householdId)
                                        if (hh != null) {
                                            pinningTargetHousehold = hh
                                            pendingPinLocation = GeoPoint(house.latitude ?: initialLat, house.longitude ?: initialLon)
                                            isPinningMode = true
                                            showHouseholdSheet = false
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                Icon(Icons.Filled.EditLocation, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("ย้ายหมุด")
                            }

                            Button(
                                onClick = { onHouseClick(house.householdId) },
                                modifier = Modifier.weight(1.5f),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                Icon(Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("ดูข้อมูลครัวเรือน", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog: Household Picker (When dropping pin and selecting which house to assign)
    if (showHouseholdPickerDialog) {
        HouseholdPickerDialog(
            households = allHouseholdsWithPersons.map { it.household },
            unmappedHouseholdIds = unmappedHouses.map { it.householdId }.toSet(),
            onDismiss = { showHouseholdPickerDialog = false },
            onSelect = { household ->
                val loc = pendingPinLocation
                if (loc != null) {
                    viewModel.updateHouseholdLocation(
                        householdId = household.id,
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        provider = "MANUAL_PIN"
                    ) { success, msg ->
                        if (success) {
                            Toast.makeText(context, "บันทึกพิกัดบ้านเลขที่ ${household.houseNo} สำเร็จ", Toast.LENGTH_SHORT).show()
                            isPinningMode = false
                            pinningTargetHousehold = null
                            pendingPinLocation = null
                            showHouseholdPickerDialog = false
                        } else {
                            Toast.makeText(context, msg ?: "เกิดข้อผิดพลาด", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )
    }

    // Dialog: Add New Household Manually
    if (showAddHouseholdDialog) {
        val center = mapViewRef?.mapCenter
        AddHouseholdDialog(
            initialLat = center?.latitude,
            initialLon = center?.longitude,
            onDismiss = { showAddHouseholdDialog = false },
            onSave = { houseNo, headName, lat, lon ->
                if (houseNo.isBlank()) {
                    Toast.makeText(context, "กรุณากรอกบ้านเลขที่", Toast.LENGTH_SHORT).show()
                    return@AddHouseholdDialog
                }
                viewModel.addNewHouseholdWithHead(
                    houseNo = houseNo,
                    headName = headName,
                    latitude = lat,
                    longitude = lon
                ) { newId ->
                    Toast.makeText(context, "บันทึกครัวเรือนใหม่สำเร็จ (ID: $newId)", Toast.LENGTH_SHORT).show()
                    showAddHouseholdDialog = false
                    if (lat != null && lon != null) {
                        mapViewRef?.controller?.animateTo(GeoPoint(lat, lon))
                    }
                }
            }
        )
    }

    // Bottom Sheet / Dialog: Unpinned Houses List
    if (showUnpinnedHousesSheet) {
        UnpinnedHousesDialog(
            unmappedHouses = unmappedHouses,
            onDismiss = { showUnpinnedHousesSheet = false },
            onPinHouse = { houseSummaryItem ->
                coroutineScope.launch {
                    val hh = viewModel.getHouseholdById(houseSummaryItem.householdId)
                    if (hh != null) {
                        pinningTargetHousehold = hh
                        isPinningMode = true
                        val center = mapViewRef?.mapCenter?.let { GeoPoint(it.latitude, it.longitude) } ?: GeoPoint(initialLat, initialLon)
                        pendingPinLocation = center
                        showUnpinnedHousesSheet = false
                        Toast.makeText(context, "แตะบนแผนที่เพื่อระบุตำแหน่งบ้านเลขที่ ${hh.houseNo}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    // Dialog: Confirm Clear Pin Location
    houseToClearLocation?.let { house ->
        AlertDialog(
            onDismissRequest = { houseToClearLocation = null },
            title = { Text("ยืนยันการลบพิกัด") },
            text = { Text("คุณต้องการลบพิกัด GPS ของบ้านเลขที่ ${house.houseNo} ใช่หรือไม่? (ข้อมูลสมาชิกและประวัติครัวเรือนจะยังคงอยู่ครบถ้วน)") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removeHouseholdLocation(house.householdId) { success, msg ->
                            if (success) {
                                Toast.makeText(context, "ลบพิกัดเรียบร้อย", Toast.LENGTH_SHORT).show()
                                selectedHouse = null
                            } else {
                                Toast.makeText(context, msg ?: "เกิดข้อผิดพลาด", Toast.LENGTH_SHORT).show()
                            }
                        }
                        houseToClearLocation = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("ลบพิกัด")
                }
            },
            dismissButton = {
                TextButton(onClick = { houseToClearLocation = null }) {
                    Text("ยกเลิก")
                }
            }
        )
    }

    // Dialog: Map Layer & Visual Perspective Selection
    if (showLayerMenu) {
        MapLayerSelectionDialog(
            currentLayer = selectedMapLayer,
            currentMarkerStyle = selectedMarkerStyle,
            onSelectLayer = { selectedMapLayer = it },
            onSelectMarkerStyle = { selectedMarkerStyle = it },
            onDismiss = { showLayerMenu = false }
        )
    }
}

@Composable
fun MapLayerSelectionDialog(
    currentLayer: MapLayerType,
    currentMarkerStyle: MarkerStyle,
    onSelectLayer: (MapLayerType) -> Unit,
    onSelectMarkerStyle: (MarkerStyle) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(EmeraldPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Layers, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(24.dp))
                        }
                        Column {
                            Text("รูปแบบมุมมองแผนที่", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("เลือกแผนที่ 2D, 3D ภูมิประเทศ หรือดาวเทียม", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "ปิด")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Text("ชั้นข้อมูลแผนที่ (Map Layers)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = EmeraldPrimary)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MapLayerType.values().forEach { layer ->
                        val isSelected = layer == currentLayer
                        val layerIcon = when (layer) {
                            MapLayerType.STANDARD_2D -> Icons.Filled.Map
                            MapLayerType.SATELLITE -> Icons.Filled.SatelliteAlt
                            MapLayerType.TERRAIN_3D -> Icons.Filled.Terrain
                            MapLayerType.HYBRID_SATELLITE -> Icons.Filled.Public
                        }

                        Surface(
                            onClick = { onSelectLayer(layer) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) EmeraldPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(
                                width = if (isSelected) 1.8.dp else 1.dp,
                                color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        layerIcon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        layer.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        layer.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onSelectLayer(layer) },
                                    colors = RadioButtonDefaults.colors(selectedColor = EmeraldPrimary)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Text("รูปแบบหมุดบ้านเรือน (Building Marker)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = EmeraldPrimary)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MarkerStyle.values().forEach { style ->
                        val isSelected = style == currentMarkerStyle
                        Surface(
                            onClick = { onSelectMarkerStyle(style) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) EmeraldPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    if (style == MarkerStyle.PIN_3D_HOUSE) Icons.Filled.Home else Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    tint = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    style.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("ตกลง / ปิดหน้าต่าง", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun HouseholdPickerDialog(
    households: List<Household>,
    unmappedHouseholdIds: Set<Long>,
    onDismiss: () -> Unit,
    onSelect: (Household) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(if (unmappedHouseholdIds.isNotEmpty()) 0 else 1) }

    val filteredList = remember(households, search, selectedTab) {
        val byTab = when (selectedTab) {
            0 -> households.filter { unmappedHouseholdIds.contains(it.id) }
            else -> households
        }
        if (search.isBlank()) byTab
        else byTab.filter { it.houseNo.contains(search.trim(), ignoreCase = true) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .padding(8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("เลือกครัวเรือนที่จะปักหมุด", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "ปิด")
                    }
                }

                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("ยังไม่มีพิกัด (${unmappedHouseholdIds.size})") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("ทั้งหมด (${households.size})") }
                    )
                }

                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("ค้นหาบ้านเลขที่...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (filteredList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("ไม่พบรายการครัวเรือน", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredList) { hh ->
                            val hasLoc = hh.latitude != null && hh.longitude != null
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(hh) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (hasLoc) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else EmeraldPrimary.copy(alpha = 0.08f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("บ้านเลขที่ ${hh.houseNo}", fontWeight = FontWeight.Bold)
                                        val villageInfo = if (hh.villageNo.isNotBlank()) "หมู่ ${hh.villageNo} " else ""
                                        val subdistrictInfo = if (hh.subdistrict.isNotBlank()) "ต.${hh.subdistrict}" else ""
                                        if (villageInfo.isNotBlank() || subdistrictInfo.isNotBlank()) {
                                            Text("$villageInfo$subdistrictInfo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    if (hasLoc) {
                                        Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                            Text("มีพิกัดเดิม", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
                                        }
                                    } else {
                                        Surface(shape = RoundedCornerShape(6.dp), color = EmeraldPrimary.copy(alpha = 0.2f)) {
                                            Text("ยังไม่มีพิกัด", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UnpinnedHousesDialog(
    unmappedHouses: List<HouseSummary>,
    onDismiss: () -> Unit,
    onPinHouse: (HouseSummary) -> Unit
) {
    var search by remember { mutableStateOf("") }
    val filtered = remember(unmappedHouses, search) {
        if (search.isBlank()) unmappedHouses
        else unmappedHouses.filter { it.houseNo.contains(search.trim(), ignoreCase = true) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .padding(8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("บ้านที่ยังไม่ได้ระบุพิกัด", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("เลือกบ้านเพื่อนำหมุดไปวางบนแผนที่", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "ปิด")
                    }
                }

                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("ค้นหาบ้านเลขที่...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (filtered.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("ทุกครัวเรือนได้รับการปักหมุดครบถ้วนแล้ว 🎉", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtered) { house ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPinHouse(house) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("บ้านเลขที่ ${house.houseNo}", fontWeight = FontWeight.Bold)
                                        Text("สมาชิก ${house.totalMembers} คน (สูงอายุ ${house.elderly}, เด็ก ${house.children})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Button(
                                        onClick = { onPinHouse(house) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                                    ) {
                                        Icon(Icons.Filled.PinDrop, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("ปักหมุด", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = tint.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = tint)
        }
    }
}

@Composable
private fun StatMiniCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtext: String,
    color: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
            Text(subtext, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MiniBadge(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = tint.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = tint)
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

fun createHouseholdMarkerDrawable(
    context: Context,
    totalMembers: Int,
    hasElderly: Boolean,
    hasChildren: Boolean,
    dataStatus: DataStatus = DataStatus.VERIFIED,
    isSelected: Boolean,
    markerStyle: MarkerStyle = MarkerStyle.PIN_3D_HOUSE
): Drawable {
    val density = context.resources.displayMetrics.density
    val width = (44 * density).toInt()
    val height = (54 * density).toInt()
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Color based on population density and vulnerability
    val pinColor = when {
        isSelected -> android.graphics.Color.rgb(16, 185, 129) // Emerald
        hasElderly -> android.graphics.Color.rgb(124, 58, 237) // Purple for Elderly
        totalMembers >= 4 -> android.graphics.Color.rgb(234, 88, 12) // Orange for High Density
        hasChildren -> android.graphics.Color.rgb(2, 132, 199) // Sky Blue for Children
        else -> android.graphics.Color.rgb(5, 150, 105) // Standard Emerald
    }

    // Shadow
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(55, 0, 0, 0)
    }
    val shadowRadius = 16 * density
    canvas.drawCircle(width / 2f, 19 * density, shadowRadius + (2 * density), shadowPaint)

    if (markerStyle == MarkerStyle.PIN_3D_HOUSE) {
        // 3D House Roof + Body Pin Style
        val path = Path()
        val circleCenterY = 20 * density
        val circleRadius = 16 * density

        // Roof Top Triangle (3D House effect)
        path.moveTo(width / 2f, 2 * density) // Roof peak
        path.lineTo(width - (4 * density), 16 * density) // Right roof eave
        path.lineTo(width - (6 * density), circleCenterY + (10 * density)) // House base right
        path.lineTo(width / 2f, height - (2 * density)) // Bottom pin needle
        path.lineTo(6 * density, circleCenterY + (10 * density)) // House base left
        path.lineTo(4 * density, 16 * density) // Left roof eave
        path.close()

        paint.color = pinColor
        paint.style = Paint.Style.FILL
        canvas.drawPath(path, paint)

        // 3D Isometric Roof Shading (Left side brighter, Right side darker)
        val roofShadeLeft = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(45, 255, 255, 255)
            style = Paint.Style.FILL
        }
        val leftRoofPath = Path().apply {
            moveTo(width / 2f, 2 * density)
            lineTo(4 * density, 16 * density)
            lineTo(width / 2f, 16 * density)
            close()
        }
        canvas.drawPath(leftRoofPath, roofShadeLeft)

        val roofShadeRight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(40, 0, 0, 0)
            style = Paint.Style.FILL
        }
        val rightRoofPath = Path().apply {
            moveTo(width / 2f, 2 * density)
            lineTo(width - (4 * density), 16 * density)
            lineTo(width / 2f, 16 * density)
            close()
        }
        canvas.drawPath(rightRoofPath, roofShadeRight)

        // Outer crisp white stroke
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 2.5f * density
        }
        canvas.drawPath(path, strokePaint)

        // Text (Number of population members)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 12 * density
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val text = if (totalMembers > 99) "99+" else "$totalMembers"
        val textBounds = Rect()
        textPaint.getTextBounds(text, 0, text.length, textBounds)
        val textY = circleCenterY + (4 * density) + (textBounds.height() / 2f)
        canvas.drawText(text, width / 2f, textY, textPaint)

    } else {
        // Standard Classic 2D Round Pin
        val path = Path()
        val circleCenterY = 18 * density
        val circleRadius = 15 * density

        path.addCircle(width / 2f, circleCenterY, circleRadius, Path.Direction.CW)
        val trianglePath = Path().apply {
            moveTo((width / 2f) - (8 * density), circleCenterY + (9 * density))
            lineTo(width / 2f, height - (2 * density))
            lineTo((width / 2f) + (8 * density), circleCenterY + (9 * density))
            close()
        }
        path.op(trianglePath, Path.Op.UNION)

        paint.color = pinColor
        paint.style = Paint.Style.FILL
        canvas.drawPath(path, paint)

        // White border
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 2.5f * density
        }
        canvas.drawPath(path, strokePaint)

        // Text (Number of population members)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 12 * density
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val text = if (totalMembers > 99) "99+" else "$totalMembers"
        val textBounds = Rect()
        textPaint.getTextBounds(text, 0, text.length, textBounds)
        val textY = circleCenterY + (textBounds.height() / 2f)
        canvas.drawText(text, width / 2f, textY, textPaint)
    }

    // Elderly indicator badge dot
    if (hasElderly) {
        val badgeCenterY = if (markerStyle == MarkerStyle.PIN_3D_HOUSE) 10 * density else 8 * density
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(251, 191, 36) // Amber/Gold
            style = Paint.Style.FILL
        }
        canvas.drawCircle((width / 2f) + (11 * density), badgeCenterY, 4.5f * density, badgePaint)
        val badgeBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * density
        }
        canvas.drawCircle((width / 2f) + (11 * density), badgeCenterY, 4.5f * density, badgeBorder)
    }

    // Data Status indicator badge (Red dot/exclamation for NEEDS_REVIEW)
    if (dataStatus == DataStatus.NEEDS_REVIEW) {
        val badgeCenterY = if (markerStyle == MarkerStyle.PIN_3D_HOUSE) 10 * density else 8 * density
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(239, 68, 68) // Red-500
            style = Paint.Style.FILL
        }
        canvas.drawCircle((width / 2f) - (11 * density), badgeCenterY, 4.5f * density, badgePaint)
        val badgeBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * density
        }
        canvas.drawCircle((width / 2f) - (11 * density), badgeCenterY, 4.5f * density, badgeBorder)

        // Draw '!' in the middle of red dot
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 7 * density
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("!", (width / 2f) - (11 * density), badgeCenterY + (2.5f * density), textPaint)
    }

    return BitmapDrawable(context.resources, bitmap)
}

fun createPendingPinMarkerDrawable(context: Context): Drawable {
    val density = context.resources.displayMetrics.density
    val width = (46 * density).toInt()
    val height = (56 * density).toInt()
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val pinColor = android.graphics.Color.rgb(220, 38, 38) // Bright red for pending pin

    val path = Path()
    val circleCenterY = 19 * density
    val circleRadius = 16 * density
    path.addCircle(width / 2f, circleCenterY, circleRadius, Path.Direction.CW)
    val trianglePath = Path().apply {
        moveTo((width / 2f) - (9 * density), circleCenterY + (10 * density))
        lineTo(width / 2f, height - (2 * density))
        lineTo((width / 2f) + (9 * density), circleCenterY + (10 * density))
        close()
    }
    path.op(trianglePath, Path.Op.UNION)

    paint.color = pinColor
    paint.style = Paint.Style.FILL
    canvas.drawPath(path, paint)

    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f * density
    }
    canvas.drawPath(path, strokePaint)

    // Center white dot
    val whiteDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(width / 2f, circleCenterY, 5.5f * density, whiteDotPaint)

    return BitmapDrawable(context.resources, bitmap)
}

fun createEventMarkerDrawable(context: Context, type: PopulationEventType, isSelected: Boolean): Drawable {
    val density = context.resources.displayMetrics.density
    val width = (42 * density).toInt()
    val height = (52 * density).toInt()
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val markerColor = when (type) {
        PopulationEventType.BIRTH -> android.graphics.Color.rgb(16, 185, 129) // Emerald
        PopulationEventType.DEATH -> android.graphics.Color.rgb(107, 114, 128) // Gray
        PopulationEventType.MOVE_IN -> android.graphics.Color.rgb(59, 130, 246) // Blue
        PopulationEventType.MOVE_OUT -> android.graphics.Color.rgb(245, 158, 11) // Amber
        PopulationEventType.HEALTH_CHECK -> android.graphics.Color.rgb(139, 92, 246) // Violet
    }

    val path = Path()
    val circleCenterY = 17 * density
    val circleRadius = 14 * density
    path.addCircle(width / 2f, circleCenterY, circleRadius, Path.Direction.CW)
    val trianglePath = Path().apply {
        moveTo((width / 2f) - (8 * density), circleCenterY + (9 * density))
        lineTo(width / 2f, height - (2 * density))
        lineTo((width / 2f) + (8 * density), circleCenterY + (9 * density))
        close()
    }
    path.op(trianglePath, Path.Op.UNION)

    paint.color = markerColor
    paint.style = Paint.Style.FILL
    canvas.drawPath(path, paint)

    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = if (isSelected) android.graphics.Color.YELLOW else android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = if (isSelected) 3.5f * density else 2.5f * density
    }
    canvas.drawPath(path, strokePaint)

    // Draw Icon letter in center
    val tPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.WHITE
        textSize = 14 * density
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    val letter = when (type) {
        PopulationEventType.BIRTH -> "B"
        PopulationEventType.DEATH -> "D"
        PopulationEventType.MOVE_IN -> "I"
        PopulationEventType.MOVE_OUT -> "O"
        PopulationEventType.HEALTH_CHECK -> "H"
    }
    val textBounds = Rect()
    tPaint.getTextBounds(letter, 0, letter.length, textBounds)
    canvas.drawText(letter, width / 2f, circleCenterY + (textBounds.height() / 2f), tPaint)

    return BitmapDrawable(context.resources, bitmap)
}

data class HouseCluster(
    val centerLat: Double,
    val centerLon: Double,
    val houses: List<HouseSummary>
)

fun createClusterMarkerDrawable(
    context: Context,
    count: Int,
    isSelected: Boolean
): Drawable {
    val density = context.resources.displayMetrics.density
    val size = (48 * density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Shadow
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(60, 0, 0, 0)
    }
    canvas.drawCircle(size / 2f, (size / 2f) + (2 * density), (size / 2f) - (4 * density), shadowPaint)

    // Background circle
    paint.color = if (isSelected) android.graphics.Color.rgb(16, 185, 129) else android.graphics.Color.rgb(5, 150, 105)
    paint.style = Paint.Style.FILL
    canvas.drawCircle(size / 2f, size / 2f, (size / 2f) - (4 * density), paint)

    // Inner white border
    paint.color = android.graphics.Color.WHITE
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 2.5f * density
    canvas.drawCircle(size / 2f, size / 2f, (size / 2f) - (5 * density), paint)

    // Text (Count)
    paint.style = Paint.Style.FILL
    paint.color = android.graphics.Color.WHITE
    paint.textSize = 14 * density
    paint.typeface = Typeface.DEFAULT_BOLD
    paint.textAlign = Paint.Align.CENTER

    val textY = (size / 2f) - ((paint.descent() + paint.ascent()) / 2f)
    canvas.drawText(count.toString(), size / 2f, textY, paint)

    return BitmapDrawable(context.resources, bitmap)
}

@Composable
fun AddHouseholdDialog(
    initialLat: Double?,
    initialLon: Double?,
    onDismiss: () -> Unit,
    onSave: (String, String, Double?, Double?) -> Unit
) {
    var houseNo by remember { mutableStateOf("") }
    var headName by remember { mutableStateOf("") }
    var latText by remember { mutableStateOf(initialLat?.toString() ?: "") }
    var lonText by remember { mutableStateOf(initialLon?.toString() ?: "") }
    var showScanner by remember { mutableStateOf(false) }
    var scannedBadgeText by remember { mutableStateOf<String?>(null) }

    if (showScanner) {
        IdCardScannerDialog(
            onDismiss = { showScanner = false },
            onScanned = { res ->
                if (!res.houseNo.isNullOrBlank()) houseNo = res.houseNo
                if (!res.fullName.isNullOrBlank()) headName = res.fullName
                scannedBadgeText = res.displaySummary
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.AddHome,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            "เพิ่มครัวเรือนใหม่",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "ปิด")
                    }
                }

                // Barcode Auto-Fill Button
                OutlinedButton(
                    onClick = { showScanner = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldPrimary),
                    border = BorderStroke(1.5.dp, EmeraldPrimary)
                ) {
                    Icon(Icons.Filled.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("สแกนบัตรประชาชน (Auto-fill)", fontWeight = FontWeight.Bold)
                }

                if (scannedBadgeText != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = EmeraldPrimary.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                            Text(
                                "ดึงข้อมูลจากบัตรประชาชนเรียบร้อยแล้ว",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = houseNo,
                    onValueChange = { houseNo = it },
                    label = { Text("บ้านเลขที่ (เช่น 123/4)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = headName,
                    onValueChange = { headName = it },
                    label = { Text("ชื่อหัวหน้าครัวเรือน") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = latText,
                        onValueChange = { latText = it },
                        label = { Text("ละติจูด (Latitude)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = lonText,
                        onValueChange = { lonText = it },
                        label = { Text("ลองจิจูด (Longitude)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Button(
                    onClick = {
                        val lat = latText.toDoubleOrNull()
                        val lon = lonText.toDoubleOrNull()
                        onSave(houseNo, headName, lat, lon)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("บันทึกลงฐานข้อมูล (Room)", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

fun createUserLocationMarkerDrawable(context: Context): Drawable {
    val density = context.resources.displayMetrics.density
    val size = (40 * density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Outer glow / pulse circle
    paint.color = android.graphics.Color.argb(70, 37, 99, 235)
    paint.style = Paint.Style.FILL
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

    // Inner solid blue circle
    paint.color = android.graphics.Color.rgb(37, 99, 235)
    canvas.drawCircle(size / 2f, size / 2f, (size / 2f) - (6 * density), paint)

    // White center dot
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(size / 2f, size / 2f, 4 * density, paint)

    return BitmapDrawable(context.resources, bitmap)
}

