package app.habittracker.android.ui.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore

/**
 * Lives in the Activity's store, so it survives rotation. Hands each form-sheet open its own [ViewModelStore]
 * and clears it on [release], so form ViewModels (and their DB listeners) die with the sheet rather than with
 * the Activity.
 */
class FormStoreHolder : ViewModel() {
    private val stores = mutableMapOf<Int, ViewModelStore>()

    fun storeFor(openId: Int): ViewModelStore = stores.getOrPut(openId) { ViewModelStore() }

    /** Clears every ViewModel held for [openId]; safe to call repeatedly. */
    fun release(openId: Int) {
        stores.remove(openId)?.clear()
    }

    override fun onCleared() {
        stores.values.forEach { it.clear() }
        stores.clear()
    }
}
