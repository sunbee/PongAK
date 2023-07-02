package com.example.pong

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.pong.ui.theme.PongTheme
import kotlinx.coroutines.delay
import androidx.lifecycle.viewmodel.compose.viewModel


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PongTheme {
                // A surface container using the 'background' color from the theme
                val gameViewModel: GameViewModel = viewModel()

                GameScreen(gameViewModel = gameViewModel)
            }
        }
    }
}

@Composable
fun GameScreen(gameViewModel: GameViewModel) {

    val TAG = "GAME SCREEN"

    Column(modifier = Modifier
        .fillMaxSize()) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = {  gameViewModel.setIsAnimationRunning(flag = true) }) {
                Text("START")
            }
            Button(onClick = { gameViewModel.setIsAnimationRunning(flag = false) }) {
                Text("QUIT")
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(text = "Score: ${gameViewModel.score}")
        }
        Box(modifier = Modifier
            .fillMaxSize()
            .background(Color.Blue)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    // Update paddle
                    Log.d(TAG, "Change amount ${dragAmount.y}")
                    gameViewModel.setDeltaY( dragAmount.y )
                    gameViewModel.setTouchXY( change.position )
                }
            }) {
            GameCanvas(gameViewModel)
        } // end BOX
    } // End Column

}

@Preview(showBackground = true, device = Devices.AUTOMOTIVE_1024p, widthDp = 720, heightDp = 360)
@Composable
fun GamePreview() {
    PongTheme {
        //Greeting("Android")
        val gameViewModel: GameViewModel = viewModel()
        GameScreen(gameViewModel = gameViewModel)
    }
}

@Composable
fun GameCanvas(gameViewModel: GameViewModel) {

    val score = gameViewModel.score
    val rectXYL = gameViewModel.rectXYL
    val rectXYR = gameViewModel.rectXYR
    val touchXY = gameViewModel.touchXY
    val deltaY = gameViewModel.deltaY
    val ballXY = gameViewModel.ballXY
    val ballVelocity = gameViewModel.ballVelocity
    val ballRadius = gameViewModel.ballRadius
    val isAnimationRunning = gameViewModel.isAnimationRunning

    LaunchedEffect(Unit) {
        /*
        * Animate the ball. The ball is rendered on canvas as a circle.
        * The ball's coordinates are updated in a forever loop in this coroutine
        * based on the velocity. This triggers recomposition of the canvas,
        * thus animating the ball.
        * */
            while (true) {
            if (isAnimationRunning.value) {
                withFrameNanos { frameTime ->
                    // Update ball position
                    gameViewModel.setBallXY(ballXY.value + ballVelocity.value)
                } // end withFrameNanos
            } // end IF ANIMATING
            delay(100L)
        } // end WHILE FOREVER
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize(),
    ) {

        /*
        * This is effectively the Game Loop in Jetpack Compose.
        * There is no forever loop here, so what makes it a Game Loop?
        * The answer lies in the LaunchedEffecrs coroutine,
        * where the ball's xy coordinates are continuously updated.
        * Since the xy coordinates are a state variable and determine
        * the composition, the canvas is recomposed with each update.
        * Thus making this canvas code part of a Game Loop.
        * Here, we manage the paddles and their interaction with ball.
        *
        * */
        // Setup params for drawing in relation to canvas dimensions
        val TAG = "GAME CANVAS"
        val canvasWidth = size.width
        val canvasHeight = size.height

        val paddleWidth = 0.03f * canvasWidth
        val paddleHeight = 0.45f * canvasHeight

        gameViewModel.setRectXYL( Offset(0.01f*canvasWidth, rectXYL.value.y) )
        gameViewModel.setRectXYR( Offset((canvasWidth - 0.01f*canvasWidth - paddleWidth), rectXYR.value.y) )

        // Check if paddle touched, knowing paddle location and dimensions
        if (touchXY.value.x in (rectXYL.value.x..rectXYL.value.x+paddleWidth) &&
            touchXY.value.y in rectXYL.value.y..(rectXYL.value.y+paddleHeight)) {
            Log.d(TAG, "YOU TOUCHED ME!")
            val newY = (rectXYL.value.y + deltaY.value).coerceIn(0f, canvasHeight-paddleHeight)
            gameViewModel.setRectXYL( Offset(rectXYL.value.x, newY) )
            gameViewModel.setDeltaY(0f)
        } // end IF
        if (touchXY.value.x in (rectXYR.value.x..rectXYR.value.x+paddleWidth) &&
            touchXY.value.y in rectXYR.value.y..(rectXYR.value.y+paddleHeight)) {
            Log.d(TAG, "YOU TOUCHED ME!")
            val newY = (rectXYR.value.y + deltaY.value).coerceIn(0f, canvasHeight-paddleHeight)
            gameViewModel.setRectXYR( Offset(rectXYR.value.x, newY) )
            gameViewModel.setDeltaY(0f) // So paddle stops when drag is paused
        } // end IF

        // Handle collisions of ball with paddles, edges
        val ballBounds = Rect(
            (ballXY.value.x - ballRadius.value),
            (ballXY.value.y - ballRadius.value),
            (ballXY.value.x + ballRadius.value),
            (ballXY.value.y + ballRadius.value)
        )
        val paddleBoundsL = Rect(
            offset = rectXYL.value,
            size = Size(paddleWidth, paddleHeight)
        )
        val paddleBoundsR = Rect(
            offset = rectXYR.value,
            size = Size(paddleWidth, paddleHeight)
        )
        if (ballBounds.overlaps(paddleBoundsL) || ballBounds.overlaps(paddleBoundsR)) {
            gameViewModel.setBallVelocity( Offset(ballVelocity.value.x * -1f, ballVelocity.value.y) )
            Log.d(TAG, "BALL TOUCHED ME! Velocity: ${ballVelocity.value.x}")
        }
        if (ballXY.value.y - ballRadius.value <= 0f || ballXY.value.y + ballRadius.value >= canvasHeight) {
            gameViewModel.setBallVelocity( Offset(ballVelocity.value.x, ballVelocity.value.y * -1f) )
        }

        // Check game over condition

        // Render paddles and ball
        drawRect(
            Color.Green,
            topLeft = rectXYL.value,
            size = Size(paddleWidth, paddleHeight))
        drawRect(
            Color.Green,
            topLeft = rectXYR.value,
            size = Size(paddleWidth, paddleHeight))
        drawCircle(
            color = Color.Cyan,
            center = ballXY.value,
            radius = ballRadius.value
        )
    } // end CANVAS
}
