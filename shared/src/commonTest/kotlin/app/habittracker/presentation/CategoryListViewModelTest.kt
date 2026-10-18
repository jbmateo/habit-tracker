package app.habittracker.presentation

import app.cash.turbine.test
import app.habittracker.HEALTH
import app.habittracker.domain.model.DomainError
import app.habittracker.domain.usecase.CreateCategory
import app.habittracker.domain.usecase.DeleteCategory
import app.habittracker.domain.usecase.ObserveCategories
import app.habittracker.domain.usecase.UpdateCategory
import app.habittracker.fakes.FakeCategoryRepository
import app.habittracker.fakes.FakeStore
import app.habittracker.habit
import app.habittracker.presentation.categories.CategoryEditor
import app.habittracker.presentation.categories.CategoryListEffect
import app.habittracker.presentation.categories.CategoryListEvent
import app.habittracker.presentation.categories.CategoryListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryListViewModelTest {
    private val store = FakeStore().apply { categories.value = listOf(HEALTH) }
    private val categories = FakeCategoryRepository(store)

    private fun vm() = CategoryListViewModel(
        ObserveCategories(categories),
        CreateCategory(categories),
        UpdateCategory(categories),
        DeleteCategory(categories),
    )

    @BeforeTest fun setUp() { Dispatchers.setMain(StandardTestDispatcher()) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    @Test fun addCategorySaves() = runTest {
        val vm = vm()
        vm.uiState.test {
            awaitItem() // initial empty
            assertEquals(listOf(HEALTH), awaitItem().categories)
            vm.onEvent(CategoryListEvent.AddClicked)
            assertEquals(CategoryEditor(null, "", "blue", null), awaitItem().editor)
            vm.onEvent(CategoryListEvent.NameChanged("Study"))
            assertEquals("Study", awaitItem().editor?.name)
            vm.onEvent(CategoryListEvent.ColorChanged("green"))
            assertEquals("green", awaitItem().editor?.colorKey)
            vm.onEvent(CategoryListEvent.SaveEditor)
            // The repository emission and the editor closing may arrive as one or two states.
            var state = awaitItem()
            if (state.editor != null) state = awaitItem()
            if (state.categories.size == 1) state = awaitItem()
            assertNull(state.editor)
            assertEquals(listOf("Health", "Study"), state.categories.map { it.name })
            assertEquals("green", state.categories.last().colorKey)
        }
    }

    @Test fun editClickedPrefillsAndUpdates() = runTest {
        val vm = vm()
        vm.uiState.test {
            awaitItem(); awaitItem()
            vm.onEvent(CategoryListEvent.EditClicked(HEALTH.id))
            assertEquals(CategoryEditor(HEALTH.id, "Health", "green", null), awaitItem().editor)
            vm.onEvent(CategoryListEvent.NameChanged("Fitness"))
            awaitItem()
            vm.onEvent(CategoryListEvent.SaveEditor)
            var state = awaitItem()
            if (state.editor != null || state.categories.single().name == "Health") state = awaitItem()
            if (state.editor != null || state.categories.single().name == "Health") state = awaitItem()
            assertNull(state.editor)
            assertEquals("Fitness", state.categories.single().name)
        }
    }

    @Test fun dismissClosesEditor() = runTest {
        val vm = vm()
        vm.uiState.test {
            awaitItem(); awaitItem()
            vm.onEvent(CategoryListEvent.AddClicked)
            assertTrue(awaitItem().editor != null)
            vm.onEvent(CategoryListEvent.DismissEditor)
            assertNull(awaitItem().editor)
        }
    }

    @Test fun invalidNameShowsInlineError() = runTest {
        val vm = vm()
        val effects = mutableListOf<CategoryListEffect>()
        backgroundScope.launch { vm.effects.toList(effects) }
        vm.uiState.test {
            awaitItem(); awaitItem()
            vm.onEvent(CategoryListEvent.AddClicked)
            awaitItem()
            vm.onEvent(CategoryListEvent.NameChanged("   "))
            awaitItem()
            vm.onEvent(CategoryListEvent.SaveEditor)
            val editor = awaitItem().editor
            assertEquals(DomainError.InvalidName, editor?.error)
            // Editing clears the stale error.
            vm.onEvent(CategoryListEvent.NameChanged("Ok"))
            assertNull(awaitItem().editor?.error)
        }
        advanceUntilIdle()
        assertTrue(effects.isEmpty())
        assertEquals(1, store.categories.value.size)
    }

    @Test fun deleteNonEmptyEmitsCategoryNotEmpty() = runTest {
        store.habits.value = listOf(habit("a"))
        val vm = vm()
        vm.effects.test {
            vm.onEvent(CategoryListEvent.DeleteClicked(HEALTH.id))
            assertEquals(CategoryListEffect.ShowDomainError(DomainError.CategoryNotEmpty), awaitItem())
        }
        assertEquals(1, store.categories.value.size)
    }

    @Test fun deleteEmptyRemovesCategory() = runTest {
        val vm = vm()
        vm.uiState.test {
            awaitItem(); awaitItem()
            vm.onEvent(CategoryListEvent.DeleteClicked(HEALTH.id))
            assertTrue(awaitItem().categories.isEmpty())
        }
    }

    @Test fun unexpectedExceptionEmitsShowError() = runTest {
        val vm = vm()
        vm.effects.test {
            store.failNextWrite = IllegalStateException("disk full")
            vm.onEvent(CategoryListEvent.DeleteClicked(HEALTH.id))
            assertEquals(CategoryListEffect.ShowError("disk full"), awaitItem())
        }
    }
}
