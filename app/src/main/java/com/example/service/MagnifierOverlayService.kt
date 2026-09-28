package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
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

    // State variables
    private var isZoomActive = true
    private var currentZoom = 2.0f
    private val minZoom = 1.0f
    private val maxZoom = 10.0f
    private val zoomStep = 0.5f

    // Lens mode: false = Janela Flutuante, true = Tela Cheia
    private var isFullScreenMode = false
    private var isSquareWindow = true

    // Contrast filter
    private var currentFilterIndex = 0
    private val filterNames = listOf("Normal", "Alto Contraste", "Invertido", "Amarelo no Preto", "P&B")

    // Dock positioning
    private var handleX = 0
    private var handleY = 400
    private var isDockedOnRight = false

    // Window position & dimensions
    private var focusWindowX = 60
    private var focusWindowY = 240
    private var focusWindowWidth = 260
    private var focusWindowHeight = 260

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
     * 2. Menu Rápido de Controles
     * Expandido a partir da aba lateral com controles essenciais.
     */
    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    private fun openQuickMenu() {
        if (quickMenuView != null) return

        val displayMetrics = resources.displayMetrics
        val menuWidth = (displayMetrics.widthPixels * 0.86f).toInt().coerceIn(300, 420)

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
            y = (handleY - 50).coerceIn(100, displayMetrics.heightPixels - 500)
        }

        val root = FrameLayout(this).apply {
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 28f
                setColor(0xF50F172A.toInt())
                setStroke(2, 0xFF38BDF8.toInt())
            }
            background = bg
            elevation = 28f
            setPadding(18, 16, 18, 16)
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // Header: Title and Collapse button
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(4, 0, 4, 10)
        }

        val title = TextView(this).apply {
            text = "AMPLIAÇÃO"
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
            setPadding(16, 8, 16, 8)
            val btnBg = GradientDrawable().apply {
                cornerRadius = 14f
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

        // Control 1: Ativar / Desativar Zoom
        val toggleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(6, 6, 6, 8)
            val rowBg = GradientDrawable().apply {
                cornerRadius = 14f
                setColor(0xFF1E293B.toInt())
            }
            background = rowBg
        }

        val toggleLabel = TextView(this).apply {
            text = "Zoom da Tela"
            setTextColor(Color.WHITE)
            textSize = 12f
            paint.isFakeBoldText = true
            setPadding(12, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        toggleRow.addView(toggleLabel)

        val toggleBtn = TextView(this).apply {
            text = if (isZoomActive) "Ativado" else "Desativado"
            setTextColor(if (isZoomActive) 0xFF0B132B.toInt() else Color.WHITE)
            textSize = 11f
            paint.isFakeBoldText = true
            setPadding(16, 8, 16, 8)
            val btnBg = GradientDrawable().apply {
                cornerRadius = 12f
                setColor(if (isZoomActive) 0xFF10B981.toInt() else 0xFF475569.toInt())
            }
            background = btnBg
            setOnClickListener {
                isZoomActive = !isZoomActive
                text = if (isZoomActive) "Ativado" else "Desativado"
                setTextColor(if (isZoomActive) 0xFF0B132B.toInt() else Color.WHITE)
                val newBg = GradientDrawable().apply {
                    cornerRadius = 12f
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

        // Control 2: Níveis de ampliação de escala (+ e -)
        val zoomRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(4, 10, 4, 6)
        }

        val zoomReadout = TextView(this).apply {
            text = "${String.format("%.1f", currentZoom)}x"
            setTextColor(0xFFFDE68A.toInt())
            textSize = 16f
            paint.isFakeBoldText = true
            setPadding(6, 0, 10, 0)
        }

        val minusBtn = TextView(this).apply {
            text = " - "
            setTextColor(Color.WHITE)
            textSize = 18f
            paint.isFakeBoldText = true
            gravity = Gravity.CENTER
            setPadding(24, 6, 24, 6)
            val btnBg = GradientDrawable().apply {
                cornerRadius = 14f
                setColor(0xFF334155.toInt())
            }
            background = btnBg
            setOnClickListener {
                if (currentZoom > minZoom) {
                    currentZoom = (currentZoom - zoomStep).coerceAtLeast(minZoom)
                    zoomReadout.text = "${String.format("%.1f", currentZoom)}x"
                    updateFocusWindowZoom()
                    syncWithNativeAccessibility()
                    hapticHelper.performStepClick()
                }
            }
        }
        zoomRow.addView(minusBtn)

        val zoomTitle = TextView(this).apply {
            text = "Escala"
            setTextColor(0xFF94A3B8.toInt())
            textSize = 11f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        zoomRow.addView(zoomTitle)
        zoomRow.addView(zoomReadout)

        val plusBtn = TextView(this).apply {
            text = " + "
            setTextColor(Color.WHITE)
            textSize = 18f
            paint.isFakeBoldText = true
            gravity = Gravity.CENTER
            setPadding(24, 6, 24, 6)
            val btnBg = GradientDrawable().apply {
                cornerRadius = 14f
                setColor(0xFF0284C7.toInt())
            }
            background = btnBg
            setOnClickListener {
                if (currentZoom < maxZoom) {
                    currentZoom = (currentZoom + zoomStep).coerceAtMost(maxZoom)
                    zoomReadout.text = "${String.format("%.1f", currentZoom)}x"
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

        // Control 3: Alternar modo de lente (janela flutuante móvel ou ampliação de tela cheia)
        val modeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(4, 6, 4, 6)
        }

        val windowModeBtn = TextView(this)
        val fullScreenModeBtn = TextView(this)

        fun updateModeButtons() {
            windowModeBtn.apply {
                text = "Janela Flutuante"
                setTextColor(if (!isFullScreenMode) Color.WHITE else 0xFF94A3B8.toInt())
                textSize = 10.5f
                paint.isFakeBoldText = !isFullScreenMode
                gravity = Gravity.CENTER
                setPadding(10, 8, 10, 8)
                val bg = GradientDrawable().apply {
                    cornerRadius = 12f
                    setColor(if (!isFullScreenMode) 0xFF0284C7.toInt() else 0xFF1E293B.toInt())
                }
                background = bg
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginEnd = 6
                }
            }

            fullScreenModeBtn.apply {
                text = "Tela Cheia"
                setTextColor(if (isFullScreenMode) Color.WHITE else 0xFF94A3B8.toInt())
                textSize = 10.5f
                paint.isFakeBoldText = isFullScreenMode
                gravity = Gravity.CENTER
                setPadding(10, 8, 10, 8)
                val bg = GradientDrawable().apply {
                    cornerRadius = 12f
                    setColor(if (isFullScreenMode) 0xFF0284C7.toInt() else 0xFF1E293B.toInt())
                }
                background = bg
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
        }

        windowModeBtn.setOnClickListener {
            if (isFullScreenMode) {
                isFullScreenMode = false
                updateModeButtons()
                recreateFocusWindow()
                syncWithNativeAccessibility()
                hapticHelper.performStepClick()
            }
        }

        fullScreenModeBtn.setOnClickListener {
            if (!isFullScreenMode) {
                isFullScreenMode = true
                updateModeButtons()
                recreateFocusWindow()
                syncWithNativeAccessibility()
                hapticHelper.performStepClick()
            }
        }

        updateModeButtons()
        modeRow.addView(windowModeBtn)
        modeRow.addView(fullScreenModeBtn)
        content.addView(modeRow)

        // Control 4: Atalhos para escala do sistema / tamanho da fonte / acessibilidade
        val shortcutRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(4, 6, 4, 4)
        }

        val fontBtn = TextView(this).apply {
            text = "Fonte & Exibição"
            setTextColor(0xFF38BDF8.toInt())
            textSize = 10.5f
            paint.isFakeBoldText = true
            gravity = Gravity.CENTER
            setPadding(10, 8, 10, 8)
            val btnBg = GradientDrawable().apply {
                cornerRadius = 12f
                setColor(0xFF1E293B.toInt())
                setStroke(1, 0xFF38BDF8.toInt())
            }
            background = btnBg
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = 6
            }
            setOnClickListener {
                try {
                    val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(intent)
                } catch (_: Exception) {
                    val intent = Intent(Settings.ACTION_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(intent)
                }
                hapticHelper.performStepClick()
            }
        }
        shortcutRow.addView(fontBtn)

        val isAccessEnabled = ScreenMagnifierAccessibilityService.isAccessibilityServiceEnabled(this)
        val accessBtn = TextView(this).apply {
            text = if (isAccessEnabled) "Lupa OS Ativa" else "Ativar no Sistema"
            setTextColor(if (isAccessEnabled) 0xFF10B981.toInt() else 0xFFFDE68A.toInt())
            textSize = 10.5f
            paint.isFakeBoldText = true
            gravity = Gravity.CENTER
            setPadding(10, 8, 10, 8)
            val btnBg = GradientDrawable().apply {
                cornerRadius = 12f
                setColor(0xFF1E293B.toInt())
                setStroke(1, if (isAccessEnabled) 0xFF10B981.toInt() else 0xFFFDE68A.toInt())
            }
            background = btnBg
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                try {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(intent)
                } catch (_: Exception) {
                    val intent = Intent(Settings.ACTION_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(intent)
                }
                hapticHelper.performStepClick()
            }
        }
        shortcutRow.addView(accessBtn)
        content.addView(shortcutRow)

        // Close Service Button
        val closeServiceBtn = TextView(this).apply {
            text = "Encerrar Serviço"
            setTextColor(0xFFEF4444.toInt())
            textSize = 10f
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 0)
            setOnClickListener {
                stopSelf()
            }
        }
        content.addView(closeServiceBtn)

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
            service.applyMagnification(currentZoom, cx, cy, !isFullScreenMode)
        } else {
            service?.resetMagnification()
        }
    }

    /**
     * 3. Mecanismo de Ampliação Interna
     * Janela de foco ajustável que amplia digitalmente os elementos gráficos e textos renderizados na tela.
     */
    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    private fun showFocusWindow() {
        if (focusWindowView != null) return

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        if (isFullScreenMode) {
            focusWindowWidth = screenWidth
            focusWindowHeight = screenHeight
            focusWindowX = 0
            focusWindowY = 0
        } else {
            focusWindowWidth = if (isSquareWindow) {
                (screenWidth * 0.74f).toInt().coerceIn(240, 360)
            } else {
                (screenWidth * 0.88f).toInt().coerceIn(280, 450)
            }
            focusWindowHeight = if (isSquareWindow) {
                focusWindowWidth
            } else {
                (screenHeight * 0.32f).toInt().coerceIn(180, 300)
            }
            focusWindowX = (screenWidth - focusWindowWidth) / 2
            focusWindowY = (screenHeight - focusWindowHeight) / 3
        }

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
                cornerRadius = if (isFullScreenMode) 0f else 28f
                setColor(0x18080E1A.toInt())
                setStroke(if (isFullScreenMode) 6 else 3, 0xFF38BDF8.toInt())
            }
            background = bg
            setPadding(8, 8, 8, 8)
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // ==========================================
        // Window Header: Anti-wrap, sleek horizontal bar
        // ==========================================
        val windowHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val barBg = GradientDrawable().apply {
                cornerRadius = 14f
                setColor(0xF00F172A.toInt())
            }
            background = barBg
            setPadding(10, 6, 10, 6)
        }

        val searchIcon = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_search)
            setColorFilter(0xFF38BDF8.toInt())
        }
        windowHeader.addView(searchIcon, LinearLayout.LayoutParams(24, 24))

        val title = TextView(this).apply {
            text = if (isFullScreenMode) "Tela Cheia" else "Lente"
            setTextColor(0xFF38BDF8.toInt())
            textSize = 11.5f
            paint.isFakeBoldText = true
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setPadding(6, 0, 8, 0)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        windowHeader.addView(title)

        if (!isFullScreenMode) {
            val shapeToggle = TextView(this).apply {
                text = if (isSquareWindow) "▢ 1:1" else "▭ 16:9"
                setTextColor(0xFFFDE68A.toInt())
                textSize = 10f
                paint.isFakeBoldText = true
                setPadding(8, 4, 8, 4)
                val btnBg = GradientDrawable().apply {
                    cornerRadius = 8f
                    setColor(0xFF1E293B.toInt())
                    setStroke(1, 0xFFFDE68A.toInt())
                }
                background = btnBg
                setOnClickListener {
                    isSquareWindow = !isSquareWindow
                    recreateFocusWindow()
                    hapticHelper.performStepClick()
                }
            }
            windowHeader.addView(shapeToggle)
        }

        val filterToggle = TextView(this).apply {
            text = filterNames[currentFilterIndex]
            setTextColor(Color.WHITE)
            textSize = 9.5f
            setPadding(8, 4, 8, 4)
            val btnBg = GradientDrawable().apply {
                cornerRadius = 8f
                setColor(0xFF334155.toInt())
            }
            background = btnBg
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = 6
            }
            setOnClickListener {
                currentFilterIndex = (currentFilterIndex + 1) % filterNames.size
                text = filterNames[currentFilterIndex]
                hapticHelper.performStepClick()
            }
        }
        windowHeader.addView(filterToggle)
        content.addView(windowHeader)

        // Viewport Area
        val viewport = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            val vBg = GradientDrawable().apply {
                cornerRadius = 16f
                setColor(0x220284C7.toInt())
                setStroke(1, 0x6638BDF8.toInt())
            }
            background = vBg
        }

        // Center reticle
        val reticleCross = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_search)
            setColorFilter(0x8838BDF8.toInt())
            layoutParams = FrameLayout.LayoutParams(36, 36).apply {
                gravity = Gravity.CENTER
            }
        }
        viewport.addView(reticleCross)

        // Center Zoom badge
        val reticleView = TextView(this).apply {
            text = "${String.format("%.1f", currentZoom)}x"
            setTextColor(0xFFFDE68A.toInt())
            textSize = 14f
            paint.isFakeBoldText = true
            gravity = Gravity.CENTER
            val badgeBg = GradientDrawable().apply {
                cornerRadius = 14f
                setColor(0xCC000000.toInt())
                setStroke(1, 0xFF38BDF8.toInt())
            }
            background = badgeBg
            setPadding(12, 4, 12, 4)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                bottomMargin = 8
            }
        }
        viewport.addView(reticleView)

        content.addView(viewport)
        root.addView(content)

        // Dragging the focus window (in window mode)
        if (!isFullScreenMode) {
            var startX = 0
            var startY = 0
            var touchX = 0f
            var touchY = 0f

            windowHeader.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = params.x
                        startY = params.y
                        touchX = event.rawX
                        touchY = event.rawY
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = startX + (event.rawX - touchX).toInt()
                        params.y = startY + (event.rawY - touchY).toInt()
                        focusWindowX = params.x
                        focusWindowY = params.y
                        windowManager.updateViewLayout(root, params)
                        syncWithNativeAccessibility()
                        true
                    }
                    else -> false
                }
            }
        }

        focusWindowView = root
        windowManager.addView(root, params)
        syncWithNativeAccessibility()
    }

    private fun recreateFocusWindow() {
        if (focusWindowView != null) {
            windowManager.removeView(focusWindowView)
            focusWindowView = null
        }
        if (isZoomActive) {
            showFocusWindow()
        }
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
