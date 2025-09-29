package dev.aaa1115910.bv.tv.screens.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import coil.compose.AsyncImage
import dev.aaa1115910.bv.component.TopNavItem
import dev.aaa1115910.bv.ui.theme.BVTheme
import dev.aaa1115910.bv.util.ifElse
import dev.aaa1115910.bv.util.isDpadRight
import dev.aaa1115910.bv.util.isDpadUp
import dev.aaa1115910.bv.util.isKeyDown

// 创建全局的FocusRequester映射表，方便外部使用
val drawerItemFocusRequesters = mutableMapOf<DrawerItem, FocusRequester>().apply {
    DrawerItem.entries.filter { it != DrawerItem.User }
        .forEach { item ->
            this[item] = FocusRequester()
        }
}

// 用于记住每个内容页当前选中的Tab
val currentSelectedTabs = mutableStateMapOf<DrawerItem, TopNavItem>()

@Composable
fun DrawerContent(
    modifier: Modifier = Modifier,
    isLogin: Boolean = false,
    avatar: String = "",
    onDrawerItemChanged: (DrawerItem) -> Unit = {},
    onShowUserPanel: () -> Unit = {},
    onFocusToContent: () -> Unit = {},
    onLogin: () -> Unit = {}
) {
    var selectedItem by remember { mutableStateOf(DrawerItem.Home) }
    var focusInUser by remember { mutableStateOf(false) }
    val centerFocusRequester = remember { FocusRequester() }
    val itemColors = NavigationDrawerItemDefaults.colors()
    val iconColors = IconButtonDefaults.colors(
        containerColor = when {
            selectedItem == DrawerItem.User -> itemColors.selectedContentColor
            else -> itemColors.containerColor
        },
        focusedContainerColor = itemColors.focusedContainerColor
    )

    LaunchedEffect(selectedItem) {
        onDrawerItemChanged(selectedItem)
    }

    Column(
        modifier = modifier
            .width(44.dp)
            .fillMaxHeight()
            .padding(vertical = 12.dp)
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.isKeyDown()) {
                    if (keyEvent.isDpadRight()) {
                        onFocusToContent()
                        return@onPreviewKeyEvent true
                    }
                    // 已经是最上时拦截事件
                    if (keyEvent.isDpadUp() && focusInUser) {
                        return@onPreviewKeyEvent true
                    }
                }
                false
            },
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 用户头像
        IconButton(
            modifier = Modifier.onFocusChanged {
                focusInUser = it.hasFocus
            },
            colors = iconColors,
            onClick = {
                if (isLogin) {
                    onShowUserPanel()
                } else {
                    onLogin()
                }
            }
        ) {
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
        Spacer(modifier = Modifier.weight(1f))

        listOf(
            DrawerItem.Search,
            DrawerItem.Home,
            // DrawerItem.UGC,
            // DrawerItem.PGC,
        ).forEach { item ->
            IconButton(
                modifier = Modifier
                    .focusRequester(drawerItemFocusRequesters[item]!!)
                    .onFocusChanged { if (it.hasFocus) selectedItem = item }
                    .ifElse(
                        item == DrawerItem.Home,
                        Modifier.focusRequester(centerFocusRequester)
                    ),
                colors = iconColors,
                onClick = {
                    selectedItem = item
                    onFocusToContent()
                }
            ) {
                Icon(
                    imageVector = item.displayIcon,
                    contentDescription = null
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        IconButton(
            modifier = Modifier
                .focusRequester(drawerItemFocusRequesters[DrawerItem.Settings]!!)
                .onFocusChanged { if (it.hasFocus) selectedItem = DrawerItem.Settings },
            colors = iconColors,
            onClick = {
                selectedItem = DrawerItem.Settings
                onFocusToContent()
            }
        ) {
            Icon(
                imageVector = DrawerItem.Settings.displayIcon,
                contentDescription = null
            )
        }
    }
}

enum class DrawerItem(
    val displayName: String,
    val displayIcon: ImageVector
) {
    User(displayName = "点击登录", displayIcon = Icons.Default.AccountCircle),
    //Search(displayName = "搜索", displayIcon = Icons.Default.Search),
    Home(displayName = "首页", displayIcon = Icons.Default.Home),
    //UGC(displayName = "UGC", displayIcon = Icons.Default.OndemandVideo),
    //PGC(displayName = "PGC", displayIcon = Icons.Default.Movie),
    Settings(displayName = "设置", displayIcon = Icons.Default.Settings), ;
}

@Preview(device = "id:tv_1080p")
@Composable
private fun DrawerContentPreview() {
    BVTheme {
        DrawerContent()
    }
}
