package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.util.HapticFeedbackHelper

class MagnifierOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var hapticHelper: HapticFeedbackHelper

    // 1. ALÇA (Dock lateral na borda da tela)
    private var handleView: FrameLayout? = null

    // 2. MENU DA ALÇA (Opções rápidas sem enrolação)
    private var quickMenuView: FrameLayout? = null

    // 3. LUPA (Lente de aumento como a de amostra)
    private var lensView: FrameLayout? = null
    private var lensDimensionLabel: TextView? = null
    private var lensZoomLabel: TextView? = null

    // Estado da lupa
    private var currentZoom = 2.5f
    private val minZoom = 1.0f
    private val maxZoom = 10.0f
    private val zoomStep = 0.5f

    // Posição da Alça lateral
    private var handleX = 0
    private var handleY = 400
    private var isDockedOnRight = false

    // Posição e dimensões da Lente (redimensionável pelas bordas)
    private var lensX = 60
    private var lensY = 240
    private var lensWidth = 280
    private var lensHeight = 220
    private val minLensW = 160
    private val minLensH = 120

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        hapticHelper = HapticFeedbackHelper(this)

        startForegroundNotification()
        createDockHandle() // Cria a Alça
        createLensWindow() // Cria a Lupa (como a de amostra)
        syncWithNativeAccessibility()
    }

    private fun startForegroundNotification() {
        val channelId = "screen_magnifier_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Ampliação de Tela",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Lupa e alça de controle visual"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Lupa Ativa")
            .setContentText("Alça e lente prontas. Toque para abrir.")
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(1001, notification)
    }

    private fun getOverlayWindowType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    // =========================================================================
    // 1. ALÇA LATERAL (Aba discreta ancorada na borda)
    // =========================================================================
    @SuppressLint("ClickableViewAccessibility")
    private fun createDockHandle() {
        if (handleView != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            getOverlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = handleX
            y = handleY
        }

        val root = FrameLayout(this).apply {
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadii = if (isDockedOnRight) {
                    floatArrayOf(32f, 32f, 0f, 0f, 0f, 0f, 32f, 32f)
                } else {
                    floatArrayOf(0f, 0f, 32f, 32f, 32f, 32f, 0f, 0f)
                }
                setColor(0xF00B132B.toInt())
                setStroke(3, 0xFF38BDF8.toInt())
            }
            background = bg
            setPadding(14, 24, 14, 24)
            elevation = 30f
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val icon = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_search)
            setColorFilter(0xFF38BDF8.toInt())
        }
        container.addView(icon, LinearLayout.LayoutParams(38, 38))

        val label = TextView(this).apply {
            text = "ALÇA"
            setTextColor(0xFF38BDF8.toInt())
            textSize = 9f
            paint.isFakeBoldText = true
            gravity = Gravity.CENTER
            setPadding(0, 3, 0, 0)
        }
        container.addView(label)

        root.addView(container)

        var startTouchX = 0f
        var startTouchY = 0f
        var initialX = 0
        var initialY = 0

        root.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startTouchX = event.rawX
                    startTouchY = event.rawY
                    initialX = params.x
                    initialY = params.y
                    hapticHelper.performStepClick()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - startTouchX).toInt()
                    val dy = (event.rawY - startTouchY).toInt()
                    params.x = initialX + dx
                    params.y = initialY + dy
                    windowManager.updateViewLayout(v, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val diffX = Math.abs(event.rawX - startTouchX)
                    val diffY = Math.abs(event.rawY - startTouchY)

                    val displayMetrics = resources.displayMetrics
                    val screenWidth = displayMetrics.widthPixels
                    isDockedOnRight = event.rawX > (screenWidth / 2)
                    params.x = if (isDockedOnRight) screenWidth - v.width else 0
                    handleX = params.x
                    handleY = params.y

                    val bg = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadii = if (isDockedOnRight) {
                            floatArrayOf(32f, 32f, 0f, 0f, 0f, 0f, 32f, 32f)
                        } else {
                            floatArrayOf(0f, 0f, 32f, 32f, 32f, 32f, 0f, 0f)
                        }
                        setColor(0xF00B132B.toInt())
                        setStroke(3, 0xFF38BDF8.toInt())
                    }
                    root.background = bg
                    windowManager.updateViewLayout(v, params)

                    if (diffX < 20 && diffY < 20) {
                        openQuickMenu()
                    }
                    true
                }
                else -> false
            }
        }

        handleView = root
        windowManager.addView(root, params)
    }

    // =========================================================================
    // 2. MENU DA ALÇA (Opções diretas: Zoom + e -, e Fechar Lupa e Alça)
    // Fica SEMPRE POR CIMA da lupa (elevation = 200f) e no topo da tela
    // =========================================================================
    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    private fun openQuickMenu() {
        if (quickMenuView != null) {
            closeQuickMenu()
            return
        }

        val displayMetrics = resources.displayMetrics
        val menuWidth = (displayMetrics.widthPixels * 0.85f).toInt().coerceIn(280, 360)

        // Posicionado no topo da tela (y = 40) para não ficar embaixo da lupa
        val params = WindowManager.LayoutParams(
            menuWidth,
            WindowManager.LayoutParams.WRAP_CONTENT,
            getOverlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = if (isDockedOnRight) {
                (displayMetrics.widthPixels - menuWidth - 20).coerceAtLeast(10)
            } else {
                20
            }
            y = 40
        }

        val root = FrameLayout(this).apply {
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 20f
                setColor(0xF80B132B.toInt())
                setStroke(2, 0xFF38BDF8.toInt())
            }
            background = bg
            elevation = 200f // Camada mais alta: NUNCA fica atrás da lupa
            setPadding(16, 12, 16, 14)
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // Topo: Título + Botão Fechar Menu
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(2, 0, 2, 8)
        }

        val title = TextView(this).apply {
            text = "CONTROLE DE ZOOM"
            setTextColor(0xFF38BDF8.toInt())
            textSize = 13f
            paint.isFakeBoldText = true
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        header.addView(title)

        val hideBtn = TextView(this).apply {
            text = "Ocultar"
            setTextColor(0xFFE2E8F0.toInt())
            textSize = 11f
            paint.isFakeBoldText = true
            setPadding(10, 5, 10, 5)
            val btnBg = GradientDrawable().apply {
                cornerRadius = 8f
                setColor(0xFF1E293B.toInt())
            }
            background = btnBg
            setOnClickListener {
                closeQuickMenu()
                hapticHelper.performStepClick()
            }
        }
        header.addView(hideBtn)
        content.addView(header)

        // Controle de Zoom: [ - ] 2.5x [ + ]
        val zoomRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(2, 6, 2, 10)
        }

        val minusBtn = TextView(this).apply {
            text = "  -  "
            setTextColor(Color.WHITE)
            textSize = 22f
            paint.isFakeBoldText = true
            gravity = Gravity.CENTER
            setPadding(18, 6, 18, 6)
            val btnBg = GradientDrawable().apply {
                cornerRadius = 10f
                setColor(0xFF334155.toInt())
            }
            background = btnBg
            setOnClickListener {
                if (currentZoom > minZoom) {
                    currentZoom = (currentZoom - zoomStep).coerceAtLeast(minZoom)
                    updateZoomDisplay()
                    syncWithNativeAccessibility()
                    hapticHelper.performStepClick()
                }
            }
        }
        zoomRow.addView(minusBtn)

        val zoomReadout = TextView(this).apply {
            tag = "zoom_readout"
            text = "${String.format("%.1f", currentZoom)}x"
            setTextColor(0xFFFDE68A.toInt())
            textSize = 20f
            paint.isFakeBoldText = true
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        zoomRow.addView(zoomReadout)

        val plusBtn = TextView(this).apply {
            text = "  +  "
            setTextColor(Color.WHITE)
            textSize = 22f
            paint.isFakeBoldText = true
            gravity = Gravity.CENTER
            setPadding(18, 6, 18, 6)
            val btnBg = GradientDrawable().apply {
                cornerRadius = 10f
                setColor(0xFF0284C7.toInt())
            }
            background = btnBg
            setOnClickListener {
                if (currentZoom < maxZoom) {
                    currentZoom = (currentZoom + zoomStep).coerceAtMost(maxZoom)
                    updateZoomDisplay()
                    syncWithNativeAccessibility()
                    hapticHelper.onZoomChanged(currentZoom, maxZoom)
                    if (currentZoom < maxZoom) {
                        hapticHelper.performStepClick()
                    }
                }
            }
        }
        zoomRow.addView(plusBtn)
        content.addView(zoomRow)

        // Botão Fechar Tudo: Encerra a Lupa, a Alça e a Ampliação do Sistema
        val closeAllBtn = TextView(this).apply {
            text = "🛑 Fechar Lupa e Alça"
            setTextColor(Color.WHITE)
            textSize = 12f
            paint.isFakeBoldText = true
            gravity = Gravity.CENTER
            setPadding(14, 10, 14, 10)
            val btnBg = GradientDrawable().apply {
                cornerRadius = 10f
                setColor(0xFFDC2626.toInt())
            }
            background = btnBg
            setOnClickListener {
                shutdownMagnifierService()
            }
        }
        content.addView(closeAllBtn)

        root.addView(content)
        quickMenuView = root
        windowManager.addView(root, params)
    }

    private fun closeQuickMenu() {
        if (quickMenuView != null) {
            windowManager.removeView(quickMenuView)
            quickMenuView = null
        }
    }

    private fun updateZoomDisplay() {
        quickMenuView?.let { root ->
            val readout = root.findViewWithTag<TextView>("zoom_readout")
            readout?.text = "${String.format("%.1f", currentZoom)}x"
        }
        lensZoomLabel?.text = "${String.format("%.1f", currentZoom)}x"
    }

    // =========================================================================
    // 3. LUPA (Lente como a de amostra)
    // Redimensionável puxando as bordas e cantos!
    // =========================================================================
    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    private fun createLensWindow() {
        if (lensView != null) return

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val params = WindowManager.LayoutParams(
            lensWidth,
            lensHeight,
            getOverlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = lensX
            y = lensY
        }

        val root = FrameLayout(this).apply {
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 18f
                setColor(0x18000000.toInt())
                setStroke(3, 0xFF38BDF8.toInt())
            }
            background = bg
            elevation = 50f
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Barra Superior: Arrastar para mover + Botão Fechar
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val barBg = GradientDrawable().apply {
                cornerRadius = 12f
                setColor(0xEE0F172A.toInt())
            }
            background = barBg
            setPadding(10, 6, 8, 6)
        }

        val lensTitle = TextView(this).apply {
            text = "Lupa"
            setTextColor(0xFF38BDF8.toInt())
            textSize = 11.5f
            paint.isFakeBoldText = true
        }
        topBar.addView(lensTitle)

        val dimenText = TextView(this).apply {
            text = "  ${lensWidth}×${lensHeight}"
            setTextColor(0xFF94A3B8.toInt())
            textSize = 10f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        lensDimensionLabel = dimenText
        topBar.addView(dimenText)

        // Botão Fechar na própria Lupa
        val closeBtn = TextView(this).apply {
            text = " ✕ "
            setTextColor(Color.WHITE)
            textSize = 12f
            paint.isFakeBoldText = true
            setPadding(6, 2, 6, 2)
            val btnBg = GradientDrawable().apply {
                cornerRadius = 6f
                setColor(0xFFDC2626.toInt())
            }
            background = btnBg
            setOnClickListener {
                shutdownMagnifierService()
            }
        }
        topBar.addView(closeBtn)
        content.addView(topBar)

        // Área Central: Retícula Óptica e Badge de Zoom
        val viewport = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        // Retícula central óptica (+)
        val reticleView = object : View(this) {
            private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xAA38BDF8.toInt()
                strokeWidth = 2.5f
            }
            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                val cx = width / 2f
                val cy = height / 2f
                canvas.drawCircle(cx, cy, 32f, paint.apply { style = Paint.Style.STROKE })
                canvas.drawLine(cx - 16f, cy, cx + 16f, cy, paint)
                canvas.drawLine(cx, cy - 16f, cx, cy + 16f, paint)
            }
        }.apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        viewport.addView(reticleView)

        // Badge de Zoom no rodapé da lente
        val zoomBadge = TextView(this).apply {
            text = "${String.format("%.1f", currentZoom)}x"
            setTextColor(0xFFFDE68A.toInt())
            textSize = 12f
            paint.isFakeBoldText = true
            gravity = Gravity.CENTER
            val badgeBg = GradientDrawable().apply {
                cornerRadius = 10f
                setColor(0xDD000000.toInt())
                setStroke(1, 0xFF38BDF8.toInt())
            }
            background = badgeBg
            setPadding(10, 3, 10, 3)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                bottomMargin = 6
            }
        }
        lensZoomLabel = zoomBadge
        viewport.addView(zoomBadge)

        content.addView(viewport)
        root.addView(content)

        // =====================================================================
        // PUXAR A BORDA PARA REDIMENSIONAR (LARGURA E ALTURA)
        // =====================================================================

        // 1. Canto Inferior Direito (Amarelo alto contraste para puxar livremente)
        val cornerGrip = object : View(this) {
            private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFFDE68A.toInt()
                strokeWidth = 3f
                strokeCap = Paint.Cap.ROUND
            }
            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                val w = width.toFloat()
                val h = height.toFloat()
                canvas.drawLine(w - 10f, h - 22f, w - 22f, h - 10f, paint)
                canvas.drawLine(w - 10f, h - 16f, w - 16f, h - 10f, paint)
                canvas.drawLine(w - 10f, h - 10f, w - 10f, h - 10f, paint)
            }
        }.apply {
            val gripBg = GradientDrawable().apply {
                cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, 16f, 16f, 0f, 0f)
                setColor(0xEE0F172A.toInt())
                setStroke(2, 0xFFFDE68A.toInt())
            }
            background = gripBg
            layoutParams = FrameLayout.LayoutParams(46, 46).apply {
                gravity = Gravity.BOTTOM or Gravity.END
            }
        }
        root.addView(cornerGrip)

        // 2. Borda Direita (puxar para ajustar largura)
        val rightEdge = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(16, FrameLayout.LayoutParams.MATCH_PARENT).apply {
                gravity = Gravity.END
            }
        }
        root.addView(rightEdge)

        // 3. Borda Inferior (puxar para ajustar altura)
        val bottomEdge = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, 16).apply {
                gravity = Gravity.BOTTOM
            }
        }
        root.addView(bottomEdge)

        // =====================================================================
        // GESTOS: MOVER E REDIMENSIONAR A LUPA PELAS BORDAS
        // =====================================================================

        // Mover a Lupa pela barra superior
        var moveStartX = 0
        var moveStartY = 0
        var moveTouchX = 0f
        var moveTouchY = 0f

        topBar.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    moveStartX = params.x
                    moveStartY = params.y
                    moveTouchX = event.rawX
                    moveTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = moveStartX + (event.rawX - moveTouchX).toInt()
                    params.y = moveStartY + (event.rawY - moveTouchY).toInt()
                    lensX = params.x
                    lensY = params.y
                    windowManager.updateViewLayout(root, params)
                    syncWithNativeAccessibility()
                    true
                }
                else -> false
            }
        }

        // Puxar canto para redimensionar (largura e altura juntos)
        var resizeStartX = 0f
        var resizeStartY = 0f
        var initialW = 0
        var initialH = 0

        cornerGrip.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    resizeStartX = event.rawX
                    resizeStartY = event.rawY
                    initialW = params.width
                    initialH = params.height
                    hapticHelper.performStepClick()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - resizeStartX).toInt()
                    val dy = (event.rawY - resizeStartY).toInt()

                    val maxW = (screenWidth - params.x).coerceAtLeast(minLensW)
                    val maxH = (screenHeight - params.y).coerceAtLeast(minLensH)

                    val newW = (initialW + dx).coerceIn(minLensW, maxW)
                    val newH = (initialH + dy).coerceIn(minLensH, maxH)

                    params.width = newW
                    params.height = newH
                    lensWidth = newW
                    lensHeight = newH

                    lensDimensionLabel?.text = "  ${newW}×${newH}"
                    windowManager.updateViewLayout(root, params)
                    syncWithNativeAccessibility()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    hapticHelper.performStepClick()
                    true
                }
                else -> false
            }
        }

        // Puxar borda direita (alargar ou estreitar)
        rightEdge.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    resizeStartX = event.rawX
                    initialW = params.width
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - resizeStartX).toInt()
                    val maxW = (screenWidth - params.x).coerceAtLeast(minLensW)
                    val newW = (initialW + dx).coerceIn(minLensW, maxW)

                    params.width = newW
                    lensWidth = newW
                    lensDimensionLabel?.text = "  ${newW}×${lensHeight}"
                    windowManager.updateViewLayout(root, params)
                    syncWithNativeAccessibility()
                    true
                }
                else -> false
            }
        }

        // Puxar borda inferior (esticar ou encolher altura)
        bottomEdge.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    resizeStartY = event.rawY
                    initialH = params.height
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dy = (event.rawY - resizeStartY).toInt()
                    val maxH = (screenHeight - params.y).coerceAtLeast(minLensH)
                    val newH = (initialH + dy).coerceIn(minLensH, maxH)

                    params.height = newH
                    lensHeight = newH
                    lensDimensionLabel?.text = "  ${lensWidth}×${newH}"
                    windowManager.updateViewLayout(root, params)
                    syncWithNativeAccessibility()
                    true
                }
                else -> false
            }
        }

        lensView = root
        windowManager.addView(root, params)
    }

    private fun syncWithNativeAccessibility() {
        val service = ScreenMagnifierAccessibilityService.instance
        if (service != null) {
            val metrics = resources.displayMetrics
            val cx = (lensX + lensWidth / 2f).coerceIn(0f, metrics.widthPixels.toFloat())
            val cy = (lensY + lensHeight / 2f).coerceIn(0f, metrics.heightPixels.toFloat())
            service.applyMagnification(currentZoom, cx, cy, true)
        }
    }

    // =========================================================================
    // FECHAR SISTEMA COMPLETO: FECHA A LUPA, FECHA A ALÇA E LIMPA TUDO
    // =========================================================================
    private fun shutdownMagnifierService() {
        hapticHelper.performStepClick()

        // 1. Reseta o zoom do sistema para 1.0x (desliga a ampliação nativa)
        val service = ScreenMagnifierAccessibilityService.instance
        service?.resetMagnification()

        // 2. Fecha o Menu da Alça
        closeQuickMenu()

        // 3. Fecha a Lupa da tela
        if (lensView != null) {
            try {
                windowManager.removeView(lensView)
            } catch (_: Exception) {}
            lensView = null
        }

        // 4. Fecha a Alça lateral
        if (handleView != null) {
            try {
                windowManager.removeView(handleView)
            } catch (_: Exception) {}
            handleView = null
        }

        // 5. Encerra o serviço em segundo plano
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        val service = ScreenMagnifierAccessibilityService.instance
        service?.resetMagnification()

        closeQuickMenu()

        if (lensView != null) {
            try {
                windowManager.removeView(lensView)
            } catch (_: Exception) {}
            lensView = null
        }
        if (handleView != null) {
            try {
                windowManager.removeView(handleView)
            } catch (_: Exception) {}
            handleView = null
        }
    }
}
