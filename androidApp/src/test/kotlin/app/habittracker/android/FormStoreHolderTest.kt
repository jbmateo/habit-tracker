package app.habittracker.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import app.habittracker.android.ui.form.FormStoreHolder
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class FormStoreHolderTest {
    private class Probe : ViewModel() {
        var cleared = 0
        override fun onCleared() { cleared++ }
    }

    private val factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T = Probe() as T
    }

    private fun FormStoreHolder.probe(openId: Int) = ViewModelProvider.create(storeFor(openId), factory)[Probe::class]

    @Test
    fun releaseClearsOnlyThatOpensViewModel() {
        val holder = FormStoreHolder()
        val first = holder.probe(1)
        val second = holder.probe(2)
        assertSame(first, holder.probe(1))
        assertNotSame(first, second)

        holder.release(1)
        assertEquals(1, first.cleared)
        assertEquals(0, second.cleared)

        holder.release(1) // idempotent
        assertEquals(1, first.cleared)
    }

    @Test
    fun clearingTheHolderClearsEveryOpenSheet() {
        val holder = FormStoreHolder()
        val a = holder.probe(1)
        val b = holder.probe(2)
        holder.release(2)
        // ViewModel.onCleared is protected; the holder is cleared through its own store in production.
        val clearHolder = FormStoreHolder::class.java.getDeclaredMethod("onCleared").apply { isAccessible = true }
        clearHolder.invoke(holder)
        assertEquals(1, a.cleared)
        assertEquals(1, b.cleared)
    }
}
