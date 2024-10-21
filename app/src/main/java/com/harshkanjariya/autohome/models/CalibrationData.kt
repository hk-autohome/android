package com.harshkanjariya.autohome.models

data class CalibrationData(
    val inPin: Int,
    val outPin: Int,
    val minOff: Float,
    val maxOff: Float,
)