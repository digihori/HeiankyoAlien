package tk.horiuchi.heiankyoalien

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.media.MediaPlayer

class CoffeeBreakScene(
    context: Context,
    private val onFinished: () -> Unit,
    private val scaleFactor: Float = 1.0f,
    private val frameInterval: Long = 150L // フレーム間隔（ms）
) {
    private val playerRightBitmap: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.player_right)
    private val playerFrontBitmap: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.player_front)
    private val alienBitmap: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.alien1)
    private val alienFallBitmap: Bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.alien_fall)
    private val holeBitmaps: List<Bitmap> = listOf(
        BitmapFactory.decodeResource(context.resources, R.drawable.hole1),
        BitmapFactory.decodeResource(context.resources, R.drawable.hole2),
        BitmapFactory.decodeResource(context.resources, R.drawable.hole3)
    )

    private val handler = Handler(Looper.getMainLooper())
    private var step = 0
    private var playerX = -1
    private var playerOffset = 0f
    private var alienX = 7
    private var alienOffset = 0f
    private var holeStep = 0
    private var stepExecuted = mutableSetOf<Int>()
    private val stepHistory = mutableListOf<String>()
    private val stepRunnable: Runnable

    private var tileSize = 96
    private var screenWidth = 768
    private var screenHeight = 1024
    private var holeCenterX = 0
    private var isFinished = false

    private var mediaPlayer: MediaPlayer? = MediaPlayer.create(context, R.raw.bgm_coffee_break)

    init {
        stepRunnable = object : Runnable {
            override fun run() {
                update()
                handler.postDelayed(this, frameInterval)
            }
        }
        mediaPlayer?.isLooping = false
    }

    fun start(tileSize: Int, screenWidth: Int) {
        this.tileSize = tileSize
        this.screenWidth = screenWidth
        this.screenHeight = tileSize * 7
        this.playerX = -1
        this.alienX = 7
        this.holeCenterX = 3  // 中央のX座標（タイル単位）
        mediaPlayer?.seekTo(0)
        mediaPlayer?.start()
        handler.post(stepRunnable)
    }

    fun stop() {
        handler.removeCallbacks(stepRunnable)

        try {
            mediaPlayer?.let { mp ->
                runCatching {
                    if (mp.isPlaying) {
                        mp.stop()
                    }
                }
                mp.release()
            }
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            Log.e("CoffeeBreakScene", "MediaPlayer state error: ${e.message}")
        }

        mediaPlayer = null

        if (!isFinished) {
            isFinished = true
            onFinished.invoke()
        }
    }

    fun draw(canvas: Canvas, paint: Paint, screenWidth: Int, tileSize: Int) {
        val centerY = canvas.height / 2 - tileSize / 2

        // 穴（中央に固定表示）
        if (holeStep > 0 && holeStep <= 3) {
            val cx = (holeCenterX * tileSize)
            val dstRect = Rect(cx, centerY, cx + tileSize, centerY + tileSize)
            canvas.drawBitmap(holeBitmaps[holeStep - 1], null, dstRect, paint)
        }

        // エイリアン
        if (step in 0..8) {
            val ax = ((alienX + alienOffset) * tileSize).toInt()
            val dstRect = Rect(ax, centerY, ax + tileSize, centerY + tileSize)
            val bitmap = if (step >= 7) alienFallBitmap else alienBitmap
            canvas.drawBitmap(bitmap, null, dstRect, paint)
        }

        // プレイヤー
        val playerBitmap = if (step < 13) playerRightBitmap else playerFrontBitmap
        val px = ((playerX + playerOffset) * tileSize).toInt()
        val playerRect = Rect(px, centerY, px + tileSize, centerY + tileSize)
        canvas.drawBitmap(playerBitmap, null, playerRect, paint)
    }

    private fun update() {
        val speed = 0.125f

        when (step) {
            0 -> {
                playerOffset += speed
                if (playerOffset >= 1f) {
                    playerX++
                    playerOffset = 0f
                    if (playerX == holeCenterX - 1) step++
                }
            }
            1 -> {
                if (!stepExecuted.contains(step)) {
                    stepExecuted.add(step)
                    handler.postDelayed({ step++ }, 300)
                }
            }
            2, 3, 4 -> {
                if (!stepExecuted.contains(step)) {
                    holeStep = step - 1
                    stepExecuted.add(step)
                    handler.postDelayed({ step++ }, 500)
                }
            }
            5 -> {
                if (!stepExecuted.contains(step)) {
                    stepExecuted.add(step)
                    handler.postDelayed({ step++ }, 1000)
                }
            }
            6 -> {
                alienOffset -= speed
                if (alienOffset <= -1f) {
                    alienX--
                    alienOffset = 0f
                    if (alienX == holeCenterX) step++
                }
            }
            7 -> {
                if (!stepExecuted.contains(step)) {
                    stepExecuted.add(step)
                    step++
                }
            }
            8 -> {
                if (!stepExecuted.contains(step)) {
                    stepExecuted.add(step)
                    handler.postDelayed({ step++ }, 800)
                }
            }
            9, 10, 11 -> {
                if (!stepExecuted.contains(step)) {
                    holeStep = 12 - step
                    stepExecuted.add(step)
                    handler.postDelayed({ step++ }, 500)
                }
            }
            12 -> {
                if (!stepExecuted.contains(step)) {
                    holeStep = 0
                    stepExecuted.add(step)
                    handler.postDelayed({ step++ }, 500)
                }
            }
            13 -> {
                if (!stepExecuted.contains(step)) {
                    stepExecuted.add(step)
                    handler.postDelayed({ step++ }, 3000)
                    //step++
                }
            }
            14 -> {
                if (!isFinished) {
                    isFinished = true
                    Log.d("CoffeeBreakScene", "onFinished() called")
                    stop()
                    onFinished()
                }
            }
        }

        val log = "STEP:$step | P:$playerX(${String.format("%.2f", playerOffset)}) | A:$alienX(${String.format("%.2f", alienOffset)}) | H:$holeStep"
        stepHistory.add(log)
    }

    fun getStepHistory(): List<String> {
        return stepHistory
    }
}
