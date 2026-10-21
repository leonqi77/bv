package dev.aaa1115910.bv.viewmodel.home

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class HomeRowLoaderTest {
    @Test
    fun loadMoreSkipsWhileRowIsLoading() = runBlocking {
        val loader = HomeRowLoader(this)
        val gate = CompletableDeferred<Unit>()
        var starts = 0
        val first = loader.loadMore(HomeRow.Recommend) { starts++; gate.await() }
        yield()
        val second = loader.loadMore(HomeRow.Recommend) { starts++ }
        assertSame(first, second)
        gate.complete(Unit)
        first.join()
        loader.loadMore(HomeRow.Recommend) { starts++ }.join()
        assertEquals(2, starts)
    }

    @Test
    fun reloadCancelsInFlightLoadBeforeResetting() = runBlocking {
        val loader = HomeRowLoader(this)
        val events = mutableListOf<String>()
        val gate = CompletableDeferred<Unit>()
        val old = loader.loadMore(HomeRow.Recommend) {
            try {
                events += "old-start"
                // 和 ViewModel 一样吞掉异常，之后的挂起点（切到主线程追加数据）会因取消而抛出
                runCatching { gate.await() }
                yield()
                events += "old-append"
            } finally {
                events += "old-end"
            }
        }
        yield()
        loader.reload(HomeRow.Recommend, reset = { events += "reset" }) { events += "new" }.join()
        gate.complete(Unit)
        old.join()
        assertEquals(listOf("old-start", "old-end", "reset", "new"), events)
    }

    @Test
    fun reloadWhenIdleResetsThenLoads() = runBlocking {
        val loader = HomeRowLoader(this)
        val events = mutableListOf<String>()
        loader.reload(HomeRow.Recommend, reset = { events += "reset" }) { events += "new" }.join()
        assertEquals(listOf("reset", "new"), events)
    }

    @Test
    fun rowsLoadIndependently() = runBlocking {
        val loader = HomeRowLoader(this)
        val gate = CompletableDeferred<Unit>()
        var popularStarted = false
        val recommend = loader.loadMore(HomeRow.Recommend) { gate.await() }
        loader.loadMore(HomeRow.Popular) { popularStarted = true }.join()
        assertEquals(true, popularStarted)
        gate.complete(Unit)
        recommend.join()
    }
}
