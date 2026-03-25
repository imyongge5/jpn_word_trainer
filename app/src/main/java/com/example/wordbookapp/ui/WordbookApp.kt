package com.example.wordbookapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
                    onEditWord = { wordId -> navController.navigate("word_editor/$deckId?wordId=$wordId") },
                    onStartExam = { navController.navigate("exam_setup?deckId=$deckId") },
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
                ) {
                    Text("생성")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
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
    onEditWord: (Long) -> Unit,
    onStartExam: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ScreenContainer(
        title = uiState.deck?.name ?: "단어장",
        onBack = onBack,
    ) {
        if (uiState.isLoading || uiState.deck == null) {
            LoadingView()
            return@ScreenContainer
        }
        val deck = uiState.deck ?: return@ScreenContainer
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(deck.description, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAddWord) { Text("단어 추가") }
                Button(onClick = onStartExam, enabled = uiState.words.isNotEmpty()) { Text("시험 시작") }
            }
            HorizontalDivider()
            if (uiState.words.isEmpty()) {
                EmptyHint("이 단어장에는 아직 단어가 없어요.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(uiState.words) { word ->
                        WordRow(word = word, onClick = { onEditWord(word.id) })
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
            Button(onClick = { viewModel.save() }) {
                Text(if (uiState.isEditMode) "수정 저장" else "단어 저장")
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
            Button(onClick = onGoHome) {
                Text("홈으로 돌아가기")
            }
        }
    }
}

@Composable
private fun ScreenContainer(
    title: String,
    onBack: (() -> Unit)? = null,
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
            if (onBack != null) {
                TextButton(onClick = onBack) { Text("뒤로") }
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
                    Button(onClick = onCreateCustomDeck) { Text("커스텀 단어장") }
                    OutlinedButton(onClick = onOpenAiDeck) { Text("AI 단어장") }
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
private fun WordRow(word: WordEntity, onClick: () -> Unit) {
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
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
                Text(
                    text = word.meaningKo,
                    style = MaterialTheme.typography.bodyLarge,
                    color = InkSoft,
                )
                if (word.meaningJa.isNotBlank()) {
                    Text(
                        text = word.meaningJa,
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkMuted,
                    )
                }
                if (word.readingKo.isNotBlank()) {
                    Text(
                        text = word.readingKo,
                        style = MaterialTheme.typography.labelMedium,
                        color = InkMuted,
                    )
                }
            }
        }
    }
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
