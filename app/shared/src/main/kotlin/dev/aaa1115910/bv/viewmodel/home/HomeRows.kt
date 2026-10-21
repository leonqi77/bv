package dev.aaa1115910.bv.viewmodel.home

enum class HomeRow { Recommend, Following, Popular }

/** 首页显示的分行：关注的 UP 主 仅在登录且有内容时显示。 */
fun homeRows(isLogin: Boolean, followingCount: Int): List<HomeRow> =
    if (isLogin && followingCount > 0) listOf(HomeRow.Recommend, HomeRow.Following, HomeRow.Popular)
    else listOf(HomeRow.Recommend, HomeRow.Popular)
