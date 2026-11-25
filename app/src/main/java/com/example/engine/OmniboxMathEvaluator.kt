package com.example.engine

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.pow

/**
 * Pure Kotlin recursive-descent arithmetic evaluator powering Chrome/Arc-style
 * instant calculations directly inside the SwiftWeb Omnibox.
 */
object OmniboxMathEvaluator {

    private val allowedChars = Regex("^[0-9.+\\-*/()%^\\s]+$")
    private val hasOperator = Regex("[+\\-*/%^]")

    fun evaluate(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.length < 3 || trimmed.length > 64) return null
        if (!allowedChars.matches(trimmed) || !hasOperator.containsMatchIn(trimmed)) return null
        // Avoid evaluating phone numbers, IP addresses, or dates like 2026-11-20 with leading zeros/dots
        if (trimmed.count { it == '.' } > 2 && !trimmed.contains(' ')) return null

        return runCatching {
            val parser = Parser(trimmed)
            val result = parser.parseExpression()
            if (!parser.isAtEnd() || result.isNaN() || result.isInfinite()) return null
            formatResult(result)
        }.getOrNull()
    }

    private fun formatResult(value: Double): String {
        val rounded = BigDecimal.valueOf(value)
            .setScale(6, RoundingMode.HALF_UP)
            .stripTrailingZeros()
        return rounded.toPlainString()
    }

    private class Parser(private val src: String) {
        private var pos = 0

        fun isAtEnd(): Boolean {
            skipSpaces()
            return pos >= src.length
        }

        private fun skipSpaces() {
            while (pos < src.length && src[pos].isWhitespace()) pos++
        }

        private fun peek(): Char? {
            skipSpaces()
            return if (pos < src.length) src[pos] else null
        }

        private fun consume(expected: Char): Boolean {
            skipSpaces()
            if (pos < src.length && src[pos] == expected) {
                pos++
                return true
            }
            return false
        }

        fun parseExpression(): Double {
            var left = parseTerm()
            while (true) {
                left = when {
                    consume('+') -> left + parseTerm()
                    consume('-') -> left - parseTerm()
                    else -> return left
                }
            }
        }

        private fun parseTerm(): Double {
            var left = parsePower()
            while (true) {
                left = when {
                    consume('*') -> left * parsePower()
                    consume('/') -> {
                        val right = parsePower()
                        if (right == 0.0) throw ArithmeticException("Division by zero")
                        left / right
                    }
                    consume('%') -> left % parsePower()
                    else -> return left
                }
            }
        }

        private fun parsePower(): Double {
            val base = parseUnary()
            return if (consume('^')) {
                base.pow(parsePower())
            } else {
                base
            }
        }

        private fun parseUnary(): Double = when {
            consume('+') -> parseUnary()
            consume('-') -> -parseUnary()
            else -> parsePrimary()
        }

        private fun parsePrimary(): Double {
            if (consume('(')) {
                val inner = parseExpression()
                if (!consume(')')) throw IllegalArgumentException("Unclosed parenthesis")
                return inner
            }
            skipSpaces()
            val start = pos
            var hasDot = false
            while (pos < src.length && (src[pos].isDigit() || (!hasDot && src[pos] == '.'))) {
                if (src[pos] == '.') hasDot = true
                pos++
            }
            if (pos == start) throw IllegalArgumentException("Expected number")
            return src.substring(start, pos).toDouble()
        }
    }
}
