package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppRepository
import com.example.data.TimingsData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class SelectedDhikrTarget(val name: String, val arabic: String, val target: Int)

class PrayerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(application)
    private val compassTracker = CompassTracker(application)

    private val _selectedDhikrTarget = MutableStateFlow<SelectedDhikrTarget?>(null)
    val selectedDhikrTarget: StateFlow<SelectedDhikrTarget?> = _selectedDhikrTarget

    fun setDhikrTarget(name: String, arabic: String, target: Int) {
        _selectedDhikrTarget.value = SelectedDhikrTarget(name, arabic, target)
    }

    fun clearDhikrTarget() {
        _selectedDhikrTarget.value = null
    }

    private val _targetQuranVerse = MutableStateFlow<Pair<Int, Int>?>(null) // surahId, verseNumber
    val targetQuranVerse: StateFlow<Pair<Int, Int>?> = _targetQuranVerse

    fun setTargetQuranVerse(surahId: Int, verseNumber: Int) {
        _targetQuranVerse.value = Pair(surahId, verseNumber)
    }

    fun clearTargetQuranVerse() {
        _targetQuranVerse.value = null
    }

    private val _targetMiracleId = MutableStateFlow<String?>(null)
    val targetMiracleId: StateFlow<String?> = _targetMiracleId

    fun setTargetMiracleId(miracleId: String) {
        _targetMiracleId.value = miracleId
    }

    fun clearTargetMiracleId() {
        _targetMiracleId.value = null
    }

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _weather = MutableStateFlow<com.example.data.WeatherResponse?>(null)
    val weather: StateFlow<com.example.data.WeatherResponse?> = _weather

    private val _timings = MutableStateFlow<TimingsData?>(null)
    val timings: StateFlow<TimingsData?> = _timings

    private val _calendar = MutableStateFlow<List<TimingsData>?>(null)
    val calendar: StateFlow<List<TimingsData>?> = _calendar

    private val _currentRoute = MutableStateFlow<String?>(null)
    val currentRoute: StateFlow<String?> = _currentRoute

    private val _isReadingSurah = MutableStateFlow(false)
    val isReadingSurah: StateFlow<Boolean> = _isReadingSurah

    fun setIsReadingSurah(isReading: Boolean) {
        _isReadingSurah.value = isReading
    }
    private val _forceDarkTheme = MutableStateFlow(false)
    val forceDarkTheme: StateFlow<Boolean> = _forceDarkTheme

    private val _selectedLanguage = MutableStateFlow("tr")
    val selectedLanguage: StateFlow<String> = _selectedLanguage

    private val _selectedEdition = MutableStateFlow("tr.diyanet")
    val selectedEdition: StateFlow<String> = _selectedEdition

    private val _selectedEditions = MutableStateFlow<Set<String>>(setOf("tr.diyanet", "tr.yazir", "tr.vakfi"))
    val selectedEditions: StateFlow<Set<String>> = _selectedEditions

    fun setLanguage(langCode: String) {
        _selectedLanguage.value = langCode
        val prefs = getApplication<Application>().getSharedPreferences("ai_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("selected_language", langCode).apply()
    }

    fun setSelectedEdition(editionIdentifier: String) {
        _selectedEdition.value = editionIdentifier
        val prefs = getApplication<Application>().getSharedPreferences("ai_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("selected_edition", editionIdentifier).apply()
    }

    fun toggleEdition(editionIdentifier: String) {
        val current = _selectedEditions.value.toMutableSet()
        if (current.contains(editionIdentifier)) {
            if (current.size > 1) {
                current.remove(editionIdentifier)
            }
        } else {
            current.add(editionIdentifier)
        }
        _selectedEditions.value = current
        val prefs = getApplication<Application>().getSharedPreferences("ai_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putStringSet("selected_editions_set", current).apply()
    }

    fun setForceDarkTheme(value: Boolean) {
        _forceDarkTheme.value = value
        val prefs = getApplication<Application>().getSharedPreferences("ai_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean("force_dark_theme", value).apply()
    }

    // Saved Quran Verses
    private val _savedVerses = MutableStateFlow<List<SavedVerse>>(emptyList())
    val savedVerses: StateFlow<List<SavedVerse>> = _savedVerses

    init {
        val prefs = getApplication<Application>().getSharedPreferences("ai_prefs", android.content.Context.MODE_PRIVATE)
        _selectedLanguage.value = prefs.getString("selected_language", "tr") ?: "tr"
        _selectedEdition.value = prefs.getString("selected_edition", "tr.diyanet") ?: "tr.diyanet"
        val savedEditions = prefs.getStringSet("selected_editions_set", null)
        if (savedEditions != null && savedEditions.isNotEmpty()) {
            _selectedEditions.value = savedEditions
        } else {
            _selectedEditions.value = setOf("tr.diyanet", "tr.yazir", "tr.vakfi")
        }
        _forceDarkTheme.value = prefs.getBoolean("force_dark_theme", false)
        loadSavedVerses()
    }

    private fun loadSavedVerses() {
        val prefs = getApplication<Application>().getSharedPreferences("quran_prefs", android.content.Context.MODE_PRIVATE)
        val jsonString = prefs.getString("saved_verses_json", "[]") ?: "[]"
        val list = mutableListOf<SavedVerse>()
        try {
            val array = org.json.JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    SavedVerse(
                        surahId = obj.optInt("surahId"),
                        surahName = obj.optString("surahName"),
                        verseNumber = obj.optInt("verseNumber"),
                        arabic = obj.optString("arabic"),
                        pronunciation = obj.optString("pronunciation"),
                        diyanetMeal = obj.optString("diyanetMeal"),
                        elmaliliMeal = obj.optString("elmaliliMeal"),
                        vakfiMeal = obj.optString("vakfiMeal"),
                        savedAtTimestamp = obj.optLong("savedAtTimestamp"),
                        selectedMeal = obj.optString("selectedMeal"),
                        mealSource = obj.optString("mealSource"),
                        footnotes = obj.optString("footnotes")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _savedVerses.value = list
    }

    fun toggleSaveVerse(verse: SavedVerse) {
        val current = _savedVerses.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.surahId == verse.surahId && it.verseNumber == verse.verseNumber }
        if (existingIndex >= 0) {
            current.removeAt(existingIndex)
        } else {
            current.add(0, verse)
        }
        _savedVerses.value = current
        saveVersesToPrefs(current)
    }

    fun isVerseSaved(surahId: Int, verseNumber: Int): Boolean {
        return _savedVerses.value.any { it.surahId == surahId && it.verseNumber == verseNumber }
    }

    fun removeSavedVerse(surahId: Int, verseNumber: Int) {
        val current = _savedVerses.value.filterNot { it.surahId == surahId && it.verseNumber == verseNumber }
        _savedVerses.value = current
        saveVersesToPrefs(current)
    }

    private fun saveVersesToPrefs(list: List<SavedVerse>) {
        val array = org.json.JSONArray()
        for (v in list) {
            val obj = org.json.JSONObject().apply {
                put("surahId", v.surahId)
                put("surahName", v.surahName)
                put("verseNumber", v.verseNumber)
                put("arabic", v.arabic)
                put("pronunciation", v.pronunciation)
                put("diyanetMeal", v.diyanetMeal)
                put("elmaliliMeal", v.elmaliliMeal)
                put("vakfiMeal", v.vakfiMeal)
                put("savedAtTimestamp", v.savedAtTimestamp)
                put("selectedMeal", v.selectedMeal)
                put("mealSource", v.mealSource)
                put("footnotes", v.footnotes)
            }
            array.put(obj)
        }
        val prefs = getApplication<Application>().getSharedPreferences("quran_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("saved_verses_json", array.toString()).apply()
    }


    private val _volumeDownEvent = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val volumeDownEvent: kotlinx.coroutines.flow.SharedFlow<Unit> = _volumeDownEvent

    val volumeKeyDhikrEnabled = kotlinx.coroutines.flow.MutableStateFlow(
        getApplication<Application>().getSharedPreferences("dhikr_prefs", android.content.Context.MODE_PRIVATE)
            .getBoolean("volume_key_dhikr_enabled", true)
    )

    fun toggleVolumeKeyDhikr() {
        val newValue = !volumeKeyDhikrEnabled.value
        volumeKeyDhikrEnabled.value = newValue
        getApplication<Application>().getSharedPreferences("dhikr_prefs", android.content.Context.MODE_PRIVATE)
            .edit().putBoolean("volume_key_dhikr_enabled", newValue).apply()
    }

    fun setCurrentRoute(route: String?) {
        _currentRoute.value = route
    }

    fun triggerVolumeDown() {
        _volumeDownEvent.tryEmit(Unit)
    }

    val cityFlow = repository.cityFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "Konum Seçiliyor..."
    )

    val latitudeFlow = repository.latitudeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val longitudeFlow = repository.longitudeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val autoLocationEnabled = repository.autoLocationFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    private var liveLocationJob: kotlinx.coroutines.Job? = null

    fun setAutoLocationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveAutoLocationEnabled(enabled)
            if (enabled) {
                startLiveLocationTracking()
                checkLocationChanged(forceFetch = true)
            } else {
                liveLocationJob?.cancel()
                liveLocationJob = null
            }
        }
    }

    fun onAppResumed() {
        if (autoLocationEnabled.value) {
            startLiveLocationTracking()
            checkLocationChanged(forceFetch = false)
        }
    }

    private fun calculateDistanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(lat1, lng1, lat2, lng2, results)
        return results[0]
    }

    fun startLiveLocationTracking() {
        if (liveLocationJob?.isActive == true) return
        liveLocationJob = viewModelScope.launch {
            repository.observeLocationChanges().collect { loc ->
                if (!autoLocationEnabled.value) return@collect
                val savedLat = repository.latitudeFlow.firstOrNull()
                val savedLng = repository.longitudeFlow.firstOrNull()
                val savedCity = repository.cityFlow.firstOrNull()

                if (savedLat == null || savedLng == null) {
                    applyAutoDetectedLocation(loc, notifyIfCityChanged = false, previousCity = savedCity)
                } else {
                    val dist = calculateDistanceMeters(savedLat, savedLng, loc.latitude, loc.longitude)
                    val cityDiffers = loc.city.isNotBlank() &&
                        loc.city != "Mevcut Konum" &&
                        savedCity != null &&
                        !loc.city.equals(savedCity, ignoreCase = true)
                    if (dist > 250f || cityDiffers) {
                        applyAutoDetectedLocation(loc, notifyIfCityChanged = true, previousCity = savedCity)
                    }
                }
            }
        }
    }

    private suspend fun applyAutoDetectedLocation(
        loc: com.example.data.LocationResult,
        notifyIfCityChanged: Boolean,
        previousCity: String?
    ) {
        repository.saveLocation(loc.latitude, loc.longitude, loc.city)
        _qiblaAngle.value = CompassTracker.calculateQiblaAngle(loc.latitude, loc.longitude)
        fetchTimings(loc.latitude, loc.longitude)
        if (notifyIfCityChanged &&
            previousCity != null &&
            previousCity != "Konum Seçiliyor..." &&
            !previousCity.equals(loc.city, ignoreCase = true)
        ) {
            _newLocationDetected.value = loc
        }
    }

    val favoriteHadithsFlow = repository.favoriteHadithsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptySet()
    )

    val memorizedHadithsFlow = repository.memorizedHadithsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptySet()
    )

    fun toggleFavoriteHadith(id: String) {
        viewModelScope.launch {
            repository.toggleFavoriteHadith(id)
        }
    }

    fun toggleMemorizedHadith(id: String) {
        viewModelScope.launch {
            repository.toggleMemorizedHadith(id)
        }
    }

    private val _newLocationDetected = MutableStateFlow<com.example.data.LocationResult?>(null)
    val newLocationDetected: StateFlow<com.example.data.LocationResult?> = _newLocationDetected

    fun clearNewLocationPrompt() {
        _newLocationDetected.value = null
    }

    val currentTheme = repository.themeFlow.map { themeStr ->
        com.example.ui.theme.AppTheme.fromStored(themeStr)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = com.example.ui.theme.AppTheme.DEFAULT
    )

    fun selectTheme(theme: com.example.ui.theme.AppTheme) {
        viewModelScope.launch {
            repository.saveTheme(theme.name)
            val prefs = getApplication<Application>().getSharedPreferences("widget_prayer_prefs", android.content.Context.MODE_PRIVATE)
            prefs.edit().putString("theme", theme.name).apply()
            PrayerWidgetProvider.triggerUpdate(getApplication())
            BackgroundServices.startCountdown(getApplication())
        }
    }

    fun resetToOriginalSingleTheme() {
        selectTheme(com.example.ui.theme.AppTheme.DEFAULT)
    }

    val selectedBackground = repository.backgroundFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    fun selectBackground(bgId: Int) {
        viewModelScope.launch {
            repository.saveBackground(bgId)
        }
    }

    val patternIntensity = repository.patternIntensityFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 1.0f
    )

    fun setPatternIntensity(intensity: Float) {
        viewModelScope.launch {
            repository.savePatternIntensity(intensity)
        }
    }

    val themeColorVariant = repository.themeColorVariantFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    fun setThemeColorVariant(variant: Int) {
        viewModelScope.launch {
            repository.saveThemeColorVariant(variant)
        }
    }

    val azimuthFlow = compassTracker.azimuthFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        0f
    )

    fun hasCompassSensors(): Boolean = compassTracker.hasCompassSensors()

    private val _qiblaAngle = MutableStateFlow(0f)
    val qiblaAngle: StateFlow<Float> = _qiblaAngle

    private val _currentAndNextPrayer = MutableStateFlow<Pair<String, String>?>(null)
    val currentAndNextPrayer: StateFlow<Pair<String, String>?> = _currentAndNextPrayer
    
    private val _remainingTime = MutableStateFlow("00:00:00")
    val remainingTime: StateFlow<String> = _remainingTime

    private val _prayerProgress = MutableStateFlow(0f)
    val prayerProgress: StateFlow<Float> = _prayerProgress

    init {
        viewModelScope.launch {
            // Start timer for countdown
            launch {
                while (true) {
                    updateNextPrayerAndCountdown()
                    delay(1000)
                }
            }

            // Periodic 5-minute location check (auto-updates if city/location changes)
            launch {
                while (true) {
                    delay(5 * 60 * 1000L)
                    if (autoLocationEnabled.value) {
                        checkLocationChanged()
                    }
                }
            }

            // Start continuous live location listener if auto-location is enabled
            launch {
                autoLocationEnabled.collect { enabled ->
                    if (enabled) {
                        startLiveLocationTracking()
                    } else {
                        liveLocationJob?.cancel()
                        liveLocationJob = null
                    }
                }
            }

            // Sync city with widget SharedPreferences
            launch {
                cityFlow.collect { cityName ->
                    if (cityName != "Konum Seçiliyor..." && cityName != "Bilinmeyen Konum") {
                        val prefs = getApplication<Application>().getSharedPreferences("widget_prayer_prefs", android.content.Context.MODE_PRIVATE)
                        prefs.edit().putString("city", cityName).apply()
                        PrayerWidgetProvider.triggerUpdate(getApplication())
                    }
                }
            }

            // Sync theme with widget SharedPreferences
            launch {
                currentTheme.collect { theme ->
                    val prefs = getApplication<Application>().getSharedPreferences("widget_prayer_prefs", android.content.Context.MODE_PRIVATE)
                    if (prefs.getString("theme", null) != theme.name) {
                        prefs.edit().putString("theme", theme.name).apply()
                        PrayerWidgetProvider.triggerUpdate(getApplication())
                    }
                }
            }

            // Load initial data from DataStore (fallback to Istanbul if not saved)
            val lat = repository.latitudeFlow.firstOrNull()
            val lng = repository.longitudeFlow.firstOrNull()
            val city = repository.cityFlow.firstOrNull()
            
            val targetLat = lat ?: 41.0082
            val targetLng = lng ?: 28.9784
            val targetCity = city ?: "İstanbul"

            if (lat == null || lng == null) {
                repository.saveLocation(targetLat, targetLng, targetCity)
            }

            _qiblaAngle.value = CompassTracker.calculateQiblaAngle(targetLat, targetLng)
            fetchTimings(targetLat, targetLng)
        }
    }

    fun updateLocation() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.saveAutoLocationEnabled(true)
                startLiveLocationTracking()
                val loc = repository.getCurrentLocation()
                if (loc != null) {
                    repository.saveLocation(loc.latitude, loc.longitude, loc.city)
                    _qiblaAngle.value = CompassTracker.calculateQiblaAngle(loc.latitude, loc.longitude)
                    fetchTimings(loc.latitude, loc.longitude)
                    android.widget.Toast.makeText(
                        getApplication(),
                        "Otomatik konum güncellendi: ${loc.city}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                } else {
                    val savedLat = repository.latitudeFlow.firstOrNull()
                    val savedLng = repository.longitudeFlow.firstOrNull()
                    val savedCity = repository.cityFlow.firstOrNull()
                    if (savedLat != null && savedLng != null) {
                        _qiblaAngle.value = CompassTracker.calculateQiblaAngle(savedLat, savedLng)
                        fetchTimings(savedLat, savedLng)
                        android.widget.Toast.makeText(
                            getApplication(),
                            "GPS konumu alınamadı. Kaydedilen konum (${savedCity ?: "Mevcut Konum"}) kullanılıyor.",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    } else {
                        val defaultLat = 41.0082
                        val defaultLng = 28.9784
                        val defaultCity = "İstanbul"
                        repository.saveLocation(defaultLat, defaultLng, defaultCity)
                        _qiblaAngle.value = CompassTracker.calculateQiblaAngle(defaultLat, defaultLng)
                        fetchTimings(defaultLat, defaultLng)
                        android.widget.Toast.makeText(
                            getApplication(),
                            "GPS konumu alınamadı. Varsayılan konum (İstanbul) seçildi.",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun checkLocationChanged(forceFetch: Boolean = false) {
        viewModelScope.launch {
            try {
                val isAuto = repository.autoLocationFlow.firstOrNull() ?: true
                if (!isAuto && !forceFetch) return@launch

                val savedLat = repository.latitudeFlow.firstOrNull()
                val savedLng = repository.longitudeFlow.firstOrNull()
                val savedCity = repository.cityFlow.firstOrNull()

                val loc = repository.getCurrentLocation()
                if (loc != null) {
                    if (savedLat == null || savedLng == null || forceFetch) {
                        applyAutoDetectedLocation(loc, notifyIfCityChanged = savedCity != null, previousCity = savedCity)
                    } else {
                        val dist = calculateDistanceMeters(savedLat, savedLng, loc.latitude, loc.longitude)
                        val cityChanged = loc.city.isNotBlank() &&
                            loc.city != "Mevcut Konum" &&
                            savedCity != null &&
                            !loc.city.equals(savedCity, ignoreCase = true)
                        // If distance is more than 250 meters or city/district has changed, auto update immediately
                        if (dist > 250f || cityChanged) {
                            applyAutoDetectedLocation(loc, notifyIfCityChanged = true, previousCity = savedCity)
                        }
                    }
                } else if (savedLat == null || savedLng == null) {
                    val defaultLat = 41.0082
                    val defaultLng = 28.9784
                    val defaultCity = "İstanbul"
                    setLocationDirectly(com.example.data.LocationResult(defaultLat, defaultLng, defaultCity))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setLocationDirectly(loc: com.example.data.LocationResult) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.saveLocation(loc.latitude, loc.longitude, loc.city)
                _qiblaAngle.value = CompassTracker.calculateQiblaAngle(loc.latitude, loc.longitude)
                fetchTimings(loc.latitude, loc.longitude)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun searchLocations(query: String): List<com.example.data.LocationResult> {
        return repository.getLocationsFromQuery(query)
    }

    fun searchAndSetLocation(cityName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val loc = repository.getLocationFromCityName(cityName)
                if (loc != null) {
                    repository.saveLocation(loc.latitude, loc.longitude, loc.city)
                    _qiblaAngle.value = CompassTracker.calculateQiblaAngle(loc.latitude, loc.longitude)
                    fetchTimings(loc.latitude, loc.longitude)
                } else {
                    android.widget.Toast.makeText(
                        getApplication(),
                        "\"$cityName\" için konum bulunamadı.",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun fetchTimings(lat: Double, lng: Double) {
        try {
            _isLoading.value = true
            
            // Fetch weather asynchronously
            viewModelScope.launch {
                val weatherData = repository.getCurrentWeather(lat, lng)
                _weather.value = weatherData
            }

            val todayTimings = repository.getTodayTimings(lat, lng)
            _timings.value = todayTimings
            
            // Save to widget preferences
            val t = todayTimings.timings
            val cleanImsak = t.Fajr.take(5)
            val cleanGunes = t.Sunrise.take(5)
            val cleanOgle = t.Dhuhr.take(5)
            val cleanIkindi = t.Asr.take(5)
            val cleanAksam = t.Maghrib.take(5)
            val cleanYatsi = t.Isha.take(5)
            
            val prefs = getApplication<Application>().getSharedPreferences("widget_prayer_prefs", android.content.Context.MODE_PRIVATE)
            prefs.edit().apply {
                putString("imsak", cleanImsak)
                putString("gunes", cleanGunes)
                putString("ogle", cleanOgle)
                putString("ikindi", cleanIkindi)
                putString("aksam", cleanAksam)
                putString("yatsi", cleanYatsi)
                apply()
            }
            PrayerWidgetProvider.triggerUpdate(getApplication())
            
            // Schedule notifications
            NotificationHelper.scheduleNotificationsForToday(
                getApplication(),
                todayTimings.timings
            )

            val cal = Calendar.getInstance()
            val month = cal.get(Calendar.MONTH) + 1
            val year = cal.get(Calendar.YEAR)
            _calendar.value = repository.getMonthlyCalendar(lat, lng, month, year)
            
            updateNextPrayerAndCountdown()
        } catch (e: Exception) {
            e.printStackTrace()
            // Try to load from monthly calendar cache first for extended offline support
            val cachedCalendar = repository.getCachedMonthlyCalendar()
            var foundToday = false
            if (cachedCalendar != null) {
                _calendar.value = cachedCalendar
                
                val format = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.ENGLISH)
                val todayStr = format.format(java.util.Date())
                
                val todayData = cachedCalendar.find { it.date.readable == todayStr }
                if (todayData != null) {
                    _timings.value = todayData
                    foundToday = true
                }
            }
            
            if (!foundToday) {
                val cachedToday = repository.getCachedTodayTimings()
                if (cachedToday != null) {
                    _timings.value = cachedToday
                }
            }
            
            updateNextPrayerAndCountdown()
        } finally {
            _isLoading.value = false
        }
    }

    private var lastNextPrayerName = ""
    private var lastCityName = ""

    private fun updateNextPrayerAndCountdown() {
        val t = _timings.value?.timings ?: return
        val city = cityFlow.value ?: "Bilinmiyor"
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        val now = Calendar.getInstance()
        val currentTimeStr = format.format(now.time)

        val countdownPrayers = listOf(
            "İmsak" to cleanTime(t.Fajr),
            "Güneş" to cleanTime(t.Sunrise),
            "Öğle" to cleanTime(t.Dhuhr),
            "İkindi" to cleanTime(t.Asr),
            "Akşam" to cleanTime(t.Maghrib),
            "Yatsı" to cleanTime(t.Isha)
        )

        val displayPrayers = listOf(
            "İmsak" to cleanTime(t.Fajr),
            "Sabah" to calculateSabahTime(cleanTime(t.Sunrise)),
            "Güneş" to cleanTime(t.Sunrise),
            "Öğle" to cleanTime(t.Dhuhr),
            "İkindi" to cleanTime(t.Asr),
            "Akşam" to cleanTime(t.Maghrib),
            "Yatsı" to cleanTime(t.Isha)
        )

        var nextName = countdownPrayers.first().first
        var nextTime = countdownPrayers.first().second
        var currentName = countdownPrayers.last().first
        var currentTimeStart = countdownPrayers.last().second
        var currentIndex = 5 // Isha default if before Imsak

        for (i in countdownPrayers.indices) {
            if (currentTimeStr < countdownPrayers[i].second) {
                nextName = countdownPrayers[i].first
                nextTime = countdownPrayers[i].second
                if (i > 0) {
                    currentName = countdownPrayers[i - 1].first
                    currentTimeStart = countdownPrayers[i - 1].second
                    currentIndex = i - 1
                }
                break
            }
        }
        
        // If it's past Isha, next is Imsak tomorrow
        if (currentTimeStr >= countdownPrayers.last().second) {
            nextName = countdownPrayers.first().first
            nextTime = countdownPrayers.first().second
            currentName = countdownPrayers.last().first
            currentTimeStart = countdownPrayers.last().second
            currentIndex = 5
        }

        _currentAndNextPrayer.value = Pair(currentName, nextName)

        try {
            val nextDate = format.parse(nextTime)
            val startDate = format.parse(currentTimeStart)
            val nowStr = format.format(now.time)
            val nowDate = format.parse(nowStr)
            
            if (nextDate != null && nowDate != null && startDate != null) {
                var diff = nextDate.time - nowDate.time
                // If it's past Isha, diff will be negative, add 24 hours
                if (diff < 0) diff += 24 * 60 * 60 * 1000
                
                // Subtract current seconds for accurate countdown
                diff -= now.get(Calendar.SECOND) * 1000

                var totalDuration = nextDate.time - startDate.time
                if (totalDuration < 0) totalDuration += 24 * 60 * 60 * 1000

                val elapsed = totalDuration - diff
                val progress = if (totalDuration > 0) (elapsed.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f) else 0f
                _prayerProgress.value = progress

                val hours = diff / (1000 * 60 * 60)
                val minutes = (diff % (1000 * 60 * 60)) / (1000 * 60)
                val seconds = (diff % (1000 * 60)) / 1000

                _remainingTime.value = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                
                if (nextName != lastNextPrayerName || city != lastCityName) {
                    lastNextPrayerName = nextName
                    lastCityName = city
                    val targetTimeMillis = System.currentTimeMillis() + diff
                    val allPrayersString = displayPrayers.joinToString("|") { "${it.first},${it.second}" }
                    val intent = android.content.Intent(getApplication<android.app.Application>(), CountdownService::class.java).apply {
                        putExtra("NEXT_PRAYER_NAME", nextName)
                        putExtra("NEXT_PRAYER_TIME_MILLIS", targetTimeMillis)
                        putExtra("LOCATION_NAME", city)
                        val displayIndex = displayPrayers.indexOfFirst { it.first == currentName }
                        putExtra("CURRENT_PRAYER_INDEX", displayIndex)
                        putExtra("ALL_PRAYERS", allPrayersString)
                        putExtra("ALL_PRAYERS_DATE", com.example.data.PrayerTimeCache.date(
                            (_timings.value?.date?.timestamp?.toLongOrNull() ?: (System.currentTimeMillis() / 1000)) * 1000))
                    }

                    BackgroundServices.startCountdown(getApplication<android.app.Application>(), intent)
                }
            }
        } catch (e: Exception) {
            _remainingTime.value = "00:00:00"
        }
    }

    private fun cleanTime(time: String) = time.take(5)

    private fun calculateSabahTime(sunrise: String): String {
        try {
            val parts = sunrise.split(":")
            if (parts.size == 2) {
                var h = parts[0].toInt()
                val m = parts[1].toInt()
                h -= 1
                if (h < 0) h += 24
                return String.format(java.util.Locale.getDefault(), "%02d:%02d", h, m)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return sunrise
    }

}
