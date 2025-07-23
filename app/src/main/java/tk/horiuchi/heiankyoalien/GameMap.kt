package tk.horiuchi.heiankyoalien

import tk.horiuchi.heiankyoalien.TileType.*

class GameMap {

    val width = 9
    val height = 9
    lateinit var enemies: List<Enemy>

    // 初期マップ定義（7x7マス）
    private val baseMap = arrayOf(
        arrayOf(WALL, WALL, WALL, WALL, WALL, WALL, WALL, WALL, WALL),
        arrayOf(WALL, CROSS, PATH, CROSS, PATH, CROSS, PATH, CROSS, WALL),
        arrayOf(WALL, PATH, WALL, PATH, WALL, PATH, WALL, PATH, WALL),
        arrayOf(WALL, CROSS, PATH, CROSS, PATH, CROSS, PATH, CROSS, WALL),
        arrayOf(WALL, PATH, WALL, PATH, WALL, PATH, WALL, PATH, WALL),
        arrayOf(WALL, CROSS, PATH, CROSS, PATH, CROSS, PATH, CROSS, WALL),
        arrayOf(WALL, PATH, WALL, PATH, WALL, PATH, WALL, PATH, WALL),
        arrayOf(WALL, CROSS, PATH, CROSS, PATH, CROSS, PATH, CROSS, WALL),
        arrayOf(WALL, WALL, WALL, WALL, WALL, WALL, WALL, WALL, WALL),
    )

    // ベース（変更不可）を保持
    val baseTiles: Array<Array<TileType>> = Array(height) { y ->
        Array(width) { x ->
            baseMap[y][x]
        }
    }

    // 実行時マップ（穴や埋め状態などを保持）
    val tiles: Array<Array<TileType>> = Array(height) { y ->
        Array(width) { x ->
            baseMap[y][x]
        }
    }

    // 指定位置が通行可能かどうか
    fun isMovable(x: Int, y: Int): Boolean {
        if (x !in 0 until width || y !in 0 until height) return false

        return when (tiles[y][x]) {
            TileType.PATH, TileType.CROSS -> true
            TileType.HOLE, TileType.FILLED, TileType.WALL -> false
        }
    }

    fun isMovableForEnemy(x: Int, y: Int): Boolean {
        if (x !in 0 until width || y !in 0 until height) return false
        return tiles[y][x] in listOf(TileType.PATH, TileType.CROSS, TileType.HOLE)
    }

    // 穴を掘る
    fun digHole(x: Int, y: Int) {
        if (tiles[y][x] == TileType.PATH || tiles[y][x] == TileType.CROSS) {
            tiles[y][x] = TileType.HOLE
        }
    }

    // 穴を埋める
    fun fillHole(x: Int, y: Int) {
        if (tiles[y][x] == TileType.HOLE) {
            tiles[y][x] = baseTiles[y][x]   // 元に戻す
        }
    }

    // 穴状態かチェック
    fun isHole(x: Int, y: Int): Boolean {
        return tiles[y][x] == TileType.HOLE
    }

    // 壁かどうか
    fun isWall(x: Int, y: Int): Boolean {
        return tiles[y][x] == TileType.WALL
    }

    fun reset() {
        for (y in 0 until height) {
            for (x in 0 until width) {
                tiles[y][x] = baseTiles[y][x]  // ← 完全に元に戻す
            }
        }
    }
}
