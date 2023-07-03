package com.example.pong

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.pong.ui.theme.PongTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room

class MainActivity : ComponentActivity() {

    // Initialize Room Database
    private val scoreDB by lazy {
        Room.databaseBuilder(
            applicationContext,
            MyScoreDB::class.java,
            "my_score_database"
        ).build()
    }

    private val gameViewModel by viewModels<GameViewModel>(
        factoryProducer = {
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return GameViewModel(scoreDB.MyScoreDao()) as T
                }
            }
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContent {
            PongTheme {
                // A surface container using the 'background' color from the theme
                // val gameViewModel: GameViewModel = viewModel()

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
            Button(onClick = {  gameViewModel.startAnimation() }) {
                Text("START")
            }
            Button(onClick = { gameViewModel.stopAnimation() }) {
                Text("QUIT")
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(text = "Score: ${gameViewModel.score.value}\nMaxScore: ${gameViewModel.maxScore.value}")
        }
        Box(modifier = Modifier
            .fillMaxSize()
            .background(Color.Blue)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    // Update paddle
                    Log.d(TAG, "Change amount ${dragAmount.y}")
                    gameViewModel.setDeltaY(dragAmount.y)
                    gameViewModel.setTouchXY(change.position)
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

    Canvas(
        modifier = Modifier
            .fillMaxSize(),
    ) {

        /*
        * This is effectively the Game Loop in Jetpack Compose.
        * There is no forever loop here, so what makes it a Game Loop?
        * The answer lies in the startAnimation coroutine,
        * where the ball's xy coordinates are continuously updated.
        * This coroutine is defined in the View Model (VM) and
        * invoked when the Start button is pressed.
        * Since the xy coordinates are a state variable and determine
        * the composition, the canvas is recomposed with each update.
        * Thus making this canvas code part of a Game Loop.
        * Here, we manage the paddles and their interaction with ball.
        *
        * */
        // Setup params for drawing in relation to canvas dimensions
        val TAG = "GAME CANVAS"
        val canvasSize = Size(size.width, size.height)
        val paddleSize = Size(0.03f * canvasSize.width, 0.45f * canvasSize.height)

        gameViewModel.setRectXYL( Offset(0.01f*canvasSize.width, rectXYL.value.y) )
        gameViewModel.setRectXYR( Offset((canvasSize.width - 0.01f*canvasSize.width - paddleSize.width), rectXYR.value.y) )

        // Check if paddle touched, knowing paddle location and dimensions
        gameViewModel.dragPaddleL(paddleSize, canvasSize)
        gameViewModel.dragPaddleR(paddleSize, canvasSize)

        // Handle collisions of ball with paddles, edges
        gameViewModel.checkBallHitsPaddle(paddleSize)

        // Check game over condition
        gameViewModel.handleBallHitsEdge(canvasSize)

        // Render paddles and ball
        drawRect(
            Color.Green,
            topLeft = rectXYL.value,
            size = Size(paddleSize.width, paddleSize.height))
        drawRect(
            Color.Green,
            topLeft = rectXYR.value,
            size = Size(paddleSize.width, paddleSize.height))
        drawCircle(
            color = Color.Cyan,
            center = ballXY.value,
            radius = ballRadius.value
        )
    } // end CANVAS
}
