package dev.aaa1115910.bv.viewmodel.home

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * 首页各分行的加载任务：每行同时最多一个加载。
 * 刷新时先取消并等待旧加载结束，再重置数据并重新加载，避免旧请求的结果落进刚清空的列表。
 * 只应在同一线程（主线程）调用。
 */
class HomeRowLoader(private val scope: CoroutineScope) {
    private val jobs = mutableMapOf<HomeRow, Job>()

    /** 该行没有进行中的加载时才开始新的加载，否则返回进行中的任务。 */
    fun loadMore(
        row: HomeRow,
        context: CoroutineContext = EmptyCoroutineContext,
        block: suspend () -> Unit
    ): Job {
        jobs[row]?.takeIf { it.isActive }?.let { return it }
        return scope.launch(context) { block() }.also { jobs[row] = it }
    }

    /** 取消并等待进行中的加载，执行 [reset] 后重新加载。 */
    suspend fun reload(
        row: HomeRow,
        context: CoroutineContext = EmptyCoroutineContext,
        reset: () -> Unit,
        block: suspend () -> Unit
    ): Job {
        jobs[row]?.cancelAndJoin()
        reset()
        return scope.launch(context) { block() }.also { jobs[row] = it }
    }
}
