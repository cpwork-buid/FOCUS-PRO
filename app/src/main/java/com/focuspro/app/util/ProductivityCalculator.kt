package com.focuspro.app.util

import kotlin.math.roundToInt

data class ProductivityBreakdown(
    val totalScore: Int,
    val focusPoints: Int,
    val taskPoints: Int,
    val disciplinePoints: Int
)

object ProductivityCalculator {

    /**
     * Transparent, deterministic formula (0 - 100):
     * 1. Focus Time vs Daily Target: up to 45 points
     * 2. Task Completion Rate: up to 35 points
     * 3. Digital Discipline (20 pts minus 2 pts per blocked app attempt): up to 20 points
     */
    fun calculate(
        focusMinutes: Int,
        dailyTargetMinutes: Int,
        completedTasks: Int,
        totalTasks: Int,
        blockedAttempts: Int
    ): ProductivityBreakdown {
        val safeTarget = dailyTargetMinutes.coerceAtLeast(1)
        val focusRatio = (focusMinutes.toFloat() / safeTarget.toFloat()).coerceIn(0f, 1f)
        val focusPoints = (focusRatio * 45f).roundToInt()

        val taskPoints = if (totalTasks > 0) {
            val taskRatio = (completedTasks.toFloat() / totalTasks.toFloat()).coerceIn(0f, 1f)
            (taskRatio * 35f).roundToInt()
        } else {
            if (focusMinutes > 0) 25 else 0
        }

        val penalty = (blockedAttempts * 2).coerceIn(0, 20)
        val disciplinePoints = (20 - penalty).coerceIn(0, 20)

        val total = (focusPoints + taskPoints + disciplinePoints).coerceIn(0, 100)
        return ProductivityBreakdown(
            totalScore = total,
            focusPoints = focusPoints,
            taskPoints = taskPoints,
            disciplinePoints = disciplinePoints
        )
    }
}
