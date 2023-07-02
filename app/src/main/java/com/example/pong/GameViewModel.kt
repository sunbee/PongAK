package com.example.pong
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {
    val TAG = "GAME VIEWMODEL"
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

    private val _ballVelocity = mutableStateOf(Offset(45f, 10f))
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
                delay(100L)
            }
        }
    }

    fun stopAnimation() {
        _isAnimationRunning.value = false
    }

    private fun updateBallPosition() {
        _ballXY.value += _ballVelocity.value
    }

    fun dragPaddleL(paddleSize: Size, canvasSize: Size) {
        if (touchXY.value.x in (rectXYL.value.x..rectXYL.value.x+paddleSize.width) &&
            touchXY.value.y in (rectXYL.value.y..rectXYL.value.y+paddleSize.height)) {
            Log.d(TAG, "LEFT PADDLE TOUCHED!")
            val newY = (rectXYL.value.y + deltaY.value).coerceIn(0f, canvasSize.height-paddleSize.height)
            setRectXYL( Offset(rectXYL.value.x, newY) )
            setDeltaY(0f)
        } // end IF
    }

    fun dragPaddleR(paddleSize: Size, canvasSize: Size) {
        if (touchXY.value.x in (rectXYR.value.x..rectXYR.value.x+paddleSize.width) &&
            touchXY.value.y in (rectXYR.value.y..rectXYR.value.y+paddleSize.height)) {
            Log.d(TAG, "RIGHT PADDLE TOUCHED!")
            val newY = (rectXYR.value.y + deltaY.value).coerceIn(0f, canvasSize.height-paddleSize.height)
            setRectXYR( Offset(rectXYR.value.x, newY) )
            setDeltaY(0f) // So paddle stops when drag is paused
        } // end IF
    }

    fun checkBallHitsPaddle(paddleSize: Size) {
        val ballBounds = Rect(
            (ballXY.value.x - ballRadius.value),
            (ballXY.value.y - ballRadius.value),
            (ballXY.value.x + ballRadius.value),
            (ballXY.value.y + ballRadius.value)
        )
        val paddleBoundsL = Rect(
            offset = rectXYL.value,
            size = paddleSize
        )
        val paddleBoundsR = Rect(
            offset = rectXYR.value,
            size = paddleSize
        )
        if (ballBounds.overlaps(paddleBoundsL))  {
            flipBallHeading(Direction.RIGHT)
            Log.d(TAG, "BALL TOUCHED ME! Velocity: ${ballVelocity.value.x}")
        } else if (ballBounds.overlaps(paddleBoundsR)) {
            flipBallHeading(Direction.LEFT)
            Log.d(TAG, "BALL TOUCHED ME! Velocity: ${ballVelocity.value.x}")
        }
    }

    enum class Direction {
        UP, DOWN, LEFT, RIGHT
    }

    fun flipBallHeading(direction: Direction) {
        _ballVelocity.value = when (direction) {
            Direction.UP -> Offset(ballVelocity.value.x,  if (ballVelocity.value.y < 0) ballVelocity.value.y else -ballVelocity.value.y)
            Direction.DOWN -> Offset(ballVelocity.value.x, if (ballVelocity.value.y > 0) ballVelocity.value.y else -ballVelocity.value.y)
            Direction.LEFT -> Offset(if (ballVelocity.value.x < 0) ballVelocity.value.x else -ballVelocity.value.x, ballVelocity.value.y)
            Direction.RIGHT -> Offset(if (ballVelocity.value.x > 0) ballVelocity.value.x else -ballVelocity.value.x, -ballVelocity.value.y)
        }
    }

    enum class Edge {
        LEFT, RIGHT, TOP, BOTTOM
    }

    fun handleBallHitsEdge(canvasSize: Size) {

        val edge: Edge

        if (ballXY.value.y - ballRadius.value <= 0f) {
            edge = Edge.TOP
            flipBallHeading(Direction.DOWN)
        } else if (ballXY.value.x + ballRadius.value >= canvasSize.width) {
            edge = Edge.RIGHT
            setIsAnimationRunning(false)
        } else if (ballXY.value.y + ballRadius.value >= canvasSize.height) {
            edge = Edge.BOTTOM
            flipBallHeading(Direction.UP)
        } else if (ballXY.value.x - ballRadius.value <= 0f) {
            edge = Edge.LEFT
            setIsAnimationRunning(false)
        }
    }
}
