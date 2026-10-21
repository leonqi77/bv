package dev.aaa1115910.bv.tv.screens.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.DrawerValue
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemColors
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.NavigationDrawer
import androidx.tv.material3.NavigationDrawerItemDefaults
import androidx.tv.material3.NavigationDrawerScope
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import androidx.tv.material3.rememberDrawerState
import coil.compose.AsyncImage
import dev.aaa1115910.bv.ui.theme.BVTheme
import dev.aaa1115910.bv.ui.theme.BvColors
import dev.aaa1115910.bv.util.ifElse
import dev.aaa1115910.bv.util.isDpadRight
import dev.aaa1115910.bv.util.isKeyDown
import kotlinx.coroutines.delay

/**
 * Expanded item width: icon plus a two-character label. With the 12dp drawer padding on each side
 * the expanded side menu is 160dp wide. The collapsed rail keeps the library width.
 */
private val ExpandedDrawerItemWidth = 136.dp

/**
 * Same as [NavigationDrawerItem], but with a narrower expanded width. The library item hardcodes
 * its width (262dp) inside a layout modifier, so it cannot be narrowed from outside.
 */
@Composable
private fun NavigationDrawerScope.DrawerNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: ListItemColors = ListItemDefaults.colors(),
    leadingContent: @Composable BoxScope.() -> Unit,
    content: @Composable () -> Unit
) {
    val width by animateDpAsState(
        targetValue = if (hasFocus) ExpandedDrawerItemWidth
        else NavigationDrawerItemDefaults.CollapsedDrawerItemWidth,
        label = "drawer item width"
    )
    ListItem(
        selected = selected,
        onClick = onClick,
        modifier = modifier.width(width),
        headlineContent = {
            AnimatedVisibility(
                visible = hasFocus,
                enter = NavigationDrawerItemDefaults.ContentAnimationEnter,
                exit = NavigationDrawerItemDefaults.ContentAnimationExit
            ) {
                content()
            }
        },
        leadingContent = {
            Box(modifier = Modifier.size(NavigationDrawerItemDefaults.IconSize)) {
                leadingContent()
            }
        },
        shape = ListItemDefaults.shape(
            shape = CircleShape,
            focusedShape = CircleShape,
            pressedShape = CircleShape,
            selectedShape = CircleShape,
            focusedSelectedShape = CircleShape,
            pressedSelectedShape = CircleShape
        ),
        colors = colors
    )
}

@Composable
fun NavigationDrawerScope.DrawerContent(
    modifier: Modifier = Modifier,
    isLogin: Boolean = false,
    avatar: String = "",
    username: String = "",
    onDrawerItemChanged: (DrawerItem) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onShowUserPanel: () -> Unit = {},
    onFocusToContent: () -> Unit = {},
    onLogin: () -> Unit = {},
    onReselect: (DrawerItem) -> Unit = {},
    itemsFocusRequester: FocusRequester = remember { FocusRequester() }
) {
    var selectedItem by remember { mutableStateOf(DrawerItem.Home) }
    val centerFocusRequester = remember { FocusRequester() }
    var tabMoved by remember { mutableStateOf(true) }

    LaunchedEffect(selectedItem) {
        tabMoved = false
        delay(200)
        onDrawerItemChanged(selectedItem)
        // 别急着向右移动焦点，动画还没结束
        delay(200)
        tabMoved = true
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(12.dp)
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.isDpadRight()) {
                    if (keyEvent.isKeyDown()) {
                        if (tabMoved) onFocusToContent()
                        return@onPreviewKeyEvent true
                    }
                }
                false
            },
        verticalArrangement = Arrangement.SpaceBetween
    ) {

        DrawerNavItem(
            modifier = Modifier,
            onClick = {
                if (isLogin) {
                    onShowUserPanel()
                } else {
                    onLogin()
                }
            },
            selected = selectedItem == DrawerItem.User,
            leadingContent = {
                if (isLogin) {
                    Surface(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        colors = SurfaceDefaults.colors(
                            containerColor = Color.Gray
                        )
                    ) {
                        AsyncImage(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            model = avatar,
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds
                        )
                    }
                } else {
                    Icon(
                        imageVector = DrawerItem.User.displayIcon,
                        contentDescription = null
                    )
                }
            }
        ) {
            Text(
                text = if (isLogin) username
                else DrawerItem.User.displayName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        LazyColumn(
            modifier = Modifier
                .focusRequester(itemsFocusRequester)
                .focusRestorer(centerFocusRequester),
            verticalArrangement = Arrangement.Center
        ) {
            listOf(
                DrawerItem.Search,
                DrawerItem.Home,
                DrawerItem.UGC,
                DrawerItem.PGC,
            ).forEach { item ->
                item {
                    DrawerNavItem(
                        modifier = Modifier
                            .onFocusChanged { if (it.hasFocus) selectedItem = item }
                            .ifElse(
                                item == DrawerItem.Home,
                                Modifier.focusRequester(centerFocusRequester)
                            ),
                        onClick = {
                            if (item == selectedItem) onReselect(item)
                            selectedItem = item
                        },
                        colors = ListItemDefaults.colors(
                            selectedContainerColor = BvColors.ChipFill,
                            selectedContentColor = BvColors.Accent,
                            focusedSelectedContainerColor = BvColors.ChipFill,
                            focusedSelectedContentColor = BvColors.Accent,
                            pressedSelectedContainerColor = BvColors.ChipFill,
                            pressedSelectedContentColor = BvColors.Accent
                        ),
                        selected = selectedItem == item,
                        leadingContent = {
                            Icon(
                                imageVector = item.displayIcon,
                                contentDescription = null
                            )
                        }
                    ) {
                        Text(text = item.displayName)
                    }
                }
            }
        }
        DrawerNavItem(
            modifier = Modifier,
            onClick = onOpenSettings,
            selected = false,
            leadingContent = {
                Icon(
                    imageVector = DrawerItem.Settings.displayIcon,
                    contentDescription = null
                )
            }
        ) {
            Text(text = DrawerItem.Settings.displayName)
        }
    }
}

enum class DrawerItem(
    val displayName: String,
    val displayIcon: ImageVector
) {
    User(displayName = "登录", displayIcon = Icons.Default.AccountCircle),
    Search(displayName = "搜索", displayIcon = Icons.Default.Search),
    Home(displayName = "首页", displayIcon = Icons.Default.Home),
    UGC(displayName = "分区", displayIcon = Icons.Default.OndemandVideo),
    PGC(displayName = "影视", displayIcon = Icons.Default.Movie),
    Settings(displayName = "设置", displayIcon = Icons.Default.Settings), ;
}

@Preview(device = "id:tv_1080p")
@Composable
private fun DrawerContentClosedPreview() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    BVTheme {
        NavigationDrawer(
            drawerContent = {
                DrawerContent()
            },
            drawerState = drawerState
        ) { }
    }
}

@Preview(device = "id:tv_1080p")
@Composable
private fun DrawerContentOpenPreview() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Open)
    BVTheme {
        NavigationDrawer(
            drawerContent = {
                DrawerContent()
            },
            drawerState = drawerState
        ) { }
    }
}