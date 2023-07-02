package com.example.pong
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {
    private val _score = mutableStateOf(0f)
    val score: State<Float> = _score
    fun setScore(score: Float) {
        _score.value = score
    }

    private val _rectXYL = mutableStateOf(Offset(0f, 0f))
    val rectXYL: State<Offset> = _rectXYL
    fun setRectXYL(ui_XY: Offset) {
        _rectXYL.value = ui_XY
    }

    private val _rectXYR = mutableStateOf(Offset(0f, 0f))
    val rectXYR: State<Offset> = _rectXYR
    fun setRectXYR(ui_XY: Offset) {
        _rectXYR.value = ui_XY
    }

    private val _touchXY = mutableStateOf(Offset(0f, 0f))
    val touchXY: State<Offset> = _touchXY
    fun setTouchXY(ui_XY: Offset) {
        _touchXY.value = ui_XY
    }

    private val _deltaY = mutableStateOf(0f)
    val deltaY: State<Float> = _deltaY
    fun setDeltaY(ui_Y: Float) {
        _deltaY.value = ui_Y
    }

    private val _ballXY = mutableStateOf(Offset(150f, 50f))
    val ballXY: State<Offset> = _ballXY
    fun setBallXY(ui_XY: Offset) {
        _ballXY.value = ui_XY
    }

    private val _ballVelocity = mutableStateOf(Offset(90f, 15f))
    val ballVelocity: State<Offset> = _ballVelocity
    fun setBallVelocity(ui_velocity: Offset) {
        _ballVelocity.value = ui_velocity
    }

    private val _ballRadius = mutableStateOf(45f)
    val ballRadius: State<Float> = _ballRadius

    private val _isAnimationRunning = mutableStateOf(false)
    val isAnimationRunning: State<Boolean> = _isAnimationRunning
    fun setIsAnimationRunning(flag: Boolean) {
        _isAnimationRunning.value = flag
    }

    fun startAnimation() {
        _isAnimationRunning.value = true
        viewModelScope.launch {
            while (_isAnimationRunning.value) {
                updateBallPosition()
                delay(100L) // Delay for 100 milliseconds
            }
        }
    }

    fun stopAnimation() {
        _isAnimationRunning.value = false
    }

    private fun updateBallPosition() {
        _ballXY.value += _ballVelocity.value
    }
}
