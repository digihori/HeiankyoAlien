// TileType.kt
package tk.horiuchi.heiankyoalien

enum class TileType {
    WALL,       // 通行不可（緑の壁）
    PATH,       // 通路（移動・穴掘り可）
    CROSS,      // 交差点（表示上の違い、扱いはPATHと同じ）
    HOLE,       // 掘られた穴（敵が落ちる）
    FILLED      // 埋め戻された穴（通行可能だが再利用不可）
}
