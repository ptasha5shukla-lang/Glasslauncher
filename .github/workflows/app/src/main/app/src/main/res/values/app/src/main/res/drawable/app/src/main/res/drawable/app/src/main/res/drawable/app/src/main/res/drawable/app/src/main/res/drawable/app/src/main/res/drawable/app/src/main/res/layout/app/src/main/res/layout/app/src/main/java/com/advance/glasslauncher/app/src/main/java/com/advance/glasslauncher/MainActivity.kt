package com.advance.glasslauncher

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Calendar

data class AppItem(val label: String, val icon: Drawable, val launch: Intent)

class MainActivity : Activity() {
    private lateinit var rv: RecyclerView
    private lateinit var welcome: View
    private var welcomeShown = true

    private val screenOn = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) { showWelcome() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        rv = findViewById(R.id.apps)
        welcome = findViewById(R.id.welcome)
        welcome.setOnClickListener { hideWelcome() }
        welcome.requestFocus()
        val hint = findViewById<View>(R.id.clickHint)
        hint.animate().alpha(0.25f).setDuration(1400).withEndAction(object : Runnable {
            override fun run() {
                val next = if (hint.alpha < 0.5f) 1f else 0.25f
                hint.animate().alpha(next).setDuration(1400).withEndAction(this).start()
            }
        }).start()
        registerReceiver(screenOn, IntentFilter(Intent.ACTION_SCREEN_ON))
        rv.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        findViewById<View>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        findViewById<View>(R.id.btnWifi).setOnClickListener {
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    override fun onResume() {
        super.onResume()
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        findViewById<TextView>(R.id.greeting).text = when {
            h < 5 -> "Good night"
            h < 12 -> "Good morning"
            h < 17 -> "Good afternoon"
            else -> "Good evening"
        }
        rv.adapter = AppAdapter(loadApps()) { startActivity(it.launch) }
        if (!welcomeShown) rv.requestFocus()
    }

    private fun showWelcome() {
        welcomeShown = true
        welcome.animate().cancel()
        welcome.alpha = 1f
        welcome.visibility = View.VISIBLE
        welcome.requestFocus()
    }

    private fun hideWelcome() {
        if (!welcomeShown) return
        welcomeShown = false
        welcome.animate().alpha(0f).setDuration(450).withEndAction {
            welcome.visibility = View.GONE
            rv.requestFocus()
        }.start()
    }

    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        if (welcomeShown) {
            if (e.action == KeyEvent.ACTION_DOWN && (e.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                    e.keyCode == KeyEvent.KEYCODE_ENTER || e.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER)) {
                hideWelcome()
            }
            return true
        }
        return super.dispatchKeyEvent(e)
    }

    override fun onDestroy() {
        unregisterReceiver(screenOn)
        super.onDestroy()
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() { /* launcher stays on home */ }

    private fun loadApps(): List<AppItem> {
        val pm = packageManager
        val seen = HashSet<String>()
        val out = ArrayList<AppItem>()
        for (cat in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            val q = Intent(Intent.ACTION_MAIN).addCategory(cat)
            for (ri in pm.queryIntentActivities(q, 0)) {
                val pkg = ri.activityInfo.packageName
                if (pkg == packageName || !seen.add(pkg)) continue
                val launch = Intent(Intent.ACTION_MAIN).addCategory(cat)
                    .setClassName(pkg, ri.activityInfo.name)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val icon = ri.activityInfo.loadBanner(pm) ?: ri.loadIcon(pm)
                out.add(AppItem(ri.loadLabel(pm).toString(), icon, launch))
            }
        }
        return out.sortedBy { it.label.lowercase() }
    }
}

class AppAdapter(
    private val items: List<AppItem>,
    private val onClick: (AppItem) -> Unit
) : RecyclerView.Adapter<AppAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val icon: ImageView = v.findViewById(R.id.icon)
        val label: TextView = v.findViewById(R.id.label)
    }

    override fun onCreateViewHolder(p: ViewGroup, t: Int): VH =
        VH(LayoutInflater.from(p.context).inflate(R.layout.item_app, p, false))

    override fun onBindViewHolder(h: VH, i: Int) {
        val item = items[i]
        h.icon.setImageDrawable(item.icon)
        h.label.text = item.label
        h.itemView.setOnClickListener { onClick(item) }
        h.itemView.setOnFocusChangeListener { v, focused ->
            val s = if (focused) 1.12f else 1f
            v.animate().scaleX(s).scaleY(s).setDuration(150).start()
            v.elevation = if (focused) 24f else 0f
        }
    }

    override fun getItemCount() = items.size
}
