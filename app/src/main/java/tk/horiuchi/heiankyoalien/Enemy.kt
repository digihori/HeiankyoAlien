package tk.horiuchi.heiankyoalien

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import kotlin.random.Random

class Enemy(
    private val context: Context,
    private val gameMap: GameMap,
    private val player: Player,   // プレイヤーの位置を参照
    val id: Int,                  // 識別用
    private val soundManager: SoundManager? = null
) {
    var x: Int = 1
    var y: Int = 1
    private val bitmap1 = BitmapFactory.decodeResource(context.resources, R.drawable.alien1)
    private val bitmap2 = BitmapFactory.decodeResource(context.resources, R.drawable.alien2)
    private var lastFrameChangeTime = 0L
    private val frameDuration = 500L  // 500msごとに切り替え
    private var currentFrame = 0


    // 初期位置
    var initialX: Int = 1
    var initialY: Int = 1

    fun draw(canvas: Canvas, paint: Paint, tileSize: Int) {
        //if (state == State.FALLEN) return
        val bitmap = if (currentFrame == 0) bitmap1 else bitmap2
        val left = (x - 1) * tileSize
        val top = (y - 1) * tileSize
        canvas.drawBitmap(bitmap, null, Rect(left, top, left + tileSize, top + tileSize), paint)
    }

    var direction: Direction = Direction.DOWN

    enum class State {
        WAITING,    // ステージ開始前の待機中
        ACTIVE,     // 通常移動中
        FALLEN,     // 穴に落ちた
        DEAD        // 埋められて倒された
    }

    var state = State.WAITING
    private var lastMoveTime = 0L
    var fallTime = 0L
        private set

    var moveInterval = 1000L  // 初期は1秒に1マス
    var aiLevel = 0           // 0=完全ランダム、1〜追尾性UP

    fun start(currentTime: Long) {
        state = State.ACTIVE
        lastMoveTime = currentTime
    }

    fun update(currentTime: Long) {
        when (state) {
            State.ACTIVE -> {
                if (currentTime - lastMoveTime >= moveInterval) {
                    moveOneStep()
                    lastMoveTime = currentTime

                    // 移動先が穴だったら落下
                    if (gameMap.isHole(x, y)) {
                        soundManager?.play(SoundManager.SoundEffect.FALL)
                        state = State.FALLEN
                        fallTime = currentTime
                    }
                }
            }

            State.FALLEN -> {
                if (currentTime - fallTime >= 5000L) {
                    // 5秒経過で復活（その場でOKとする）
                    if (gameMap.tiles[y][x] == TileType.HOLE) {
                        // まだ埋められてない → 這い出す
                        state = State.ACTIVE
                    } else {
                        // 埋められていた → 消滅
                        state = State.DEAD
                    }
                }
            }

            else -> {} // WAITING, DEAD は何もしない
        }
        // アニメーションフレーム切り替え
        if (currentTime - lastFrameChangeTime >= frameDuration) {
            currentFrame = (currentFrame + 1) % 2
            lastFrameChangeTime = currentTime
        }
    }

    private fun moveOneStep() {
        val directions = getAvailableDirections()
        direction = selectDirection(directions)
        x += direction.dx
        y += direction.dy
    }

    private fun getAvailableDirections(): List<Direction> {
        return Direction.values().filter { dir ->
            val nx = x + dir.dx
            val ny = y + dir.dy

            // 移動可能なマス かつ 他の敵がいないマス
            gameMap.isMovableForEnemy(nx, ny) &&
                    !isOtherEnemyAt(nx, ny)
        }
    }

    private fun isOtherEnemyAt(nx: Int, ny: Int): Boolean {
        return gameMap.enemies.any { it !== this && it.x == nx && it.y == ny && it.state != Enemy.State.DEAD }
    }


    private fun selectDirection(options: List<Direction>): Direction {
        if (options.isEmpty()) return direction

        val scored = options.map { dir ->
            val nx = x + dir.dx
            val ny = y + dir.dy
            val dist = manhattan(nx, ny, player.x, player.y)
            Pair(dir, dist)
        }.sortedBy { it.second }

        return when (aiLevel) {
            0 -> options.random()
            1 -> if (Random.nextFloat() < 0.5f) scored[0].first else options.random()
            2 -> if (Random.nextFloat() < 0.7f) scored[0].first else scored.take(2).random().first
            3 -> if (Random.nextFloat() < 0.85f) scored[0].first else scored.take(2).random().first
            else -> scored[0].first // 100%で最短方向を選ぶ
        }
    }

    private fun manhattan(x1: Int, y1: Int, x2: Int, y2: Int): Int {
        return kotlin.math.abs(x1 - x2) + kotlin.math.abs(y1 - y2)
    }

    fun isAlive(): Boolean {
        return state != State.DEAD
    }

    fun isFalling(): Boolean {
        return state == State.FALLEN
    }
}
