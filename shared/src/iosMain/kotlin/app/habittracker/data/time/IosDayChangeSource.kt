package app.habittracker.data.time

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.Foundation.NSCalendarDayChangedNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplicationSignificantTimeChangeNotification
import platform.UIKit.UIApplicationWillEnterForegroundNotification

class IosDayChangeSource : DayChangeSource {
    override val changes: Flow<Unit> = callbackFlow {
        val center = NSNotificationCenter.defaultCenter
        val names = listOf(
            NSCalendarDayChangedNotification,
            UIApplicationSignificantTimeChangeNotification,
            UIApplicationWillEnterForegroundNotification,
        )
        val observers = names.map { name ->
            center.addObserverForName(name, null, NSOperationQueue.mainQueue) { _ -> trySend(Unit) }
        }
        awaitClose { observers.forEach { center.removeObserver(it) } }
    }
}
