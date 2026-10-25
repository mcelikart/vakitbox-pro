package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.content.ContextCompat
import com.example.ui.theme.AppTheme
import com.example.ui.theme.LocalAppTheme
import com.example.ui.theme.LocalThemeIsDark
import com.example.ui.theme.buttonBorder
import com.example.ui.theme.buttonShape
import com.example.ui.theme.cardShape
import com.example.ui.theme.glassSurfaceColor
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.random.Random

enum class StrawPaperLineStyle(val label: String) {
    RULED("Çizgili"),
    DOTTED("Noktalı"),
    PLAIN("Düz")
}

data class UserNote(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isImported: Boolean = false,
    val category: String = "Genel",
    val isPinned: Boolean = false,
    val paperStyle: String = StrawPaperLineStyle.RULED.name,
    val useSerifFont: Boolean = true,
    val fontSizeSp: Int = 16
)

private val UserNoteStateSaver = Saver<UserNote?, String>(
    save = { note ->
        note?.let {
            JSONObject().apply {
                put("id", it.id); put("title", it.title); put("content", it.content)
                put("createdAt", it.createdAt); put("updatedAt", it.updatedAt)
                put("isImported", it.isImported); put("category", it.category); put("isPinned", it.isPinned)
                put("paperStyle", it.paperStyle); put("useSerifFont", it.useSerifFont); put("fontSizeSp", it.fontSizeSp)
            }.toString()
        }.orEmpty()
    },
    restore = { raw ->
        if (raw.isBlank()) null else JSONObject(raw).let {
            UserNote(
                id = it.getString("id"), title = it.getString("title"), content = it.getString("content"),
                createdAt = it.getLong("createdAt"), updatedAt = it.getLong("updatedAt"),
                isImported = it.getBoolean("isImported"), category = it.getString("category"), isPinned = it.getBoolean("isPinned"),
                paperStyle = it.getString("paperStyle"), useSerifFont = it.getBoolean("useSerifFont"), fontSizeSp = it.getInt("fontSizeSp")
            )
        }
    }
)

internal data class NoteCategoryRenameResult(
    val categories: List<String>,
    private val previousName: String,
    val acceptedName: String?
) {
    fun selectionAfter(currentSelection: String?): String? =
        if (currentSelection == previousName && acceptedName != null) acceptedName else currentSelection
}

object NotesStore {
    private const val PREF_NAME = "user_notes_prefs"
    private const val KEY_NOTES_JSON = "notes_json_v1"
    private const val KEY_CATEGORIES_JSON = "categories_json_v1"
    const val UNCATEGORIZED = "Kategorisiz"

    val categories = listOf(
        "Genel",
        "Dua & Zikir",
        "Ayet & Hadis",
        "Sohbet & Hutbe",
        "Kişisel",
        "Yapılacaklar"
    )

    fun loadCategories(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_CATEGORIES_JSON, null) ?: return categories
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                val item = array.optString(i).trim()
                if (item.isNotEmpty() && item !in list) {
                    list.add(item)
                }
            }
            if (list.isEmpty()) categories else list
        } catch (_: Exception) {
            categories
        }
    }

    fun saveCategories(context: Context, updatedCategories: List<String>) {
        val cleaned = updatedCategories.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        val finalCategories = if (cleaned.isEmpty()) listOf("Genel") else cleaned
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        finalCategories.forEach { array.put(it) }
        prefs.edit().putString(KEY_CATEGORIES_JSON, array.toString()).apply()
    }

    fun addCategory(context: Context, name: String): List<String> {
        val trimmed = name.trim()
        val current = loadCategories(context).toMutableList()
        if (trimmed.isNotEmpty() && !trimmed.equals("Tümü", ignoreCase = true) && current.none { it.equals(trimmed, ignoreCase = true) }) {
            current.add(trimmed)
            saveCategories(context, current)
        }
        return current
    }

    fun renameCategory(context: Context, oldName: String, newName: String): List<String> =
        renameCategoryWithResult(context, oldName, newName).categories

    internal fun renameCategoryWithResult(context: Context, oldName: String, newName: String): NoteCategoryRenameResult {
        val trimmed = newName.trim()
        val current = loadCategories(context).toMutableList()
        val index = current.indexOf(oldName)
        if (index < 0 || trimmed.isEmpty() || trimmed.equals("Tümü", ignoreCase = true) ||
            current.withIndex().any { it.index != index && it.value.equals(trimmed, ignoreCase = true) }) {
            return NoteCategoryRenameResult(current, oldName, acceptedName = null)
        }
        current[index] = trimmed
        saveCategories(context, current)
        val notes = loadNotes(context)
        if (notes.any { it.category == oldName }) {
            saveNotes(context, notes.map { if (it.category == oldName) it.copy(category = trimmed) else it })
        }
        return NoteCategoryRenameResult(current, oldName, acceptedName = trimmed)
    }

    fun deleteCategory(context: Context, name: String): List<String> {
        val current = loadCategories(context).toMutableList()
        if (name == UNCATEGORIZED) return current
        current.remove(name)
        val notes = loadNotes(context)
        val affected = notes.any { it.category == name }
        if (affected || current.isEmpty()) {
            if (UNCATEGORIZED !in current) current.add(UNCATEGORIZED)
        }
        saveCategories(context, current)
        if (affected) {
            saveNotes(context, notes.map { if (it.category == name) it.copy(category = UNCATEGORIZED) else it })
        }
        return current
    }

    fun loadNotes(context: Context): List<UserNote> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_NOTES_JSON, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<UserNote>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    UserNote(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        title = obj.optString("title", "İsimsiz Not"),
                        content = obj.optString("content", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                        isImported = obj.optBoolean("isImported", false),
                        category = obj.optString("category", "Genel").ifBlank { "Genel" },
                        isPinned = obj.optBoolean("isPinned", false),
                        paperStyle = obj.optString("paperStyle", StrawPaperLineStyle.RULED.name),
                        useSerifFont = obj.optBoolean("useSerifFont", true),
                        fontSizeSp = obj.optInt("fontSizeSp", 16).coerceIn(13, 24)
                    )
                )
            }
            list.sortedWith(compareByDescending<UserNote> { it.isPinned }.thenByDescending { it.updatedAt })
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveNotes(context: Context, notes: List<UserNote>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        notes.forEach { note ->
            val obj = JSONObject().apply {
                put("id", note.id)
                put("title", note.title)
                put("content", note.content)
                put("createdAt", note.createdAt)
                put("updatedAt", note.updatedAt)
                put("isImported", note.isImported)
                put("category", note.category)
                put("isPinned", note.isPinned)
                put("paperStyle", note.paperStyle)
                put("useSerifFont", note.useSerifFont)
                put("fontSizeSp", note.fontSizeSp)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_NOTES_JSON, array.toString()).apply()
    }

    fun addQuickNote(
        context: Context,
        title: String,
        content: String,
        category: String = "Ayet & Hadis"
    ): UserNote {
        val current = loadNotes(context).toMutableList()
        val newNote = UserNote(
            title = title.trim().ifEmpty { "Not" },
            content = content.trim(),
            category = category,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        current.add(0, newNote)
        saveNotes(context, current)
        return newNote
    }

    fun parseImportedText(rawText: String, fallbackFileName: String? = null): Pair<String, String> {
        val normalized = rawText.replace("\r\n", "\n").replace("\r", "\n").trim()
        if (normalized.isEmpty()) {
            val fallbackTitle = fallbackFileName
                ?.substringBeforeLast(".")
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: "İçe Aktarılan Not"
            return fallbackTitle to ""
        }
        val lines = normalized.lines()
        val firstNonBlankIndex = lines.indexOfFirst { it.isNotBlank() }
        if (firstNonBlankIndex == -1) {
            return "İçe Aktarılan Not" to ""
        }
        val rawFirstLine = lines[firstNonBlankIndex].trim()
            .removePrefix("#")
            .removePrefix("\uFEFF")
            .trim()
        val title = if (rawFirstLine.length > 80) {
            rawFirstLine.take(80).trim() + "…"
        } else {
            rawFirstLine.ifEmpty { "İçe Aktarılan Not" }
        }
        val remainingLines = lines.drop(firstNonBlankIndex + 1)
        val body = remainingLines.joinToString("\n").trim()
        val finalBody = if (body.isEmpty()) rawFirstLine else body
        return title to finalBody
    }
}

fun parseImportedNote(rawText: String, fallbackFileName: String? = null): Pair<String, String> =
    NotesStore.parseImportedText(rawText, fallbackFileName)

@Composable
fun StrawPaperBackground(
    modifier: Modifier = Modifier,
    lineStyle: StrawPaperLineStyle = StrawPaperLineStyle.RULED,
    lineSpacingDp: Float = 28f,
    showMarginLine: Boolean = true,
    drawLines: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val strawLight = Color(0xFFF7EEDB)
    val strawMid = Color(0xFFF0E2C6)
    val strawDeep = Color(0xFFE6D3AF)
    val fiberDark = Color(0xFF9C7A4C)
    val fiberLight = Color(0xFFC8A97E)
    val ruledLineColor = Color(0xFFB89B72).copy(alpha = 0.34f)
    val marginLineColor = Color(0xFFC0564B).copy(alpha = 0.34f)

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(strawLight, strawMid, strawDeep, strawMid),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                )
            )

            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFFD2B88C).copy(alpha = 0.18f),
                        Color(0xFFB89660).copy(alpha = 0.32f)
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.5f),
                    radius = maxOf(size.width, size.height) * 0.72f
                )
            )

            val rng = Random(42199)
            val fiberCount = 160
            for (i in 0 until fiberCount) {
                val fx = rng.nextFloat() * size.width
                val fy = rng.nextFloat() * size.height
                val length = (3f + rng.nextFloat() * 10f) * density
                val angle = rng.nextFloat() * 3.14159f
                val dx = kotlin.math.cos(angle) * length * 0.5f
                val dy = kotlin.math.sin(angle) * length * 0.5f
                val alpha = 0.04f + rng.nextFloat() * 0.09f
                val color = if (i % 3 == 0) fiberDark.copy(alpha = alpha) else fiberLight.copy(alpha = alpha + 0.03f)

                drawLine(
                    color = color,
                    start = Offset(fx - dx, fy - dy),
                    end = Offset(fx + dx, fy + dy),
                    strokeWidth = (0.7f + rng.nextFloat() * 1.0f) * density,
                    cap = StrokeCap.Round
                )
            }

            if (drawLines) {
                val stepPx = lineSpacingDp.dp.toPx().coerceAtLeast(18f)
                val topHeaderOffset = 42.dp.toPx()
                when (lineStyle) {
                    StrawPaperLineStyle.RULED -> {
                        var y = topHeaderOffset
                        while (y < size.height - 16.dp.toPx()) {
                            drawLine(
                                color = ruledLineColor,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1.dp.toPx()
                            )
                            y += stepPx
                        }
                    }
                    StrawPaperLineStyle.DOTTED -> {
                        var y = topHeaderOffset
                        val dotSpacing = 18.dp.toPx()
                        while (y < size.height - 16.dp.toPx()) {
                            var x = 16.dp.toPx()
                            while (x < size.width - 16.dp.toPx()) {
                                drawCircle(
                                    color = ruledLineColor.copy(alpha = 0.42f),
                                    radius = 1.3f * density,
                                    center = Offset(x, y)
                                )
                                x += dotSpacing
                            }
                            y += stepPx
                        }
                    }
                    StrawPaperLineStyle.PLAIN -> Unit
                }

                if (showMarginLine && lineStyle == StrawPaperLineStyle.RULED) {
                    val marginX = 34.dp.toPx()
                    drawLine(
                        color = marginLineColor,
                        start = Offset(marginX, 0f),
                        end = Offset(marginX, size.height),
                        strokeWidth = 1.2.dp.toPx()
                    )
                }
            }
        }

        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPickerBottomSheet(
    categories: List<String>,
    selectedCategory: String,
    includeAllOption: Boolean = true,
    onSelectCategory: (String) -> Unit,
    onAddCategory: (String) -> Unit,
    onRenameCategory: (String, String) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newCategoryName by remember { mutableStateOf("") }
    var editingCategory by remember { mutableStateOf<String?>(null) }
    var editingText by remember { mutableStateOf("") }
    var categoryToDelete by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFF6EBD8),
        contentColor = Color(0xFF2B1B0E),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Klasörler",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF2B1B0E)
                )
                TextButton(onClick = onDismiss) {
                    Text("Kapat", fontWeight = FontWeight.Bold, color = Color(0xFF8C5319))
                }
            }

            // Add new category row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    placeholder = { Text("Yeni klasör…", fontSize = 13.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFFBF5E9),
                        unfocusedContainerColor = Color(0xFFFBF5E9),
                        focusedBorderColor = Color(0xFF8C5319),
                        unfocusedBorderColor = Color(0xFFB68952),
                        focusedTextColor = Color(0xFF2B1B0E),
                        unfocusedTextColor = Color(0xFF2B1B0E)
                    ),
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF8C5319),
                    modifier = Modifier.clickable {
                        val trimmed = newCategoryName.trim()
                        if (trimmed.isNotEmpty() && !trimmed.equals("Tümü", ignoreCase = true)) {
                            val existing = categories.firstOrNull { it.equals(trimmed, ignoreCase = true) }
                            if (existing == null) onAddCategory(trimmed)
                            onSelectCategory(existing ?: trimmed)
                            newCategoryName = ""
                        }
                    }
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Klasör ekle",
                            tint = Color(0xFFFFF8EE),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFB68952).copy(alpha = 0.35f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (includeAllOption) {
                    val isAllSelected = selectedCategory == "Tümü"
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isAllSelected) Color(0xFF8C5319) else Color(0xFFEAD7B7),
                        border = BorderStroke(1.dp, Color(0xFFB68952).copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectCategory("Tümü")
                                onDismiss()
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Tümü",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAllSelected) Color(0xFFFFF8EE) else Color(0xFF2B1B0E)
                            )
                            if (isAllSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFFFFF8EE),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                categories.forEach { cat ->
                    val isEditing = editingCategory == cat
                    val isSelected = selectedCategory == cat
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected && !isEditing) Color(0xFF8C5319) else Color(0xFFEAD7B7),
                        border = BorderStroke(1.dp, Color(0xFFB68952).copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isEditing) {
                                onSelectCategory(cat)
                                onDismiss()
                            }
                    ) {
                        if (isEditing) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                BasicTextField(
                                    value = editingText,
                                    onValueChange = { editingText = it },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2B1B0E)
                                    ),
                                    cursorBrush = SolidColor(Color(0xFF8C5319)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("notes-rename-folder-name")
                                        .background(Color(0xFFFBF5E9), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                                IconButton(
                                    onClick = {
                                        val clean = editingText.trim()
                                        if (clean.isNotEmpty() && clean != cat) {
                                            onRenameCategory(cat, clean)
                                        }
                                        editingCategory = null
                                    },
                                    enabled = editingText.trim().let { clean ->
                                        clean.isNotEmpty() && !clean.equals("Tümü", ignoreCase = true) &&
                                            categories.none { it != cat && it.equals(clean, ignoreCase = true) }
                                    },
                                    modifier = Modifier.size(32.dp).testTag("notes-rename-folder-save")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Kaydet",
                                        tint = Color(0xFF15803D),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { editingCategory = null },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "İptal",
                                        tint = Color(0xFF6A4E31),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color(0xFFFFF8EE),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        text = cat,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isSelected) Color(0xFFFFF8EE) else Color(0xFF2B1B0E)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    IconButton(
                                        onClick = {
                                            editingCategory = cat
                                            editingText = cat
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Düzenle",
                                            tint = if (isSelected) Color(0xFFFFF8EE) else Color(0xFF8C5319),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    if (cat != NotesStore.UNCATEGORIZED) {
                                        IconButton(
                                            onClick = { categoryToDelete = cat },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Sil",
                                                tint = if (isSelected) Color(0xFFFECACA) else Color(0xFFB91C1C),
                                                modifier = Modifier.size(16.dp)
                                            )
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
    categoryToDelete?.let { name ->
        NoteCategoryDeleteDialog(
            category = name,
            onDismiss = { categoryToDelete = null },
            onConfirm = {
                onDeleteCategory(name)
                categoryToDelete = null
            }
        )
    }

}

@Composable
private fun CategoryManagerDialog(
    categories: List<String>,
    onAddCategory: (String) -> Unit,
    onRenameCategory: (String, String) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onDismiss: () -> Unit
) {
    CategoryPickerBottomSheet(
        categories = categories,
        selectedCategory = "",
        includeAllOption = false,
        onSelectCategory = {},
        onAddCategory = onAddCategory,
        onRenameCategory = onRenameCategory,
        onDeleteCategory = onDeleteCategory,
        onDismiss = onDismiss
    )
}

@Composable
private fun NoteCategoryDeleteDialog(category: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kategoriyi sil") },
        text = { Text("“$category” silinsin mi? Notlar silinmez, Kategorisiz'e taşınır.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Sil") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } }
    )
}

@Composable
internal fun NotesFolderList(
    categories: List<String>,
    notes: List<UserNote>,
    onOpenFolder: (String) -> Unit,
    onAddCategory: (String) -> Unit,
    onManageCategories: () -> Unit,
    isWarmPaperStyle: Boolean = true
) {
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var newName by rememberSaveable { mutableStateOf("") }
    val folders = (listOf("Tümü", NotesStore.UNCATEGORIZED) + categories + notes.map { it.category }).distinct()
    val background = if (isWarmPaperStyle) Color(0xFFF3E4C8) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    val ink = if (isWarmPaperStyle) Color(0xFF2B1B0E) else MaterialTheme.colorScheme.onSurface
    Column(Modifier.fillMaxWidth().testTag("notes-folders"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Klasörler", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = ink)
            IconButton(onClick = { newName = ""; showAddDialog = true }, modifier = Modifier.testTag("notes-add-folder")) {
                Icon(Icons.Default.CreateNewFolder, contentDescription = "Klasör ekle", tint = ink)
            }
            IconButton(onClick = onManageCategories, modifier = Modifier.testTag("notes-manage-folders")) {
                Icon(Icons.Default.MoreVert, contentDescription = "Klasörleri yönet", tint = ink)
            }
        }
        folders.forEach { name ->
            val count = if (name == "Tümü") notes.size else notes.count { it.category == name }
            Surface(
                shape = RoundedCornerShape(14.dp), color = background,
                modifier = Modifier.fillMaxWidth().testTag("notes-folder-$name").clickable { onOpenFolder(name) }
            ) {
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(if (name == "Tümü") Icons.Default.FolderCopy else Icons.Default.Folder,
                        contentDescription = null, tint = ink.copy(alpha = 0.75f), modifier = Modifier.size(24.dp))
                    Text(name, modifier = Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = ink)
                    Text("$count", fontSize = 13.sp, color = ink.copy(alpha = 0.7f))
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = ink.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
    if (showAddDialog) {
        val clean = newName.trim()
        val valid = clean.isNotEmpty() && folders.none { it.equals(clean, ignoreCase = true) }
        AlertDialog(
            onDismissRequest = { showAddDialog = false }, title = { Text("Klasör ekle") },
            text = { OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Klasör adı") },
                singleLine = true, modifier = Modifier.fillMaxWidth().testTag("notes-new-folder-name")) },
            confirmButton = { TextButton(onClick = { onAddCategory(clean); showAddDialog = false }, enabled = valid,
                modifier = Modifier.testTag("notes-save-folder")) { Text("Ekle") } },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Vazgeç") } }
        )
    }
}

@Composable
private fun NoteFolderBreadcrumb(folder: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.testTag("notes-back-to-folders")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Klasörlere dön")
        }
        Text(folder, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, maxLines = 2)
    }
}

@Composable
fun ToolsNotesSection(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeTheme = LocalAppTheme.current
    val isDark = LocalThemeIsDark.current
    val isGlass = activeTheme != AppTheme.CLASSIC_EMERALD && activeTheme.spec.isGlass
    val primary = MaterialTheme.colorScheme.primary

    var notes by remember { mutableStateOf(NotesStore.loadNotes(context)) }
    var userCategories by remember { mutableStateOf(NotesStore.loadCategories(context)) }
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    var showFullScreenNotebook by rememberSaveable { mutableStateOf(false) }
    var editingNote by rememberSaveable(stateSaver = UserNoteStateSaver) { mutableStateOf<UserNote?>(null) }
    var isCreatingNewNote by rememberSaveable { mutableStateOf(false) }
    var newNoteCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var showCategoryDialog by remember { mutableStateOf(false) }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(isExpanded, showFullScreenNotebook) {
        notes = NotesStore.loadNotes(context)
        userCategories = NotesStore.loadCategories(context)
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                var fileName: String? = null
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0 && cursor.moveToFirst()) {
                        fileName = cursor.getString(nameIndex)
                    }
                }

                val textContent = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).readText()
                } ?: ""

                val (parsedTitle, parsedBody) = NotesStore.parseImportedText(textContent, fileName)
                val newNote = UserNote(
                    title = parsedTitle,
                    content = parsedBody,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    isImported = true,
                    category = userCategories.firstOrNull() ?: "Genel"
                )
                val updated = listOf(newNote) + notes
                notes = updated.sortedWith(compareByDescending<UserNote> { it.isPinned }.thenByDescending { it.updatedAt })
                NotesStore.saveNotes(context, notes)
                isExpanded = true
                editingNote = newNote
                isCreatingNewNote = false
                Toast.makeText(
                    context,
                    "\"$parsedTitle\" içe aktarıldı",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Not içe aktarılamadı",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val containerShape = if (isGlass) activeTheme.cardShape else RoundedCornerShape(24.dp)
    val buttonShape = if (isGlass) activeTheme.buttonShape else RoundedCornerShape(14.dp)
    val containerBorder = if (isGlass) {
        activeTheme.buttonBorder
    } else {
        BorderStroke(1.dp, primary.copy(alpha = 0.22f))
    }

    val filteredNotes = remember(notes, searchQuery, selectedCategory) {
        notes.filter { note ->
            val matchesCat = selectedCategory == "Tümü" || note.category == selectedCategory
            val matchesQuery = searchQuery.isBlank() ||
                note.title.contains(searchQuery, ignoreCase = true) ||
                note.content.contains(searchQuery, ignoreCase = true)
            matchesCat && matchesQuery
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = containerShape,
        color = if (isGlass) {
            glassSurfaceColor(activeTheme)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.88f else 0.96f)
        },
        border = containerBorder,
        shadowElevation = if (isGlass) 0.dp else 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(buttonShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFD9B382), Color(0xFFB68952))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Notes,
                            contentDescription = "Notlarım",
                            tint = Color(0xFF3A2610),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Not Defteri",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFD9B382).copy(alpha = 0.28f)
                        ) {
                            Text(
                                text = "${notes.size}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFF3DEC0) else Color(0xFF6B4C24),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showFullScreenNotebook = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "Tam Ekran",
                            tint = primary
                        )
                    }
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Daralt" else "Genişlet",
                            tint = primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = buttonShape,
                    color = primary,
                    border = if (isGlass) activeTheme.buttonBorder else null,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 42.dp)
                        .clip(buttonShape)
                        .clickable {
                            editingNote = null
                            newNoteCategory = selectedCategory?.takeUnless { it == "Tümü" }
                            isCreatingNewNote = true
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Yeni Not",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            maxLines = 1
                        )
                    }
                }

                Surface(
                    shape = buttonShape,
                    color = if (isGlass) glassSurfaceColor(activeTheme) else primary.copy(alpha = 0.12f),
                    border = if (isGlass) activeTheme.buttonBorder else BorderStroke(1.dp, primary.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 42.dp)
                        .clip(buttonShape)
                        .clickable {
                            importLauncher.launch(arrayOf("text/*", "application/octet-stream"))
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = null,
                            tint = primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "İçe Aktar",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = primary,
                            maxLines = 1
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (selectedCategory == null) {
                        NotesFolderList(
                            categories = userCategories, notes = notes,
                            onOpenFolder = { selectedCategory = it },
                            onAddCategory = { userCategories = NotesStore.addCategory(context, it) },
                            onManageCategories = { showCategoryDialog = true }, isWarmPaperStyle = false
                        )
                    } else {
                        NoteFolderBreadcrumb(selectedCategory.orEmpty(), onBack = { selectedCategory = null })
                    if (filteredNotes.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Henüz not yok",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        filteredNotes.forEach { note ->
                            NoteTitleListItem(
                                note = note,
                                buttonShape = buttonShape,
                                isGlass = isGlass,
                                activeTheme = activeTheme,
                                onOpenFullScreen = {
                                    editingNote = note
                                    isCreatingNewNote = false
                                },
                                onTogglePin = {
                                    val updated = notes.map {
                                        if (it.id == note.id) it.copy(isPinned = !it.isPinned) else it
                                    }.sortedWith(compareByDescending<UserNote> { it.isPinned }.thenByDescending { it.updatedAt })
                                    notes = updated
                                    NotesStore.saveNotes(context, updated)
                                },
                                onDelete = {
                                    val updated = notes.filterNot { it.id == note.id }
                                    notes = updated
                                    NotesStore.saveNotes(context, updated)
                                }
                            )
                        }
                    }
                    }
                }
            }
        }
    }

    if (showCategoryDialog) {
        CategoryPickerBottomSheet(
            categories = userCategories,
            selectedCategory = selectedCategory.orEmpty(),
            includeAllOption = false,
            onSelectCategory = {},
            onAddCategory = { newCat ->
                userCategories = NotesStore.addCategory(context, newCat)
            },
            onRenameCategory = { oldCat, newCat ->
                val renamed = NotesStore.renameCategoryWithResult(context, oldCat, newCat)
                userCategories = renamed.categories
                notes = NotesStore.loadNotes(context)
                selectedCategory = renamed.selectionAfter(selectedCategory)
            },
            onDeleteCategory = { delCat ->
                userCategories = NotesStore.deleteCategory(context, delCat)
                notes = NotesStore.loadNotes(context)
                if (selectedCategory == delCat) selectedCategory = null
            },
            onDismiss = { showCategoryDialog = false }
        )
    }

    if (showFullScreenNotebook) {
        FullScreenNotebookAppModal(
            notes = notes,
            categories = userCategories,
            onCategoriesChanged = {
                userCategories = NotesStore.loadCategories(context)
                notes = NotesStore.loadNotes(context)
            },
            onUpdateNotes = { updated ->
                notes = updated.sortedWith(compareByDescending<UserNote> { it.isPinned }.thenByDescending { it.updatedAt })
                NotesStore.saveNotes(context, notes)
            },
            onLaunchImport = {
                importLauncher.launch(arrayOf("text/*", "application/octet-stream"))
            },
            onOpenNoteEditor = { noteToEdit, folder ->
                newNoteCategory = folder
                if (noteToEdit == null) {
                    editingNote = null
                    isCreatingNewNote = true
                } else {
                    editingNote = noteToEdit
                    isCreatingNewNote = false
                }
            },
            onDismiss = { showFullScreenNotebook = false }
        )
    }

    if (isCreatingNewNote || editingNote != null) {
        val currentTarget = editingNote
        FullScreenStrawPaperNoteEditor(
            initialNote = currentTarget,
            initialCategory = newNoteCategory,
            categories = userCategories,
            onCategoriesChanged = {
                userCategories = NotesStore.loadCategories(context)
                notes = NotesStore.loadNotes(context)
            },
            onDismiss = {
                isCreatingNewNote = false
                editingNote = null
            },
            onSave = { savedNote ->
                val existingIdx = notes.indexOfFirst { it.id == savedNote.id }
                val updated = if (existingIdx >= 0) {
                    notes.toMutableList().apply { set(existingIdx, savedNote) }
                } else {
                    listOf(savedNote) + notes
                }.sortedWith(compareByDescending<UserNote> { it.isPinned }.thenByDescending { it.updatedAt })
                notes = updated
                NotesStore.saveNotes(context, updated)
                isExpanded = true
                isCreatingNewNote = false
                editingNote = null
            },
            onDelete = if (currentTarget != null) {
                {
                    val updated = notes.filterNot { it.id == currentTarget.id }
                    notes = updated
                    NotesStore.saveNotes(context, updated)
                    isCreatingNewNote = false
                    editingNote = null
                }
            } else null
        )
    }

}

@Composable
private fun NoteTitleListItem(
    note: UserNote,
    buttonShape: androidx.compose.ui.graphics.Shape,
    isGlass: Boolean,
    activeTheme: AppTheme,
    onOpenFullScreen: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
    showEditAction: Boolean = false,
    showDeleteAction: Boolean = false
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM", Locale("tr")) }
    val formattedDate = remember(note.updatedAt) { dateFormat.format(Date(note.updatedAt)) }

    Surface(
        shape = buttonShape,
        color = Color(0xFFF5E8CF),
        border = BorderStroke(
            width = if (note.isPinned) 1.5.dp else 1.dp,
            color = if (showDeleteAction) Color(0xFFB91C1C).copy(alpha = 0.6f)
            else if (note.isPinned) Color(0xFFB45309)
            else Color(0xFFC4A474).copy(alpha = 0.65f)
        ),
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("note-item-${note.id}")
            .clip(buttonShape)
            .clickable {
                if (showDeleteAction) {
                    onDelete()
                } else {
                    onOpenFullScreen()
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (note.isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = null,
                            tint = Color(0xFFB45309),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = if (note.useSerifFont) FontFamily.Serif else FontFamily.SansSerif
                        ),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D1E0F),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF8C6239).copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = note.category,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF5A3E1B),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                if (note.content.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = note.content.replace("\n", " "),
                        fontSize = 12.sp,
                        color = Color(0xFF5C4328),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (showEditAction) {
                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Sabitle",
                            tint = if (note.isPinned) Color(0xFFB45309) else Color(0xFF7A5C38).copy(alpha = 0.5f),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF8C5319),
                        modifier = Modifier.clickable { onOpenFullScreen() }
                    ) {
                        Text(
                            text = "Düzenle",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFF8EE),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                } else if (showDeleteAction) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFB91C1C),
                        modifier = Modifier.clickable { onDelete() }
                    ) {
                        Text(
                            text = "Sil",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                } else {
                    Text(
                        text = formattedDate,
                        fontSize = 11.sp,
                        color = Color(0xFF7A5C38)
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Aç",
                        tint = Color(0xFF7A5C38),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FullScreenStrawPaperNoteEditor(
    initialNote: UserNote?,
    initialCategory: String? = null,
    categories: List<String> = NotesStore.categories,
    onCategoriesChanged: () -> Unit = {},
    onDismiss: () -> Unit,
    onSave: (UserNote) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var currentCategories by remember(categories) { mutableStateOf(categories) }
    var showCategoryDialog by remember { mutableStateOf(false) }

    val draftId by rememberSaveable(initialNote?.id) { mutableStateOf(initialNote?.id ?: UUID.randomUUID().toString()) }
    val draftCreatedAt by rememberSaveable(initialNote?.id) { mutableLongStateOf(initialNote?.createdAt ?: System.currentTimeMillis()) }
    var title by rememberSaveable(draftId) { mutableStateOf(initialNote?.title ?: "") }
    var contentValue by rememberSaveable(draftId, stateSaver = TextFieldValue.Saver) {
        val text = initialNote?.content ?: ""
        mutableStateOf(TextFieldValue(text = text, selection = TextRange(text.length)))
    }
    var category by rememberSaveable(draftId) { mutableStateOf(initialNote?.category ?: initialCategory ?: currentCategories.firstOrNull() ?: "Genel") }
    var isPinned by rememberSaveable(draftId) { mutableStateOf(initialNote?.isPinned ?: false) }
    var paperStyle by rememberSaveable(draftId) {
        mutableStateOf(
            runCatching { StrawPaperLineStyle.valueOf(initialNote?.paperStyle ?: StrawPaperLineStyle.RULED.name) }
                .getOrDefault(StrawPaperLineStyle.RULED)
        )
    }
    var useSerifFont by rememberSaveable(draftId) { mutableStateOf(initialNote?.useSerifFont ?: true) }
    var fontSizeSp by rememberSaveable(draftId) { mutableIntStateOf(initialNote?.fontSizeSp ?: 17) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val undoStack = remember { mutableStateListOf<String>() }
    val redoStack = remember { mutableStateListOf<String>() }

    fun updateContentWithHistory(newValue: TextFieldValue) {
        if (newValue.text != contentValue.text) {
            if (undoStack.isEmpty() || undoStack.last() != contentValue.text) {
                undoStack.add(contentValue.text)
                if (undoStack.size > 40) undoStack.removeAt(0)
            }
            redoStack.clear()
        }
        contentValue = newValue
    }

    fun insertAtCursor(snippet: String) {
        val currentText = contentValue.text
        val selection = contentValue.selection
        val start = selection.min.coerceIn(0, currentText.length)
        val end = selection.max.coerceIn(0, currentText.length)
        val newText = currentText.substring(0, start) + snippet + currentText.substring(end)
        val newCursor = (start + snippet.length).coerceIn(0, newText.length)
        updateContentWithHistory(TextFieldValue(text = newText, selection = TextRange(newCursor)))
    }

    val dictation = rememberNoteDictationController(contentValue) { value -> updateContentWithHistory(value) }
    var microphoneMessage by remember { mutableStateOf<String?>(null) }
    val microphonePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) { microphoneMessage = null; dictation.start() }
        else microphoneMessage = "Sesle yazmak için mikrofon izni gerekli."
    }
    val toggleDictation: () -> Unit = {
        if (dictation.isActive) dictation.stop()
        else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            microphoneMessage = null
            dictation.start()
        } else microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    val inkPrimary = Color(0xFF2B1B0E)
    val inkSecondary = Color(0xFF6A4E31)
    val headerWarmBg = Color(0xFFEAD7B7)
    val toolbarWarmBg = Color(0xFFE5CEAA)
    val accentBronze = Color(0xFF8C5319)

    val saveAndClose = {
        dictation.stop()
        val rawTitle = title.trim()
        val rawBody = contentValue.text.trim()
        if (rawTitle.isEmpty() && rawBody.isEmpty()) {
            onDismiss()
        } else {
            val resolvedTitle = rawTitle.ifEmpty {
                rawBody.lines().firstOrNull { it.isNotBlank() }?.take(60)?.trim() ?: "İsimsiz Not"
            }
            val saved = (initialNote ?: UserNote(id = draftId, title = resolvedTitle, content = rawBody, createdAt = draftCreatedAt)).copy(
                title = resolvedTitle,
                content = contentValue.text,
                category = category,
                isPinned = isPinned,
                paperStyle = paperStyle.name,
                useSerifFont = useSerifFont,
                fontSizeSp = fontSizeSp,
                updatedAt = System.currentTimeMillis()
            )
            onSave(saved)
        }
    }

    Dialog(
        onDismissRequest = { saveAndClose() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            dialogWindow?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }

        BackHandler { saveAndClose() }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(headerWarmBg)
                .statusBarsPadding()
                .imePadding()
        ) {
            // Top bar
            Surface(
                color = headerWarmBg,
                shadowElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { saveAndClose() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Geri",
                                    tint = inkPrimary
                                )
                            }
                            Text(
                                text = if (initialNote == null) "Yeni Not" else "Notu Düzenle",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = inkPrimary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = {
                                    if (undoStack.isNotEmpty()) {
                                        val prev = undoStack.removeAt(undoStack.lastIndex)
                                        redoStack.add(contentValue.text)
                                        contentValue = TextFieldValue(prev, TextRange(prev.length))
                                    }
                                },
                                enabled = undoStack.isNotEmpty(),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Undo,
                                    contentDescription = "Geri Al",
                                    tint = if (undoStack.isNotEmpty()) inkPrimary else inkSecondary.copy(alpha = 0.35f),
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (redoStack.isNotEmpty()) {
                                        val next = redoStack.removeAt(redoStack.lastIndex)
                                        undoStack.add(contentValue.text)
                                        contentValue = TextFieldValue(next, TextRange(next.length))
                                    }
                                },
                                enabled = redoStack.isNotEmpty(),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Redo,
                                    contentDescription = "İleri Al",
                                    tint = if (redoStack.isNotEmpty()) inkPrimary else inkSecondary.copy(alpha = 0.35f),
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            IconButton(
                                onClick = { isPinned = !isPinned },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = "Sabitle",
                                    tint = if (isPinned) Color(0xFFB45309) else inkSecondary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    val shareText = "${title.ifBlank { "Not" }}\n\n${contentValue.text}"
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, title.ifBlank { "Not" })
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }
                                    runCatching {
                                        context.startActivity(Intent.createChooser(sendIntent, "Paylaş"))
                                    }
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Paylaş",
                                    tint = inkPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            if (onDelete != null) {
                                IconButton(
                                    onClick = { showDeleteConfirm = true },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Sil",
                                        tint = Color(0xFFB91C1C),
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = accentBronze,
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .testTag("note-editor-save")
                                    .clickable { saveAndClose() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFFFFF8EE),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Kaydet",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFF8EE)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Minimal options & single Category Picker trigger (Bottom Sheet in Note Editor)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFFD7BC94),
                            modifier = Modifier.clickable {
                                val styles = StrawPaperLineStyle.entries
                                paperStyle = styles[(styles.indexOf(paperStyle) + 1) % styles.size]
                            }
                        ) {
                            Text(
                                text = paperStyle.label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = inkPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFFD7BC94),
                            modifier = Modifier.clickable { useSerifFont = !useSerifFont }
                        ) {
                            Text(
                                text = if (useSerifFont) "Serif" else "Düz Yazı",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = inkPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFFD7BC94)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "A-",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = inkPrimary,
                                    modifier = Modifier
                                        .clickable { fontSizeSp = (fontSizeSp - 2).coerceAtLeast(13) }
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                                Text(
                                    text = "$fontSizeSp",
                                    fontSize = 10.sp,
                                    color = inkSecondary
                                )
                                Text(
                                    text = "A+",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = inkPrimary,
                                    modifier = Modifier
                                        .clickable { fontSizeSp = (fontSizeSp + 2).coerceAtMost(24) }
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = accentBronze,
                            modifier = Modifier.clickable { showCategoryDialog = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Category,
                                    contentDescription = "Kategori Seç",
                                    tint = Color(0xFFFFF8EE),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Kategori Seç: $category",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFF8EE)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = Color(0xFFFFF8EE),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }

            val lineHeightSp = (fontSizeSp + 12)
            val density = LocalDensity.current
            val lineHeightPx = with(density) { lineHeightSp.sp.toPx() }
            var bodyLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

            StrawPaperBackground(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                lineStyle = paperStyle,
                lineSpacingDp = lineHeightSp.toFloat(),
                showMarginLine = false,
                drawLines = false
            ) {
                val scrollState = rememberScrollState()
                val ruledLineColor = Color(0xFFB08D5B).copy(alpha = 0.42f)
                val marginLineColor = Color(0xFFC0564B).copy(alpha = 0.36f)
                val contentStartPad = if (paperStyle == StrawPaperLineStyle.RULED) 46.dp else 20.dp

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .drawBehind {
                            if (paperStyle == StrawPaperLineStyle.RULED) {
                                val marginX = 34.dp.toPx()
                                drawLine(
                                    color = marginLineColor,
                                    start = Offset(marginX, 0f),
                                    end = Offset(marginX, size.height),
                                    strokeWidth = 1.2.dp.toPx()
                                )
                            }
                        }
                ) {
                    // Title section that scrolls with the sheet
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = contentStartPad,
                                end = 20.dp,
                                top = 16.dp,
                                bottom = 8.dp
                            )
                    ) {
                        BasicTextField(
                            value = title,
                            onValueChange = { title = it },
                            textStyle = TextStyle(
                                fontFamily = if (useSerifFont) FontFamily.Serif else FontFamily.SansSerif,
                                fontSize = (fontSizeSp + 5).sp,
                                fontWeight = FontWeight.Bold,
                                color = inkPrimary
                            ),
                            cursorBrush = SolidColor(accentBronze),
                            modifier = Modifier.fillMaxWidth().testTag("note-editor-title"),
                            decorationBox = { innerTextField ->
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    if (title.isEmpty()) {
                                        Text(
                                            text = "Başlık",
                                            style = TextStyle(
                                                fontFamily = if (useSerifFont) FontFamily.Serif else FontFamily.SansSerif,
                                                fontSize = (fontSizeSp + 5).sp,
                                                fontWeight = FontWeight.Bold,
                                                color = inkSecondary.copy(alpha = 0.45f)
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }

                    HorizontalDivider(
                        color = Color(0xFF9C7A4C).copy(alpha = 0.48f),
                        thickness = 1.5.dp
                    )

                    val bodyTextStyle = TextStyle(
                        fontFamily = if (useSerifFont) FontFamily.Serif else FontFamily.SansSerif,
                        fontSize = fontSizeSp.sp,
                        lineHeight = lineHeightSp.sp,
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Proportional,
                            trim = LineHeightStyle.Trim.None
                        ),
                        fontWeight = FontWeight.Normal,
                        color = inkPrimary
                    )

                    // Body section with lines drawn directly behind each text line and scrolling together
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 640.dp)
                            .drawBehind {
                                val stepPx = lineHeightPx.coerceAtLeast(20f)
                                val layout = bodyLayoutResult
                                when (paperStyle) {
                                    StrawPaperLineStyle.RULED -> {
                                        var lastLineY = 0f
                                        if (layout != null && layout.lineCount > 0) {
                                            for (i in 0 until layout.lineCount) {
                                                val y = layout.getLineBottom(i) - 2.dp.toPx()
                                                drawLine(
                                                    color = ruledLineColor,
                                                    start = Offset(0f, y),
                                                    end = Offset(size.width, y),
                                                    strokeWidth = 1.dp.toPx()
                                                )
                                                lastLineY = y
                                            }
                                        }
                                        var y = if (lastLineY > 0f) lastLineY + stepPx else (stepPx - 2.dp.toPx())
                                        while (y < size.height) {
                                            drawLine(
                                                color = ruledLineColor,
                                                start = Offset(0f, y),
                                                end = Offset(size.width, y),
                                                strokeWidth = 1.dp.toPx()
                                            )
                                            y += stepPx
                                        }
                                    }
                                    StrawPaperLineStyle.DOTTED -> {
                                        val dotSpacing = 18.dp.toPx()
                                        var lastLineY = 0f
                                        if (layout != null && layout.lineCount > 0) {
                                            for (i in 0 until layout.lineCount) {
                                                val y = layout.getLineBottom(i) - 2.dp.toPx()
                                                var x = 16.dp.toPx()
                                                while (x < size.width - 16.dp.toPx()) {
                                                    drawCircle(
                                                        color = ruledLineColor.copy(alpha = 0.50f),
                                                        radius = 1.3f * density.density,
                                                        center = Offset(x, y)
                                                    )
                                                    x += dotSpacing
                                                }
                                                lastLineY = y
                                            }
                                        }
                                        var y = if (lastLineY > 0f) lastLineY + stepPx else (stepPx - 2.dp.toPx())
                                        while (y < size.height) {
                                            var x = 16.dp.toPx()
                                            while (x < size.width - 16.dp.toPx()) {
                                                drawCircle(
                                                    color = ruledLineColor.copy(alpha = 0.50f),
                                                    radius = 1.3f * density.density,
                                                    center = Offset(x, y)
                                                )
                                                x += dotSpacing
                                            }
                                            y += stepPx
                                        }
                                    }
                                    StrawPaperLineStyle.PLAIN -> Unit
                                }
                            }
                            .padding(
                                start = contentStartPad,
                                end = 20.dp,
                                bottom = 48.dp
                            )
                    ) {
                        BasicTextField(
                            value = contentValue,
                            onValueChange = { updateContentWithHistory(it) },
                            readOnly = dictation.isActive,
                            onTextLayout = { bodyLayoutResult = it },
                            textStyle = bodyTextStyle,
                            cursorBrush = SolidColor(accentBronze),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("note-editor-body")
                                .heightIn(min = 600.dp),
                            decorationBox = { innerTextField ->
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    if (contentValue.text.isEmpty()) {
                                        Text(
                                            text = "Notunuzu yazın...",
                                            style = bodyTextStyle.copy(
                                                color = inkSecondary.copy(alpha = 0.45f)
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }
                }
            }

            // Elevated bottom formatting toolbar positioned well above system navigation bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(toolbarWarmBg)
                    .navigationBarsPadding()
                    .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = com.example.ui.theme.navBarBottomPadding(extra = 8.dp))
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFDFC49A),
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.2.dp, Color(0xFF8C5319).copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                    val voiceStatus = microphoneMessage ?: dictation.status
                    if (!voiceStatus.isNullOrBlank()) {
                        Text(voiceStatus, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp).testTag("note-voice-status"),
                            fontSize = 12.sp, color = Color(0xFF4A3219))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EditorFormatChip(
                            icon = if (dictation.isActive) Icons.Default.StopCircle else Icons.Default.Mic,
                            label = if (dictation.isActive) "Durdur" else "Sesle yaz",
                            tag = "note-format-Sesle yaz",
                            active = dictation.isActive,
                            onClick = toggleDictation
                        )
                        EditorFormatChip(
                            icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                            label = "Madde",
                            onClick = { insertAtCursor("\n• ") }
                        )
                        EditorFormatChip(
                            icon = Icons.Default.CheckBoxOutlineBlank,
                            label = "Yapılacak",
                            onClick = { insertAtCursor("\n☐ ") }
                        )
                        EditorFormatChip(
                            icon = Icons.Default.CheckBox,
                            label = "Tamamlandı",
                            onClick = { insertAtCursor("\n☑ ") }
                        )
                        EditorFormatChip(
                            icon = Icons.Default.Title,
                            label = "Alt Başlık",
                            onClick = { insertAtCursor("\n\n❖ ") }
                        )
                        EditorFormatChip(
                            icon = Icons.Default.AutoAwesome,
                            label = "Besmele",
                            onClick = { insertAtCursor("﷽\nBismillâhirrahmânirrahîm\n\n") }
                        )
                        EditorFormatChip(
                            icon = Icons.Default.Schedule,
                            label = "Tarih",
                            onClick = {
                                val nowStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr")).format(Date())
                                insertAtCursor(" [$nowStr] ")
                            }
                        )
                        EditorFormatChip(
                            icon = Icons.Default.HorizontalRule,
                            label = "Ayraç",
                            onClick = { insertAtCursor("\n────────────────────────\n") }
                        )
                        EditorFormatChip(
                            icon = Icons.Default.ContentCopy,
                            label = "Kopyala",
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(
                                    ClipData.newPlainText(title.ifBlank { "Not" }, "${title}\n\n${contentValue.text}")
                                )
                                Toast.makeText(context, "Kopyalandı", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    }
                }
            }
        }
    }

    if (showCategoryDialog) {
        CategoryPickerBottomSheet(
            categories = currentCategories,
            selectedCategory = category,
            includeAllOption = false,
            onSelectCategory = { category = it },
            onAddCategory = { newCat ->
                currentCategories = NotesStore.addCategory(context, newCat)
                category = newCat
                onCategoriesChanged()
            },
            onRenameCategory = { oldCat, newCat ->
                val renamed = NotesStore.renameCategoryWithResult(context, oldCat, newCat)
                currentCategories = renamed.categories
                category = renamed.selectionAfter(category) ?: category
                onCategoriesChanged()
            },
            onDeleteCategory = { delCat ->
                currentCategories = NotesStore.deleteCategory(context, delCat)
                if (category == delCat) {
                    category = NotesStore.UNCATEGORIZED
                    currentCategories = NotesStore.addCategory(context, NotesStore.UNCATEGORIZED)
                }
                onCategoriesChanged()
            },
            onDismiss = { showCategoryDialog = false }
        )
    }

    if (showDeleteConfirm && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Notu Sil", fontWeight = FontWeight.Bold) },
            text = { Text("\"${title.ifBlank { "Bu not" }}\" silinsin mi?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    }
                ) {
                    Text("Sil", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Vazgeç")
                }
            }
        )
    }
}

@Composable
private fun EditorFormatChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tag: String = "note-format-$label",
    active: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (active) Color(0xFF8C5319) else Color(0xFFF6EBD8),
        border = BorderStroke(1.dp, Color(0xFFB68952).copy(alpha = 0.5f)),
        modifier = Modifier
            .testTag(tag)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) Color(0xFFFFF8EE) else Color(0xFF4A3219),
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (active) Color(0xFFFFF8EE) else Color(0xFF2B1B0E)
            )
        }
    }
}

@Composable
private fun FullScreenNotebookAppModal(
    notes: List<UserNote>,
    categories: List<String>,
    onCategoriesChanged: () -> Unit,
    onUpdateNotes: (List<UserNote>) -> Unit,
    onLaunchImport: () -> Unit,
    onOpenNoteEditor: (UserNote?, String?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var folder by rememberSaveable { mutableStateOf<String?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var showCategoryManager by remember { mutableStateOf(false) }
    var clearAllConfirm by remember { mutableStateOf(false) }
    var noteToDelete by remember { mutableStateOf<UserNote?>(null) }
    var deleteMode by rememberSaveable { mutableStateOf(false) }
    val filtered = remember(notes, searchQuery, folder) {
        notes.filter { note ->
            (folder == "Tümü" || note.category == folder) && (searchQuery.isBlank() ||
                note.title.contains(searchQuery, ignoreCase = true) || note.content.contains(searchQuery, ignoreCase = true))
        }
    }
    val back: () -> Unit = {
        when {
            deleteMode -> deleteMode = false
            folder != null -> { folder = null; searchQuery = "" }
            else -> onDismiss()
        }
    }
    Dialog(onDismissRequest = back, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = true)) {
        BackHandler(onBack = back)
        StrawPaperBackground(modifier = Modifier.fillMaxSize(), lineStyle = StrawPaperLineStyle.PLAIN, showMarginLine = false) {
            Column(Modifier.fillMaxSize().systemBarsPadding()) {
                Surface(color = Color(0xFFE6D1AE), shadowElevation = 3.dp) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = back, modifier = Modifier.testTag(if (folder == null) "notes-exit" else "notes-back-to-folders")) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = if (folder == null) "Geri" else "Klasörlere dön", tint = Color(0xFF2B1B0E))
                            }
                            Text(folder ?: "Not Defteri", modifier = Modifier.weight(1f), maxLines = 2,
                                style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif), fontWeight = FontWeight.Bold, color = Color(0xFF2B1B0E))
                            Box {
                                IconButton(onClick = { menuOpen = true }, modifier = Modifier.testTag("notes-overflow")) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Not defteri menüsü", tint = Color(0xFF2B1B0E))
                                }
                                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                    DropdownMenuItem(text = { Text("Klasörleri yönet") }, onClick = { menuOpen = false; showCategoryManager = true })
                                    DropdownMenuItem(text = { Text("İçe aktar") }, onClick = { menuOpen = false; onLaunchImport() })
                                    if (folder != null && filtered.isNotEmpty()) {
                                        DropdownMenuItem(text = { Text(if (deleteMode) "Silmeyi bitir" else "Not sil") }, onClick = { menuOpen = false; deleteMode = !deleteMode })
                                    }
                                    if (notes.isNotEmpty()) {
                                        DropdownMenuItem(text = { Text("Tüm notları sil") }, onClick = { menuOpen = false; clearAllConfirm = true })
                                    }
                                }
                            }
                        }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { onOpenNoteEditor(null, folder?.takeUnless { it == "Tümü" }) }, modifier = Modifier.testTag("notes-new-note")) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp)); Text("Yeni Not")
                            }
                            if (folder != null) {
                                Text("Not Defteri / ${folder.orEmpty()}", modifier = Modifier.weight(1f), maxLines = 2,
                                    fontSize = 11.sp, color = Color(0xFF6A4E31))
                            }
                        }
                        if (folder != null) {
                            OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it },
                                placeholder = { Text("Notlarda ara…") }, singleLine = true, shape = RoundedCornerShape(12.dp),
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                modifier = Modifier.fillMaxWidth().testTag("notes-search"))
                        }
                    }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp).testTag("notes-browser-list"),
                    contentPadding = PaddingValues(top = 14.dp, bottom = com.example.ui.theme.navBarBottomPadding(extra = 16.dp)),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (folder == null) {
                        item {
                            NotesFolderList(categories, notes, onOpenFolder = { folder = it; searchQuery = "" },
                                onAddCategory = { NotesStore.addCategory(context, it); onCategoriesChanged() },
                                onManageCategories = { showCategoryManager = true })
                        }
                    } else {
                        if (deleteMode) item { Text("Silmek için nota dokunun", fontSize = 12.sp, color = Color(0xFF991B1B)) }
                        if (filtered.isEmpty()) item {
                            Text(if (searchQuery.isBlank()) "Henüz not yok" else "Not bulunamadı", modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                                color = Color(0xFF6A4E31), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                        items(filtered, key = { it.id }) { note ->
                            NoteTitleListItem(note = note, buttonShape = RoundedCornerShape(14.dp), isGlass = false,
                                activeTheme = AppTheme.CLASSIC_EMERALD, onOpenFullScreen = { onOpenNoteEditor(note, null) },
                                onTogglePin = { onUpdateNotes(notes.map { if (it.id == note.id) it.copy(isPinned = !it.isPinned) else it }) },
                                onDelete = { noteToDelete = note }, showDeleteAction = deleteMode)
                        }
                    }
                }
            }
        }
    }
    if (showCategoryManager) {
        CategoryManagerDialog(categories = categories,
            onAddCategory = { NotesStore.addCategory(context, it); onCategoriesChanged() },
            onRenameCategory = { old, new ->
                val renamed = NotesStore.renameCategoryWithResult(context, old, new)
                folder = renamed.selectionAfter(folder)
                onCategoriesChanged()
            },
            onDeleteCategory = {
                NotesStore.deleteCategory(context, it)
                if (folder == it) folder = null
                onCategoriesChanged()
            }, onDismiss = { showCategoryManager = false })
    }
    noteToDelete?.let { note ->
        AlertDialog(onDismissRequest = { noteToDelete = null }, title = { Text("Notu sil") }, text = { Text("“${note.title}” silinsin mi?") },
            confirmButton = { TextButton(onClick = { onUpdateNotes(notes.filterNot { it.id == note.id }); noteToDelete = null }) { Text("Sil") } },
            dismissButton = { TextButton(onClick = { noteToDelete = null }) { Text("Vazgeç") } })
    }
    if (clearAllConfirm) {
        AlertDialog(onDismissRequest = { clearAllConfirm = false }, title = { Text("Tüm notları sil") }, text = { Text("Tüm notlar kalıcı olarak silinsin mi?") },
            confirmButton = { TextButton(onClick = { onUpdateNotes(emptyList()); clearAllConfirm = false; deleteMode = false }) { Text("Tümünü Sil") } },
            dismissButton = { TextButton(onClick = { clearAllConfirm = false }) { Text("Vazgeç") } })
    }
}

@Composable
fun NotesTopSectionCard(
    onOpenNotes: () -> Unit
) {
    ToolsNotesSection()
}

@Composable
fun NotesScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var notes by remember { mutableStateOf(NotesStore.loadNotes(context)) }
    var userCategories by remember { mutableStateOf(NotesStore.loadCategories(context)) }
    var editingNote by rememberSaveable(stateSaver = UserNoteStateSaver) { mutableStateOf<UserNote?>(null) }
    var isCreatingNewNote by rememberSaveable { mutableStateOf(false) }
    var newNoteCategory by rememberSaveable { mutableStateOf<String?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                var fileName: String? = null
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0 && cursor.moveToFirst()) {
                        fileName = cursor.getString(nameIndex)
                    }
                }
                val textContent = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).readText()
                } ?: ""
                val (parsedTitle, parsedBody) = NotesStore.parseImportedText(textContent, fileName)
                val newNote = UserNote(
                    title = parsedTitle,
                    content = parsedBody,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    isImported = true,
                    category = userCategories.firstOrNull() ?: "Genel"
                )
                val updated = (listOf(newNote) + notes)
                    .sortedWith(compareByDescending<UserNote> { it.isPinned }.thenByDescending { it.updatedAt })
                notes = updated
                NotesStore.saveNotes(context, updated)
                editingNote = newNote
                isCreatingNewNote = false
                Toast.makeText(
                    context,
                    "\"$parsedTitle\" içe aktarıldı",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Not içe aktarılamadı",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    FullScreenNotebookAppModal(
        notes = notes,
        categories = userCategories,
        onCategoriesChanged = {
            userCategories = NotesStore.loadCategories(context)
            notes = NotesStore.loadNotes(context)
        },
        onUpdateNotes = { updated ->
            notes = updated.sortedWith(compareByDescending<UserNote> { it.isPinned }.thenByDescending { it.updatedAt })
            NotesStore.saveNotes(context, notes)
        },
        onLaunchImport = {
            importLauncher.launch(arrayOf("text/*", "application/octet-stream"))
        },
        onOpenNoteEditor = { noteToEdit, folder ->
            newNoteCategory = folder
            if (noteToEdit == null) {
                editingNote = null
                isCreatingNewNote = true
            } else {
                editingNote = noteToEdit
                isCreatingNewNote = false
            }
        },
        onDismiss = onBack
    )

    if (isCreatingNewNote || editingNote != null) {
        val currentTarget = editingNote
        FullScreenStrawPaperNoteEditor(
            initialNote = currentTarget,
            initialCategory = newNoteCategory,
            categories = userCategories,
            onCategoriesChanged = {
                userCategories = NotesStore.loadCategories(context)
                notes = NotesStore.loadNotes(context)
            },
            onDismiss = {
                isCreatingNewNote = false
                editingNote = null
            },
            onSave = { savedNote ->
                val existingIdx = notes.indexOfFirst { it.id == savedNote.id }
                val updated = if (existingIdx >= 0) {
                    notes.toMutableList().apply { set(existingIdx, savedNote) }
                } else {
                    listOf(savedNote) + notes
                }.sortedWith(compareByDescending<UserNote> { it.isPinned }.thenByDescending { it.updatedAt })
                notes = updated
                NotesStore.saveNotes(context, updated)
                isCreatingNewNote = false
                editingNote = null
            },
            onDelete = if (currentTarget != null) {
                {
                    val updated = notes.filterNot { it.id == currentTarget.id }
                    notes = updated
                    NotesStore.saveNotes(context, updated)
                    isCreatingNewNote = false
                    editingNote = null
                }
            } else null
        )
    }
}
