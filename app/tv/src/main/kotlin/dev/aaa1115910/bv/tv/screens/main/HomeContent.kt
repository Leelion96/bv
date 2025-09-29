package dev.aaa1115910.bv.tv.screens.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import dev.aaa1115910.bv.component.HomeTopNavItem
import dev.aaa1115910.bv.component.TopNav
import dev.aaa1115910.bv.tv.screens.main.home.UserScreen
import dev.aaa1115910.bv.tv.screens.main.home.DynamicsScreen
import dev.aaa1115910.bv.tv.screens.main.home.PopularScreen
import dev.aaa1115910.bv.tv.screens.main.home.RecommendScreen
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.requestFocus
import dev.aaa1115910.bv.viewmodel.UserViewModel
import dev.aaa1115910.bv.viewmodel.home.DynamicViewModel
import dev.aaa1115910.bv.viewmodel.home.PopularViewModel
import dev.aaa1115910.bv.viewmodel.home.RecommendViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeContent(
    modifier: Modifier = Modifier,
    contentFocusRequester: FocusRequester,
    navFocusRequester: FocusRequester,
    recommendViewModel: RecommendViewModel = koinViewModel(),
    popularViewModel: PopularViewModel = koinViewModel(),
    dynamicViewModel: DynamicViewModel = koinViewModel(),
    userViewModel: UserViewModel = koinViewModel()
) {
    val scope = rememberCoroutineScope()
    val logger = KotlinLogging.logger("HomeContent")

    val recommendState = rememberLazyListState()
    val popularState = rememberLazyListState()
    val dynamicState = rememberLazyListState()
    val userState = rememberLazyListState()

    var focusOnContent by remember { mutableStateOf(false) }
    var topNavHasFocus by remember { mutableStateOf(false) }

    // 已登录的话，优先选择动态Tab
    val initialSelectedTab = if (Prefs.isLogin) {
        HomeTopNavItem.Dynamics
    } else {
        HomeTopNavItem.Dynamics
    }

    // 从全局状态获取上次选择的标签位置，如果没有则默认为Dynamics
    // 将这个值提到可组合函数的顶部，避免在重组时重新计算
    val initialSelectedTabIndex = currentSelectedTabs[DrawerItem.Home]
    var selectedTab by remember(initialSelectedTabIndex) {
        mutableStateOf(
            (initialSelectedTabIndex as? HomeTopNavItem)
                ?.let { HomeTopNavItem.entries.getOrNull(it.ordinal) }
                ?: initialSelectedTab
        )
    }

    // 当选中标签变化时，保存到全局状态
    LaunchedEffect(selectedTab) {
        currentSelectedTabs[DrawerItem.Home] = selectedTab
    }

    val currentListOnTop by remember {
        derivedStateOf {
            with(
                when (selectedTab) {
                    // HomeTopNavItem.Recommend -> recommendState
                    // HomeTopNavItem.Popular -> popularState
                    HomeTopNavItem.Dynamics -> dynamicState
                    HomeTopNavItem.User -> userState
                }
            ) {
                firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0
            }
        }
    }

    //启动时刷新数据
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            // recommendViewModel.loadMore()
        }
        scope.launch(Dispatchers.IO) {
            // popularViewModel.loadMore()
        }
        scope.launch(Dispatchers.IO) {
            dynamicViewModel.loadMoreVideo()
        }
        scope.launch(Dispatchers.IO) {
            userViewModel.updateUserInfo()
        }
    }

    //监听登录变化
    LaunchedEffect(userViewModel.isLogin) {
        if (userViewModel.isLogin) {
            //login
            userViewModel.updateUserInfo()
        } else {
            //logout
            userViewModel.clearUserInfo()
        }
    }

    BackHandler(focusOnContent || topNavHasFocus) {
        if (topNavHasFocus) {
            drawerItemFocusRequesters[DrawerItem.Home]?.requestFocus(scope)
            return@BackHandler
        }
        navFocusRequester.requestFocus(scope)
        // scroll to top
        scope.launch(Dispatchers.Main) {
            when (selectedTab) {
                // HomeTopNavItem.Recommend -> recommendState.animateScrollToItem(0)
                // HomeTopNavItem.Popular -> popularState.animateScrollToItem(0)
                HomeTopNavItem.Dynamics -> dynamicState.animateScrollToItem(0)
                HomeTopNavItem.User -> {} // 用户页面不需要滚动到顶部
            }
        }
    }

    // 使用自定义的标签顺序或默认顺序
    // 创建一个可观察的 homeTabOrder 状态
    val homeTabOrder by remember { mutableStateOf(Prefs.homeTabOrder) }

    // 解析标签顺序，每次 homeTabOrder 变化时重新计算
    val items = remember(homeTabOrder) {
        val savedOrder = Prefs.homeTabOrder
        val tabList = mutableListOf<HomeTopNavItem>()

        if (savedOrder.isNotEmpty()) {
            // 解析保存的顺序
            savedOrder.split(",").forEach { ordinal ->
                try {
                    val index = ordinal.toInt()
                    HomeTopNavItem.entries.getOrNull(index)?.let {
                        tabList.add(it)
                    }
                } catch (e: Exception) {
                    // 忽略无效条目
                }
            }

            // 添加任何缺失的标签（以防在更新中添加了新标签）
            HomeTopNavItem.entries.forEach { tab ->
                if (!tabList.contains(tab)) {
                    tabList.add(tab)
                }
            }
            tabList
        } else {
            // 使用默认顺序
            if (Prefs.isLogin) {
                listOf(HomeTopNavItem.Dynamics,  HomeTopNavItem.User)
            } else {
                listOf( HomeTopNavItem.Dynamics, HomeTopNavItem.User)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopNav(
                modifier = Modifier
                    .focusRequester(navFocusRequester)
                    .padding(end = 60.dp)
                    .onFocusChanged { topNavHasFocus = it.hasFocus },
                items = items,
                isLargePadding = !focusOnContent && currentListOnTop,
                initialSelectedItem = selectedTab,
                onSelectedChanged = { nav ->
                    selectedTab = nav as HomeTopNavItem
                    when (nav) {
                        // HomeTopNavItem.Recommend -> {}
                        // HomeTopNavItem.Popular -> {}
                        HomeTopNavItem.Dynamics -> {
                            if (!dynamicViewModel.loadingVideo && dynamicViewModel.isLogin && dynamicViewModel.dynamicVideoList.isEmpty()) {
                                scope.launch(Dispatchers.IO) { dynamicViewModel.loadMoreVideo() }
                            }
                        }
                        HomeTopNavItem.User -> {} // 用户页面不需要特殊处理
                    }
                },
                onClick = { nav ->
                    when (nav) {
                        // HomeTopNavItem.Recommend -> {}

                        // HomeTopNavItem.Popular -> {}

                        HomeTopNavItem.Dynamics -> {
                            logger.fInfo { "clear dynamic data" }
                            dynamicViewModel.clearVideoData()
                            logger.fInfo { "reload dynamic data" }
                            scope.launch(Dispatchers.IO) { dynamicViewModel.loadMoreVideo() }
                        }

                        HomeTopNavItem.User -> {
                            // 用户页面不需要刷新数据
                        }
                    }
                },
                onLeftKeyEvent = {
                    // 顶部栏最左侧按左键时，跳转到左侧导航栏
                    drawerItemFocusRequesters[DrawerItem.Home]?.requestFocus(scope)
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .focusRequester(contentFocusRequester)
                .onFocusChanged { focusOnContent = it.hasFocus }
        ) {
            AnimatedContent(
                targetState = selectedTab,
                label = "home animated content",
                transitionSpec = {
                    val coefficient = 10
                    if (targetState.ordinal < initialState.ordinal) {
                        fadeIn() + slideInHorizontally { -it / coefficient } togetherWith
                                fadeOut() + slideOutHorizontally { it / coefficient }
                    } else {
                        fadeIn() + slideInHorizontally { it / coefficient } togetherWith
                                fadeOut() + slideOutHorizontally { -it / coefficient }
                    }
                }
            ) { screen ->
                when (screen) {
                    //HomeTopNavItem.Recommend -> RecommendScreen(lazyListState = recommendState)
                    //HomeTopNavItem.Popular -> PopularScreen(lazyListState = popularState)
                    HomeTopNavItem.Dynamics -> DynamicsScreen(lazyListState = dynamicState)
                    HomeTopNavItem.User -> UserScreen(
                        contentFocusRequester = contentFocusRequester,
                        topNavFocusRequester = navFocusRequester
                    )
                }
            }
        }
    }
}
