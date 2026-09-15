package com.example.ui.study.calculator

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.data.local.CalculatorHistoryEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScientificCalculatorScreen(
    navController: NavController,
    viewModel: CalculatorViewModel = viewModel()
) {
    val expression by viewModel.expression.collectAsState()
    val resultPreview by viewModel.resultPreview.collectAsState()
    val isDegreeMode by viewModel.isDegreeMode.collectAsState()
    val isMemoryActive by viewModel.isMemoryActive.collectAsState()
    val memoryValue by viewModel.memoryValue.collectAsState()
    val historyList by viewModel.history.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var showHistorySheet by remember { mutableStateOf(false) }
    var showScientificToggle by remember { mutableStateOf(false) }

    // Infinite blinking transition for typing cursor
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )

    // Layout configuration depending on size constraints
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight
        val scaffoldContainerColor = MaterialTheme.colorScheme.background

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Scientific Calculator", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            navController.popBackStack()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        // Undo
                        IconButton(onClick = { 
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.undo() 
                        }) {
                            Icon(Icons.Filled.Undo, contentDescription = "Undo")
                        }
                        // Redo
                        IconButton(onClick = { 
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.redo() 
                        }) {
                            Icon(Icons.Filled.Redo, contentDescription = "Redo")
                        }
                        // History Toggle
                        IconButton(onClick = { 
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showHistorySheet = true 
                        }) {
                            Icon(Icons.Filled.History, contentDescription = "History")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = scaffoldContainerColor)
                )
            },
            containerColor = scaffoldContainerColor
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .navigationBarsPadding()
            ) {
                // 1. Display Area (top)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.End
                ) {
                    // DEG/RAD Toggle indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.toggleDegreeMode()
                            }
                        ) {
                            Text(
                                text = if (isDegreeMode) "DEG" else "RAD",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }

                        if (isMemoryActive) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Text(
                                    text = "M (${MathEvaluator.formatResult(memoryValue)})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        // Copy / Share buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (expression.isNotEmpty()) {
                                    clipboardManager.setText(AnnotatedString(expression))
                                    Toast.makeText(context, "Expression Copied", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy Expression", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val resultToShare = resultPreview.ifEmpty { expression }
                                if (resultToShare.isNotEmpty()) {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, "Calculation: $expression = $resultToShare")
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share Result"))
                                }
                            }) {
                                Icon(Icons.Filled.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Expression display (scrollable horizontally)
                    val expressionScrollState = rememberScrollState()
                    LaunchedEffect(expression) {
                        expressionScrollState.animateScrollTo(expressionScrollState.maxValue)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(expressionScrollState),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = expression.ifEmpty { "0" },
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = if (expression.length > 15) 30.sp else 40.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.End
                        )
                        // Blinking cursor
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(40.dp)
                                .alpha(cursorAlpha)
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(start = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Real-time Preview Result
                    if (resultPreview.isNotEmpty()) {
                        Text(
                            text = "= $resultPreview",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontFamily = FontFamily.Monospace
                            ),
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

                // 2. Keyboard Area
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isLandscape) {
                        LandscapeKeyboard(viewModel, haptic)
                    } else {
                        // In portrait, show option scrollable Scientific tray on top, and standard numbers at bottom
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ScientificFunctionChip("sin", viewModel, haptic)
                            ScientificFunctionChip("cos", viewModel, haptic)
                            ScientificFunctionChip("tan", viewModel, haptic)
                            ScientificFunctionChip("ln", viewModel, haptic)
                            ScientificFunctionChip("log", viewModel, haptic)
                            ScientificFunctionChip("√", viewModel, haptic)
                            ScientificFunctionChip("π", viewModel, haptic)
                            ScientificFunctionChip("e", viewModel, haptic)
                            ScientificFunctionChip("^", viewModel, haptic)
                            ScientificFunctionChip("!", viewModel, haptic)
                            ScientificFunctionChip("mod", viewModel, haptic)
                        }

                        PortraitStandardKeyboard(viewModel, haptic, showScientificToggle) {
                            showScientificToggle = !showScientificToggle
                        }
                    }
                }
            }
        }
    }

    // Modern slide-up bottom history sheet
    if (showHistorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Calculation History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (historyList.isNotEmpty()) {
                        TextButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.clearAllHistory()
                        }) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear All")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search history
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search operations...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (historyList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No history available", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(historyList, key = { it.id }) { item ->
                            HistoryRow(item, onSelect = {
                                viewModel.clear()
                                viewModel.append(item.expression)
                                showHistorySheet = false
                            }, onDelete = {
                                viewModel.deleteHistoryItem(item.id)
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScientificFunctionChip(label: String, viewModel: CalculatorViewModel, haptic: HapticFeedback) {
    InputChip(
        selected = false,
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.append(label)
        },
        label = { Text(label, fontWeight = FontWeight.Bold) },
        colors = InputChipDefaults.inputChipColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
            labelColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    )
}

@Composable
fun PortraitStandardKeyboard(
    viewModel: CalculatorViewModel,
    haptic: HapticFeedback,
    isScientificExpanded: Boolean,
    onScientificToggle: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (isScientificExpanded) {
            // Expanded extra Scientific Row
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                CalculatorButton("asin", ButtonType.Scientific, Modifier.weight(1f)) { viewModel.append("asin") }
                CalculatorButton("acos", ButtonType.Scientific, Modifier.weight(1f)) { viewModel.append("acos") }
                CalculatorButton("atan", ButtonType.Scientific, Modifier.weight(1f)) { viewModel.append("atan") }
                CalculatorButton("sinh", ButtonType.Scientific, Modifier.weight(1f)) { viewModel.append("sinh") }
                CalculatorButton("cosh", ButtonType.Scientific, Modifier.weight(1f)) { viewModel.append("cosh") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                CalculatorButton("tanh", ButtonType.Scientific, Modifier.weight(1f)) { viewModel.append("tanh") }
                CalculatorButton("abs", ButtonType.Scientific, Modifier.weight(1f)) { viewModel.append("abs") }
                CalculatorButton("exp", ButtonType.Scientific, Modifier.weight(1f)) { viewModel.append("exp") }
                CalculatorButton("³√", ButtonType.Scientific, Modifier.weight(1f)) { viewModel.append("³√") }
                CalculatorButton("log10", ButtonType.Scientific, Modifier.weight(1f)) { viewModel.append("log10") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                CalculatorButton("MC", ButtonType.Memory, Modifier.weight(1f)) { viewModel.memoryClear() }
                CalculatorButton("MR", ButtonType.Memory, Modifier.weight(1f)) { viewModel.memoryRecall() }
                CalculatorButton("MS", ButtonType.Memory, Modifier.weight(1f)) { viewModel.memoryStore() }
                CalculatorButton("M+", ButtonType.Memory, Modifier.weight(1f)) { viewModel.memoryAdd() }
                CalculatorButton("M-", ButtonType.Memory, Modifier.weight(1f)) { viewModel.memorySubtract() }
            }
        }

        // Keypad Rows
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            CalculatorButton("C", ButtonType.Action, Modifier.weight(1f)) { viewModel.clear() }
            CalculatorButton("()", ButtonType.Action, Modifier.weight(1f)) { 
                // Context-aware parenthesis entry
                viewModel.append("(") 
            }
            CalculatorButton("%", ButtonType.Action, Modifier.weight(1f)) { viewModel.append("%") }
            CalculatorButton("÷", ButtonType.Operator, Modifier.weight(1f)) { viewModel.append("÷") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            CalculatorButton("7", ButtonType.Number, Modifier.weight(1f)) { viewModel.append("7") }
            CalculatorButton("8", ButtonType.Number, Modifier.weight(1f)) { viewModel.append("8") }
            CalculatorButton("9", ButtonType.Number, Modifier.weight(1f)) { viewModel.append("9") }
            CalculatorButton("×", ButtonType.Operator, Modifier.weight(1f)) { viewModel.append("×") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            CalculatorButton("4", ButtonType.Number, Modifier.weight(1f)) { viewModel.append("4") }
            CalculatorButton("5", ButtonType.Number, Modifier.weight(1f)) { viewModel.append("5") }
            CalculatorButton("6", ButtonType.Number, Modifier.weight(1f)) { viewModel.append("6") }
            CalculatorButton("-", ButtonType.Operator, Modifier.weight(1f)) { viewModel.append("-") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            CalculatorButton("1", ButtonType.Number, Modifier.weight(1f)) { viewModel.append("1") }
            CalculatorButton("2", ButtonType.Number, Modifier.weight(1f)) { viewModel.append("2") }
            CalculatorButton("3", ButtonType.Number, Modifier.weight(1f)) { viewModel.append("3") }
            CalculatorButton("+", ButtonType.Operator, Modifier.weight(1f)) { viewModel.append("+") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            CalculatorButton("f(x)", ButtonType.ActionSecondary, Modifier.weight(1f), isSelected = isScientificExpanded) { onScientificToggle() }
            CalculatorButton("0", ButtonType.Number, Modifier.weight(1f)) { viewModel.append("0") }
            CalculatorButton(".", ButtonType.Number, Modifier.weight(1f)) { viewModel.append(".") }
            CalculatorButton("⌫", ButtonType.Action, Modifier.weight(1f), onLongClick = { viewModel.clear() }) { viewModel.backspace() }
        }

        // Dedicated Big Equals Row to look clean
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            CalculatorButton("=", ButtonType.Equals, Modifier.fillMaxWidth()) { viewModel.calculateResult() }
        }
    }
}

@Composable
fun LandscapeKeyboard(viewModel: CalculatorViewModel, haptic: HapticFeedback) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        // Advanced scientific functions column
        Column(modifier = Modifier.weight(1.2f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalculatorButton("sin", ButtonType.Scientific, Modifier.weight(1f)) { viewModel.append("sin") }
                CalculatorButton("cos", ButtonType.Scientific, Modifier.weight(1f)) { viewModel.append("cos") }
                CalculatorButton("tan", ButtonType.Scientific, Modifier.weight(1f)) { viewModel.append("tan") }
                CalculatorButton("asin", ButtonType.Scientific, Modifier.weight(1f)) { viewModel.append("asin") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalculatorButton("sinh", ButtonType.Scientific, Modifier.weight(1f)) { viewModel.append("sinh") }
                CalculatorButton("cosh", ButtonType.Scientific, Modifier.weight(1f)) { viewModel.append("cosh") }
                CalculatorButton("tanh", ButtonType.Scientific, Modifier.weight(1f)) { viewModel.append("tanh") }
                CalculatorButton("acos", ButtonType.Scientific, Modifier.weight(1f)) { viewModel.append("acos") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalculatorButton("ln", ButtonType.Scientific, Modifier.weight(1f)) { viewModel.append("ln") }
                CalculatorButton("log", ButtonType.Scientific, Modifier.weight(1f)) { viewModel.append("log") }
                CalculatorButton("√", ButtonType.Scientific, Modifier.weight(1f)) { viewModel.append("√") }
                CalculatorButton("atan", ButtonType.Scientific, Modifier.weight(1f)) { viewModel.append("atan") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalculatorButton("π", ButtonType.Scientific, Modifier.weight(1f)) { viewModel.append("π") }
                CalculatorButton("e", ButtonType.Scientific, Modifier.weight(1f)) { viewModel.append("e") }
                CalculatorButton("^", ButtonType.Scientific, Modifier.weight(1f)) { viewModel.append("^") }
                CalculatorButton("!", ButtonType.Scientific, Modifier.weight(1f)) { viewModel.append("!") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalculatorButton("MC", ButtonType.Memory, Modifier.weight(1f)) { viewModel.memoryClear() }
                CalculatorButton("MR", ButtonType.Memory, Modifier.weight(1f)) { viewModel.memoryRecall() }
                CalculatorButton("MS", ButtonType.Memory, Modifier.weight(1f)) { viewModel.memoryStore() }
                CalculatorButton("M+", ButtonType.Memory, Modifier.weight(1f)) { viewModel.memoryAdd() }
            }
        }

        // Numbers & basic operations column
        Column(modifier = Modifier.weight(2f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalculatorButton("C", ButtonType.Action, Modifier.weight(1f)) { viewModel.clear() }
                CalculatorButton("()", ButtonType.Action, Modifier.weight(1f)) { viewModel.append("(") }
                CalculatorButton("%", ButtonType.Action, Modifier.weight(1f)) { viewModel.append("%") }
                CalculatorButton("÷", ButtonType.Operator, Modifier.weight(1f)) { viewModel.append("÷") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalculatorButton("7", ButtonType.Number, Modifier.weight(1f)) { viewModel.append("7") }
                CalculatorButton("8", ButtonType.Number, Modifier.weight(1f)) { viewModel.append("8") }
                CalculatorButton("9", ButtonType.Number, Modifier.weight(1f)) { viewModel.append("9") }
                CalculatorButton("×", ButtonType.Operator, Modifier.weight(1f)) { viewModel.append("×") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalculatorButton("4", ButtonType.Number, Modifier.weight(1f)) { viewModel.append("4") }
                CalculatorButton("5", ButtonType.Number, Modifier.weight(1f)) { viewModel.append("5") }
                CalculatorButton("6", ButtonType.Number, Modifier.weight(1f)) { viewModel.append("6") }
                CalculatorButton("-", ButtonType.Operator, Modifier.weight(1f)) { viewModel.append("-") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalculatorButton("1", ButtonType.Number, Modifier.weight(1f)) { viewModel.append("1") }
                CalculatorButton("2", ButtonType.Number, Modifier.weight(1f)) { viewModel.append("2") }
                CalculatorButton("3", ButtonType.Number, Modifier.weight(1f)) { viewModel.append("3") }
                CalculatorButton("+", ButtonType.Operator, Modifier.weight(1f)) { viewModel.append("+") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalculatorButton(".", ButtonType.Number, Modifier.weight(1f)) { viewModel.append(".") }
                CalculatorButton("0", ButtonType.Number, Modifier.weight(1f)) { viewModel.append("0") }
                CalculatorButton("⌫", ButtonType.Action, Modifier.weight(1f)) { viewModel.backspace() }
                CalculatorButton("=", ButtonType.Equals, Modifier.weight(1.0f)) { viewModel.calculateResult() }
            }
        }
    }
}

enum class ButtonType {
    Number, Operator, Action, ActionSecondary, Scientific, Memory, Equals
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalculatorButton(
    text: String,
    type: ButtonType,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    val containerColor = when (type) {
        ButtonType.Number -> MaterialTheme.colorScheme.surfaceVariant
        ButtonType.Operator -> MaterialTheme.colorScheme.primary
        ButtonType.Action -> MaterialTheme.colorScheme.secondaryContainer
        ButtonType.ActionSecondary -> if (isSelected) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer
        ButtonType.Scientific -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
        ButtonType.Memory -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        ButtonType.Equals -> MaterialTheme.colorScheme.primary
    }

    val contentColor = when (type) {
        ButtonType.Number -> MaterialTheme.colorScheme.onSurfaceVariant
        ButtonType.Operator -> MaterialTheme.colorScheme.onPrimary
        ButtonType.Action -> MaterialTheme.colorScheme.onSecondaryContainer
        ButtonType.ActionSecondary -> if (isSelected) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
        ButtonType.Scientific -> MaterialTheme.colorScheme.onTertiaryContainer
        ButtonType.Memory -> MaterialTheme.colorScheme.onSurfaceVariant
        ButtonType.Equals -> MaterialTheme.colorScheme.onPrimary
    }

    Box(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(27.dp))
            .background(containerColor)
            .combinedClickable(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                },
                onLongClick = onLongClick?.let {
                    {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        it()
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = if (text.length > 3) 14.sp else 18.sp,
                fontWeight = FontWeight.Bold
            ),
            color = contentColor
        )
    }
}

@Composable
fun HistoryRow(
    item: CalculatorHistoryEntity,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onSelect()
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.expression,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "= ${item.answer}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
            IconButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onDelete()
            }) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
