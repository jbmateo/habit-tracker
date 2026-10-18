package app.habittracker.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

/** Owns a ViewModel on iOS; Swift calls [clear] when the screen goes away (runs onCleared, cancels viewModelScope). */
class ViewModelHandle<VM : ViewModel>(val viewModel: VM, private val store: ViewModelStore) {
    fun clear() = store.clear()
}

/** Builds a [ViewModelHandle] with its own fresh [ViewModelStore]. */
internal inline fun <reified VM : ViewModel> handle(noinline create: () -> VM): ViewModelHandle<VM> {
    val store = ViewModelStore()
    val vm = ViewModelProvider.create(store, viewModelFactory { initializer { create() } })[VM::class]
    return ViewModelHandle(vm, store)
}
