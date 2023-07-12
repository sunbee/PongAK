package com.example.pong
import android.content.Context
import android.media.MediaPlayer
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

class GameViewModel(context: Context, private val myScoreDao: MyScoreDao) : ViewModel() {
    val TAG = "GAME VIEWMODEL"
    private val reward = 10

    /*
    * SUB-SYSTEM: SOUND EFFECTS
    * Play sounds on different occasions.
    * */
    private val buzzer: MediaPlayer = MediaPlayer.create(context, R.raw.buzzer)
    private val noink: MediaPlayer = MediaPlayer.create(context, R.raw.noink)
    private val note: MediaPlayer = MediaPlayer.create(context, R.raw.note)

    enum class SOUND {
        BUZZER, NOINK, NOTE
    }

    private fun playSound(sound: SOUND) {
        when(sound) {
            SOUND.BUZZER -> buzzer.start()
            SOUND.NOINK -> noink.start()
            SOUND.NOTE -> note.start()
        }
    }

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
    * SUB-SYSTEM: Persist max score
    * Performs CRUD operation on Room DB.
    * Room DB has the following components:
    * @Entity is the annotated Kotlin class for max score
    * @Dao is the interface for CRUD operations to use in View Model with wrappers
    * @Database is the database
    * After implementing all three, the MainActivity has creation of the following:
    * 1. DB instance
    * 2. View Model instance using Object Factory
    * In the View Model, we have the following:
    * 1. observeScoreChanges() flow changes from DB -> View Model with observer pattern
    * 2. init {} invoke observeScoreChanges() to start observation
    * 3. updateMaxScore() coroutine wrapper around @Dao to (C)RUD max score
    * */
    private val _maxScore = mutableStateOf(0)
    val maxScore: State<Int> = _maxScore
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
    fun observeScoreChanges() {
        viewModelScope.launch {
            myScoreDao.getMaxScore().collect { newScore ->
                _maxScore.value = newScore
            }
        }
    }

    init {
        observeScoreChanges()  // Start observing
    }

    private fun updateMaxScore() {
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
        Log.d(TAG, "Score ${score.value} Max Score ${maxScore.value}")
        val newscore = score.value  // MITIGATE RACE CONDITION!!!!
        if (score.value > maxScore.value) {
            viewModelScope.launch(Dispatchers.IO) {
                myScoreDao.insert(MyScore(1, newscore))
            }
        }
    }

    /*
    * EVENT-HANDLER:
    * WHEN paddle is dragged THEN move paddle
    *
    * WHEN paddle is dragged .. (touch event)
    *   The modifier pointerInput() is applied to Box
    *   containing Canvas to capture drag gesture.
    * THEN move paddle.. (callback)
    *   dragPaddleL()
    *   dragPaddleR()
    * are invoked in game loop of Canvas composable.
    * Actors:
    *   _rectXYL is paddle L's top-left corner
    *   _rectXYR is paddle R's top-left corner
    *   _touchXY is XY coordinates of touch
    *   _deltaY is movement of paddle
    * */
    private val _rectXYL = mutableStateOf(Offset(0f, 0f))
    val rectXYL: State<Offset> = _rectXYL
    /*
    * The top-left corner of the left paddle.
    * Once canvas is composed into the view,
    * the canvas dimensions are known
    * and serve as the reference for paddle size
    * and placement on canvas.
    * Triggers recomposition of canvas.
    * */
    fun setRectXYL(ui_XY: Offset) {
        _rectXYL.value = ui_XY
    }

    private val _rectXYR = mutableStateOf(Offset(0f, 0f))
    val rectXYR: State<Offset> = _rectXYR
    /*
    * The top-left corner of the right paddle.
    * Triggers recomposition of the canvas.
    * */
    fun setRectXYR(ui_XY: Offset) {
        _rectXYR.value = ui_XY
    }

    private val _touchXY = mutableStateOf(Offset(0f, 0f))
    val touchXY: State<Offset> = _touchXY
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
    fun setTouchXY(ui_XY: Offset) {
        _touchXY.value = ui_XY
    }

    private val _deltaY = mutableStateOf(0f)
    val deltaY: State<Float> = _deltaY
    /*
    * The change amount from drag gesture.
    * This is updated from view with with
    * pointerInput() modifier method upon
    * box enclosing canvas.
    * Triggers recomposition of canvas when
    * the paddle's position is recalculated
    * as a result of drag gesture.
    * */
    fun setDeltaY(ui_Y: Float) {
        _deltaY.value = ui_Y
    }

    fun dragPaddleL(paddleSize: Size, canvasSize: Size) {
        /*
        * Drag Paddle
        * Invoked in the canvas composable.
        * In each frame, the paddle is moved
        * if warranted, i.e. paddle is touched
        * and dragged.
        * */
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
    * EVENT-HANDLER
    * WHEN timer fires THEN advance ball to next position
    *
    * WHEN timer fires.. (clock's timer)
    *   Timer updates ball's XY coordinates in an async forever-loop.
    *   When _isAnimationRunning is false, the forever-loop exits. While
    *   _isAnimationRunning is true, the ball's animation drives the game
    *   loop by forcing the canvas to recompose with each update, thus
    *   advancing the game frame-by-frame.
    * THEN advance ball to next position.. (callback)
    *   startAnimation() has the forever-loop to update ball's position
    *   stopAnimation() is the exit procedure
    *   updateBallPosition() increments the ball's XY coordinates
    * Actors:
    *   _ballXY is the ball's XY coordinates
    *   _ballVelocity is the ball's velocity
    *   _ballRadius is the ball's radius
    *   _isAnimationRunning is the flag
    * */

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

    private fun updateBallPosition() {
        /*
        * ANIMATION CENTRAL!!!!
        * This coroutine runs in the main UI thread
        * and updates ball's position to a timer
        * thus triggering canvas recomposition,
        * effectively producing game loop.
        * The event-handlers for collision, drag, etc.
        * are executed in the game's canvas composable.
        * */
        if (isGravityEnabled.value) {
            // Apply gravity effect
            _ballVelocity.value += gravityAcceleration
        }
        _ballXY.value += _ballVelocity.value
    }

    fun startAnimation() {
        /*
        * WHEN Start button is clicked THEN ..
        * */
        _isAnimationRunning.value = true
        viewModelScope.launch {
            while (_isAnimationRunning.value) {
                updateBallPosition()
                delay(100L)
            }
        }
    }

    fun stopAnimation() {
        /*
        * WHEN Quit button is clicked THEN ..
        * */
        _isAnimationRunning.value = false
        updateMaxScore() // Coroutine!
        resetGame()
    }

    /*
    * EVENT-HANDLER:
    * WHEN ball touches paddle THEN reset ball's heading
    *
    * WHEN ball touches paddle.. (sprite collision)
    *   Checks in game loop if ball hit paddle and updates velocity
    *   for bounce.
    * THEN reset ball's heading.. (callback)
    *   checkBallHitsPaddle() checks for collision
    *   flipBallHeading() updates ball heading
    * Actors:
    * */

    fun checkBallHitsPaddle(paddleSize: Size) {
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
            playSound(SOUND.NOINK)
            Log.d(TAG, "BALL TOUCHED ME! Velocity: ${ballVelocity.value.x}")
        } else if (ballBounds.overlaps(paddleBoundsR)) {
            incrementScore(reward)
            flipBallHeading(Direction.LEFT)
            playSound(SOUND.NOINK)
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

    /*
    * EVENT-HANDLER:
    * WHEN ball reaches canvas edge
    *
    * THEN rebound or end game depending on the edge
    * WHEN ball reaches canvas edge.. (edge reached event)
    *   Checks in game loop if ball has reached canvas edge
    *   and rebounds the ball or ends the game according to
    *   the edge reached.
    * THEN rebound or end game depending on the edge.. (callback)
    *   handleBallHitsEdge() implements check
    *   flipBallHeading()
    *   handleGameOver()
    * Actors:
    *   _isGameOver flags end of game
    * */
    private val _isGameOver = mutableStateOf(false)
    val isGameOver: State<Boolean> = _isGameOver

    enum class Edge {
        LEFT, RIGHT, TOP, BOTTOM
    }

    fun handleBallHitsEdge(canvasSize: Size) {
        val edge: Edge

        if (ballXY.value.y - ballRadius.value <= 0f) {
            edge = Edge.TOP
            flipBallHeading(Direction.DOWN)
            playSound(SOUND.NOTE)
        } else if (ballXY.value.x + ballRadius.value >= canvasSize.width) {
            edge = Edge.RIGHT
            handleGameOver()
            playSound(SOUND.BUZZER)
        } else if (ballXY.value.y + ballRadius.value >= canvasSize.height) {
            edge = Edge.BOTTOM
            flipBallHeading(Direction.UP)
            playSound(SOUND.NOTE)
        } else if (ballXY.value.x - ballRadius.value <= 0f) {
            edge = Edge.LEFT
            handleGameOver()
            playSound(SOUND.BUZZER)
        }
    }

    fun handleGameOver() {
        /*
        * Ends the game by setting _isAnimationRunning to false
        * causing forever-loop that updates ball's position to exit.
        * Launches pop-up with "Game over!" message.
        * Persists the max score by calling updateMaxScore().
        * */
        _isAnimationRunning.value = false   // End game
        _isGameOver.value = true            // Show pop-up message
        updateMaxScore()                    // Persist max score
    }

    fun resetGame() {
        _score.value = 0
        _rectXYL.value = Offset.Zero
        _rectXYR.value = Offset.Zero
        _ballXY.value = Offset(150f, 50f)
        _isGameOver.value = false
    }

    /*
    * EVENT-HANDLER:
    * WHEN button clicked THEN turn de/activate gravity
    *
    * WHEN button clicked.. (click event)
    *   Press button to de/activate gravity
    * THEN turn de/activate gravity.. (callback)
    *   toggleGravity()
    * Actors:
    *   _isGravityEnabled
    *   gravityAcceleration
    * */

    private val gravityAcceleration = Offset(0.0f, 1.5f)

    private val _isGravityEnabled = mutableStateOf(true)
    val isGravityEnabled: State<Boolean> = _isGravityEnabled
    fun toggleGravity() {
        _isGravityEnabled.value = !_isGravityEnabled.value
    }
}

