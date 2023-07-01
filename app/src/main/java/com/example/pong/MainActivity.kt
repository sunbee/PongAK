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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PongTheme {
                // A surface container using the 'background' color from the theme
                GameScreen()
            }
        }
    }
}

@Composable
fun GameScreen() {
    val TAG = "GAME SCREEN"
    val score = remember { mutableStateOf(0f) }
    val rectXYL = remember { mutableStateOf(Offset(0f, 0f)) }
    val rectXYR = remember { mutableStateOf(Offset(0f, 0f)) }
    val touchXY = remember { mutableStateOf(Offset(0f, 0f)) }
    val deltaY = remember { mutableStateOf(0f) }
    val ballXY =  remember { mutableStateOf(Offset(150f, 50f)) }
    val ballVelocity = remember { mutableStateOf(Offset(90f, 15f)) }
    val ballRadius = 45f
    val isAnimationRunning = remember { mutableStateOf(true) }

    Column(modifier = Modifier
        .fillMaxSize()) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { isAnimationRunning.value = true }) {
                Text("START")
            }
            Button(onClick = { isAnimationRunning.value = false }) {
                Text("QUIT")
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(text = "Score: ${score.value}")
        }
        Box(modifier = Modifier
            .fillMaxSize()
            .background(Color.Blue)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    // Update paddle
                    Log.d(TAG, "Change amount ${dragAmount.y}")
                    deltaY.value = dragAmount.y
                    touchXY.value = change.position
                }
            }) {
            GameCanvas(
                rectXYL = rectXYL,
                rectXYR = rectXYR,
                touchXY = touchXY,
                deltaY = deltaY,
                ballXY = ballXY,
                ballVelocity = ballVelocity,
                ballRadius = ballRadius,
                isAnimationRunning = isAnimationRunning
            )
        } // end BOX
    } // End Column

}

@Preview(showBackground = true, device = Devices.AUTOMOTIVE_1024p, widthDp = 720, heightDp = 360)
@Composable
fun GreetingPreview() {
    PongTheme {
        //Greeting("Android")
        GameScreen()
    }
}

@Composable
fun GameCanvas(
    rectXYL: MutableState<Offset>,
    rectXYR: MutableState<Offset>,
    touchXY: MutableState<Offset>,
    deltaY: MutableState<Float>,
    ballXY: MutableState<Offset>,
    ballVelocity: MutableState<Offset>,
    ballRadius: Float,
    isAnimationRunning: MutableState<Boolean>
) {
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
                    ballXY.value += ballVelocity.value
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
        * Effectively a Game Loop in Jetpack Compose.
        * The Canvas manages the paddles and their interaction with ball.
        * The ball's animation is controlled by the LaunchedEffects coroutine.
        * */
        // Setup params for drawing in relation to canvas dimensions
        val TAG = "GAME CANVAS"
        val canvasWidth = size.width
        val canvasHeight = size.height

        val paddleWidth = 0.03f * canvasWidth
        val paddleHeight = 0.45f * canvasHeight

        rectXYL.value = Offset(0.01f*canvasWidth, rectXYL.value.y)
        rectXYR.value = Offset((canvasWidth - 0.01f*canvasWidth - paddleWidth), rectXYR.value.y)

        // Check if paddle touched, knowing paddle location and dimensions
        if (touchXY.value.x in (rectXYL.value.x..rectXYL.value.x+paddleWidth) &&
            touchXY.value.y in rectXYL.value.y..(rectXYL.value.y+paddleHeight)) {
            Log.d(TAG, "YOU TOUCHED ME!")
            val newY = (rectXYL.value.y + deltaY.value).coerceIn(0f, canvasHeight-paddleHeight)
            rectXYL.value = Offset(rectXYL.value.x, newY)
            deltaY.value = 0f // So paddle stops when drag is paused
        } // end IF
        if (touchXY.value.x in (rectXYR.value.x..rectXYR.value.x+paddleWidth) &&
            touchXY.value.y in rectXYR.value.y..(rectXYR.value.y+paddleHeight)) {
            Log.d(TAG, "YOU TOUCHED ME!")
            val newY = (rectXYR.value.y + deltaY.value).coerceIn(0f, canvasHeight-paddleHeight)
            rectXYR.value = Offset(rectXYR.value.x, newY)
            deltaY.value = 0f // So paddle stops when drag is paused
        } // end IF

        // Handle collisions of ball with paddles, edges
        val ballBounds = Rect(
            (ballXY.value.x - ballRadius),
            (ballXY.value.y - ballRadius),
            (ballXY.value.x + ballRadius),
            (ballXY.value.y + ballRadius)
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
            ballVelocity.value = Offset(ballVelocity.value.x * -1f, ballVelocity.value.y)
            Log.d(TAG, "BALL TOUCHED ME! Velocity: ${ballVelocity.value.x}")
        }
        if (ballXY.value.y - ballRadius <= 0f || ballXY.value.y + ballRadius >= canvasHeight) {
            ballVelocity.value = Offset(ballVelocity.value.x, ballVelocity.value.y * -1f)
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
            color = Color.Red,
            center = ballXY.value,
            radius = ballRadius
        )
    } // end CANVAS
}
