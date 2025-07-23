package tk.horiuchi.heiankyoalien

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

class SoundManager(context: Context) {
    private val soundPool: SoundPool
    private val soundMap: MutableMap<SoundEffect, Int> = mutableMapOf()
    private var playingStreamId: Int = 0

    enum class SoundEffect {
        CLEAR, DEAD, DIG, FILL, FALL, HIT, SPD, OVER
    }

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()

        // 効果音のロード（res/raw にファイルを置いておく）
        soundMap[SoundEffect.CLEAR] = soundPool.load(context, R.raw.se_stage_clear, 1)
        soundMap[SoundEffect.DEAD] = soundPool.load(context, R.raw.se_player_dead, 1)
        soundMap[SoundEffect.DIG] = soundPool.load(context, R.raw.se_player_dig, 1)
        soundMap[SoundEffect.FILL] = soundPool.load(context, R.raw.se_player_fill, 1)
        soundMap[SoundEffect.FALL] = soundPool.load(context, R.raw.se_enemy_fall, 1)
        soundMap[SoundEffect.SPD] = soundPool.load(context, R.raw.se_enemy_speedup, 1)
        soundMap[SoundEffect.HIT] = soundPool.load(context, R.raw.se_enemy_hit, 1)
        soundMap[SoundEffect.OVER] = soundPool.load(context, R.raw.se_gameover, 1)
    }

    fun play(effect: SoundEffect) {
        val soundId = soundMap[effect] ?: return
        soundMap[effect]?.let { soundId ->
            playingStreamId = soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        }
        //soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
    }

    fun stopCurrent() {
        soundPool.stop(playingStreamId)
    }

    fun release() {
        soundPool.release()
    }
}
