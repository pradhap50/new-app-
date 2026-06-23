package com.example.ui

import java.lang.Exception
import java.util.Locale

data class FormulaToken(val type: String, val value: String)

object FormulaSerializer {
    fun serialize(tokens: List<FormulaToken>): String {
        val sb = StringBuilder()
        sb.append("[")
        tokens.forEachIndexed { idx, token ->
            val escapedValue = token.value.replace("\"", "\\\"")
            sb.append("{\"type\":\"${token.type}\",\"value\":\"${escapedValue}\"}")
            if (idx < tokens.size - 1) {
                sb.append(",")
            }
        }
        sb.append("]")
        return sb.toString()
    }

    fun deserialize(json: String): List<FormulaToken>? {
        val trimmed = json.trim()
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) return null
        val tokens = mutableListOf<FormulaToken>()
        var i = 0
        val len = trimmed.length
        while (i < len) {
            val objStart = trimmed.indexOf('{', i)
            if (objStart == -1) break
            val objEnd = trimmed.indexOf('}', objStart)
            if (objEnd == -1) break
            val objStr = trimmed.substring(objStart + 1, objEnd)
            val typePattern = Regex("\"type\"\\s*:\\s*\"([^\"]+)\"")
            val valPattern = Regex("\"value\"\\s*:\\s*\"([^\"]+)\"")
            
            val typeMatch = typePattern.find(objStr)
            val valMatch = valPattern.find(objStr)
            
            if (typeMatch != null && valMatch != null) {
                val type = typeMatch.groupValues[1]
                val value = valMatch.groupValues[1].replace("\\\"", "\"")
                tokens.add(FormulaToken(type, value))
            }
            i = objEnd + 1
        }
        return tokens
    }
}

object FormulaEvaluator {

    fun tokenize(expr: String): List<FormulaToken> {
        val tokens = mutableListOf<FormulaToken>()
        var i = 0
        val len = expr.length
        while (i < len) {
            val c = expr[i]
            if (c.isWhitespace()) {
                i++
                continue
            }
            when (c) {
                '+', '-', '*', '/', '×', '÷' -> {
                    val opStr = when (c) {
                        '×' -> "*"
                        '÷' -> "/"
                        else -> c.toString()
                    }
                    tokens.add(FormulaToken("operator", opStr))
                    i++
                }
                '(', ')' -> {
                    tokens.add(FormulaToken("bracket", c.toString()))
                    i++
                }
                else -> {
                    if (c.isDigit() || c == '.') {
                        val sb = StringBuilder()
                        while (i < len && (expr[i].isDigit() || expr[i] == '.')) {
                            sb.append(expr[i])
                            i++
                        }
                        tokens.add(FormulaToken("constant", sb.toString()))
                    } else if (c.isLetter() || c == '_') {
                        val sb = StringBuilder()
                        while (i < len && (expr[i].isLetterOrDigit() || expr[i] == '_')) {
                            sb.append(expr[i])
                            i++
                        }
                        tokens.add(FormulaToken("variable", sb.toString()))
                    } else {
                        tokens.add(FormulaToken("unknown", c.toString()))
                        i++
                    }
                }
            }
        }
        return tokens
    }

    fun validateTokens(tokens: List<FormulaToken>, allowedVariables: Map<String, Double>? = null): String? {
        if (tokens.isEmpty()) return "Formula is empty"

        var openParenCount = 0
        var lastToken: FormulaToken? = null
        val allowedSet = allowedVariables?.keys ?: emptySet()

        for (i in tokens.indices) {
            val token = tokens[i]
            
            when (token.type) {
                "unknown" -> {
                    return "Unknown or invalid character '${token.value}'"
                }
                "bracket" -> {
                    if (token.value == "(") {
                        openParenCount++
                        // e.g. value before ( is missing operator
                        if (lastToken != null && (lastToken.type == "variable" || lastToken.type == "constant" || (lastToken.type == "bracket" && lastToken.value == ")"))) {
                            return "Missing operator before '('"
                        }
                    } else if (token.value == ")") {
                        openParenCount--
                        if (openParenCount < 0) {
                            return "Mismatched closed parenthesis ')' without opening '('"
                        }
                        if (lastToken != null && lastToken.type == "operator") {
                            return "An operator '${lastToken.value}' cannot precede closing parenthesis ')'"
                        }
                    }
                }
                "operator" -> {
                    if (lastToken == null) {
                        if (token.value == "*" || token.value == "/") {
                            return "Formula cannot start with operator '${token.value}'"
                        }
                    } else if (lastToken.type == "operator") {
                        return "Consecutive operators are not allowed: '${lastToken.value} ${token.value}'"
                    } else if (lastToken.type == "bracket" && lastToken.value == "(") {
                        if (token.value == "*" || token.value == "/") {
                            return "Operator '${token.value}' cannot follow opening parenthesis '('"
                        }
                    }
                }
                "variable" -> {
                    if (allowedVariables != null && allowedVariables.isNotEmpty() && !allowedSet.contains(token.value)) {
                        return "Variable '${token.value}' is not defined in this configuration"
                    }
                    if (lastToken != null && (lastToken.type == "variable" || lastToken.type == "constant" || (lastToken.type == "bracket" && lastToken.value == ")"))) {
                        return "Missing operator before variable '${token.value}'"
                    }
                }
                "constant" -> {
                    val d = token.value.toDoubleOrNull()
                    if (d == null || d.isNaN()) {
                        return "Invalid decimal constant '${token.value}'"
                    }
                    if (lastToken != null && (lastToken.type == "variable" || lastToken.type == "constant" || (lastToken.type == "bracket" && lastToken.value == ")"))) {
                        return "Missing operator before constant '${token.value}'"
                    }
                }
            }
            lastToken = token
        }

        if (openParenCount > 0) {
            return "Mismatched parentheses: $openParenCount unclosed opening parenthesis '('"
        }

        if (lastToken != null && lastToken.type == "operator") {
            return "Formula cannot end with trailing operator '${lastToken.value}'"
        }

        try {
            val plainExprStr = tokensToPlainExpression(tokens)
            evaluateExpression(plainExprStr)
        } catch (e: Exception) {
            return "Mathematical error: ${e.message ?: "Invalid syntax"}"
        }

        return null
    }

    fun tokensToPlainExpression(tokens: List<FormulaToken>): String {
        return tokens.joinToString(" ") { token ->
            when (token.type) {
                "operator" -> when (token.value) {
                    "×" -> "*"
                    "÷" -> "/"
                    else -> token.value
                }
                else -> token.value
            }
        }
    }

    fun tokensToFormattedExpression(tokens: List<FormulaToken>): String {
        val sb = StringBuilder()
        for (i in tokens.indices) {
            val t = tokens[i]
            if (t.type == "operator") {
                val visualOp = when (t.value) {
                    "*" -> " × "
                    "/" -> " ÷ "
                    "+" -> " + "
                    "-" -> " - "
                    else -> " ${t.value} "
                }
                sb.append(visualOp)
            } else if (t.type == "bracket") {
                if (t.value == "(") {
                    sb.append(" (")
                } else {
                    sb.append(") ")
                }
            } else {
                sb.append(t.value)
            }
        }
        return sb.toString().replace(Regex("\\s+"), " ").trim()
    }

    /**
     * Evaluates a formula string (can be structured JSON or plain text)
     * by substituting variable values and performing calculation.
     */
    fun evaluate(formula: String, variablesRaw: Map<String, Double>): Double {
        val tokens = if (formula.trim().startsWith("[")) {
            FormulaSerializer.deserialize(formula) ?: tokenize(formula)
        } else {
            tokenize(formula)
        }
        val plainExpr = tokensToPlainExpression(tokens)
        
        var expr = plainExpr
        val sortedKeys = variablesRaw.keys.sortedByDescending { it.length }
        for (symbol in sortedKeys) {
            val value = variablesRaw[symbol] ?: 0.0
            val regex = Regex("\\b$symbol\\b")
            expr = expr.replace(regex, String.format(Locale.US, "%.8f", value))
        }

        return evaluateExpression(expr)
    }

    /**
     * Evaluates clean mathematical algebraic structures offline.
     */
    fun evaluateExpression(exprRaw: String): Double {
        val cleanExpr = exprRaw.replace(" ", "")
        if (cleanExpr.isEmpty()) return Double.NaN
        return try {
            exprEval(cleanExpr)
        } catch (e: Exception) {
            Double.NaN
        }
    }

    private fun exprEval(str: String): Double {
        return object {
            var pos = -1
            var ch = -1

            fun nextChar() {
                ch = if (++pos < str.length) str[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < str.length) return Double.NaN
                return x
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    if (eat('+'.code)) x += parseTerm()
                    else if (eat('-'.code)) x -= parseTerm()
                    else return x
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    if (eat('*'.code)) x *= parseFactor()
                    else if (eat('/'.code)) x /= parseFactor()
                    else return x
                }
            }

            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor()
                if (eat('-'.code)) return -parseFactor()

                var x: Double
                val startPos = this.pos
                if (eat('('.code)) {
                    x = parseExpression()
                    eat(')'.code)
                } else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code || ch == 'e'.code || ch == 'E'.code) {
                    while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code || ch == 'e'.code || ch == 'E'.code) {
                        nextChar()
                    }
                    val token = str.substring(startPos, this.pos)
                    x = token.toDoubleOrNull() ?: Double.NaN
                } else {
                    x = Double.NaN
                }
                return x
            }
        }.parse()
    }
}
