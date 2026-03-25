package com.example.wordbookapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.compose.BackHandler
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import com.example.wordbookapp.data.local.entity.WordEntity
import com.example.wordbookapp.data.model.DeckWithCount
import com.example.wordbookapp.data.model.ThemePreset
import com.example.wordbookapp.data.model.WordField
import com.example.wordbookapp.data.model.WordOrder
import com.example.wordbookapp.data.repository.WordbookRepository
import com.example.wordbookapp.ui.theme.DividerSoft
import com.example.wordbookapp.ui.theme.InkMuted
import com.example.wordbookapp.ui.theme.InkSoft
import com.example.wordbookapp.ui.theme.CardBorderStrong
import com.example.wordbookapp.ui.theme.ExamGreen
import com.example.wordbookapp.ui.theme.ExamGreenSoft
import com.example.wordbookapp.ui.theme.PaperElevated
import com.example.wordbookapp.ui.theme.PrimaryBlue
import com.example.wordbookapp.ui.theme.PrimaryBlueSoft
import com.example.wordbookapp.ui.theme.SecondaryCoral
import com.example.wordbookapp.ui.theme.SecondaryCoralSoft
import com.example.wordbookapp.ui.theme.themePaletteForPreset

@Composable
fun WordbookApp(
    repository: WordbookRepository,
    currentThemePreset: ThemePreset,
    onPreviewTheme: (ThemePreset) -> Unit,
    onCancelThemePreview: () -> Unit,
    onApplyTheme: (ThemePreset) -> Unit,
    navController: NavHostController = rememberNavController(),
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val canNavigateBack = navController.previousBackStackEntry != null

    BackHandler(enabled = canNavigateBack) {
        navController.popBackStack()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding),
        ) {
            composable("home") {
                val viewModel: HomeViewModel = viewModel(
                    factory = WordbookViewModelFactory { HomeViewModel(repository) },
                )
                HomeRoute(
                    viewModel = viewModel,
                    currentThemePreset = currentThemePreset,
                    onPreviewTheme = onPreviewTheme,
                    onCancelThemePreview = onCancelThemePreview,
                    onApplyTheme = onApplyTheme,
                    onOpenDeck = { deckId -> navController.navigate("deck/$deckId") },
                    onOpenDeckStats = { deckId -> navController.navigate("deck_stats/$deckId") },
                    onStartDeckExam = { deckId -> navController.navigate("exam_setup?deckId=$deckId") },
                    onOpenAiDeck = { navController.navigate("exam_setup?ai=true") },
                    onDeckCreated = { deckId -> navController.navigate("deck/$deckId") },
                    onOpenAllWords = { navController.navigate("all_words") },
                )
            }
            composable("all_words") {
                val viewModel: AllWordsViewModel = viewModel(
                    factory = WordbookViewModelFactory { AllWordsViewModel(repository) },
                )
                AllWordsRoute(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onOpenWord = { wordId -> navController.navigate("word/$wordId?deckId=-1") },
                )
            }
            composable(
                route = "deck_stats/{deckId}",
                arguments = listOf(navArgument("deckId") { type = NavType.LongType }),
            ) { backStackEntry ->
                val deckId = backStackEntry.arguments?.getLong("deckId") ?: return@composable
                val viewModel: DeckStatsViewModel = viewModel(
                    key = "deck-stats-$deckId",
                    factory = WordbookViewModelFactory { DeckStatsViewModel(repository, deckId) },
                )
                DeckStatsRoute(
                    viewModel = viewModel,
                    onOpenDateStats = { dateKey -> navController.navigate("deck_stats/$deckId/date/$dateKey") },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = "deck_stats/{deckId}/date/{dateKey}",
                arguments = listOf(
                    navArgument("deckId") { type = NavType.LongType },
                    navArgument("dateKey") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val deckId = backStackEntry.arguments?.getLong("deckId") ?: return@composable
                val dateKey = backStackEntry.arguments?.getString("dateKey") ?: return@composable
                val viewModel: DeckDateStatsViewModel = viewModel(
                    key = "deck-date-stats-$deckId-$dateKey",
                    factory = WordbookViewModelFactory { DeckDateStatsViewModel(repository, deckId, dateKey) },
                )
                DeckDateStatsRoute(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = "deck/{deckId}",
                arguments = listOf(navArgument("deckId") { type = NavType.LongType }),
            ) { backStackEntry ->
                val deckId = backStackEntry.arguments?.getLong("deckId") ?: return@composable
                val viewModel: DeckDetailViewModel = viewModel(
                    key = "deck-$deckId",
                    factory = WordbookViewModelFactory { DeckDetailViewModel(repository, deckId) },
                )
                DeckRoute(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onAddWord = { navController.navigate("word_editor/$deckId") },
                    onOpenWord = { wordId -> navController.navigate("word/$wordId?deckId=$deckId") },
                    onStartExam = { navController.navigate("exam_setup?deckId=$deckId") },
                )
            }
            composable(
                route = "word/{wordId}?deckId={deckId}",
                arguments = listOf(
                    navArgument("wordId") { type = NavType.LongType },
                    navArgument("deckId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                ),
            ) { backStackEntry ->
                val wordId = backStackEntry.arguments?.getLong("wordId") ?: return@composable
                val rawDeckId = backStackEntry.arguments?.getLong("deckId") ?: -1L
                val deckId = rawDeckId.takeIf { it > 0 }
                val viewModel: WordDetailViewModel = viewModel(
                    key = "word-$wordId",
                    factory = WordbookViewModelFactory { WordDetailViewModel(repository, wordId) },
                )
                WordDetailRoute(
                    viewModel = viewModel,
                    deckId = deckId,
                    onBack = { navController.popBackStack() },
                    onEdit = { targetWordId ->
                        navController.navigate("word_editor/${deckId ?: -1}?wordId=$targetWordId")
                    },
                    onOpenWord = { targetWordId ->
                        navController.navigate("word/$targetWordId?deckId=${deckId ?: -1}")
                    },
                )
            }
            composable(
                route = "word_editor/{deckId}?wordId={wordId}",
                arguments = listOf(
                    navArgument("deckId") { type = NavType.LongType },
                    navArgument("wordId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                ),
            ) { backStackEntry ->
                val deckId = backStackEntry.arguments?.getLong("deckId") ?: return@composable
                val rawWordId = backStackEntry.arguments?.getLong("wordId") ?: -1L
                val wordId = rawWordId.takeIf { it > 0 }
                val viewModel: WordEditorViewModel = viewModel(
                    key = "editor-$deckId-$rawWordId",
                    factory = WordbookViewModelFactory { WordEditorViewModel(repository, deckId, wordId) },
                )
                WordEditorRoute(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = "exam_setup?deckId={deckId}&ai={ai}",
                arguments = listOf(
                    navArgument("deckId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                    navArgument("ai") {
                        type = NavType.BoolType
                        defaultValue = false
                    },
                ),
            ) { backStackEntry ->
                val rawDeckId = backStackEntry.arguments?.getLong("deckId") ?: -1L
                val deckId = rawDeckId.takeIf { it > 0 }
                val isAiDeck = backStackEntry.arguments?.getBoolean("ai") ?: false
                val viewModel: ExamSetupViewModel = viewModel(
                    key = "setup-${deckId ?: 0}-$isAiDeck",
                    factory = WordbookViewModelFactory { ExamSetupViewModel(repository, deckId, isAiDeck) },
                )
                ExamSetupRoute(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onStartExam = { sessionId ->
                        navController.navigate("exam/$sessionId") {
                            popUpTo(navController.currentDestination?.route ?: return@navigate)
                        }
                    },
                )
            }
            composable(
                route = "exam/{sessionId}",
                arguments = listOf(navArgument("sessionId") { type = NavType.LongType }),
            ) { backStackEntry ->
                val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: return@composable
                val viewModel: ExamViewModel = viewModel(
                    key = "exam-$sessionId",
                    factory = WordbookViewModelFactory { ExamViewModel(repository, sessionId) },
                )
                ExamRoute(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onFinished = { navController.navigate("result/$sessionId") },
                )
            }
            composable(
                route = "result/{sessionId}",
                arguments = listOf(navArgument("sessionId") { type = NavType.LongType }),
            ) { backStackEntry ->
                val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: return@composable
                val viewModel: ResultViewModel = viewModel(
                    key = "result-$sessionId",
                    factory = WordbookViewModelFactory { ResultViewModel(repository, sessionId) },
                )
                ResultRoute(
                    viewModel = viewModel,
                    onGoHome = {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun HomeRoute(
    viewModel: HomeViewModel,
    currentThemePreset: ThemePreset,
    onPreviewTheme: (ThemePreset) -> Unit,
    onCancelThemePreview: () -> Unit,
    onApplyTheme: (ThemePreset) -> Unit,
    onOpenDeck: (Long) -> Unit,
    onOpenDeckStats: (Long) -> Unit,
    onStartDeckExam: (Long) -> Unit,
    onOpenAiDeck: () -> Unit,
    onDeckCreated: (Long) -> Unit,
    onOpenAllWords: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var deckName by remember { mutableStateOf("") }
    var pendingThemePreset by remember(currentThemePreset, showSettingsDialog) { mutableStateOf(currentThemePreset) }
    val scope = rememberCoroutineScope()

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("커스텀 단어장 만들기") },
            text = {
                OutlinedTextField(
                    value = deckName,
                    onValueChange = { deckName = it },
                    label = { Text("단어장 이름") },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val newDeckId = viewModel.createCustomDeck(deckName)
                            showDialog = false
                            deckName = ""
                            onDeckCreated(newDeckId)
                        }
                    },
                    enabled = deckName.isNotBlank(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text("생성")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDialog = false },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text("취소")
                }
            },
        )
    }

    if (showSettingsDialog) {
        ThemeSettingsDialog(
            selectedPreset = pendingThemePreset,
            onPresetChange = {
                pendingThemePreset = it
                onPreviewTheme(it)
            },
            onDismiss = {
                showSettingsDialog = false
                pendingThemePreset = currentThemePreset
                onCancelThemePreview()
            },
            onConfirm = {
                showSettingsDialog = false
                onApplyTheme(pendingThemePreset)
            },
        )
    }

    ScreenContainer(
        title = "일본어 단어장",
        actions = {
            AppHeaderIconButton(
                onClick = {
                    pendingThemePreset = currentThemePreset
                    showSettingsDialog = true
                },
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "설정",
                )
            }
        },
    ) {
        if (uiState.isLoading || uiState.data == null) {
            LoadingView()
            return@ScreenContainer
        }
        val data = uiState.data ?: return@ScreenContainer
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SummaryCard(
                    totalWords = data.totalWordCount,
                    recentSessionCount = data.recentSessions.size,
                    onOpenAiDeck = onOpenAiDeck,
                    onOpenAllWords = onOpenAllWords,
                )
            }
            item {
                SectionTitle("JLPT 기본 단어장")
            }
            items(data.jlptDecks) { deck ->
                DeckCard(
                    deck = deck,
                    onClick = { onOpenDeck(deck.id) },
                    onStartExam = { onStartDeckExam(deck.id) },
                    onOpenStats = { onOpenDeckStats(deck.id) },
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionTitle("커스텀 단어장")
                    OutlinedButton(
                        onClick = { showDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = null,
                            )
                            Text("추가")
                        }
                    }
                }
            }
            if (data.customDecks.isEmpty()) {
                item {
                    EmptyHint("아직 만든 커스텀 단어장이 없어요.")
                }
            } else {
                items(data.customDecks) { deck ->
                    DeckCard(
                        deck = deck,
                        onClick = { onOpenDeck(deck.id) },
                        onStartExam = { onStartDeckExam(deck.id) },
                        onOpenStats = { onOpenDeckStats(deck.id) },
                    )
                }
            }
            if (data.recentSessions.isNotEmpty()) {
                item {
                    SectionTitle("최근 시험")
                }
                items(data.recentSessions) { session ->
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(session.deckName, fontWeight = FontWeight.Bold)
                            Text("정답 ${session.correctCount} / ${session.totalCount} (${session.accuracyPercent}%)")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSettingsDialog(
    selectedPreset: ThemePreset,
    onPresetChange: (ThemePreset) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("설정") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "테마",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                ThemePresetDropdown(
                    selectedPreset = selectedPreset,
                    onPresetChange = onPresetChange,
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "사용 색상",
                        style = MaterialTheme.typography.labelLarge,
                        color = InkMuted,
                    )
                    ThemeSwatchRow(
                        colors = themePaletteForPreset(selectedPreset).swatches,
                        squareSize = 20.dp,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text("적용")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text("취소")
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemePresetDropdown(
    selectedPreset: ThemePreset,
    onPresetChange: (ThemePreset) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selectedPreset.displayName,
            onValueChange = {},
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            readOnly = true,
            label = { Text("테마 프리셋") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            ThemePreset.entries.forEach { preset ->
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = preset.displayName,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            ThemeSwatchRow(
                                colors = themePaletteForPreset(preset).swatches.take(5),
                                squareSize = 12.dp,
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onPresetChange(preset)
                    },
                )
            }
        }
    }
}

@Composable
private fun ThemeSwatchRow(
    colors: List<Color>,
    squareSize: androidx.compose.ui.unit.Dp,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        colors.forEach { color ->
            Box(
                modifier = Modifier
                    .width(squareSize)
                    .height(squareSize)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
                    .then(
                        if (color.alpha == 0f) Modifier else Modifier
                    ),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeckRoute(
    viewModel: DeckDetailViewModel,
    onBack: () -> Unit,
    onAddWord: () -> Unit,
    onOpenWord: (Long) -> Unit,
    onStartExam: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showPartOfSpeech by rememberSaveable { mutableStateOf(true) }
    var showReadingKo by rememberSaveable { mutableStateOf(false) }
    var showMeaningKo by rememberSaveable { mutableStateOf(true) }
    var showMeaningJa by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedPartOfSpeech by rememberSaveable { mutableStateOf("전체") }
    var selectedTag by rememberSaveable { mutableStateOf("전체") }
    var showFilterSheet by rememberSaveable { mutableStateOf(false) }

    if (showFilterSheet) {
        val partOfSpeechOptions = listOf("전체") + uiState.words
            .map { it.partOfSpeech.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        val tagOptions = listOf("전체") + uiState.words
            .map { it.tag.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            containerColor = PaperElevated,
        ) {
            FilterBottomSheetContent(
                onReset = {
                    selectedPartOfSpeech = "전체"
                    selectedTag = "전체"
                },
            ) {
                FilterChipRow(
                    title = "품사",
                    options = partOfSpeechOptions,
                    selected = selectedPartOfSpeech,
                    onSelect = { selectedPartOfSpeech = it },
                )
                FilterChipRow(
                    title = "태그",
                    options = tagOptions,
                    selected = selectedTag,
                    onSelect = { selectedTag = it },
                )
            }
        }
    }

    ScreenContainer(
        title = uiState.deck?.name ?: "단어장",
        onBack = onBack,
    ) {
        if (uiState.isLoading || uiState.deck == null) {
            LoadingView()
            return@ScreenContainer
        }
        val deck = uiState.deck ?: return@ScreenContainer
        val filteredWords by remember(
            uiState.words,
            searchQuery,
            selectedPartOfSpeech,
            selectedTag,
        ) {
            derivedStateOf {
                uiState.words.filter { word ->
                    val matchesQuery = searchQuery.isBlank() || word.matchesSearchQuery(searchQuery)
                    val matchesPartOfSpeech = selectedPartOfSpeech == "전체" || word.partOfSpeech == selectedPartOfSpeech
                    val matchesTag = selectedTag == "전체" || word.tag == selectedTag
                    matchesQuery && matchesPartOfSpeech && matchesTag
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(deck.description, style = MaterialTheme.typography.bodyMedium)
            DisplayOptionRow(
                showPartOfSpeech = showPartOfSpeech,
                onShowPartOfSpeechChange = { showPartOfSpeech = it },
                showReadingKo = showReadingKo,
                onShowReadingKoChange = { showReadingKo = it },
                showMeaningKo = showMeaningKo,
                onShowMeaningKoChange = { showMeaningKo = it },
                showMeaningJa = showMeaningJa,
                onShowMeaningJaChange = { showMeaningJa = it },
            )
            AppSearchField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = "단어 검색",
                placeholder = "한자, 읽기, 뜻, 태그로 찾기",
            )
            Text(
                text = buildFilterSummary(
                    filteredCount = filteredWords.size,
                    totalCount = uiState.words.size,
                    selectedPartOfSpeech = selectedPartOfSpeech,
                    selectedTag = selectedTag,
                ),
                style = MaterialTheme.typography.labelMedium,
                color = InkMuted,
            )
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppCompactActionButton(
                    icon = Icons.Outlined.Menu,
                    text = "필터",
                    contentDescription = "필터",
                    onClick = { showFilterSheet = true },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppCompactActionButton(
                        icon = Icons.Outlined.PlayArrow,
                        text = "시험",
                        contentDescription = "시험 시작",
                        enabled = uiState.words.isNotEmpty(),
                        highlighted = true,
                        onClick = onStartExam,
                    )
                    AppCompactActionButton(
                        icon = Icons.Outlined.Add,
                        text = "추가",
                        contentDescription = "단어 추가",
                        onClick = onAddWord,
                    )
                }
            }
            if (uiState.words.isEmpty()) {
                EmptyHint("이 단어장에는 아직 단어가 없어요.")
            } else if (filteredWords.isEmpty()) {
                EmptyHint("조건에 맞는 단어가 없어요.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(filteredWords) { word ->
                        WordRow(
                            word = word,
                            showPartOfSpeech = showPartOfSpeech,
                            showReadingKo = showReadingKo,
                            showMeaningKo = showMeaningKo,
                            showMeaningJa = showMeaningJa,
                            allWords = filteredWords,
                            onClick = { onOpenWord(word.id) },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AllWordsRoute(
    viewModel: AllWordsViewModel,
    onBack: () -> Unit,
    onOpenWord: (Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showPartOfSpeech by rememberSaveable { mutableStateOf(true) }
    var showReadingKo by rememberSaveable { mutableStateOf(false) }
    var showMeaningKo by rememberSaveable { mutableStateOf(true) }
    var showMeaningJa by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedDeckId by rememberSaveable { mutableStateOf(-1L) }
    var selectedPartOfSpeech by rememberSaveable { mutableStateOf("전체") }
    var selectedTag by rememberSaveable { mutableStateOf("전체") }
    var showFilterSheet by rememberSaveable { mutableStateOf(false) }

    val deckOptions = remember(uiState.decks) {
        listOf(-1L to "전체 단어장") + uiState.decks.map { it.id to it.name }
    }
    val deckFilteredWords = remember(uiState.words, uiState.deckWordIds, selectedDeckId) {
        if (selectedDeckId <= 0L) {
            uiState.words
        } else {
            val allowedIds = uiState.deckWordIds[selectedDeckId].orEmpty()
            uiState.words.filter { it.id in allowedIds }
        }
    }
    val partOfSpeechOptions = remember(deckFilteredWords) {
        listOf("전체") + deckFilteredWords.map { it.partOfSpeech.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }
    val tagOptions = remember(deckFilteredWords) {
        listOf("전체") + deckFilteredWords.map { it.tag.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }
    val filteredWords by remember(
        deckFilteredWords,
        searchQuery,
        selectedPartOfSpeech,
        selectedTag,
    ) {
        derivedStateOf {
            deckFilteredWords.filter { word ->
                val matchesQuery = searchQuery.isBlank() || word.matchesSearchQuery(searchQuery)
                val matchesPartOfSpeech = selectedPartOfSpeech == "전체" || word.partOfSpeech == selectedPartOfSpeech
                val matchesTag = selectedTag == "전체" || word.tag == selectedTag
                matchesQuery && matchesPartOfSpeech && matchesTag
            }
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            containerColor = PaperElevated,
        ) {
            FilterBottomSheetContent(
                onReset = {
                    selectedDeckId = -1L
                    selectedPartOfSpeech = "전체"
                    selectedTag = "전체"
                },
            ) {
                FilterChipIdRow(
                    title = "단어장",
                    options = deckOptions,
                    selected = selectedDeckId,
                    onSelect = { selectedDeckId = it },
                )
                FilterChipRow(
                    title = "품사",
                    options = partOfSpeechOptions,
                    selected = selectedPartOfSpeech,
                    onSelect = { selectedPartOfSpeech = it },
                )
                FilterChipRow(
                    title = "태그",
                    options = tagOptions,
                    selected = selectedTag,
                    onSelect = { selectedTag = it },
                )
            }
        }
    }

    ScreenContainer(
        title = "모든 단어",
        onBack = onBack,
    ) {
        if (uiState.isLoading) {
            LoadingView()
            return@ScreenContainer
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DisplayOptionRow(
                showPartOfSpeech = showPartOfSpeech,
                onShowPartOfSpeechChange = { showPartOfSpeech = it },
                showReadingKo = showReadingKo,
                onShowReadingKoChange = { showReadingKo = it },
                showMeaningKo = showMeaningKo,
                onShowMeaningKoChange = { showMeaningKo = it },
                showMeaningJa = showMeaningJa,
                onShowMeaningJaChange = { showMeaningJa = it },
            )
            AppSearchField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = "전체 단어 검색",
                placeholder = "한자, 읽기, 뜻, 태그로 찾기",
            )
            Text(
                text = buildAllWordsFilterSummary(
                    filteredCount = filteredWords.size,
                    totalCount = uiState.words.size,
                    selectedDeckName = deckOptions.firstOrNull { it.first == selectedDeckId }?.second ?: "전체 단어장",
                    selectedPartOfSpeech = selectedPartOfSpeech,
                    selectedTag = selectedTag,
                ),
                style = MaterialTheme.typography.labelMedium,
                color = InkMuted,
            )
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppCompactActionButton(
                    icon = Icons.Outlined.Menu,
                    text = "필터",
                    contentDescription = "필터",
                    onClick = { showFilterSheet = true },
                )
            }
            if (uiState.words.isEmpty()) {
                EmptyHint("등록된 단어가 아직 없어요.")
            } else if (filteredWords.isEmpty()) {
                EmptyHint("조건에 맞는 단어가 없어요.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(filteredWords) { word ->
                        WordRow(
                            word = word,
                            showPartOfSpeech = showPartOfSpeech,
                            showReadingKo = showReadingKo,
                            showMeaningKo = showMeaningKo,
                            showMeaningJa = showMeaningJa,
                            allWords = filteredWords,
                            onClick = { onOpenWord(word.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WordEditorRoute(
    viewModel: WordEditorViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) onBack()
    }
    ScreenContainer(
        title = if (uiState.isEditMode) "단어 수정" else "단어 추가",
        onBack = onBack,
    ) {
        if (uiState.isLoading) {
            LoadingView()
            return@ScreenContainer
        }
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            EditorField("한자", uiState.draft.kanji) {
                viewModel.updateDraft { draft -> draft.copy(kanji = it) }
            }
            EditorField("읽는 방법(일본어)", uiState.draft.readingJa) {
                viewModel.updateDraft { draft -> draft.copy(readingJa = it) }
            }
            EditorField("읽는 방법(한국어)", uiState.draft.readingKo) {
                viewModel.updateDraft { draft -> draft.copy(readingKo = it) }
            }
            EditorField("뜻(한국어)", uiState.draft.meaningKo) {
                viewModel.updateDraft { draft -> draft.copy(meaningKo = it) }
            }
            EditorField("뜻(일본어)", uiState.draft.meaningJa) {
                viewModel.updateDraft { draft -> draft.copy(meaningJa = it) }
            }
            EditorField("예문(일본어)", uiState.draft.exampleJa) {
                viewModel.updateDraft { draft -> draft.copy(exampleJa = it) }
            }
            EditorField("예문 뜻(한국어)", uiState.draft.exampleKo) {
                viewModel.updateDraft { draft -> draft.copy(exampleKo = it) }
            }
            EditorField("품사", uiState.draft.partOfSpeech) {
                viewModel.updateDraft { draft -> draft.copy(partOfSpeech = it) }
            }
            EditorField("문법", uiState.draft.grammar) {
                viewModel.updateDraft { draft -> draft.copy(grammar = it) }
            }
            EditorField("태그", uiState.draft.tag) {
                viewModel.updateDraft { draft -> draft.copy(tag = it) }
            }
            EditorField("비고", uiState.draft.note) {
                viewModel.updateDraft { draft -> draft.copy(note = it) }
            }
            uiState.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = { viewModel.save() },
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(if (uiState.isEditMode) "수정 저장" else "단어 저장")
            }
        }
    }
}

@Composable
private fun WordDetailRoute(
    viewModel: WordDetailViewModel,
    deckId: Long?,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onOpenWord: (Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeckDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.saveToDeckSuccess) {
        if (uiState.saveToDeckSuccess) {
            showDeckDialog = false
            viewModel.clearSaveToDeckSuccess()
        }
    }

    ScreenContainer(
        title = "단어사전",
        onBack = onBack,
        actions = {
            val wordId = uiState.detail?.word?.id
            if (wordId != null) {
                IconButton(onClick = { onEdit(wordId) }) {
                    Icon(Icons.Outlined.Edit, contentDescription = "수정")
                }
            }
        },
    ) {
        if (uiState.isLoading || uiState.detail == null) {
            LoadingView()
            return@ScreenContainer
        }

        val detail = uiState.detail ?: return@ScreenContainer
        val word = detail.word
        val excludedDeckIds = detail.includedDecks.map { it.id }.toSet()
        val addableDecks = detail.allDecks.filter { it.id !in excludedDeckIds }

        if (showDeckDialog) {
            AlertDialog(
                onDismissRequest = { showDeckDialog = false },
                title = { Text("단어장에 추가") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (addableDecks.isEmpty()) {
                            Text("이미 모든 단어장에 들어 있어요.")
                        } else {
                            addableDecks.forEach { deck ->
                                OutlinedButton(
                                    onClick = { viewModel.addToDeck(deck.id) },
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                ) {
                                    Text(deck.name)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { showDeckDialog = false },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text("닫기")
                    }
                },
            )
        }

        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            WordDictionaryHeader(
                word = word,
                showReadingKo = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showDeckDialog = true },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text("단어장에 넣기")
                }
                if (deckId != null) {
                    OutlinedButton(
                        onClick = { onEdit(word.id) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text("이 단어 수정")
                    }
                }
            }
            DetailSection("포함된 단어장") {
                if (detail.includedDecks.isEmpty()) {
                    EmptyHint("아직 연결된 단어장이 없어요.")
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        detail.includedDecks.forEach { deck ->
                            AssistChip(
                                onClick = { },
                                label = { Text(deck.name) },
                                colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
                                    containerColor = PrimaryBlueSoft,
                                    labelColor = PrimaryBlue,
                                ),
                            )
                        }
                    }
                }
            }
            DetailSection("뜻(한국어)") {
                Text(word.meaningKo, style = MaterialTheme.typography.bodyLarge)
            }
            if (word.meaningJa.isNotBlank()) {
                DetailSection("뜻(일본어)") {
                    LinkedJapaneseText(
                        text = word.meaningJa,
                        currentWordId = word.id,
                        allWords = detail.allWords,
                        onOpenWord = onOpenWord,
                    )
                }
            }
            if (word.exampleJa.isNotBlank() || word.exampleKo.isNotBlank()) {
                DetailSection("예문") {
                    if (word.exampleJa.isNotBlank()) {
                        LinkedJapaneseText(
                            text = word.exampleJa,
                            currentWordId = word.id,
                            allWords = detail.allWords,
                            onOpenWord = onOpenWord,
                        )
                    }
                    if (word.exampleKo.isNotBlank()) {
                        Text(
                            word.exampleKo,
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkSoft,
                        )
                    }
                }
            }
            DetailSection("기본 정보") {
                DictionaryMetaRow("읽는 방법(일본어)", word.readingJa)
                if (word.readingKo.isNotBlank()) {
                    DictionaryMetaRow("읽는 방법(한국어)", word.readingKo)
                }
                if (word.partOfSpeech.isNotBlank()) {
                    DictionaryMetaRow("품사", word.partOfSpeech)
                }
                if (word.grammar.isNotBlank()) {
                    DictionaryMetaRow("문법", word.grammar)
                }
                if (word.tag.isNotBlank()) {
                    DictionaryMetaRow("태그", word.tag)
                }
                if (word.note.isNotBlank()) {
                    DictionaryMetaRow("비고", word.note)
                }
            }
        }
    }
}

@Composable
private fun ExamSetupRoute(
    viewModel: ExamSetupViewModel,
    onBack: () -> Unit,
    onStartExam: (Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    ScreenContainer(
        title = if (uiState.isAiDeck) "AI 단어장 설정" else "시험 설정",
        onBack = onBack,
    ) {
        if (uiState.isLoading) {
            LoadingView()
            return@ScreenContainer
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PaperElevated),
                border = BorderStroke(1.dp, DividerSoft),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = uiState.deck?.name ?: "AI 단어장",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if (uiState.isAiDeck) {
                            "자주 틀린 단어와 새 단어를 섞어 30문제를 구성합니다."
                        } else {
                            "출제 방식을 고른 뒤 바로 시험을 시작할 수 있어요."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSoft,
                    )
                }
            }

            SettingGroup(
                title = "출제 순서",
                selectedLabel = orderLabel(uiState.settings.wordOrder),
                options = WordOrder.entries.map { it to orderLabel(it) },
                current = uiState.settings.wordOrder,
                onSelect = viewModel::setWordOrder,
            )
            SettingGroup(
                title = "앞면 표시값",
                selectedLabel = fieldLabel(uiState.settings.frontField),
                options = WordField.entries.map { it to fieldLabel(it) },
                current = uiState.settings.frontField,
                onSelect = viewModel::setFrontField,
            )
            SettingGroup(
                title = "터치 후 공개값",
                selectedLabel = fieldLabel(uiState.settings.revealField),
                options = WordField.entries.map { it to fieldLabel(it) },
                current = uiState.settings.revealField,
                onSelect = viewModel::setRevealField,
            )

            AppPrimaryButton(
                text = "시험 시작",
                onClick = {
                    scope.launch {
                        onStartExam(viewModel.startExam())
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.canStart,
            )
        }
    }
}

@Composable
private fun ExamRoute(
    viewModel: ExamViewModel,
    onBack: () -> Unit,
    onFinished: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    ScreenContainer(title = "암기 시험", onBack = onBack) {
        if (uiState.isLoading || uiState.sessionData == null) {
            LoadingView()
            return@ScreenContainer
        }
        val sessionData = uiState.sessionData ?: return@ScreenContainer
        if (sessionData.words.isEmpty()) {
            EmptyHint("출제할 단어가 없어요.")
            return@ScreenContainer
        }
        val showExamRuby = sessionData.session.revealField != WordField.READING_JA &&
            sessionData.session.revealField != WordField.READING_KO
        val currentIndex = sessionData.answersCount
        val currentWord = sessionData.words[currentIndex]
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("${currentIndex + 1} / ${sessionData.words.size}", style = MaterialTheme.typography.titleMedium)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 20.dp)
                        .clickable(enabled = !uiState.revealed) { viewModel.reveal() },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp)),
                    ) {
                        Text(
                            text = if (uiState.revealed) "정답 공개됨" else "터치해서 보기",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(
                        text = fieldLabel(sessionData.session.frontField),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF7A97BB),
                    )
                    RubyFieldText(
                        word = currentWord,
                        field = sessionData.session.frontField,
                        mainStyle = MaterialTheme.typography.headlineSmall,
                        rubyStyle = MaterialTheme.typography.labelMedium,
                        showRuby = showExamRuby,
                        alignCenter = true,
                    )
                    if (uiState.revealed) {
                        RubyFieldText(
                            word = currentWord,
                            field = sessionData.session.revealField,
                            mainStyle = MaterialTheme.typography.titleLarge,
                            rubyStyle = MaterialTheme.typography.labelMedium,
                            showRuby = showExamRuby,
                            alignCenter = true,
                            rubyColor = Color(0xFFE5807A),
                        )
                        Text(
                            currentWord.meaningKo,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                        )
                    } else {
                        Text(
                            "카드를 터치해서 정답을 확인하세요.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            if (uiState.revealed) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            scope.launch {
                                if (viewModel.answer(true)) onFinished()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text("맞았어요")
                    }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                if (viewModel.answer(false)) onFinished()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text("틀렸어요")
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultRoute(
    viewModel: ResultViewModel,
    onGoHome: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ScreenContainer(title = "시험 결과") {
        if (uiState.isLoading || uiState.result == null) {
            LoadingView()
            return@ScreenContainer
        }
        val result = uiState.result ?: return@ScreenContainer
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(result.summary.deckName, fontWeight = FontWeight.Bold)
                    Text("총 ${result.summary.totalCount}문제")
                    Text("정답 ${result.summary.correctCount} / 오답 ${result.summary.wrongCount}")
                    Text("정답률 ${result.summary.accuracyPercent}%")
                }
            }
            SectionTitle("자주 틀리는 단어")
            if (result.topMissedWords.isEmpty()) {
                EmptyHint("아직 통계가 충분하지 않아요.")
            } else {
                result.topMissedWords.forEach { stat ->
                    Card {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stat.word.kanji, fontWeight = FontWeight.Bold)
                            Text("${stat.word.readingJa} / ${stat.word.meaningKo}")
                            Text("누적 오답 ${stat.wrongCount}회 / 응시 ${stat.attemptCount}회")
                        }
                    }
                }
            }
            Button(
                onClick = onGoHome,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text("홈으로 돌아가기")
            }
        }
    }
}

@Composable
private fun ScreenContainer(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                actions?.invoke()
                if (onBack != null) {
                    AppHeaderIconButton(
                        onClick = onBack,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "뒤로",
                        )
                    }
                }
            }
        }
        content()
    }
}

@Composable
private fun AppHeaderIconButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
    ) {
        content()
    }
}

@Composable
private fun DisplayOptionRow(
    showPartOfSpeech: Boolean,
    onShowPartOfSpeechChange: (Boolean) -> Unit,
    showReadingKo: Boolean,
    onShowReadingKoChange: (Boolean) -> Unit,
    showMeaningKo: Boolean,
    onShowMeaningKoChange: (Boolean) -> Unit,
    showMeaningJa: Boolean,
    onShowMeaningJaChange: (Boolean) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        InlineOptionCheckbox(
            label = "품사",
            checked = showPartOfSpeech,
            onCheckedChange = onShowPartOfSpeechChange,
        )
        InlineOptionCheckbox(
            label = "한글 읽기",
            checked = showReadingKo,
            onCheckedChange = onShowReadingKoChange,
        )
        InlineOptionCheckbox(
            label = "한글 뜻",
            checked = showMeaningKo,
            onCheckedChange = onShowMeaningKoChange,
        )
        InlineOptionCheckbox(
            label = "일본어 뜻",
            checked = showMeaningJa,
            onCheckedChange = onShowMeaningJaChange,
        )
    }
}

@Composable
private fun InlineOptionCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.padding(0.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = InkSoft,
        )
    }
}

@Composable
private fun AppCompactActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    highlighted: Boolean = false,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(
            1.dp,
            if (highlighted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.outline,
        ),
        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
            containerColor = if (highlighted) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            contentColor = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        ),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.padding(end = 4.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun LoadingView() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
}

@Composable
private fun StatsKeyChip(
    label: String,
    value: String,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PaperElevated),
        border = BorderStroke(1.dp, DividerSoft),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = InkMuted)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun StatsWordRow(
    stat: com.example.wordbookapp.data.model.WordAggregateStat,
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PaperElevated),
        border = BorderStroke(1.dp, DividerSoft),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stat.word.kanji.ifBlank { stat.word.readingJa },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stat.word.meaningKo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSoft,
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text("오답 ${stat.wrongCount}회", style = MaterialTheme.typography.labelLarge, color = SecondaryCoral)
                Text("응시 ${stat.attemptCount}회", style = MaterialTheme.typography.bodySmall, color = InkMuted)
                Text("오답률 ${stat.wrongRatePercent}%", style = MaterialTheme.typography.bodySmall, color = InkMuted)
            }
        }
    }
}

@Composable
private fun DeckDailyBarChart(
    stats: List<com.example.wordbookapp.data.model.DeckDailyStat>,
) {
    val chartItems = stats.take(7).reversed()
    val maxQuestionCount = chartItems.maxOfOrNull { it.totalQuestionCount }?.coerceAtLeast(1) ?: 1

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PaperElevated),
        border = BorderStroke(1.dp, DividerSoft),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("최근 일자별 응시량", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                chartItems.forEach { daily ->
                    val totalBarRatio = daily.totalQuestionCount.toFloat() / maxQuestionCount.toFloat()
                    val wrongBarRatio = daily.wrongCount.toFloat() / maxQuestionCount.toFloat()
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier.height(120.dp),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height((120f * totalBarRatio).dp.coerceAtLeast(6.dp))
                                    .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                                    .background(PrimaryBlueSoft),
                            )
                            if (daily.wrongCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.6f)
                                        .height((120f * wrongBarRatio).dp.coerceAtLeast(6.dp))
                                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                        .background(SecondaryCoral),
                                )
                            }
                        }
                        Text(
                            text = daily.dateLabel.takeLast(5),
                            style = MaterialTheme.typography.labelSmall,
                            color = InkMuted,
                        )
                    }
                }
            }
            Text(
                text = "파랑: 총 문제 수 · 코랄: 오답 수",
                style = MaterialTheme.typography.bodySmall,
                color = InkMuted,
            )
        }
    }
}

@Composable
private fun AppPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(text)
    }
}

@Composable
private fun AppSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(text)
    }
}

@Composable
private fun AppActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        colors = androidx.compose.material3.IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.primary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
        )
    }
}

@Composable
private fun AppFilterButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppHeaderIconButton(
        onClick = onClick,
    ) {
        Icon(
            imageVector = Icons.Outlined.Menu,
            contentDescription = "필터",
        )
    }
}

@Composable
private fun AppSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
            )
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
            unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            cursorColor = MaterialTheme.colorScheme.primary,
        ),
        shape = MaterialTheme.shapes.large,
    )
}

@Composable
private fun SummaryCard(
    totalWords: Int,
    recentSessionCount: Int,
    onOpenAiDeck: () -> Unit,
    onOpenAllWords: () -> Unit,
) {
    Card {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(PrimaryBlueSoft, PaperElevated),
                    ),
                ),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "총 단어 ${totalWords}개",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "최근 시험 ${recentSessionCount}회",
                    color = InkSoft,
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenAiDeck() },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, DividerSoft),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .width(44.dp)
                                .height(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(PrimaryBlueSoft, SecondaryCoralSoft),
                                    ),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "AI",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue,
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = "AI 시험 기능",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "자주 틀린 단어와 새 단어를 섞어 바로 시험해요.",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSoft,
                            )
                        }
                        Icon(
                            imageVector = Icons.Outlined.PlayArrow,
                            contentDescription = null,
                            tint = PrimaryBlue,
                        )
                    }
                }
                OutlinedButton(
                    onClick = onOpenAllWords,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Text("모든 단어 보기")
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-18).dp, y = 18.dp)
                    .width(72.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(SecondaryCoralSoft),
            )
        }
    }
}

@Composable
private fun DeckStatsRoute(
    viewModel: DeckStatsViewModel,
    onOpenDateStats: (String) -> Unit,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ScreenContainer(
        title = "단어장 통계",
        onBack = onBack,
    ) {
        if (uiState.isLoading || uiState.stats == null) {
            LoadingView()
            return@ScreenContainer
        }
        val stats = uiState.stats ?: return@ScreenContainer
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stats.summary.deckName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        StatsKeyChip("완료 시험", "${stats.summary.completedSessionCount}회")
                        StatsKeyChip("시험 본 단어", "${stats.summary.studiedWordCount}개")
                        StatsKeyChip("미응시 단어", "${stats.summary.unstudiedWordCount}개")
                        StatsKeyChip("누적 정답률", "${stats.summary.accuracyPercent}%")
                    }
                }
            }
            item {
                SectionTitle("일자별 기록")
            }
            if (stats.dailyStats.isEmpty()) {
                item {
                    EmptyHint("아직 완료된 시험 기록이 없어요.")
                }
            } else {
                item {
                    DeckDailyBarChart(stats = stats.dailyStats)
                }
                items(stats.dailyStats) { daily ->
                    Card(
                        modifier = Modifier.clickable { onOpenDateStats(daily.dateKey) },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = PaperElevated),
                        border = BorderStroke(1.dp, DividerSoft),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(daily.dateLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                "시험 ${daily.completedSessionCount}회 · 문제 ${daily.totalQuestionCount}개 · 오답 ${daily.wrongCount}개 · 정답률 ${daily.accuracyPercent}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkSoft,
                            )
                        }
                    }
                }
            }
            item {
                SectionTitle("자주 틀린 단어")
            }
            if (stats.topMissedWords.isEmpty()) {
                item {
                    EmptyHint("아직 오답 기록이 없어요.")
                }
            } else {
                items(stats.topMissedWords) { stat ->
                    StatsWordRow(stat = stat)
                }
            }
            item {
                SectionTitle("단어별 집계")
            }
            items(stats.allWordStats) { stat ->
                StatsWordRow(stat = stat)
            }
        }
    }
}

@Composable
private fun DeckDateStatsRoute(
    viewModel: DeckDateStatsViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ScreenContainer(
        title = "날짜 통계",
        onBack = onBack,
    ) {
        if (uiState.isLoading || uiState.stats == null) {
            LoadingView()
            return@ScreenContainer
        }
        val stats = uiState.stats ?: return@ScreenContainer
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("${stats.deckName} · ${stats.dateLabel}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        StatsKeyChip("시험 수", "${stats.sessions.size}회")
                        StatsKeyChip("시험 본 단어", "${stats.studiedWordCount}개")
                        StatsKeyChip("미응시 단어", "${stats.unstudiedWordCount}개")
                    }
                }
            }
            item {
                SectionTitle("세션 기록")
            }
            if (stats.sessions.isEmpty()) {
                item {
                    EmptyHint("이 날짜에는 완료된 시험이 없어요.")
                }
            } else {
                items(stats.sessions) { session ->
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = PaperElevated),
                        border = BorderStroke(1.dp, DividerSoft),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text("세션 #${session.sessionId}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                "문제 ${session.totalCount}개 · 오답 ${session.wrongCount}개 · 정답률 ${session.accuracyPercent}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkSoft,
                            )
                        }
                    }
                }
            }
            item {
                SectionTitle("그날 자주 틀린 단어")
            }
            if (stats.topMissedWords.isEmpty()) {
                item {
                    EmptyHint("이 날짜에는 오답 기록이 없어요.")
                }
            } else {
                items(stats.topMissedWords) { stat ->
                    StatsWordRow(stat = stat)
                }
            }
        }
    }
}

@Composable
private fun DeckCard(
    deck: DeckWithCount,
    onClick: () -> Unit,
    onStartExam: () -> Unit,
    onOpenStats: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.25.dp, CardBorderStrong),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(deck.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(deck.description, color = InkSoft)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(
                        onClick = onClick,
                        label = { Text(deck.sourceTag) },
                        colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
                            containerColor = PrimaryBlueSoft,
                            labelColor = PrimaryBlue,
                        ),
                    )
                    Text("${deck.wordCount}개 단어", color = InkMuted)
                }
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Button(
                    onClick = onStartExam,
                    modifier = Modifier.width(90.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ExamGreenSoft,
                        contentColor = ExamGreen,
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 9.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PlayArrow,
                            contentDescription = null,
                        )
                        Text("시험")
                    }
                }
                OutlinedButton(
                    onClick = onOpenStats,
                    modifier = Modifier.width(90.dp),
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 9.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.QueryStats,
                            contentDescription = null,
                        )
                        Text("통계")
                    }
                }
            }
        }
    }
}

@Composable
private fun WordRow(
    word: WordEntity,
    showPartOfSpeech: Boolean,
    showReadingKo: Boolean,
    showMeaningKo: Boolean,
    showMeaningJa: Boolean,
    allWords: List<WordEntity>,
    onClick: () -> Unit,
) {
    val hasMeaningColumn = showMeaningKo || showMeaningJa
    val prioritizeJapaneseMeaning = showMeaningJa && !showMeaningKo
    val leftColumnWeight = when {
        !hasMeaningColumn -> 1f
        prioritizeJapaneseMeaning -> 0.36f
        else -> 0.42f
    }
    val rightColumnWeight = if (prioritizeJapaneseMeaning) 0.64f else 0.58f
    val meaningJaBaseStyle = if (prioritizeJapaneseMeaning) {
        MaterialTheme.typography.bodyLarge.copy(
            fontWeight = FontWeight.Medium,
            fontSize = 17.sp,
            lineHeight = 24.sp,
            letterSpacing = (-0.1).sp,
        )
    } else {
        MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            letterSpacing = (-0.1).sp,
        )
    }
    val meaningJaRubyStyle = if (prioritizeJapaneseMeaning) {
        MaterialTheme.typography.labelSmall.copy(
            fontSize = 10.sp,
            lineHeight = 11.sp,
            letterSpacing = (-0.1).sp,
        )
    } else {
        MaterialTheme.typography.labelSmall.copy(
            fontSize = 9.5.sp,
            lineHeight = 10.sp,
            letterSpacing = (-0.1).sp,
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PaperElevated),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.25.dp, CardBorderStrong),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(
                modifier = Modifier
                    .width(4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Brush.verticalGradient(listOf(PrimaryBlueSoft, SecondaryCoralSoft))),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(leftColumnWeight),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (showPartOfSpeech) {
                        Text(
                            text = word.partOfSpeech.ifBlank { word.tag.ifBlank { "단어" } },
                            style = MaterialTheme.typography.labelMedium,
                            color = PrimaryBlue,
                        )
                    }
                    RubyFieldText(
                        word = word,
                        field = WordField.KANJI,
                        mainStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        rubyStyle = MaterialTheme.typography.labelMedium,
                        rubyColor = SecondaryCoral,
                    )
                    if (showReadingKo && word.readingKo.isNotBlank()) {
                        Text(
                            text = word.readingKo,
                            style = MaterialTheme.typography.labelMedium,
                            color = InkMuted,
                        )
                    }
                }
                if (hasMeaningColumn) {
                    Column(
                        modifier = Modifier.weight(rightColumnWeight),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (showMeaningKo && word.meaningKo.isNotBlank()) {
                            Text(
                                text = word.meaningKo,
                                style = MaterialTheme.typography.bodyLarge,
                                color = InkSoft,
                            )
                        }
                        if (showMeaningJa && word.meaningJa.isNotBlank()) {
                            NonInteractiveJapaneseText(
                                text = word.meaningJa,
                                currentWordId = word.id,
                                allWords = allWords,
                                baseStyle = meaningJaBaseStyle,
                                rubyStyle = meaningJaRubyStyle,
                                baseColor = InkSoft,
                                rubyColor = SecondaryCoral,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WordDictionaryHeader(
    word: WordEntity,
    showReadingKo: Boolean,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = PaperElevated),
        border = BorderStroke(1.25.dp, CardBorderStrong),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(0.46f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = word.partOfSpeech.ifBlank { word.tag.ifBlank { "단어" } },
                    style = MaterialTheme.typography.labelMedium,
                    color = PrimaryBlue,
                )
                RubyFieldText(
                    word = word,
                    field = WordField.KANJI,
                    mainStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    rubyStyle = MaterialTheme.typography.labelMedium,
                    rubyColor = SecondaryCoral,
                )
                if (showReadingKo && word.readingKo.isNotBlank()) {
                    Text(word.readingKo, style = MaterialTheme.typography.bodySmall, color = InkMuted)
                }
            }
            Column(
                modifier = Modifier.weight(0.54f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(word.meaningKo, style = MaterialTheme.typography.titleMedium, color = InkSoft)
                if (word.meaningJa.isNotBlank()) {
                    Text(word.meaningJa, style = MaterialTheme.typography.bodyMedium, color = InkMuted)
                }
            }
        }
    }
}

@Composable
private fun DetailSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, DividerSoft),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                content()
            }
        }
    }
}

@Composable
private fun DictionaryMetaRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(0.32f),
            style = MaterialTheme.typography.labelLarge,
            color = InkMuted,
        )
        Text(
            text = value,
            modifier = Modifier.weight(0.68f),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LinkedJapaneseText(
    text: String,
    currentWordId: Long,
    allWords: List<WordEntity>,
    onOpenWord: (Long) -> Unit,
) {
    RubySentenceText(
        text = text,
        currentWordId = currentWordId,
        allWords = allWords,
        baseStyle = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            lineHeight = 22.sp,
            letterSpacing = (-0.15).sp,
        ),
        rubyStyle = MaterialTheme.typography.labelSmall.copy(
            fontSize = 10.sp,
            lineHeight = 10.sp,
            letterSpacing = (-0.1).sp,
        ),
        baseColor = MaterialTheme.colorScheme.onSurface,
        rubyColor = SecondaryCoral,
        onOpenWord = onOpenWord,
    )
}

@Composable
private fun NonInteractiveJapaneseText(
    text: String,
    currentWordId: Long,
    allWords: List<WordEntity>,
    baseStyle: TextStyle,
    rubyStyle: TextStyle,
    baseColor: Color,
    rubyColor: Color,
) {
    RubySentenceText(
        text = text,
        currentWordId = currentWordId,
        allWords = allWords,
        baseStyle = baseStyle,
        rubyStyle = rubyStyle,
        baseColor = baseColor,
        rubyColor = rubyColor,
        onOpenWord = null,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RubySentenceText(
    text: String,
    currentWordId: Long,
    allWords: List<WordEntity>,
    baseStyle: TextStyle,
    rubyStyle: TextStyle,
    baseColor: Color,
    rubyColor: Color,
    onOpenWord: ((Long) -> Unit)?,
) {
    val segments = remember(text, currentWordId, allWords) {
        buildLinkedSegments(
            text = text,
            currentWordId = currentWordId,
            allWords = allWords,
        )
    }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        segments.forEach { segment ->
            when (segment) {
                is LinkedSegment.Plain -> SentencePlainSegment(
                    text = segment.text,
                    baseStyle = baseStyle,
                    rubyStyle = rubyStyle,
                    baseColor = baseColor,
                )

                is LinkedSegment.WordMatch -> {
                    val modifier = if (onOpenWord != null) {
                        Modifier.clickable(enabled = segment.word.id != currentWordId) {
                            if (segment.word.id != currentWordId) {
                                onOpenWord(segment.word.id)
                            }
                        }
                    } else {
                        Modifier
                    }
                    Column(
                        modifier = modifier,
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        InlineRubyText(
                            displayText = segment.displayText,
                            readingText = segment.word.readingJa,
                            baseStyle = baseStyle,
                            rubyStyle = rubyStyle,
                            baseColor = if (onOpenWord == null || segment.word.id == currentWordId) baseColor else PrimaryBlue,
                            rubyColor = rubyColor,
                        )
                    }
                }
            }
        }
    }
}

private sealed interface LinkedSegment {
    data class Plain(val text: String) : LinkedSegment
    data class WordMatch(val displayText: String, val word: WordEntity) : LinkedSegment
}

private fun buildLinkedSegments(
    text: String,
    currentWordId: Long,
    allWords: List<WordEntity>,
): List<LinkedSegment> {
    if (text.isBlank()) return emptyList()

    val candidates = allWords
        .flatMap { word ->
            buildList {
                val kanji = word.kanji.trim()
                val reading = word.readingJa.trim()
                if (kanji.isNotBlank()) add(kanji to word)
                if (reading.isNotBlank() && reading != kanji) add(reading to word)
            }
        }
        .distinctBy { it.first to it.second.id }
        .sortedWith(
            compareByDescending<Pair<String, WordEntity>> { it.first.length }
                .thenByDescending { it.second.id == currentWordId }
        )

    val segments = mutableListOf<LinkedSegment>()
    var index = 0
    while (index < text.length) {
        val match = candidates.firstOrNull { (token, _) ->
            token.isNotBlank() && text.regionMatches(index, token, 0, token.length)
        }
        if (match != null) {
            segments += LinkedSegment.WordMatch(
                displayText = match.first,
                word = match.second,
            )
            index += match.first.length
        } else {
            segments += LinkedSegment.Plain(text[index].toString())
            index += 1
        }
    }
    return segments
}

@Composable
private fun InlineRubyText(
    displayText: String,
    readingText: String,
    baseStyle: TextStyle,
    rubyStyle: TextStyle,
    baseColor: Color,
    rubyColor: Color,
) {
    val parts = remember(displayText, readingText) {
        splitRubyToken(displayText, readingText)
    }
    val rubySlotHeight = rememberRubySlotHeight(rubyStyle)

    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        parts.forEach { part ->
            when (part) {
                is RubyInlinePart.Plain -> SentencePlainSegment(
                    text = part.text,
                    baseStyle = baseStyle,
                    rubyStyle = rubyStyle,
                    baseColor = baseColor,
                )

                is RubyInlinePart.Annotated -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    Box(
                        modifier = Modifier.height(rubySlotHeight),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Text(
                            text = part.reading,
                            style = rubyStyle,
                            color = rubyColor,
                        )
                    }
                    Text(
                        text = part.base,
                        style = baseStyle,
                        color = baseColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun SentencePlainSegment(
    text: String,
    baseStyle: TextStyle,
    rubyStyle: TextStyle,
    baseColor: Color,
) {
    val rubySlotHeight = rememberRubySlotHeight(rubyStyle)

    Column(
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Box(modifier = Modifier.height(rubySlotHeight))
        Text(
            text = text,
            style = baseStyle,
            color = baseColor,
        )
    }
}

@Composable
private fun rememberRubySlotHeight(rubyStyle: TextStyle) = with(LocalDensity.current) {
    val baseHeight: TextUnit = if (rubyStyle.lineHeight != TextUnit.Unspecified) {
        rubyStyle.lineHeight
    } else {
        rubyStyle.fontSize
    }
    (baseHeight * 1.45f).toDp()
}

private sealed interface RubyInlinePart {
    data class Plain(val text: String) : RubyInlinePart
    data class Annotated(val base: String, val reading: String) : RubyInlinePart
}

private data class RubyMatch(
    val baseStart: Int,
    val baseEndExclusive: Int,
    val readingStart: Int,
    val readingEndExclusive: Int,
)

private fun splitRubyToken(
    displayText: String,
    readingText: String,
): List<RubyInlinePart> {
    if (displayText.isBlank() || readingText.isBlank()) {
        return listOf(RubyInlinePart.Plain(displayText))
    }

    var left = 0
    val maxPrefix = minOf(displayText.length, readingText.length)
    while (
        left < maxPrefix &&
        displayText[left] == readingText[left] &&
        !displayText[left].isKanji()
    ) {
        left += 1
    }

    var displayRight = displayText.length
    var readingRight = readingText.length
    while (
        displayRight > left &&
        readingRight > left &&
        displayText[displayRight - 1] == readingText[readingRight - 1] &&
        !displayText[displayRight - 1].isKanji()
    ) {
        displayRight -= 1
        readingRight -= 1
    }

    val prefix = displayText.substring(0, left)
    val coreBase = displayText.substring(left, displayRight)
    val coreReading = readingText.substring(left, readingRight)
    val suffix = displayText.substring(displayRight)

    if (coreBase.isBlank() || coreReading.isBlank() || !coreBase.any { it.isKanji() }) {
        return listOf(RubyInlinePart.Plain(displayText))
    }

    val segmentedCore = splitRubyCore(coreBase, coreReading)
    return buildList {
        if (prefix.isNotEmpty()) add(RubyInlinePart.Plain(prefix))
        addAll(segmentedCore)
        if (suffix.isNotEmpty()) add(RubyInlinePart.Plain(suffix))
    }
}

private fun Char.isKana(): Boolean =
    this in '\u3040'..'\u309f' || this in '\u30a0'..'\u30ff'

private fun Char.isKanji(): Boolean =
    this in '\u4e00'..'\u9fff' || this in '\u3400'..'\u4dbf'

private fun splitRubyCore(
    baseText: String,
    readingText: String,
): List<RubyInlinePart> {
    if (baseText.all(Char::isKanji)) {
        return listOf(RubyInlinePart.Annotated(baseText, readingText))
    }

    val matches = mutableListOf<RubyMatch>()
    val found = matchRubySegments(baseText, readingText, 0, 0, matches)
    if (!found || matches.isEmpty()) {
        return listOf(RubyInlinePart.Annotated(baseText, readingText))
    }

    val sortedMatches = matches.sortedBy { it.baseStart }
    return buildList {
        var baseCursor = 0
        sortedMatches.forEach { match ->
            if (match.baseStart > baseCursor) {
                add(RubyInlinePart.Plain(baseText.substring(baseCursor, match.baseStart)))
            }
            add(
                RubyInlinePart.Annotated(
                    base = baseText.substring(match.baseStart, match.baseEndExclusive),
                    reading = readingText.substring(match.readingStart, match.readingEndExclusive),
                )
            )
            baseCursor = match.baseEndExclusive
        }
        if (baseCursor < baseText.length) {
            add(RubyInlinePart.Plain(baseText.substring(baseCursor)))
        }
    }
}

private fun matchRubySegments(
    baseText: String,
    readingText: String,
    baseIndex: Int,
    readingIndex: Int,
    matches: MutableList<RubyMatch>,
): Boolean {
    if (baseIndex == baseText.length && readingIndex == readingText.length) {
        return true
    }
    if (baseIndex >= baseText.length || readingIndex > readingText.length) {
        return false
    }

    val currentChar = baseText[baseIndex]
    if (!currentChar.isKanji()) {
        if (readingIndex >= readingText.length || currentChar != readingText[readingIndex]) {
            return false
        }
        return matchRubySegments(baseText, readingText, baseIndex + 1, readingIndex + 1, matches)
    }

    val nextBaseIndex = baseIndex + 1
    var nextLiteralChar: Char? = null
    for (i in nextBaseIndex until baseText.length) {
        if (!baseText[i].isKanji()) {
            nextLiteralChar = baseText[i]
            break
        }
    }

    val maxReadingEndExclusive = if (nextLiteralChar == null) {
        readingText.length
    } else {
        val candidate = readingText.indexOf(nextLiteralChar, readingIndex)
        if (candidate == -1) return false
        candidate
    }

    for (readingEndExclusive in (readingIndex + 1)..maxReadingEndExclusive) {
        matches.add(
            RubyMatch(
                baseStart = baseIndex,
                baseEndExclusive = baseIndex + 1,
                readingStart = readingIndex,
                readingEndExclusive = readingEndExclusive,
            )
        )
        if (matchRubySegments(baseText, readingText, nextBaseIndex, readingEndExclusive, matches)) {
            return true
        }
        matches.removeAt(matches.lastIndex)
    }
    return false
}

@Composable
private fun EditorField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
    )
}

@Composable
private fun <T> SettingGroup(
    title: String,
    selectedLabel: String,
    options: List<Pair<T, String>>,
    current: T,
    onSelect: (T) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PaperElevated),
        border = BorderStroke(1.dp, DividerSoft),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "현재: $selectedLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted,
                )
            }
            options.chunked(2).forEach { rowOptions ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowOptions.forEach { (value, label) ->
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .selectable(selected = current == value, onClick = { onSelect(value) })
                                .padding(horizontal = 2.dp, vertical = 0.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = current == value, onClick = { onSelect(value) })
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    if (rowOptions.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun FilterBottomSheetContent(
    onReset: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("필터", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = onReset) {
                Text("초기화")
            }
        }
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterChipRow(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = InkMuted)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = selected == option,
                    onClick = { onSelect(option) },
                    label = { Text(option) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.secondary,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected == option,
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        selectedBorderColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterChipIdRow(
    title: String,
    options: List<Pair<Long, String>>,
    selected: Long,
    onSelect: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = InkMuted)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { (id, label) ->
                FilterChip(
                    selected = selected == id,
                    onClick = { onSelect(id) },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.secondary,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected == id,
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        selectedBorderColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                )
            }
        }
    }
}

@Composable
private fun OptionCheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

private fun buildFilterSummary(
    filteredCount: Int,
    totalCount: Int,
    selectedPartOfSpeech: String,
    selectedTag: String,
): String {
    val filters = buildList {
        if (selectedPartOfSpeech != "전체") add("품사 $selectedPartOfSpeech")
        if (selectedTag != "전체") add("태그 $selectedTag")
    }
    return if (filters.isEmpty()) {
        "표시 $filteredCount / 전체 $totalCount"
    } else {
        "표시 $filteredCount / 전체 $totalCount · ${filters.joinToString(" · ")}"
    }
}

private fun buildAllWordsFilterSummary(
    filteredCount: Int,
    totalCount: Int,
    selectedDeckName: String,
    selectedPartOfSpeech: String,
    selectedTag: String,
): String {
    val filters = buildList {
        if (selectedDeckName != "전체 단어장") add("단어장 $selectedDeckName")
        if (selectedPartOfSpeech != "전체") add("품사 $selectedPartOfSpeech")
        if (selectedTag != "전체") add("태그 $selectedTag")
    }
    return if (filters.isEmpty()) {
        "표시 $filteredCount / 전체 $totalCount"
    } else {
        "표시 $filteredCount / 전체 $totalCount · ${filters.joinToString(" · ")}"
    }
}

private fun WordEntity.matchesSearchQuery(query: String): Boolean {
    val normalizedQuery = query.trim().lowercase()
    if (normalizedQuery.isBlank()) return true
    return listOf(
        kanji,
        readingJa,
        readingKo,
        meaningKo,
        meaningJa,
        tag,
        partOfSpeech,
        grammar,
        note,
    ).any { it.lowercase().contains(normalizedQuery) }
}

private fun orderLabel(order: WordOrder): String = when (order) {
    WordOrder.SEQUENTIAL -> "순차"
    WordOrder.RANDOM -> "무작위"
}

private fun fieldLabel(field: WordField): String = when (field) {
    WordField.KANJI -> "한자"
    WordField.READING_JA -> "읽는 방법(일본어)"
    WordField.READING_KO -> "읽는 방법(한국어)"
    WordField.MEANING_KO -> "뜻(한국어)"
}

private fun displayField(word: WordEntity, field: WordField): String = when (field) {
    WordField.KANJI -> word.kanji
    WordField.READING_JA -> word.readingJa
    WordField.READING_KO -> word.readingKo
    WordField.MEANING_KO -> word.meaningKo
}

@Composable
private fun RubyFieldText(
    word: WordEntity,
    field: WordField,
    mainStyle: TextStyle,
    rubyStyle: TextStyle,
    modifier: Modifier = Modifier,
    showRuby: Boolean = true,
    alignCenter: Boolean = false,
    rubyColor: Color = SecondaryCoral,
) {
    val mainText = displayField(word, field)
    val rubyText = rubyTextFor(word, field).takeIf { showRuby }

    Box(
        modifier = modifier.then(
            if (alignCenter) Modifier.fillMaxWidth() else Modifier.wrapContentWidth(),
        ),
        contentAlignment = if (alignCenter) Alignment.Center else Alignment.CenterStart,
    ) {
        if (rubyText == null) {
            Column(
                horizontalAlignment = if (alignCenter) Alignment.CenterHorizontally else Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                Text(
                    text = " ",
                    style = rubyStyle,
                    color = Color.Transparent,
                    textAlign = if (alignCenter) TextAlign.Center else TextAlign.Start,
                )
                Text(
                    text = mainText,
                    style = mainStyle,
                    textAlign = if (alignCenter) TextAlign.Center else TextAlign.Start,
                )
            }
        } else {
            InlineRubyText(
                displayText = mainText,
                readingText = rubyText,
                baseStyle = mainStyle,
                rubyStyle = rubyStyle,
                baseColor = LocalContentColor.current,
                rubyColor = rubyColor,
            )
        }
    }
}

private fun rubyTextFor(word: WordEntity, field: WordField): String? = when (field) {
    WordField.KANJI -> word.readingJa.takeIf { it.isNotBlank() && word.kanji.any(Char::isKanji) }
    WordField.READING_JA -> null
    WordField.READING_KO -> null
    WordField.MEANING_KO -> null
}
