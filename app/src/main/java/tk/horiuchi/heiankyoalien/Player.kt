package tk.horiuchi.heiankyoalien

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log


class Player(
    private val context: Context,
    private val gameMap: GameMap,
    private val enemies: List<Enemy>,
    private val scoreManager: ScoreManager,
    private val soundManager: SoundManager
) {
    var x: Int = 3  // 初期位置（中央）
    var y: Int = 3
    var direction: Direction = Direction.UP
    var targetX = 0
        private set
    var targetY = 0
        private set
    private val dugHoles = mutableListOf<Pair<Int, Int>>()

    var isBlinking = false
    var blinkStartTime = 0L
    val blinkDuration = 2000L // 点滅時間（ミリ秒）
    // 初期位置を記録しておく（毎ステージで設定）
    val initialX = 5
    val initialY = 5

    private val bitmapUp = BitmapFactory.decodeResource(context.resources, R.drawable.player_up)
    private val bitmapDown = BitmapFactory.decodeResource(context.resources, R.drawable.player_down)
    private val bitmapLeft = BitmapFactory.decodeResource(context.resources, R.drawable.player_left)
    private val bitmapRight = BitmapFactory.decodeResource(context.resources, R.drawable.player_right)

    fun draw(canvas: Canvas, paint: Paint, tileSize: Int) {
        val bitmap = when (direction) {
            Direction.UP -> bitmapUp
            Direction.DOWN -> bitmapDown
            Direction.LEFT -> bitmapLeft
            Direction.RIGHT -> bitmapRight
        }

        val left = (x - 1) * tileSize
        val top = (y - 1) * tileSize
        canvas.drawBitmap(bitmap, null, Rect(left, top, left + tileSize, top + tileSize), paint)

        // 穴掘り・埋め中のアニメーションを追加
        if (state == State.DIGGING || state == State.FILLING) {
            drawHoleProgress(canvas, paint, tileSize)
        }
    }

    private fun drawHoleProgress(canvas: Canvas, paint: Paint, tileSize: Int) {
        val centerX = (targetX - 1 + 0.5f) * tileSize
        val centerY = (targetY - 1 + 0.5f) * tileSize

        val effectiveProgress = when (state) {
            State.DIGGING -> actionProgress
            State.FILLING -> 2 - actionProgress.coerceAtMost(2)
            else -> return
        }
        val radius = when (effectiveProgress) {
            0 -> tileSize * 0.2f
            1 -> tileSize * 0.3f
            2 -> tileSize * 0.4f
            else -> return
        }

        paint.style = Paint.Style.FILL
        paint.strokeWidth = 4f
        paint.color = if (state == State.DIGGING) Color.BLUE else Color.GRAY
        canvas.drawCircle(centerX, centerY, radius, paint)
    }

    enum class State {
        NORMAL,
        DIGGING,
        FILLING
    }

    var state: State = State.NORMAL
    var actionProgress = 0  // 穴掘り／埋めの進行段階（0〜3）

    private var actionTimer = 0L
    private val stepDuration = 500L  // 1段階あたり0.5秒（ms）

    fun update(currentTime: Long) {
        when (state) {
            State.DIGGING, State.FILLING -> {
                if (currentTime - actionTimer >= stepDuration) {
                    actionProgress++
                    actionTimer = currentTime
                    if (actionProgress >= 3) {
                        if (state == State.DIGGING) {
                            gameMap.digHole(targetX, targetY)
                            dugHoles.add(Pair(targetX, targetY)) // ← 掘った穴を記録
                        } else {
                            gameMap.fillHole(targetX, targetY)

                            // ★ 敵が穴にいたら即座にスコア加算＋DEAD
                            enemies.filter {
                                it.x == targetX && it.y == targetY && it.state == Enemy.State.FALLEN
                            }.forEach { enemy ->
                                val elapsed = currentTime - enemy.fallTime
                                val points = when {
                                    elapsed <= 3000L -> 300
                                    elapsed <= 4000L -> 200
                                    else -> 100
                                }
                                scoreManager.add(points)
                                soundManager.play(SoundManager.SoundEffect.HIT)
                                enemy.state = Enemy.State.DEAD
                            }
                            // 穴を埋めたら dugHoles から削除
                            dugHoles.remove(Pair(targetX, targetY))
                        }
                        state = State.NORMAL
                        actionProgress = 0
                    }
                }
            }
            else -> {} // 通常時は何もしない
        }
    }

    fun tryMove(dir: Direction) {
        if (state != State.NORMAL) return  // 操作中は移動できない

        direction = dir  // 向きは常に更新

        val newX = x + dir.dx
        val newY = y + dir.dy
        if (gameMap.isMovable(newX, newY)) {
            x = newX
            y = newY
            direction = dir
        }
    }

    fun startDig(currentTime: Long) {
        if (state == State.NORMAL) {
            val tx = x + direction.dx
            val ty = y + direction.dy

            Log.d("Player", "start Dig called: tx=$tx ty=$ty tile=${gameMap.tiles[ty][tx]}")

            if (gameMap.isMovable(tx, ty) &&
                gameMap.tiles[ty][tx] in listOf(TileType.PATH, TileType.CROSS)
            ) {
                // 既に掘った穴が2つあるかチェック
                if (dugHoles.size >= 2) {
                    Log.d("Player", "dig denied: dug hole limit reached")
                    return
                }
                soundManager.play(SoundManager.SoundEffect.DIG)
                state = State.DIGGING
                actionProgress = 0
                actionTimer = currentTime

                targetX = tx
                targetY = ty
            }
        }
    }

    fun startFill(currentTime: Long) {
        if (state == State.NORMAL) {
            val tx = x + direction.dx
            val ty = y + direction.dy

            Log.d("Player", "startFill called: tx=$tx ty=$ty tile=${gameMap.tiles[ty][tx]}")

            if (tx in 0 until gameMap.width && ty in 0 until gameMap.height &&
                gameMap.tiles[ty][tx] == TileType.HOLE
            ) {
                soundManager.play(SoundManager.SoundEffect.FILL)
                state = State.FILLING
                actionProgress = 0
                actionTimer = currentTime

                targetX = tx
                targetY = ty
            }
        }
    }

    fun isBusy(): Boolean {
        return state != State.NORMAL
    }

    fun resetDugHoles() {
        dugHoles.clear()
    }
}
