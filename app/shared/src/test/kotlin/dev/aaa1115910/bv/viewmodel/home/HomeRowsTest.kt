package dev.aaa1115910.bv.viewmodel.home

import kotlin.test.Test
import kotlin.test.assertEquals

class HomeRowsTest {
    @Test
    fun followingHiddenWhenLoggedOut() {
        assertEquals(listOf(HomeRow.Recommend, HomeRow.Popular), homeRows(isLogin = false, followingCount = 10))
    }

    @Test
    fun followingHiddenWhenEmpty() {
        assertEquals(listOf(HomeRow.Recommend, HomeRow.Popular), homeRows(isLogin = true, followingCount = 0))
    }

    @Test
    fun followingShownWhenLoggedInWithItems() {
        assertEquals(
            listOf(HomeRow.Recommend, HomeRow.Following, HomeRow.Popular),
            homeRows(isLogin = true, followingCount = 3)
        )
    }
}
