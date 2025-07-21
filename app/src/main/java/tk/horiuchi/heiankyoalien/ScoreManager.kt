package tk.horiuchi.heiankyoalien

//class ScoreManager {
class ScoreManager(private val onScoreChanged: ((Int) -> Unit)? = null) {
    var score = 0
        private set

    private val maxScore = 9999

    fun add(points: Int) {
        score = (score + points).coerceAtMost(maxScore)
        onScoreChanged?.invoke(score)
    }

    fun reset() {
        score = 0
        onScoreChanged?.invoke(score)
    }
}
