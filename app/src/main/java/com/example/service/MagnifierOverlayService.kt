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
import android.provider.Settings
import android.text.TextUtils
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

    // Overlay components
    private var handleView: FrameLayout? = null
    private var quickMenuView: FrameLayout? = null
    private var focusWindowView: FrameLayout? = null
    private var dimensionLabel: TextView? = null

    // State variables
    private var isZoomActive = true
    private var currentZoom = 2.5f
    private val minZoom = 1.0f
    private val maxZoom = 10.0f
    private val zoomStep = 0.5f

    // Dock positioning
    private var handleX = 0
    private var handleY = 400
    private var isDockedOnRight = false

    // Window position & dimensions (Dynamic pull-to-resize)
    private var focusWindowX = 60
    private var focusWindowY = 240
    private var focusWindowWidth = 280
    private var focusWindowHeight = 240
    private val minWindowW = 160
    private val minWindowH = 120

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        hapticHelper = HapticFeedbackHelper(this)

        startForegroundNotification()
        createDockHandle()
    }

    private fun startForegroundNotification() {
        val channelId = "screen_magnifier_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Ampliação de Tela",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controle flutuante de acessibilidade visual"
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
            .setContentTitle("Lente de Aumento Ativa")
            .setContentText("Aba lateral pronta para uso. Toque para abrir os controles.")
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

    /**
     * 1. Controle Flutuante / Aba Lateral ("Alça")
     * Discreta, ancorada na borda da tela (dock lateral).
     */
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
            setPadding(14, 28, 14, 28)
            elevation = 16f
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val icon = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_search)
            setColorFilter(0xFF38BDF8.toInt())
        }
        container.addView(icon, LinearLayout.LayoutParams(40, 40))

        val label = TextView(this).apply {
            text = "LUPA"
            setTextColor(0xFF38BDF8.toInt())
            textSize = 9f
            paint.isFakeBoldText = true
            gravity = Gravity.CENTER
            setPadding(0, 4, 0, 0)
        }
        container.addView(label)

        root.addView(container)

        // Dragging & Tapping logic
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

                    // Docking to nearest edge
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

                    if (diffX < 24 && diffY < 24) {
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

    /**
     * 2. Menu Rápido Simplificado e Direto
     * Sem excesso de botões: apenas Escala (+ e -), Ligar/Desligar e Ocultar.
     */
    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    private fun openQuickMenu() {
        if (quickMenuView != null) return

        val displayMetrics = resources.displayMetrics
        val menuWidth = (displayMetrics.widthPixels * 0.82f).toInt().coerceIn(280, 360)

        val params = WindowManager.LayoutParams(
            menuWidth,
            WindowManager.LayoutParams.WRAP_CONTENT,
            getOverlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = if (isDockedOnRight) {
                (displayMetrics.widthPixels - menuWidth - 20).coerceAtLeast(10)
            } else {
                20
            }
            y = (handleY - 50).coerceIn(100, displayMetrics.heightPixels - 450)
        }

        val root = FrameLayout(this).apply {
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 24f
                setColor(0xF50F172A.toInt())
                setStroke(2, 0xFF38BDF8.toInt())
            }
            background = bg
            elevation = 28f
            setPadding(16, 14, 16, 14)
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // Header: Título e Botão Ocultar
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(2, 0, 2, 8)
        }

        val title = TextView(this).apply {
            text = "LUPA DE TELA"
            setTextColor(0xFF38BDF8.toInt())
            textSize = 13f
            paint.isFakeBoldText = true
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        header.addView(title)

        val collapseBtn = TextView(this).apply {
            text = "Ocultar"
            setTextColor(0xFFE2E8F0.toInt())
            textSize = 11f
            paint.isFakeBoldText = true
            setPadding(14, 6, 14, 6)
            val btnBg = GradientDrawable().apply {
                cornerRadius = 12f
                setColor(0xFF1E293B.toInt())
                setStroke(1, 0xFF64748B.toInt())
            }
            background = btnBg
            setOnClickListener {
                closeQuickMenu()
                hapticHelper.performStepClick()
            }
        }
        header.addView(collapseBtn)
        content.addView(header)

        // Controle 1: Nível de Ampliação (+ e -)
        val zoomRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(2, 8, 2, 8)
            val rowBg = GradientDrawable().apply {
                cornerRadius = 16f
                setColor(0xFF1E293B.toInt())
            }
            background = rowBg
        }

        val minusBtn = TextView(this).apply {
            text = " - "
            setTextColor(Color.WHITE)
            textSize = 20f
            paint.isFakeBoldText = true
            gravity = Gravity.CENTER
            setPadding(20, 8, 20, 8)
            val btnBg = GradientDrawable().apply {
                cornerRadius = 12f
                setColor(0xFF334155.toInt())
            }
            background = btnBg
            setOnClickListener {
                if (currentZoom > minZoom) {
                    currentZoom = (currentZoom - zoomStep).coerceAtLeast(minZoom)
                    updateFocusWindowZoom()
                    syncWithNativeAccessibility()
                    hapticHelper.performStepClick()
                }
            }
        }
        zoomRow.addView(minusBtn)

        val zoomReadout = TextView(this).apply {
            text = "${String.format("%.1f", currentZoom)}x"
            setTextColor(0xFFFDE68A.toInt())
            textSize = 18f
            paint.isFakeBoldText = true
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        zoomRow.addView(zoomReadout)

        val plusBtn = TextView(this).apply {
            text = " + "
            setTextColor(Color.WHITE)
            textSize = 20f
            paint.isFakeBoldText = true
            gravity = Gravity.CENTER
            setPadding(20, 8, 20, 8)
            val btnBg = GradientDrawable().apply {
                cornerRadius = 12f
                setColor(0xFF0284C7.toInt())
            }
            background = btnBg
            setOnClickListener {
                if (currentZoom < maxZoom) {
                    currentZoom = (currentZoom + zoomStep).coerceAtMost(maxZoom)
                    updateFocusWindowZoom()
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

        // Controle 2: Ligar / Desligar Lupa
        val toggleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(6, 8, 6, 4)
        }

        val toggleLabel = TextView(this).apply {
            text = "Janela Visível"
            setTextColor(0xFF94A3B8.toInt())
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        toggleRow.addView(toggleLabel)

        val toggleBtn = TextView(this).apply {
            text = if (isZoomActive) "Ativada" else "Oculta"
            setTextColor(if (isZoomActive) 0xFF0B132B.toInt() else Color.WHITE)
            textSize = 11f
            paint.isFakeBoldText = true
            setPadding(14, 6, 14, 6)
            val btnBg = GradientDrawable().apply {
                cornerRadius = 10f
                setColor(if (isZoomActive) 0xFF10B981.toInt() else 0xFF475569.toInt())
            }
            background = btnBg
            setOnClickListener {
                isZoomActive = !isZoomActive
                text = if (isZoomActive) "Ativada" else "Oculta"
                val newBg = GradientDrawable().apply {
                    cornerRadius = 10f
                    setColor(if (isZoomActive) 0xFF10B981.toInt() else 0xFF475569.toInt())
                }
                background = newBg
                updateFocusWindowVisibility()
                syncWithNativeAccessibility()
                hapticHelper.performStepClick()
            }
        }
        toggleRow.addView(toggleBtn)
        content.addView(toggleRow)

        // Dica de redimensionamento direto
        val hintText = TextView(this).apply {
            text = "Dica: Puxe o canto inferior direito ⤡ da lente para alterar a largura e altura livremente."
            setTextColor(0xFF38BDF8.toInt())
            textSize = 10f
            gravity = Gravity.CENTER
            setPadding(4, 8, 4, 4)
        }
        content.addView(hintText)

        root.addView(content)
        quickMenuView = root
        windowManager.addView(root, params)

        if (isZoomActive) {
            showFocusWindow()
        }
    }

    private fun closeQuickMenu() {
        if (quickMenuView != null) {
            windowManager.removeView(quickMenuView)
            quickMenuView = null
        }
    }

    private fun syncWithNativeAccessibility() {
        val service = ScreenMagnifierAccessibilityService.instance
        if (service != null && isZoomActive) {
            val metrics = resources.displayMetrics
            val cx = (focusWindowX + focusWindowWidth / 2f).coerceIn(0f, metrics.widthPixels.toFloat())
            val cy = (focusWindowY + focusWindowHeight / 2f).coerceIn(0f, metrics.heightPixels.toFloat())
            service.applyMagnification(currentZoom, cx, cy, true)
        } else {
            service?.resetMagnification()
        }
    }

    /**
     * 3. Janela de Foco Ajustável com REDIMENSIONAMENTO DIRETO NA BORDA
     * O usuário pode puxar a borda / canto para deixar no tamanho exato que desejar.
     */
    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    private fun showFocusWindow() {
        if (focusWindowView != null) return

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val params = WindowManager.LayoutParams(
            focusWindowWidth,
            focusWindowHeight,
            getOverlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = focusWindowX
            y = focusWindowY
        }

        val root = FrameLayout(this).apply {
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 22f
                setColor(0x18080E1A.toInt())
                setStroke(3, 0xFF38BDF8.toInt())
            }
            background = bg
            setPadding(4, 4, 4, 4)
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        // ==========================================
        // Top Bar: Mover a lente e fechar
        // ==========================================
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val barBg = GradientDrawable().apply {
                cornerRadius = 14f
                setColor(0xF00F172A.toInt())
            }
            background = barBg
            setPadding(10, 6, 8, 6)
        }

        val searchIcon = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_search)
            setColorFilter(0xFF38BDF8.toInt())
        }
        topBar.addView(searchIcon, LinearLayout.LayoutParams(22, 22))

        val title = TextView(this).apply {
            text = "Lente"
            setTextColor(0xFF38BDF8.toInt())
            textSize = 12f
            paint.isFakeBoldText = true
            setPadding(6, 0, 6, 0)
        }
        topBar.addView(title)

        val dimenText = TextView(this).apply {
            text = "${focusWindowWidth}×${focusWindowHeight}"
            setTextColor(0xFF94A3B8.toInt())
            textSize = 10f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        dimensionLabel = dimenText
        topBar.addView(dimenText)

        // Botão Fechar rápido
        val closeBtn = TextView(this).apply {
            text = "✕"
            setTextColor(0xFF94A3B8.toInt())
            textSize = 13f
            paint.isFakeBoldText = true
            setPadding(10, 2, 10, 2)
            setOnClickListener {
                isZoomActive = false
                updateFocusWindowVisibility()
                syncWithNativeAccessibility()
                hapticHelper.performStepClick()
            }
        }
        topBar.addView(closeBtn)
        content.addView(topBar)

        // ==========================================
        // Viewport: Área Central da Lente
        // ==========================================
        val viewport = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            val vBg = GradientDrawable().apply {
                cornerRadius = 14f
                setColor(0x1F0284C7.toInt())
                setStroke(1, 0x5538BDF8.toInt())
            }
            background = vBg
        }

        // Retícula central
        val reticleCross = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_search)
            setColorFilter(0x6638BDF8.toInt())
            layoutParams = FrameLayout.LayoutParams(32, 32).apply {
                gravity = Gravity.CENTER
            }
        }
        viewport.addView(reticleCross)

        // Badge de Zoom no rodapé do viewport
        val reticleView = TextView(this).apply {
            text = "${String.format("%.1f", currentZoom)}x"
            setTextColor(0xFFFDE68A.toInt())
            textSize = 13f
            paint.isFakeBoldText = true
            gravity = Gravity.CENTER
            val badgeBg = GradientDrawable().apply {
                cornerRadius = 12f
                setColor(0xD0000000.toInt())
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
        viewport.addView(reticleView)
        content.addView(viewport)

        root.addView(content)

        // ==========================================
        // 4. ALÇA DE PUXAR NA BORDA / CANTO INFERIOR DIREITO
        // ==========================================
        val resizeGrip = object : View(this) {
            private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFFDE68A.toInt() // Amarelo alto contraste para fácil localização
                strokeWidth = 3f
                strokeCap = Paint.Cap.ROUND
            }

            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                val w = width.toFloat()
                val h = height.toFloat()

                // Desenha as ranhuras diagonais de redimensionamento clássicas ◢
                canvas.drawLine(w - 10f, h - 22f, w - 22f, h - 10f, paint)
                canvas.drawLine(w - 10f, h - 16f, w - 16f, h - 10f, paint)
                canvas.drawLine(w - 10f, h - 10f, w - 10f, h - 10f, paint)
            }
        }.apply {
            val gripBg = GradientDrawable().apply {
                cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, 18f, 18f, 0f, 0f)
                setColor(0xEE0F172A.toInt())
                setStroke(2, 0xFFFDE68A.toInt())
            }
            background = gripBg
            layoutParams = FrameLayout.LayoutParams(48, 48).apply {
                gravity = Gravity.BOTTOM or Gravity.END
            }
        }
        root.addView(resizeGrip)

        // ==========================================
        // EVENTOS DE TOQUE:
        // 1. Top bar: ARRASTAR PARA MOVER A LENTE
        // 2. Canto resizeGrip: PUXAR PARA REDIMENSIONAR
        // ==========================================
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
                    focusWindowX = params.x
                    focusWindowY = params.y
                    windowManager.updateViewLayout(root, params)
                    syncWithNativeAccessibility()
                    true
                }
                else -> false
            }
        }

        var resizeStartX = 0f
        var resizeStartY = 0f
        var initialWidth = 0
        var initialHeight = 0

        resizeGrip.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    resizeStartX = event.rawX
                    resizeStartY = event.rawY
                    initialWidth = params.width
                    initialHeight = params.height
                    hapticHelper.performStepClick()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - resizeStartX).toInt()
                    val dy = (event.rawY - resizeStartY).toInt()

                    val maxW = (screenWidth - params.x).coerceAtLeast(minWindowW)
                    val maxH = (screenHeight - params.y).coerceAtLeast(minWindowH)

                    val newW = (initialWidth + dx).coerceIn(minWindowW, maxW)
                    val newH = (initialHeight + dy).coerceIn(minWindowH, maxH)

                    params.width = newW
                    params.height = newH
                    focusWindowWidth = newW
                    focusWindowHeight = newH

                    dimensionLabel?.text = "${newW}×${newH}"

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

        focusWindowView = root
        windowManager.addView(root, params)
        syncWithNativeAccessibility()
    }

    private fun updateFocusWindowVisibility() {
        if (isZoomActive) {
            if (focusWindowView == null) {
                showFocusWindow()
            }
        } else {
            if (focusWindowView != null) {
                windowManager.removeView(focusWindowView)
                focusWindowView = null
            }
        }
    }

    private fun updateFocusWindowZoom() {
        focusWindowView?.let { root ->
            val content = root.getChildAt(0) as? LinearLayout
            val viewport = content?.getChildAt(1) as? FrameLayout
            val reticle = viewport?.getChildAt(viewport.childCount - 1) as? TextView
            reticle?.text = "${String.format("%.1f", currentZoom)}x"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val service = ScreenMagnifierAccessibilityService.instance
        service?.resetMagnification()

        if (quickMenuView != null) {
            windowManager.removeView(quickMenuView)
            quickMenuView = null
        }
        if (focusWindowView != null) {
            windowManager.removeView(focusWindowView)
            focusWindowView = null
        }
        if (handleView != null) {
            windowManager.removeView(handleView)
            handleView = null
        }
    }
}
