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
import androidx.compose.runtime.MutableState
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
import androidx.compose.ui.graphics.drawscope.DrawScope
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
    var score = remember { mutableStateOf(0f) }
    val rectXYL = remember { mutableStateOf(Offset(0f, 0f)) }
    val rectXYR = remember { mutableStateOf(Offset(0f, 0f)) }
    var touchXY = remember { mutableStateOf(Offset(0f, 0f)) }
    val deltaY = remember { mutableStateOf(0f) }

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
            GameCanvas(rectXYL = rectXYL, rectXYR = rectXYR, touchXY = touchXY, deltaY = deltaY)
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
    deltaY: MutableState<Float>
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        val TAG = "GAME CANVAS"
        val canvasWidth = size.width.toFloat()
        val canvasHeight = size.height.toFloat()

        val paddleWidth = 0.03f * canvasWidth
        val paddleHeight = 0.45f * canvasHeight

        rectXYL.value = Offset(0.01f*canvasWidth, rectXYL.value.y)
        rectXYR.value = Offset((canvasWidth - 0.01f*canvasWidth - paddleWidth), rectXYR.value.y)

        if (touchXY.value.x in (rectXYL.value.x..rectXYL.value.x+paddleWidth) &&
            touchXY.value.y in rectXYL.value.y..(rectXYL.value.y+paddleHeight)) {
            Log.d(TAG, "YOU TOUCHED ME!")
            val newY = (rectXYL.value.y + deltaY.value).coerceIn(0f, canvasHeight-paddleHeight)
            rectXYL.value = Offset(rectXYL.value.x, newY)
            deltaY.value = 0f // So paddle stops when drag is paused
        } // end IF
        drawRect(
            Color.Green,
            topLeft = rectXYL.value,
            size = Size(paddleWidth, paddleHeight))
        if (touchXY.value.x in (rectXYR.value.x..rectXYR.value.x+paddleWidth) &&
            touchXY.value.y in rectXYR.value.y..(rectXYR.value.y+paddleHeight)) {
            Log.d(TAG, "YOU TOUCHED ME!")
            val newY = (rectXYR.value.y + deltaY.value).coerceIn(0f, canvasHeight-paddleHeight)
            rectXYR.value = Offset(rectXYR.value.x, newY)
            deltaY.value = 0f // So paddle stops when drag is paused
        } // end IF
        drawRect(
            Color.Green,
            topLeft = rectXYR.value,
            size = Size(paddleWidth, paddleHeight))


    } // end CANVAS
}

