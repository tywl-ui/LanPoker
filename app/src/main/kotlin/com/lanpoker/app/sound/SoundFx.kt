package com.lanpoker.app.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.lanpoker.app.R

/**
 * 音效播放（SoundPool 预加载 res/raw 资源，无网络依赖）。
 */
object SoundFx {
    private var pool: SoundPool? = null
    private var inited = false
    private val ids = mutableMapOf<String, Int>()

    fun init(context: Context) {
        if (inited) return
        inited = true
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val sp = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(attrs)
            .build()
        fun load(name: String, resId: Int) {
            ids[name] = sp.load(context, resId, 1)
        }
        load("deal", R.raw.deal)
        load("flip", R.raw.flip)
        load("tick", R.raw.tick)
        load("raise", R.raw.raise)
        load("compare", R.raw.compare)
        load("fold", R.raw.fold)
        load("win", R.raw.win)
        load("lose", R.raw.lose)
        pool = sp
    }

    fun play(name: String) {
        val id = ids[name] ?: return
        pool?.play(id, 0.7f, 0.7f, 1, 0, 1f)
    }

    /** 按动作文本推断音效 */
    fun playForAction(action: String?) {
        if (action == null) return
        when {
            action.contains("比牌") -> play("compare")
            action.contains("弃牌") -> play("fold")
            action.contains("加注") -> play("raise")
            action.contains("看牌") -> play("flip")
            else -> play("tick")
        }
    }
}
