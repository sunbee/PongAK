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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.pong.ui.theme.PongTheme

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
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Composable
fun GameScreen() {
    val TAG: String = "GAME SCREEN"
    var score = remember {
        mutableStateOf(0f)
    }
    var touchXY = remember {
        mutableStateOf(Offset(0f, 0f))
    }
    val paddleY = remember {
        mutableStateOf(0f)
    }
    val paddleX = remember {
        0f
    }
    val deltaY = remember {
        mutableStateOf(0f)
    }

    Column(modifier = Modifier
        .fillMaxSize()) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { /*TODO*/ }) {
                Text("START")
            }
            Button(onClick = { /*TODO*/ }) {
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
                    deltaY.value = dragAmount.y.toFloat()
                    touchXY.value = change.position
                }
            }) {
            Canvas(modifier = Modifier
                .fillMaxSize(),
            ) {
                val canvasWidth = size.width.toFloat()
                val canvasHeight = size.height.toFloat()

                val paddleWidth = 0.03 * canvasWidth
                val paddleHeight = 0.45 * canvasHeight

                if (touchXY.value.x in (paddleX..paddleX+paddleWidth.toFloat()) &&
                        touchXY.value.y in paddleY.value..(paddleY.value + paddleHeight.toFloat())) {
                            Log.d(TAG, "YOU TOUCHED ME!")
                            drawRect(
                                Color.Cyan,
                                topLeft = Offset(10f, paddleY.value),
                                size = Size(paddleWidth.toFloat(), paddleHeight.toFloat()))
                                paddleY.value += deltaY.value
                                deltaY.value = 0f
                } // end IF
                drawRect(Color.Cyan, topLeft = Offset(10f, paddleY.value), size = Size(paddleWidth.toFloat(), paddleHeight.toFloat()))
            } // end CANVAS
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
