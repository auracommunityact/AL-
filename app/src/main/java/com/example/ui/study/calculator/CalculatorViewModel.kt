package com.example.ui.study.calculator

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.CalculatorHistoryDao
import com.example.data.local.CalculatorHistoryEntity
import com.example.data.local.PlannerDatabase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Stack

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {
    private val db = PlannerDatabase.getDatabase(application)
    private val historyDao: CalculatorHistoryDao = db.calculatorHistoryDao()

    private val _expression = MutableStateFlow("")
    val expression: StateFlow<String> = _expression.asStateFlow()

    private val _resultPreview = MutableStateFlow("")
    val resultPreview: StateFlow<String> = _resultPreview.asStateFlow()

    private val _isDegreeMode = MutableStateFlow(true)
    val isDegreeMode: StateFlow<Boolean> = _isDegreeMode.asStateFlow()

    private val _memoryValue = MutableStateFlow(0.0)
    val memoryValue: StateFlow<Double> = _memoryValue.asStateFlow()

    private val _isMemoryActive = MutableStateFlow(false)
    val isMemoryActive: StateFlow<Boolean> = _isMemoryActive.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Undo and Redo stacks
    private val undoStack = Stack<String>()
    private val redoStack = Stack<String>()

    // History Flow linked to Database
    val history: StateFlow<List<CalculatorHistoryEntity>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                historyDao.getAllHistory()
            } else {
                historyDao.searchHistory("%$query%")
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private fun pushToUndoStack() {
        if (undoStack.isEmpty() || undoStack.peek() != _expression.value) {
            undoStack.push(_expression.value)
        }
    }

    fun toggleDegreeMode() {
        _isDegreeMode.value = !_isDegreeMode.value
        evaluateLivePreview()
    }

    fun append(value: String) {
        pushToUndoStack()
        redoStack.clear()

        val current = _expression.value

        // Custom decimal point validation
        if (value == ".") {
            if (hasDecimalInCurrentNumber()) return
        }

        // Auto parentheses for scientific functions
        val formattedValue = when (value) {
            "sin", "cos", "tan", "asin", "acos", "atan", "sinh", "cosh", "tanh", "log10", "log", "ln", "abs", "exp" -> "$value("
            "³√" -> "³√("
            "√" -> "√("
            else -> value
        }

        // Avoid double operators or replace them
        if (isOperator(formattedValue) && current.isNotEmpty() && isOperator(current.last().toString())) {
            _expression.value = current.substring(0, current.length - 1) + formattedValue
        } else {
            _expression.value = current + formattedValue
        }

        evaluateLivePreview()
    }

    fun clear() {
        pushToUndoStack()
        redoStack.clear()
        _expression.value = ""
        _resultPreview.value = ""
    }

    fun backspace() {
        val current = _expression.value
        if (current.isEmpty()) return

        pushToUndoStack()
        redoStack.clear()

        val functions = listOf(
            "sin(", "cos(", "tan(", "asin(", "acos(", "atan(", 
            "sinh(", "cosh(", "tanh(", "log10(", "log(", "ln(", 
            "abs(", "exp(", "³√(", "√("
        )

        var deleted = false
        for (f in functions) {
            if (current.endsWith(f)) {
                _expression.value = current.substring(0, current.length - f.length)
                deleted = true
                break
            }
        }

        if (!deleted) {
            _expression.value = current.substring(0, current.length - 1)
        }

        evaluateLivePreview()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack.push(_expression.value)
            _expression.value = undoStack.pop()
            evaluateLivePreview()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack.push(_expression.value)
            _expression.value = redoStack.pop()
            evaluateLivePreview()
        }
    }

    fun calculateResult() {
        val expr = _expression.value
        if (expr.isEmpty()) return

        try {
            val evaluator = MathEvaluator(_isDegreeMode.value)
            val evalValue = evaluator.evaluate(expr)
            val resultStr = MathEvaluator.formatResult(evalValue)

            pushToUndoStack()
            redoStack.clear()

            // Save to database history
            viewModelScope.launch {
                historyDao.insertHistory(
                    CalculatorHistoryEntity(
                        expression = expr,
                        answer = resultStr
                    )
                )
            }

            _expression.value = resultStr
            _resultPreview.value = ""
        } catch (e: Exception) {
            _resultPreview.value = "Error"
        }
    }

    private fun evaluateLivePreview() {
        val expr = _expression.value
        if (expr.isEmpty()) {
            _resultPreview.value = ""
            return
        }

        // Avoid evaluating trailing operator or unclosed function immediately to prevent parsing spam errors
        if (isOperator(expr.last().toString()) || expr.endsWith("(")) {
            return
        }

        try {
            val evaluator = MathEvaluator(_isDegreeMode.value)
            val evalValue = evaluator.evaluate(expr)
            _resultPreview.value = MathEvaluator.formatResult(evalValue)
        } catch (e: Exception) {
            // Live preview doesn't show errors to keep standard elegant feeling
        }
    }

    // Memory Functions
    fun memoryClear() {
        _memoryValue.value = 0.0
        _isMemoryActive.value = false
    }

    fun memoryRecall() {
        if (_isMemoryActive.value) {
            append(MathEvaluator.formatResult(_memoryValue.value))
        }
    }

    fun memoryStore() {
        val currentValueStr = _resultPreview.value.ifEmpty { _expression.value }
        try {
            val value = currentValueStr.toDouble()
            _memoryValue.value = value
            _isMemoryActive.value = true
        } catch (e: Exception) {
            // Fallback: evaluate expression directly
            try {
                val evaluator = MathEvaluator(_isDegreeMode.value)
                val value = evaluator.evaluate(_expression.value)
                _memoryValue.value = value
                _isMemoryActive.value = true
            } catch (ex: Exception) {
                // Ignore invalid memory stores
            }
        }
    }

    fun memoryAdd() {
        val currentValueStr = _resultPreview.value.ifEmpty { _expression.value }
        try {
            val value = currentValueStr.toDouble()
            _memoryValue.value += value
            _isMemoryActive.value = true
        } catch (e: Exception) {
            try {
                val evaluator = MathEvaluator(_isDegreeMode.value)
                val value = evaluator.evaluate(_expression.value)
                _memoryValue.value += value
                _isMemoryActive.value = true
            } catch (ex: Exception) {
                // Ignore
            }
        }
    }

    fun memorySubtract() {
        val currentValueStr = _resultPreview.value.ifEmpty { _expression.value }
        try {
            val value = currentValueStr.toDouble()
            _memoryValue.value -= value
            _isMemoryActive.value = true
        } catch (e: Exception) {
            try {
                val evaluator = MathEvaluator(_isDegreeMode.value)
                val value = evaluator.evaluate(_expression.value)
                _memoryValue.value -= value
                _isMemoryActive.value = true
            } catch (ex: Exception) {
                // Ignore
            }
        }
    }

    // History Database Operations
    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            historyDao.deleteHistoryById(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            historyDao.deleteAllHistory()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private fun hasDecimalInCurrentNumber(): Boolean {
        var i = _expression.value.length - 1
        while (i >= 0) {
            val c = _expression.value[i]
            if (c == '.') return true
            if (isOperator(c.toString()) || c == '(' || c == ')') break
            i--
        }
        return false
    }

    private fun isOperator(s: String): Boolean {
        return s == "+" || s == "-" || s == "×" || s == "÷" || s == "mod" || s == "%" || s == "^"
    }
}
