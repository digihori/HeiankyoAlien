package tk.horiuchi.heiankyoalien

import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import kotlin.math.min

class GameView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var tileSize = 96  // 1マスのピクセルサイズ
    private val gameMap = GameMap()
    private val enemies = mutableListOf<Enemy>()
    private val scoreManager = ScoreManager { newScore ->
        scoreUpdateListener?.invoke(newScore)
    }

    private lateinit var soundManager: SoundManager

    //private val player = Player(context, gameMap, enemies, scoreManager, soundManager)
    private lateinit var player: Player

    //private var startTime = SystemClock.elapsedRealtime()
    //private var currentTime = startTime

    private var stage = 1
    private var score = 0
    private var lives = 3
    private var isGameOver = false

    private var isStageClearing = false
    private var stageClearStartTime = 0L
    private var stageClearDetectedTime: Long = 0L
    private var stageStartTime: Long = 0
    private var pauseCounter = 0

    private val paint = Paint()


    init {
        isFocusable = true
        isFocusableInTouchMode = true
        soundManager = SoundManager(context)
        player = Player(context, gameMap, enemies, scoreManager, soundManager)
        startStage(stage)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        soundManager.release() // ★リソース解放
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // 実際に表示するのは中央 7x7 のみ
        val visibleTilesX = 7
        val visibleTilesY = 7

        tileSize = minOf(w / visibleTilesX, h / visibleTilesY)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // 現在の表示領域に合わせてtileSizeを決定
        val visibleTiles = 7
        val availableWidth = MeasureSpec.getSize(widthMeasureSpec)
        val availableHeight = MeasureSpec.getSize(heightMeasureSpec)

        tileSize = min(availableWidth, availableHeight) / visibleTiles

        val desiredSize = tileSize * visibleTiles
        setMeasuredDimension(desiredSize, desiredSize)
    }


    private fun startStage(stage: Int) {
        gameMap.reset()
        gameMap.enemies = enemies
        enemies.clear()
        stageStartTime = SystemClock.elapsedRealtime()
        player.resetDugHoles()
        soundManager.stopCurrent()

        for (enemy in gameMap.enemies) {
            enemy.moveInterval = 1000L  // ステージ開始時に速度リセット
        }
        // プレイヤー初期化
        player.x = 5
        player.y = 5

        // 敵数をステージに応じて決定
        val enemyCount = when (stage) {
            1 -> 2
            2 -> 3
            else -> 4
        }

        val positions = listOf(
            Pair(1, 1), Pair(1, 7), Pair(7, 1), Pair(7, 7)
        )

        for (i in 0 until enemyCount) {
            val e = Enemy(context, gameMap, player, i, soundManager)
            e.x = positions[i].first
            e.y = positions[i].second
            e.initialX = e.x  // ← 初期位置保存
            e.initialY = e.y
            //e.aiLevel = if (stage <= 2) 2 else (stage).coerceAtMost(4)
            e.aiLevel = when (stage) {
                1 -> 1
                2 -> 2
                3 -> 3
                else -> 4
            }
            enemies.add(e)
        }

        postDelayed({
            val t = SystemClock.elapsedRealtime()
            enemies.forEach { it.start(t) }
        }, 1000)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (isGamePaused()) {
            postInvalidateDelayed(16L)
            return
        }
        val now = SystemClock.elapsedRealtime()
        updateGame(now)

        // ステージクリア中は点滅演出（500ms周期）
        if (isStageClearing && (now - stageClearStartTime) / 250 % 2 == 0L) {
            // GameView内のフィールドだけを白で塗る
            paint.style = Paint.Style.FILL
            paint.color = Color.WHITE
            val fieldWidth = gameMap.width * tileSize
            val fieldHeight = gameMap.height * tileSize
            canvas.drawRect(0f, 0f, fieldWidth.toFloat(), fieldHeight.toFloat(), paint)
        } else {
            canvas.drawColor(Color.BLACK)
            drawField(canvas)
            drawEnemies(canvas)
            drawPlayer(canvas)
            drawStatus(canvas)
        }

        postInvalidateDelayed(16L)
    }

    private fun updateGame(time: Long) {
        if (isGameOver) return

        // ★ 点滅中はゲーム進行を停止
        if (player.isBlinking) {
            if (time - player.blinkStartTime >= player.blinkDuration) {
                player.isBlinking = false
                if (lives <= 0) {
                    isGameOver = true
                    soundManager.play(SoundManager.SoundEffect.OVER)
                } else {
                    // プレイヤーを初期位置に戻す
                    player.x = player.initialX
                    player.y = player.initialY
                    // 敵も初期位置に戻す
                    enemies.forEach { enemy ->
                        if (enemy.state != Enemy.State.DEAD) {
                            enemy.x = enemy.initialX
                            enemy.y = enemy.initialY
                            enemy.state = Enemy.State.WAITING  // 状態リセット（必要に応じて）
                            enemy.start(SystemClock.elapsedRealtime())  // すぐ動き出す
                        }
                    }
                    gameMap.reset()
                    player.resetDugHoles()
                }
            }
            return  // ← 点滅中は以降の処理をスキップ
        }

        // ここで30秒経過チェックを行い、敵の速度を調整
        val elapsed = time - stageStartTime
        if (elapsed > 60_000L) {
            for (enemy in gameMap.enemies) {
                if (enemy.moveInterval > 500L) {
                    enemy.moveInterval = 500L
                }
            }
        }

        player.update(time)

        //敵の移動
        enemies.forEach { it.update(time) }

        // 衝突チェック
        enemies.forEach { e ->
            if (e.isAlive() && !e.isFalling() && e.x == player.x && e.y == player.y) {
                lives--
                soundManager.play(SoundManager.SoundEffect.DEAD)
                player.isBlinking = true
                player.blinkStartTime = time
                statusUpdateListener?.invoke(stage, lives)
                return  // 1回だけ処理
            }
        }

        // ステージクリア判定（敵全滅検出 → 1秒待って点滅開始）
        if (!isStageClearing && stageClearDetectedTime == 0L && enemies.all { it.state == Enemy.State.DEAD }) {
            stageClearDetectedTime = time
            soundManager.play(SoundManager.SoundEffect.CLEAR)
        }

        if (!isStageClearing && stageClearDetectedTime > 0L && time - stageClearDetectedTime > 1000L) {
            isStageClearing = true
            stageClearStartTime = time
            stageClearDetectedTime = 0L  // リセット
        }

        if (isStageClearing) {
            val elapsed = time - stageClearStartTime
            if (elapsed > 2000L) {
                stage++
                startStage(stage)
                statusUpdateListener?.invoke(stage, lives)
                isStageClearing = false
            }
        }
    }

    private fun drawField(canvas: Canvas) {
        for (y in 1 until gameMap.height - 1) { // ← 外周を除く1〜7
            for (x in 1 until gameMap.width - 1) {
                val tile = gameMap.tiles[y][x]

                val left = (x - 1) * tileSize // 表示上は7x7で描画
                val top = (y - 1) * tileSize
                val right = left + tileSize
                val bottom = top + tileSize

                val cx = left + tileSize / 2f
                val cy = top + tileSize / 2f
                val radius = tileSize / 2.5f

                paint.style = Paint.Style.FILL
                paint.color = when (tile) {
                    TileType.WALL -> Color.DKGRAY
                    TileType.PATH, TileType.CROSS, TileType.HOLE -> Color.WHITE
                    TileType.FILLED -> Color.GRAY
                }

                canvas.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), paint)

                // 穴だけ特殊描画
                if (tile == TileType.HOLE && !(player.state == Player.State.FILLING && x == player.targetX && y == player.targetY)) {
                    paint.style = Paint.Style.FILL
                    paint.color = Color.BLUE
                    canvas.drawCircle(cx, cy, radius, paint)
                }
            }
        }

        // 穴埋め中のアニメーションも補正（中央7x7基準で再配置）
        if (player.state == Player.State.FILLING) {
            val tx = player.targetX
            val ty = player.targetY

            if (tx in 1..7 && ty in 1..7) {
                val cx = (tx - 1) * tileSize + tileSize / 2f
                val cy = (ty - 1) * tileSize + tileSize / 2f

                val maxRadius = tileSize / 2.5f
                val radius = when (player.actionProgress) {
                    0 -> maxRadius
                    1 -> maxRadius * 0.66f
                    2 -> maxRadius * 0.33f
                    else -> 0f
                }

                if (radius > 0f) {
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 6f
                    paint.color = Color.BLUE
                    canvas.drawCircle(cx, cy, radius, paint)
                }
            }
        }
    }


    private fun drawPlayer(canvas: Canvas) {
        val now = SystemClock.elapsedRealtime()
        if (player.isBlinking) {
            if ((now - player.blinkStartTime) >= player.blinkDuration) {
                player.isBlinking = false
            } else {
                // 点滅中 → 500ms周期で表示・非表示切り替え
                val blinkCycle = 250L
                if ((now / blinkCycle) % 2 == 0L) {
                    player.draw(canvas, paint, tileSize)
                }
                return
            }
        }

        player.draw(canvas, paint, tileSize)
    }

    private fun drawEnemies(canvas: Canvas) {
        val now = SystemClock.elapsedRealtime()
        enemies.forEach { enemy ->
            // 死亡済みの敵は描画しない
            if (enemy.state == Enemy.State.DEAD) return@forEach

            // 穴埋め中に該当位置のFALLEN敵は描かない
            val isSameHole = enemy.x == player.targetX && enemy.y == player.targetY
            val isFilling = player.state == Player.State.FILLING
            val isFallen = enemy.state == Enemy.State.FALLEN

            if ((isFilling && isSameHole && isFallen) ||
                (gameMap.tiles[enemy.y][enemy.x] == TileType.FILLED && isFallen)) {
                return@forEach
            }

            // FALLEN（穴に落ちた）状態のときだけ点滅描画
            if (isFallen) {
                if ((now / 250) % 2 == 0L) {
                    enemy.draw(canvas, paint, tileSize)
                }
            } else {
                // 通常描画
                enemy.draw(canvas, paint, tileSize)
            }
        }
    }

    private fun drawStatus(canvas: Canvas) {
        if (isGameOver) {
            paint.textSize = 120f
            paint.color = Color.RED
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.setShadowLayer(10f, 4f, 4f, Color.BLACK)

            val centerX = width / 2f

            // フィールドの高さを元にセンタリング（上下中央）
            val fieldHeight = gameMap.height * tileSize
            val fm = paint.fontMetrics
            val textHeight = fm.bottom - fm.top
            val centerY = fieldHeight / 2f - textHeight / 2f - fm.top

            canvas.drawText("GAME OVER", centerX, centerY, paint)
            // 後処理（リセット）
            paint.textAlign = Paint.Align.LEFT
            paint.clearShadowLayer()
        }
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            if (isGameOver) {
                // タップでリスタート
                isGameOver = false
                lives = 3
                stage = 1
                scoreManager.reset()
                startStage(stage)
            } else {
                val now = SystemClock.elapsedRealtime()
                if (gameMap.tiles[player.y][player.x] == TileType.HOLE) {
                    player.startFill(now)
                } else {
                    player.startDig(now)
                }
            }
        }
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (player.isBusy()) return true
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> move(Direction.UP)
            KeyEvent.KEYCODE_DPAD_DOWN -> move(Direction.DOWN)
            KeyEvent.KEYCODE_DPAD_LEFT -> move(Direction.LEFT)
            KeyEvent.KEYCODE_DPAD_RIGHT -> move(Direction.RIGHT)
            KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BUTTON_START -> act()
        }
        return true
    }

    fun move(direction: Direction) {
        if (!player.isBusy() && !isGameOver && !player.isBlinking) {
            player.tryMove(direction)
        }
    }

    fun act() {
        if (isGameOver) {
            // Aボタン or 穴掘りボタンで再スタート
            isGameOver = false
            lives = 3
            stage = 1
            scoreManager.reset()
            startStage(stage)
            return
        }
        if (!player.isBusy() && !player.isBlinking) {
            val now = SystemClock.elapsedRealtime()

            val tx = player.x + player.direction.dx
            val ty = player.y + player.direction.dy

            if (tx in 0 until gameMap.width && ty in 0 until gameMap.height &&
                gameMap.tiles[ty][tx] == TileType.HOLE) {
                player.startFill(now)
            } else {
                player.startDig(now)
            }
        }
    }

    private var scoreUpdateListener: ((Int) -> Unit)? = null

    fun setScoreUpdateListener(listener: (Int) -> Unit) {
        scoreUpdateListener = listener
        listener.invoke(scoreManager.score)  // 現在のスコアを即通知
    }

    private var statusUpdateListener: ((Int, Int) -> Unit)? = null

    fun setStatusUpdateListener(listener: (stage: Int, lives: Int) -> Unit) {
        statusUpdateListener = listener
        listener.invoke(stage, lives) // 初期通知
    }

    fun pauseGame() {
        soundManager.stopCurrent()
        pauseCounter++
    }

    fun resumeGame() {
        if (pauseCounter > 0) pauseCounter--
    }

    private fun isGamePaused(): Boolean {
        return pauseCounter > 0
    }
}
