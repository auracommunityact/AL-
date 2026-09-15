package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R

class AuraSearchWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_search_bar)

            // Intent to open MainActivity directly to Global Search screen
            val intent = Intent(context, MainActivity::class.java).apply {
                action = "com.example.ACTION_GLOBAL_SEARCH"
                putExtra("deep_link", "global_search")
                putExtra("auto_focus", true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

            val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                pendingIntentFlags
            )

            // Attach PendingIntent to all clickable elements in the widget
            views.setOnClickPendingIntent(R.id.widget_search_bar, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_search_icon, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_search_text, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_mic_icon, pendingIntent)
            views.setOnClickPendingIntent(R.id.widget_brand_logo, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
