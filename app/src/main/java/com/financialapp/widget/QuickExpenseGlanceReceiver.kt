package com.financialapp.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * Receiver para vincular el Widget de Jetpack Glance con el sistema de Android y Xiaomi HyperOS.
 */
class QuickExpenseGlanceReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickExpenseGlanceWidget()
}
