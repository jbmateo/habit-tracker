package app.habittracker.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ViewModelHandleTest {
    private class TestViewModel : ViewModel() {
        var cleared = false
        override fun onCleared() {
            cleared = true
        }
    }

    @Test
    fun clearInvokesOnCleared() = runTest {
        val handle = handle { TestViewModel() }
        val vm = handle.viewModel
        val job: Job = vm.viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) { awaitCancellation() }
        assertFalse(vm.cleared)
        assertTrue(job.isActive)

        handle.clear()

        assertTrue(vm.cleared)
        assertTrue(job.isCancelled)
    }
}
