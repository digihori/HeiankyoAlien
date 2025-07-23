package tk.horiuchi.heiankyoalien

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View.MeasureSpec
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.min

class DebugCoffeeBreakActivity : AppCompatActivity() {

    private lateinit var surfaceView: SurfaceView
    private lateinit var thread: CoffeeBreakThread
    private lateinit var coffeeBreakScene: CoffeeBreakScene

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        surfaceView = object : SurfaceView(this), SurfaceHolder.Callback {
            init {
                holder.addCallback(this)
            }

            override fun surfaceCreated(holder: SurfaceHolder) {
                val screenWidth = width
                val tileSize = screenWidth / 7

                coffeeBreakScene = CoffeeBreakScene(
                    context = context,
                    onFinished = {},
                    scaleFactor = 1.0f,
                    frameInterval = 100L
                )
                coffeeBreakScene.start(tileSize, screenWidth)

                thread = CoffeeBreakThread(holder, coffeeBreakScene, tileSize)
                thread.running = true
                thread.start()
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                thread.running = false
                thread.join()
            }
        }

        setContentView(surfaceView)
    }

    private class CoffeeBreakThread(
        private val holder: SurfaceHolder,
        private val scene: CoffeeBreakScene,
        private val tileSize: Int
    ) : Thread() {
        var running = false
        private val paint = Paint()

        override fun run() {
            while (running) {
                val canvas: Canvas? = holder.lockCanvas()
                if (canvas != null) {
                    synchronized(holder) {
                        canvas.drawColor(Color.BLACK)
                        //scene.update()
                        scene.draw(canvas, paint, canvas.width, tileSize)
                    }
                    holder.unlockCanvasAndPost(canvas)
                }

                sleep(33) // 約30fps
            }
        }
    }
}
