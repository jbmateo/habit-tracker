package app.habittracker.data.time

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class AndroidDayChangeSource(private val context: Context) : DayChangeSource {
    override val changes: Flow<Unit> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(Unit)
            }
        }
        val observer = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                trySend(Unit)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        val main = Handler(Looper.getMainLooper())

        // No suspension point between registration and awaitClose, so cleanup always runs.
        // Lifecycle observers are main-thread-confined: add and remove are both posted to the
        // main looper, so removal is ordered after the add.
        context.registerReceiver(receiver, filter)
        main.post { ProcessLifecycleOwner.get().lifecycle.addObserver(observer) }
        awaitClose {
            context.unregisterReceiver(receiver)
            main.post { ProcessLifecycleOwner.get().lifecycle.removeObserver(observer) }
        }
    }
}
