package com.example.pong
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class GameViewModel(private val myScoreDao: MyScoreDao) : ViewModel() {
    val TAG = "GAME VIEWMODEL"
    private val reward = 10
    private val gravityAcceleration = Offset(0.0f, 1.5f)

    /*
    * Player's score.
    * Updated upon collision of ball and paddle.
    * Triggers recomposition of the text view
    * for score.
    * */
    private val _score = mutableStateOf(0)
    val score: State<Int> = _score
    fun setScore(score: Int) {
        _score.value = score
    }
    fun incrementScore(reward: Int) {
        _score.value += reward
    }

    /*
    * Max Score on Device
    * The max score is persisted to local DB (Room).
    * The max score is updated when game ends,
    * comparing the game score with historic max.
    * 1. DB -> UI
    * The DAO C(R)UD function returns a Flow<Int>
    * so that changes are observable in the UI.
    * Refer to the method observeScoreChanges()
    * which must be run once in the init {} routine.
    * 2. UI -> DB
    * The DB is updated with method updateMaxScore().
    * 3. Coroutines
    * Since DAO CRUD functions are not allowed to run
    * on main UI thread, they must be run on
    * Dispatchers.IO in viewModelScope. This can have
    * undesirable side-effects due to asynchronicity
    * between main thread and IO thread.
    * */
    private val _maxScore = mutableStateOf(0)
    val maxScore: State<Int> = _maxScore
    fun observeScoreChanges() {
        viewModelScope.launch {
            myScoreDao.getMaxScore().collect { newScore ->
                _maxScore.value = newScore
            }
        }
    }

    init {
        observeScoreChanges()
    }

    /*
    * Persist new max score to DB
    * The coroutine runs on IO thread so the score
    * may change in main thread before the CR(U)D
    * operation.
    * Hence the checks are executed first and
    * local copy made before launching the coroutine
    * where DAO CR(U)D operation is executed.
    * Once the coroutine is launched then race conditions
    * may arise as main thread is trying to reset state.
    * */
    private fun updateMaxScore() {
        Log.d(TAG, "Score ${score.value} Max Score ${maxScore.value}")
        val newscore = score.value  // MITIGATE RACE CONDITION!!!!
        if (score.value > maxScore.value) {
            viewModelScope.launch(Dispatchers.IO) {
                myScoreDao.insert(MyScore(1, newscore))
            }
        }
    }
    /*
    * The top-left corner of the left paddle.
    * Once canvas is composed into the view,
    * the canvas dimensions are known
    * and serve as the reference for paddle size
    * and placement on canvas.
    * Triggers recomposition of canvas.
    * */
    private val _rectXYL = mutableStateOf(Offset(0f, 0f))
    val rectXYL: State<Offset> = _rectXYL
    fun setRectXYL(ui_XY: Offset) {
        _rectXYL.value = ui_XY
    }

    /*
    * The top-left corner of the right paddle.
    * Triggers recomposition of the canvas.
    * */
    private val _rectXYR = mutableStateOf(Offset(0f, 0f))
    val rectXYR: State<Offset> = _rectXYR
    fun setRectXYR(ui_XY: Offset) {
        _rectXYR.value = ui_XY
    }

    /*
    * Coordinates of the touch location
    * on the box enclosing the canvas.
    * These are updated from view with
    * pointerInput() modifier method upon
    * box enclosing canvas.
    * Used to detect if player touches paddle.
    * Triggers recomposition of canvas when
    * the paddle's position is recalculated
    * as a result of drag gesture.
    * */
    private val _touchXY = mutableStateOf(Offset(0f, 0f))
    val touchXY: State<Offset> = _touchXY
    fun setTouchXY(ui_XY: Offset) {
        _touchXY.value = ui_XY
    }

    /*
    * The change amount from drag gesture.
    * This is updated from view with with
    * pointerInput() modifier method upon
    * box enclosing canvas.
    * Triggers recomposition of canvas when
    * the paddle's position is recalculated
    * as a result of drag gesture.
    * */
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

    /*
    * ANIMATION CENTRAL!!!!
    * This coroutine runs in the main UI thread
    * and updates ball's position to a timer
    * thus triggering canvas recomposition,
    * effectively producing game loop.
    * The event-handlers for collision, drag, etc.
    * are executed in the game's canvas composable.
    * */
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
        updateMaxScore()
        resetGame()
    }

    private fun updateBallPosition() {
        if (isGravityEnabled.value) {
            // Apply gravity effect
            _ballVelocity.value += gravityAcceleration
        }

        _ballXY.value += _ballVelocity.value
    }

    /*
    * Drag Paddle
    * Invoked in the canvas composable.
    * In each frame, the paddle is moved
    * if warranted, i.e. paddle is touched
    * and dragged.
    * */
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

    /*
    * Bounce Ball off Paddle
    * Invoked in the canvas composable.
    * In each frame, collision event is
    * checked for, and if detected,
    * ball's velocity is recalculated.
    * The next frame will then use this
    * new velocity.
    * The flipBallHeading() method handles
    * changes in ball's heading.
    * */
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
            incrementScore(reward)
            flipBallHeading(Direction.RIGHT)
            Log.d(TAG, "BALL TOUCHED ME! Velocity: ${ballVelocity.value.x}")
        } else if (ballBounds.overlaps(paddleBoundsR)) {
            incrementScore(reward)
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

    private val _isGravityEnabled = mutableStateOf(true)
    val isGravityEnabled: State<Boolean> = _isGravityEnabled
    fun toggleGravity() {
        _isGravityEnabled.value = !_isGravityEnabled.value
    }



    fun resetGame() {
        _score.value = 0
        _rectXYL.value = Offset.Zero
        _rectXYR.value = Offset.Zero
        _ballXY.value = Offset(150f, 50f)
        _isAnimationRunning.value = false
    }
}

