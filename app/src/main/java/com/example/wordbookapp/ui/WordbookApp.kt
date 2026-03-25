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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
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
import com.example.wordbookapp.data.model.WordField
import com.example.wordbookapp.data.model.WordOrder
import com.example.wordbookapp.data.repository.WordbookRepository
import com.example.wordbookapp.ui.theme.DividerSoft
import com.example.wordbookapp.ui.theme.InkMuted
import com.example.wordbookapp.ui.theme.InkSoft
import com.example.wordbookapp.ui.theme.CardBorderStrong
import com.example.wordbookapp.ui.theme.PaperElevated
import com.example.wordbookapp.ui.theme.PrimaryBlue
import com.example.wordbookapp.ui.theme.PrimaryBlueSoft
import com.example.wordbookapp.ui.theme.SecondaryCoral
import com.example.wordbookapp.ui.theme.SecondaryCoralSoft

@Composable
fun WordbookApp(
    repository: WordbookRepository,
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
                    onOpenDeck = { deckId -> navController.navigate("deck/$deckId") },
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
    onOpenDeck: (Long) -> Unit,
    onOpenAiDeck: () -> Unit,
    onDeckCreated: (Long) -> Unit,
    onOpenAllWords: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    var deckName by remember { mutableStateOf("") }
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

    ScreenContainer(title = "일본어 단어장") {
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
                    onCreateCustomDeck = { showDialog = true },
                    onOpenAiDeck = onOpenAiDeck,
                    onOpenAllWords = onOpenAllWords,
                )
            }
            item {
                SectionTitle("JLPT 기본 단어장")
            }
            items(data.jlptDecks) { deck ->
                DeckCard(deck = deck, onClick = { onOpenDeck(deck.id) })
            }
            item {
                SectionTitle("커스텀 단어장")
            }
            if (data.customDecks.isEmpty()) {
                item {
                    EmptyHint("아직 만든 커스텀 단어장이 없어요.")
                }
            } else {
                items(data.customDecks) { deck ->
                    DeckCard(deck = deck, onClick = { onOpenDeck(deck.id) })
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

@Composable
private fun DeckRoute(
    viewModel: DeckDetailViewModel,
    onBack: () -> Unit,
    onAddWord: () -> Unit,
    onOpenWord: (Long) -> Unit,
    onStartExam: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var showReadingKo by rememberSaveable { mutableStateOf(false) }
    var showMeaningJa by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedPartOfSpeech by rememberSaveable { mutableStateOf("전체") }
    var selectedTag by rememberSaveable { mutableStateOf("전체") }
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text("필터", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    OutlinedButton(
                        onClick = {
                            selectedPartOfSpeech = "전체"
                            selectedTag = "전체"
                        },
                    ) {
                        Text("필터 초기화")
                    }
                    if (!uiState.isLoading && uiState.deck != null) {
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
        }
    ) {
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onAddWord,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                ) { Text("단어 추가") }
                Button(
                    onClick = onStartExam,
                    enabled = uiState.words.isNotEmpty(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                ) { Text("시험 시작") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { showReadingKo = !showReadingKo },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(if (showReadingKo) "한국어 읽기 숨기기" else "한국어 읽기 보기")
                }
                OutlinedButton(
                    onClick = { showMeaningJa = !showMeaningJa },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(if (showMeaningJa) "뜻을 한국어로" else "뜻을 일본어로")
                }
                OutlinedButton(
                    onClick = { scope.launch { drawerState.open() } },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text("필터")
                }
                }
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("단어 검색") },
                    placeholder = { Text("한자, 읽기, 뜻, 태그로 찾기") },
                    singleLine = true,
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
                if (uiState.words.isEmpty()) {
                    EmptyHint("이 단어장에는 아직 단어가 없어요.")
                } else if (filteredWords.isEmpty()) {
                    EmptyHint("조건에 맞는 단어가 없어요.")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(filteredWords) { word ->
                            WordRow(
                                word = word,
                                showReadingKo = showReadingKo,
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
}

@Composable
private fun AllWordsRoute(
    viewModel: AllWordsViewModel,
    onBack: () -> Unit,
    onOpenWord: (Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var showReadingKo by rememberSaveable { mutableStateOf(false) }
    var showMeaningJa by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedDeckId by rememberSaveable { mutableStateOf(-1L) }
    var selectedPartOfSpeech by rememberSaveable { mutableStateOf("전체") }
    var selectedTag by rememberSaveable { mutableStateOf("전체") }

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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text("필터", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    OutlinedButton(
                        onClick = {
                            selectedDeckId = -1L
                            selectedPartOfSpeech = "전체"
                            selectedTag = "전체"
                        },
                    ) {
                        Text("필터 초기화")
                    }
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
        },
    ) {
        ScreenContainer(
            title = "모든 단어",
            onBack = onBack,
        ) {
            if (uiState.isLoading) {
                LoadingView()
                return@ScreenContainer
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showReadingKo = !showReadingKo },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text(if (showReadingKo) "한국어 읽기 숨기기" else "한국어 읽기 보기")
                    }
                    OutlinedButton(
                        onClick = { showMeaningJa = !showMeaningJa },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text(if (showMeaningJa) "뜻을 한국어로" else "뜻을 일본어로")
                    }
                    OutlinedButton(
                        onClick = { scope.launch { drawerState.open() } },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text("필터")
                    }
                }
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("전체 단어 검색") },
                    placeholder = { Text("한자, 읽기, 뜻, 태그로 찾기") },
                    singleLine = true,
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
                if (uiState.words.isEmpty()) {
                    EmptyHint("등록된 단어가 아직 없어요.")
                } else if (filteredWords.isEmpty()) {
                    EmptyHint("조건에 맞는 단어가 없어요.")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(filteredWords) { word ->
                            WordRow(
                                word = word,
                                showReadingKo = showReadingKo,
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
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(uiState.deck?.name ?: "AI가 자주 틀린 단어와 새 단어를 섞어 30문제를 구성합니다.")
            SettingGroup(
                title = "출제 순서",
                selectedLabel = uiState.settings.wordOrder.name,
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
            Button(
                onClick = {
                    scope.launch {
                        onStartExam(viewModel.startExam())
                    }
                },
                enabled = uiState.canStart,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text("시험 시작")
            }
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
                        alignCenter = true,
                    )
                    if (uiState.revealed) {
                        RubyFieldText(
                            word = currentWord,
                            field = sessionData.session.revealField,
                            mainStyle = MaterialTheme.typography.titleLarge,
                            rubyStyle = MaterialTheme.typography.labelMedium,
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                actions?.invoke()
                if (onBack != null) {
                    TextButton(
                        onClick = onBack,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    ) { Text("뒤로") }
                }
            }
        }
        content()
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
private fun SummaryCard(
    totalWords: Int,
    recentSessionCount: Int,
    onCreateCustomDeck: () -> Unit,
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onCreateCustomDeck,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    ) { Text("커스텀 단어장") }
                    OutlinedButton(
                        onClick = onOpenAiDeck,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    ) { Text("AI 단어장") }
                }
                OutlinedButton(
                    onClick = onOpenAllWords,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) { Text("모든 단어 보기") }
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
private fun DeckCard(deck: DeckWithCount, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.25.dp, CardBorderStrong),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
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
    }
}

@Composable
private fun WordRow(
    word: WordEntity,
    showReadingKo: Boolean,
    showMeaningJa: Boolean,
    allWords: List<WordEntity>,
    onClick: () -> Unit,
) {
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
                    modifier = Modifier.weight(0.42f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = word.partOfSpeech.ifBlank { word.tag.ifBlank { "단어" } },
                        style = MaterialTheme.typography.labelMedium,
                        color = PrimaryBlue,
                    )
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
                Column(
                    modifier = Modifier.weight(0.58f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = if (showMeaningJa) "뜻(일본어)" else "뜻(한국어)",
                        style = MaterialTheme.typography.labelMedium,
                        color = InkMuted,
                    )
                    if (showMeaningJa && word.meaningJa.isNotBlank()) {
                        NonInteractiveJapaneseText(
                            text = word.meaningJa,
                            currentWordId = word.id,
                            allWords = allWords,
                            baseStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                lineHeight = 20.sp,
                                letterSpacing = (-0.1).sp,
                            ),
                            rubyStyle = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                lineHeight = 9.sp,
                                letterSpacing = (-0.1).sp,
                            ),
                            baseColor = InkSoft,
                            rubyColor = SecondaryCoral,
                        )
                    } else {
                        Text(
                            text = word.meaningKo,
                            style = MaterialTheme.typography.bodyLarge,
                            color = InkSoft,
                        )
                    }
                    val secondaryMeaning = if (showMeaningJa) word.meaningKo else word.meaningJa
                    if (secondaryMeaning.isNotBlank()) {
                        Text(
                            text = secondaryMeaning,
                            style = MaterialTheme.typography.bodySmall,
                            color = InkMuted,
                        )
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
                    Text(
                        text = part.reading,
                        style = rubyStyle,
                        color = rubyColor,
                    )
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
    Column(
        verticalArrangement = Arrangement.spacedBy((-1).dp),
    ) {
        Text(
            text = " ",
            style = rubyStyle,
            color = Color.Transparent,
        )
        Text(
            text = text,
            style = baseStyle,
            color = baseColor,
        )
    }
}

private sealed interface RubyInlinePart {
    data class Plain(val text: String) : RubyInlinePart
    data class Annotated(val base: String, val reading: String) : RubyInlinePart
}

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
        displayText[left].isKana()
    ) {
        left += 1
    }

    var displayRight = displayText.length
    var readingRight = readingText.length
    while (
        displayRight > left &&
        readingRight > left &&
        displayText[displayRight - 1] == readingText[readingRight - 1] &&
        displayText[displayRight - 1].isKana()
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

    return buildList {
        if (prefix.isNotEmpty()) add(RubyInlinePart.Plain(prefix))
        add(RubyInlinePart.Annotated(coreBase, coreReading))
        if (suffix.isNotEmpty()) add(RubyInlinePart.Plain(suffix))
    }
}

private fun Char.isKana(): Boolean =
    this in '\u3040'..'\u309f' || this in '\u30a0'..'\u30ff'

private fun Char.isKanji(): Boolean =
    this in '\u4e00'..'\u9fff' || this in '\u3400'..'\u4dbf'

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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontWeight = FontWeight.Bold)
        Text("현재: $selectedLabel")
        options.forEach { (value, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = current == value, onClick = { onSelect(value) }),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = current == value, onClick = { onSelect(value) })
                Text(label)
            }
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium)
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
                )
            }
        }
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
    alignCenter: Boolean = false,
    rubyColor: Color = SecondaryCoral,
) {
    val mainText = displayField(word, field)
    val rubyText = rubyTextFor(word, field)

    if (rubyText == null) {
        Text(
            text = mainText,
            modifier = modifier,
            style = mainStyle,
            textAlign = if (alignCenter) TextAlign.Center else TextAlign.Start,
        )
        return
    }

    Column(
        modifier = modifier.then(
            if (alignCenter) Modifier.fillMaxWidth() else Modifier.wrapContentWidth(),
        ),
        horizontalAlignment = if (alignCenter) Alignment.CenterHorizontally else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(
            text = rubyText,
            style = rubyStyle,
            color = rubyColor,
            textAlign = if (alignCenter) TextAlign.Center else TextAlign.Start,
        )
        Text(
            text = mainText,
            style = mainStyle,
            textAlign = if (alignCenter) TextAlign.Center else TextAlign.Start,
        )
    }
}

private fun rubyTextFor(word: WordEntity, field: WordField): String? = when (field) {
    WordField.KANJI -> word.readingJa.takeIf { it.isNotBlank() }
    WordField.READING_JA -> null
    WordField.READING_KO -> null
    WordField.MEANING_KO -> null
}
