package com.sevenbits.myfit.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.sevenbits.myfit.MainActivity
import com.sevenbits.myfit.R
import com.sevenbits.myfit.core.domain.calculator.RestTimerCalculator
import com.sevenbits.myfit.timer.RestTimerEngine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import javax.inject.Inject

/**
 * 휴식 타이머 Foreground Service. (FN-TOL-004/005 / 02_개발환경및빌드정의서 §5.2)
 *
 * **역할은 두 가지뿐이다.**
 * 1. 앱이 백그라운드·화면잠금 상태여도 프로세스가 유지되게 한다
 * 2. 잠금화면에 잔여 시간을 알림으로 표시하고, 종료 시 진동으로 알린다
 *
 * 시간 계산은 [RestTimerEngine] 이 담당한다. 서비스는 상태를 관찰만 한다.
 *
 * FGS 타입은 `health` — 운동 세션 추적 용도로 Android 14+ 정책에 부합한다.
 */
@AndroidEntryPoint
class RestTimerService : Service() {

    @Inject
    lateinit var engine: RestTimerEngine

    private val scope = CoroutineScope(SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()

        engine.state
            .onEach { state ->
                if (!state.isRunning) {
                    stopSelf()
                    return@onEach
                }
                notificationManager.notify(NOTIFICATION_ID, buildNotification(state.remainingMillis, state.isPaused))
            }
            .launchIn(scope)

        // 종료 시 진동으로 알린다. 헬스장에서는 소리를 못 듣는 경우가 많다. (FN-TOL-005)
        engine.finished
            .onEach { vibrateOnFinish() }
            .launchIn(scope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val state = engine.state.value

        // 알림을 못 띄우는 것과 앱이 죽는 것은 전혀 다른 문제다.
        // 잔여 시간은 RestTimerEngine 이 elapsedRealtime 델타로 따로 계산하므로,
        // 여기서 실패해도 화면 안의 타이머는 정확하게 계속 간다. 운동 기록을
        // 날리는 것보다 알림을 포기하는 편이 낫다.
        //
        // 실제로 겪은 사고: FGS 타입을 health 로 두었더니 Android 14+ 가 센서 권한을
        // 요구하며 SecurityException 을 던졌고, 세트를 완료할 때마다 앱이 죽었다.
        runCatching {
            startForeground(
                NOTIFICATION_ID,
                buildNotification(state.remainingMillis, state.isPaused),
            )
        }.onFailure { error ->
            Timber.w(error, "휴식 타이머 알림을 띄우지 못했습니다. 타이머는 계속 동작합니다.")
            stopSelf()
        }

        // 시스템이 서비스를 종료해도 재생성하지 않는다 — 타이머는 사용자가 다시 시작한다
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private val notificationManager: NotificationManager
        get() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_rest_timer),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            setShowBadge(false)
            enableVibration(false) // 진동은 종료 시점에만 직접 발생시킨다
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildNotification(remainingMillis: Long, isPaused: Boolean): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val remaining = RestTimerCalculator.formatRemaining(remainingMillis)
        val title = if (isPaused) {
            getString(R.string.rest_timer_paused)
        } else {
            getString(R.string.rest_timer_running)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(remaining)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .build()
    }

    private fun vibrateOnFinish() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.vibrate(
            VibrationEffect.createWaveform(FINISH_VIBRATION_PATTERN, VIBRATION_NO_REPEAT),
        )
    }

    companion object {
        private const val CHANNEL_ID = "ch_rest_timer"
        private const val NOTIFICATION_ID = 1001
        private val FINISH_VIBRATION_PATTERN = longArrayOf(0, 300, 150, 300)
        private const val VIBRATION_NO_REPEAT = -1

        fun start(context: Context) {
            context.startForegroundService(Intent(context, RestTimerService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, RestTimerService::class.java))
        }
    }
}
