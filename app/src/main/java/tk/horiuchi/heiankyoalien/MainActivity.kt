package tk.horiuchi.heiankyoalien

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowInsets
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class MainActivity : AppCompatActivity() {
    private lateinit var gameView: GameView
    private lateinit var scoreText: TextView
    private lateinit var segViews: List<ImageView>
    private lateinit var segBitmaps: List<Bitmap>
    private lateinit var stageText: TextView
    private lateinit var livesLayout: LinearLayout
    private lateinit var debugText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.title = "Heiankyo Alien"
        setContentView(R.layout.activity_main)

        // ステータスバーを隠す処理を安全に呼ぶ
        window.decorView.post {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.hide(WindowInsets.Type.statusBars())
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
            }
        }
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.overflowIcon?.setTint(Color.WHITE)

        gameView = findViewById(R.id.gameView)

        // ImageView取得
        segViews = listOf(
            findViewById(R.id.seg1),
            findViewById(R.id.seg2),
            findViewById(R.id.seg3),
            findViewById(R.id.seg4)
        )

        // ビットマップ読み込み（seg7_0～seg7_9, seg7_blank）
        segBitmaps = listOf(
            R.drawable.seg7_0, R.drawable.seg7_1, R.drawable.seg7_2, R.drawable.seg7_3,
            R.drawable.seg7_4, R.drawable.seg7_5, R.drawable.seg7_6, R.drawable.seg7_7,
            R.drawable.seg7_8, R.drawable.seg7_9, R.drawable.seg7_blank
        ).map { resId -> BitmapFactory.decodeResource(resources, resId) }

        // GameViewからスコア更新を受け取るリスナー
        gameView.setScoreUpdateListener { score ->
            runOnUiThread {
                updateScoreDisplay(score)
            }
        }

        stageText = findViewById(R.id.stageText)
        debugText = findViewById(R.id.debugText)
        livesLayout = findViewById(R.id.livesLayout)

        // GameViewからステージやライフを通知してもらう
        gameView.setStatusUpdateListener { stage, lives ->
            stageText.text = "STAGE: $stage"
            updateLivesDisplay(lives)
        }
        gameView.setDebugTextListener { text ->
            debugText.text = "$text"
        }

        findViewById<ImageButton>(R.id.btn_left).setOnClickListener {
            gameView.move(Direction.LEFT)
        }

        findViewById<ImageButton>(R.id.btn_right).setOnClickListener {
            gameView.move(Direction.RIGHT)
        }

        findViewById<ImageButton>(R.id.btn_up).setOnClickListener {
            gameView.move(Direction.UP)
        }

        findViewById<ImageButton>(R.id.btn_down).setOnClickListener {
            gameView.move(Direction.DOWN)
        }

        findViewById<ImageButton>(R.id.btn_act).setOnClickListener {
            gameView.act()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_about -> {
                gameView.pauseGame()
                showAboutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateScoreDisplay(score: Int) {
        val scoreStr = score.toString().padStart(4, ' ')  // 左詰め、ゼロ埋めなし

        for (i in 0 until 4) {
            val ch = scoreStr.getOrNull(i)
            val bmp = if (ch != null && ch in '0'..'9') {
                segBitmaps[ch.digitToInt()]
            } else {
                segBitmaps[10]  // blank
            }
            segViews[i].setImageBitmap(bmp)
        }
    }

    private fun updateLivesDisplay(lives: Int) {
        livesLayout.removeAllViews()
        repeat(lives - 1) {
            val img = ImageView(this)
            img.setImageResource(R.drawable.player_down)
            val size = resources.getDimensionPixelSize(R.dimen.life_icon_size)
            val params = LinearLayout.LayoutParams(size, size)
            params.setMargins(4, 0, 4, 0)
            img.layoutParams = params
            livesLayout.addView(img)
        }
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.about_title))
            .setMessage(getString(R.string.about_message))
            //.setPositiveButton(getString(R.string.about_ok), null)
            .setPositiveButton(getString(R.string.about_ok)) { dialog, _ ->
                dialog.dismiss()
                gameView.resumeGame()  // ★ ダイアログが閉じられたときにゲームを再開
            }
            .setNeutralButton(getString(R.string.about_hyperlink_name)) { _, _ ->
                val url = getString(R.string.about_hyperlink)
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            }
            .setOnCancelListener {
                gameView.resumeGame()  // ★ 戻るボタンなどでもゲームを再開
            }
            .show()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.repeatCount == 0) {
            return gameView.onKeyDown(keyCode, event)
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onPause() {
        super.onPause()
        gameView.pauseGame()
    }

    override fun onResume() {
        super.onResume()
        gameView.resumeGame()
    }

}