package com.carromdual.screen.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.carromdual.screen.R
import com.carromdual.screen.data.PrefsManager
import com.carromdual.screen.ui.MainActivity
import com.carromdual.screen.utils.MultiWindowHelper
import com.carromdual.screen.utils.PermissionHelper
import kotlin.math.abs

class FloatingWindowService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var prefs: PrefsManager
    private var rootView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private lateinit var panelContainer: LinearLayout
    private lateinit var bubbleContainer: FrameLayout
    private lateinit var tvFeedback: TextView

    private var isMinimized = false
    private var isMaximized = false
    private var savedWidthPx = 0
    private var savedHeightPx = 0

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        prefs = PrefsManager(this)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        if (!PermissionHelper.hasOverlayPermission(this)) {
            stopSelf()
            return
        }

        createNotificationChannel()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIF_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIF_ID, notification)
        }

        setupOverlayView()
        isRunning = true
        notifyStateChanged()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_UPDATE_SETTINGS -> {
                applySavedSettings()
            }
        }
        return START_STICKY
    }

    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    private fun setupOverlayView() {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.layout_floating_window, null)
        rootView = view

        panelContainer = view.findViewById(R.id.floatingPanelContainer)
        bubbleContainer = view.findViewById(R.id.floatingBubbleContainer)
        tvFeedback = view.findViewById(R.id.tvFloatFeedback)

        val dragHeader: View = view.findViewById(R.id.floatingDragHeader)
        val btnMin: ImageButton = view.findViewById(R.id.btnFloatMinimize)
        val btnMax: ImageButton = view.findViewById(R.id.btnFloatMaximize)
        val btnClose: ImageButton = view.findViewById(R.id.btnFloatClose)
        val btnScreen1: Button = view.findViewById(R.id.btnFloatScreen1)
        val btnScreen2: Button = view.findViewById(R.id.btnFloatScreen2)
        val btnSplit: Button = view.findViewById(R.id.btnFloatSplitLaunch)
        val btnDash: Button = view.findViewById(R.id.btnFloatDashboard)
        val resizeHandle: ImageView = view.findViewById(R.id.btnFloatResizeHandle)

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val widthPx = dpToPx(prefs.floatingWidthDp)
        val heightPx = dpToPx(prefs.floatingHeightDp)
        savedWidthPx = widthPx
        savedHeightPx = heightPx

        val params = WindowManager.LayoutParams(
            widthPx,
            heightPx,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = prefs.floatingPosX
            y = prefs.floatingPosY
        }
        layoutParams = params

        panelContainer.layoutParams = FrameLayout.LayoutParams(widthPx, heightPx)
        panelContainer.alpha = prefs.floatingOpacityPercent / 100f

        setupDragListener(dragHeader, isBubble = false)
        setupDragListener(bubbleContainer, isBubble = true)
        setupResizeListener(resizeHandle)

        btnMin.setOnClickListener { setMinimized(true) }
        btnMax.setOnClickListener { toggleMaximized() }
        btnClose.setOnClickListener { stopSelf() }

        btnScreen1.setOnClickListener {
            val res = MultiWindowHelper.launchSingle(
                this,
                prefs.screen1Package,
                prefs.screen1UserSerial
            )
            tvFeedback.text = res.message
        }

        btnScreen2.setOnClickListener {
            val res = MultiWindowHelper.launchInMultiWindow(
                context = this,
                packageName = prefs.screen2Package,
                userSerial = prefs.screen2UserSerial,
                useFreeform = prefs.launchMode == PrefsManager.MODE_FREEFORM,
                slotIndex = 2,
                preset = prefs.splitLayoutPreset
            )
            tvFeedback.text = res.message
        }

        btnSplit.setOnClickListener {
            val useFreeform = prefs.launchMode == PrefsManager.MODE_FREEFORM
            val res1 = MultiWindowHelper.launchInMultiWindow(
                context = this,
                packageName = prefs.screen1Package,
                userSerial = prefs.screen1UserSerial,
                useFreeform = useFreeform,
                slotIndex = 1,
                preset = prefs.splitLayoutPreset
            )
            tvFeedback.text = res1.message
            mainHandler.postDelayed({
                val res2 = MultiWindowHelper.launchInMultiWindow(
                    context = this,
                    packageName = prefs.screen2Package,
                    userSerial = prefs.screen2UserSerial,
                    useFreeform = useFreeform,
                    slotIndex = 2,
                    preset = prefs.splitLayoutPreset
                )
                tvFeedback.text = "${res1.message} | ${res2.message}"
            }, 450L)
        }

        btnDash.setOnClickListener {
            val dashIntent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
            startActivity(dashIntent)
        }

        windowManager.addView(view, params)

        if (prefs.floatingStartMinimized) {
            setMinimized(true)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDragListener(target: View, isBubble: Boolean) {
        var initialX = 0
        var initialY = 0
        var touchStartX = 0f
        var touchStartY = 0f
        var moved = false

        target.setOnTouchListener { _, event ->
            val params = layoutParams ?: return@setOnTouchListener false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    touchStartX = event.rawX
                    touchStartY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchStartX).toInt()
                    val dy = (event.rawY - touchStartY).toInt()
                    if (abs(dx) > 6 || abs(dy) > 6) {
                        moved = true
                    }
                    val (screenW, screenH) = getScreenSize()
                    val viewW = if (isBubble) dpToPx(54) else params.width
                    val viewH = if (isBubble) dpToPx(54) else params.height
                    params.x = (initialX + dx).coerceIn(0, (screenW - viewW).coerceAtLeast(0))
                    params.y = (initialY + dy).coerceIn(0, (screenH - viewH).coerceAtLeast(0))
                    updateOverlayLayout()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isBubble && !moved) {
                        setMinimized(false)
                    } else if (isBubble && prefs.floatingSnapToEdge) {
                        val (screenW, _) = getScreenSize()
                        val bubbleW = dpToPx(54)
                        params.x = if (params.x + bubbleW / 2 < screenW / 2) {
                            dpToPx(8)
                        } else {
                            (screenW - bubbleW - dpToPx(8)).coerceAtLeast(0)
                        }
                        updateOverlayLayout()
                    }
                    prefs.floatingPosX = params.x
                    prefs.floatingPosY = params.y
                    true
                }
                else -> false
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupResizeListener(handle: View) {
        var startW = 0
        var startH = 0
        var touchStartX = 0f
        var touchStartY = 0f

        handle.setOnTouchListener { _, event ->
            val params = layoutParams ?: return@setOnTouchListener false
            val (screenW, screenH) = getScreenSize()
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startW = params.width
                    startH = params.height
                    touchStartX = event.rawX
                    touchStartY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val minW = dpToPx(210)
                    val minH = dpToPx(180)
                    val maxW = (screenW * 0.94f).toInt()
                    val maxH = (screenH * 0.72f).toInt()
                    val newW = (startW + (event.rawX - touchStartX).toInt()).coerceIn(minW, maxW)
                    val newH = (startH + (event.rawY - touchStartY).toInt()).coerceIn(minH, maxH)
                    params.width = newW
                    params.height = newH
                    panelContainer.layoutParams = FrameLayout.LayoutParams(newW, newH)
                    savedWidthPx = newW
                    savedHeightPx = newH
                    isMaximized = false
                    updateOverlayLayout()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    prefs.floatingWidthDp = pxToDp(params.width)
                    prefs.floatingHeightDp = pxToDp(params.height)
                    true
                }
                else -> false
            }
        }
    }

    private fun setMinimized(minimized: Boolean) {
        val params = layoutParams ?: return
        isMinimized = minimized
        if (minimized) {
            panelContainer.visibility = View.GONE
            bubbleContainer.visibility = View.VISIBLE
            params.width = WindowManager.LayoutParams.WRAP_CONTENT
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
        } else {
            bubbleContainer.visibility = View.GONE
            panelContainer.visibility = View.VISIBLE
            val w = if (isMaximized) (getScreenSize().first * 0.92f).toInt() else savedWidthPx
            val h = if (isMaximized) (getScreenSize().second * 0.50f).toInt() else savedHeightPx
            params.width = w
            params.height = h
            panelContainer.layoutParams = FrameLayout.LayoutParams(w, h)
        }
        updateOverlayLayout()
    }

    private fun toggleMaximized() {
        val params = layoutParams ?: return
        val (screenW, screenH) = getScreenSize()
        isMaximized = !isMaximized
        val targetW = if (isMaximized) (screenW * 0.92f).toInt() else savedWidthPx
        val targetH = if (isMaximized) (screenH * 0.50f).toInt() else savedHeightPx
        params.width = targetW
        params.height = targetH
        if (isMaximized) {
            params.x = ((screenW - targetW) / 2).coerceAtLeast(0)
        }
        panelContainer.layoutParams = FrameLayout.LayoutParams(targetW, targetH)
        updateOverlayLayout()
    }

    private fun applySavedSettings() {
        val params = layoutParams ?: return
        savedWidthPx = dpToPx(prefs.floatingWidthDp)
        savedHeightPx = dpToPx(prefs.floatingHeightDp)
        panelContainer.alpha = prefs.floatingOpacityPercent / 100f
        if (!isMinimized && !isMaximized) {
            params.width = savedWidthPx
            params.height = savedHeightPx
            panelContainer.layoutParams = FrameLayout.LayoutParams(savedWidthPx, savedHeightPx)
            updateOverlayLayout()
        }
    }

    private fun updateOverlayLayout() {
        val view = rootView ?: return
        val params = layoutParams ?: return
        try {
            windowManager.updateViewLayout(view, params)
        } catch (_: Throwable) {
            // View might already be detached
        }
    }

    private fun getScreenSize(): Pair<Int, Int> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            bounds.width() to bounds.height()
        } else {
            val dm = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(dm)
            dm.widthPixels to dm.heightPixels
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun pxToDp(px: Int): Int {
        return (px / resources.displayMetrics.density).toInt()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notif_channel_desc)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openDashIntent = Intent(this, MainActivity::class.java)
        val pendingOpen = PendingIntent.getActivity(
            this,
            0,
            openDashIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, FloatingWindowService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_floating_window)
            .setContentTitle(getString(R.string.notif_floating_title))
            .setContentText(getString(R.string.notif_floating_text))
            .setContentIntent(pendingOpen)
            .addAction(R.drawable.ic_close, "Close", pendingStop)
            .setOngoing(true)
            .build()
    }

    private fun notifyStateChanged() {
        val intent = Intent(ACTION_STATE_CHANGED).setPackage(packageName)
        sendBroadcast(intent)
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        rootView?.let {
            try {
                windowManager.removeViewImmediate(it)
            } catch (_: Throwable) {
            }
        }
        rootView = null
        isRunning = false
        notifyStateChanged()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "carrom_dual_floating_channel"
        private const val NOTIF_ID = 1042

        const val ACTION_STOP = "com.carromdual.screen.ACTION_STOP_FLOATING"
        const val ACTION_UPDATE_SETTINGS = "com.carromdual.screen.ACTION_UPDATE_FLOATING"
        const val ACTION_STATE_CHANGED = "com.carromdual.screen.FLOATING_STATE_CHANGED"

        @Volatile
        var isRunning: Boolean = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, FloatingWindowService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FloatingWindowService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
