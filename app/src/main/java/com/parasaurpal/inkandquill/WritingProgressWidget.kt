package com.parasaurpal.inkandquill

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.parasaurpal.inkandquill.data.NovelStudioStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WritingProgressWidget : AppWidgetProvider() {
    companion object {
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(android.content.ComponentName(context, WritingProgressWidget::class.java))
            if (ids.isNotEmpty()) WritingProgressWidget().onUpdate(context, manager, ids)
        }
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val store = NovelStudioStore(context)
        val db = store.readableDatabase
        val universe = db.rawQuery("SELECT id,title FROM universes ORDER BY updated_at DESC LIMIT 1", null).use {
            if (it.moveToFirst()) it.getString(0) to it.getString(1) else null
        }
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val words = universe?.let { pair ->
            db.rawQuery("SELECT COALESCE(SUM(word_delta),0) FROM daily_logs WHERE universe_id=? AND day_key=?", arrayOf(pair.first, today)).use {
                if (it.moveToFirst()) it.getInt(0) else 0
            }
        } ?: 0
        val goal = universe?.let { pair ->
            db.rawQuery("SELECT target_words FROM writing_goals WHERE universe_id=? ORDER BY created_at DESC LIMIT 1", arrayOf(pair.first)).use {
                if (it.moveToFirst()) it.getInt(0) else 0
            }
        } ?: 0
        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_writing_progress)
            views.setTextViewText(R.id.widget_title, universe?.second ?: "Ink & Quill")
            views.setTextViewText(R.id.widget_words, "$words words today")
            views.setTextViewText(R.id.widget_goal, if (goal > 0) "$goal word goal" else "Open your studio")
            val intent = Intent(context, MainActivity::class.java)
            val pending = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_root, pending)
            manager.updateAppWidget(id, views)
        }
        store.close()
    }
}
