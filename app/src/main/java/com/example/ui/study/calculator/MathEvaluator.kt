package com.example.ui.study.calculator

import kotlin.math.*

class MathEvaluator(private val isDegreeMode: Boolean) {
    private var str: String = ""
    private var pos = -1
    private var ch = 0

    private fun nextChar() {
        ch = if (++pos < str.length) str[pos].code else -1
    }

    private fun eat(charToEat: Int): Boolean {
        while (ch == ' '.code) nextChar()
        if (ch == charToEat) {
            nextChar()
            return true
        }
        return false
    }

    fun evaluate(expression: String): Double {
        str = preprocess(expression)
        pos = -1
        ch = 0
        nextChar()
        val x = parseExpression()
        if (pos < str.length) throw IllegalArgumentException("Unexpected character: " + ch.toChar())
        return x
    }

    private fun preprocess(expr: String): String {
        // Balance parentheses
        var balanced = expr
        var openCount = 0
        var closeCount = 0
        for (i in expr.indices) {
            if (expr[i] == '(') openCount++
            else if (expr[i] == ')') closeCount++
        }
        if (openCount > closeCount) {
            balanced += ")".repeat(openCount - closeCount)
        }

        // Replace custom symbols
        var processed = balanced
            .replace("×", "*")
            .replace("÷", "/")
            .replace("mod", "%")

        return insertImplicitMultiplication(processed)
    }

    private fun insertImplicitMultiplication(s: String): String {
        if (s.isEmpty()) return s
        val sb = StringBuilder()
        for (i in 0 until s.length) {
            val curr = s[i]
            sb.append(curr)
            if (i < s.length - 1) {
                val next = s[i + 1]
                val isCurrOperand = curr.isDigit() || curr == '.' || curr == 'π' || curr == 'e' || curr == ')'
                val isNextOperand = next.isDigit() || next == '.' || next == 'π' || next == 'e' || next == '(' || next.isLetter()
                
                val isCurrLetter = curr.isLetter()
                val isNextLetter = next.isLetter()
                
                if (isCurrOperand && isNextOperand) {
                    val isCurrNum = curr.isDigit() || curr == '.'
                    val isNextNum = next.isDigit() || next == '.'
                    
                    if (!(isCurrNum && isNextNum) && !(isCurrLetter && isNextLetter)) {
                        if ((curr == 'π' || curr == 'e') && (next.isDigit() || next == '(' || next == 'π' || next == 'e' || next.isLetter())) {
                            sb.append('*')
                        } else if ((curr.isDigit() || curr == ')') && (next == 'π' || next == 'e')) {
                            sb.append('*')
                        } else if (curr.isDigit() && (next == '(' || next.isLetter())) {
                            sb.append('*')
                        } else if (curr == ')' && (next.isDigit() || next == '(' || next.isLetter() || next == 'π' || next == 'e')) {
                            sb.append('*')
                        }
                    }
                }
            }
        }
        return sb.toString()
    }

    private fun parseExpression(): Double {
        var x = parseTerm()
        while (true) {
            if (eat('+'.code)) x += parseTerm()
            else if (eat('-'.code)) x -= parseTerm()
            else break
        }
        return x
    }

    private fun parseTerm(): Double {
        var x = parseFactor()
        while (true) {
            if (eat('*'.code)) x *= parseFactor()
            else if (eat('/'.code)) {
                val valFactor = parseFactor()
                if (valFactor == 0.0) throw ArithmeticException("Division by zero")
                x /= valFactor
            } else if (eat('%'.code)) {
                val valFactor = parseFactor()
                if (valFactor == 0.0) throw ArithmeticException("Division by zero")
                x %= valFactor
            } else break
        }
        return x
    }

    private fun parseFactor(): Double {
        if (eat('+'.code)) return +parseFactor()
        if (eat('-'.code)) return -parseFactor()

        var x: Double
        val startPos = this.pos
        if (eat('('.code)) {
            x = parseExpression()
            if (!eat(')'.code)) throw IllegalArgumentException("Missing closing parenthesis")
        } else if (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) {
            while (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) nextChar()
            x = str.substring(startPos, this.pos).toDouble()
        } else if (ch >= 'a'.code && ch <= 'z'.code || ch >= 'A'.code && ch <= 'Z'.code || ch == '√'.code || ch == '³'.code) {
            var func = ""
            if (ch == '√'.code) {
                func = "√"
                nextChar()
            } else if (ch == '³'.code) {
                func = "³√"
                nextChar()
                if (ch == '√'.code) nextChar()
            } else {
                while (ch >= 'a'.code && ch <= 'z'.code || ch >= 'A'.code && ch <= 'Z'.code || (ch >= '0'.code && ch <= '9'.code)) {
                    func += ch.toChar()
                    nextChar()
                }
            }

            val arg = parseFactor()
            
            x = when (func) {
                "sin" -> if (isDegreeMode) sin(Math.toRadians(arg)) else sin(arg)
                "cos" -> if (isDegreeMode) cos(Math.toRadians(arg)) else cos(arg)
                "tan" -> if (isDegreeMode) tan(Math.toRadians(arg)) else tan(arg)
                "asin" -> if (isDegreeMode) Math.toDegrees(asin(arg)) else asin(arg)
                "acos" -> if (isDegreeMode) Math.toDegrees(acos(arg)) else acos(arg)
                "atan" -> if (isDegreeMode) Math.toDegrees(atan(arg)) else atan(arg)
                "sinh" -> sinh(arg)
                "cosh" -> cosh(arg)
                "tanh" -> tanh(arg)
                "log10" -> log10(arg)
                "log" -> log10(arg)
                "ln" -> ln(arg)
                "√" -> if (arg < 0) throw IllegalArgumentException("Square root of negative number") else sqrt(arg)
                "³√" -> Math.cbrt(arg)
                "abs" -> abs(arg)
                "exp" -> exp(arg)
                else -> throw IllegalArgumentException("Unknown function: $func")
            }
        } else if (ch == 'π'.code) {
            x = PI
            nextChar()
        } else if (ch == 'e'.code) {
            x = E
            nextChar()
        } else {
            throw IllegalArgumentException("Unexpected character: " + ch.toChar())
        }

        while (true) {
            if (eat('^'.code)) {
                val exponent = parseFactor()
                x = x.pow(exponent)
            } else if (eat('!'.code)) {
                x = factorial(x)
            } else {
                break
            }
        }

        return x
    }

    private fun factorial(n: Double): Double {
        if (n < 0 || n != floor(n)) throw IllegalArgumentException("Factorial is defined for non-negative integers only")
        if (n > 170) throw ArithmeticException("Factorial overflow")
        var res = 1.0
        for (i in 1..n.toInt()) {
            res *= i
        }
        return res
    }

    companion object {
        fun formatResult(value: Double): String {
            if (value.isNaN()) return "Error: NaN"
            if (value.isInfinite()) return if (value < 0) "-Infinity" else "Infinity"
            
            if (value == value.toLong().toDouble()) {
                return value.toLong().toString()
            }
            
            // Format to 10 decimal places, stripping trailing zeros
            val df = java.text.DecimalFormat("0.##########", java.text.DecimalFormatSymbols(java.util.Locale.US))
            df.maximumFractionDigits = 10
            val result = df.format(value)
            return if (result == "-0") "0" else result
        }
    }
}
