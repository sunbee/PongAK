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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
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
    val density = LocalDensity.current
    var score = remember { mutableStateOf(0f) }
    var touchXY = remember { mutableStateOf(Offset(0f, 0f)) }
    val deltaY = remember { mutableStateOf(0f) }
    val paddleColor = remember { mutableStateOf(Color.Cyan) }
    val paddleTopLeft = remember { mutableStateOf(Offset(10f, 10f)) }
    var paddleSize = remember { mutableStateOf(Size(100f, 50f)) }

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
        } // end ROW
        Box(modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                paddleSize.value = Size(0.03f * it.size.width / density.density, 0.45f * it.size.height / density.density)
            }
            .background(Color.Blue)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    // Update paddle
                    Log.d(TAG, "Position ${change.position.x / density.density} Pad @ at ${paddleTopLeft.value.x}, ${paddleSize.value.width}")
                    deltaY.value = dragAmount.y.toFloat()
                    touchXY.value = change.position / density.density
                }
            }
        ) {
            Paddle(color = paddleColor.value, topLeft = paddleTopLeft.value, size = paddleSize.value)
            if (touchXY.value.x in (paddleTopLeft.value.x..paddleTopLeft.value.x + paddleSize.value.width) &&
                touchXY.value.y in paddleTopLeft.value.y..(paddleTopLeft.value.y + paddleSize.value.height)) {
                Log.d(TAG, "YOU TOUCHED ME!")
                paddleTopLeft.value = Offset(paddleTopLeft.value.x, paddleTopLeft.value.y + deltaY.value / density.density)
                deltaY.value = 0f
            } // end IF
        } // end BOX
    } // End Column
}

@Composable
fun Paddle(color: Color, topLeft: Offset, size: Size) {
    Box(
        modifier = Modifier
            .offset(topLeft.x.dp, topLeft.y.dp)
            .size(size.width.dp, size.height.dp)
            .background(color)
    )
}

@Preview(showBackground = true, device = Devices.AUTOMOTIVE_1024p, widthDp = 720, heightDp = 360)
@Composable
fun GreetingPreview() {
    PongTheme {
        //Greeting("Android")
        GameScreen()
    }
}

