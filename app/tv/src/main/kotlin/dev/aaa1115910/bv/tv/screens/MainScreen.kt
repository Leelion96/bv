package dev.aaa1115910.bv.tv.screens

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.component.HomeTopNavItem
import dev.aaa1115910.bv.component.UserPanel
import dev.aaa1115910.bv.tv.activities.user.*
import dev.aaa1115910.bv.tv.screens.main.*
import dev.aaa1115910.bv.tv.screens.search.SearchInputScreen
import dev.aaa1115910.bv.tv.screens.settings.SettingsScreen
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.fException
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.viewmodel.UserViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.androidx.compose.koinViewModel

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    userViewModel: UserViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val logger = KotlinLogging.logger("MainScreen")
    var showUserPanel by remember { mutableStateOf(false) }
    var lastPressBack: Long by remember { mutableLongStateOf(0L) }
    var selectedDrawerItem by remember { mutableStateOf(DrawerItem.Home) }

    val mainFocusRequester = remember { FocusRequester() }
    val mainNavFocusRequester = remember { FocusRequester() }
    val ugcFocusRequester = remember { FocusRequester() }
    val pgcFocusRequester = remember { FocusRequester() }
    val searchFocusRequester = remember { FocusRequester() }
    val settingFocusRequester = remember { FocusRequester() }

    val handleBack = {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastPressBack < 1500) {
            logger.fInfo { "Exiting bug video" }
            (context as Activity).finish()
        } else {
            lastPressBack = currentTime
            R.string.home_press_back_again_to_exit.toast(context)
        }
    }

    val onFocusToContent: () -> Unit = {
        when (selectedDrawerItem) {
            DrawerItem.Home -> {
                if (!Prefs.isLogin && currentSelectedTabs[DrawerItem.Home] == HomeTopNavItem.Dynamics) {
                    // 未登录情况下只能获取标题栏的焦点而不能是内容的
                    mainNavFocusRequester.requestFocus()
                } else {
                    // todo:如果没加载到内容应该页不要获取这里的焦点
                    mainFocusRequester.requestFocus()
                }
            }
            //DrawerItem.UGC -> ugcFocusRequester.requestFocus()
            //DrawerItem.PGC -> pgcFocusRequester.requestFocus()
            //DrawerItem.Search -> searchFocusRequester.requestFocus()
            DrawerItem.Settings -> settingFocusRequester.requestFocus()
            else -> {}
        }
    }

    LaunchedEffect(Unit) {
        runCatching {
            mainNavFocusRequester.requestFocus()
        }.onFailure {
            logger.fException(it) { "request default focus requester failed" }
        }
    }

    BackHandler {
        handleBack()
    }

    // 使用Row替代NavigationDrawer
    Row(modifier = modifier) {
        // 侧边导航栏
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .background(Color(0xFF1A1A1A))
                .padding(start = 8.dp)
        ) {
            // 使用Column加入普通DrawerContent
            DrawerContent(
                isLogin = userViewModel.isLogin,
                avatar = userViewModel.face,
                onDrawerItemChanged = {
                    selectedDrawerItem = it
                },
                onShowUserPanel = {
                    showUserPanel = true
                },
                onFocusToContent = onFocusToContent,
                onLogin = {
                    context.startActivity(Intent(context, LoginActivity::class.java))
                }
            )
        }

        // 占位的区域，用来让内容页向左返回时先被这个占位抢占到焦点，然后再分发焦点到对应左侧栏位置
        LeftPlaceHolder {
            drawerItemFocusRequesters[selectedDrawerItem]?.requestFocus()
        }

        // 内容区域
        Box(modifier = Modifier.weight(1f)) {
            Column {
                // 占位的区域，用来让内容页向左返回时先被这个占位抢占到焦点，然后再分发焦点到对应左侧栏位置
                TopPlaceHolder{
                    drawerItemFocusRequesters[selectedDrawerItem]?.requestFocus()
                }
                AnimatedContent(
                    targetState = selectedDrawerItem,
                    label = "main animated content",
                    transitionSpec = {
                        val coefficient = 20
                        if (targetState.ordinal < initialState.ordinal) {
                            fadeIn() + slideInVertically { -it / coefficient } togetherWith
                                fadeOut() + slideOutVertically { it / coefficient }
                        } else {
                            fadeIn() + slideInVertically { it / coefficient } togetherWith
                                fadeOut() + slideOutVertically { -it / coefficient }
                        }
                    }
                ) { screen ->
                    when (screen) {
                        DrawerItem.Home -> HomeContent(
                            contentFocusRequester = mainFocusRequester,
                            navFocusRequester = mainNavFocusRequester
                        )
                        //DrawerItem.UGC -> UgcContent(contentFocusRequester = ugcFocusRequester)
                        //DrawerItem.PGC -> PgcContent(contentFocusRequester = pgcFocusRequester)
                        //DrawerItem.Search -> SearchInputScreen(defaultFocusRequester = searchFocusRequester)
                        DrawerItem.Settings -> SettingsScreen(defaultFocusRequester = settingFocusRequester)
                        else -> {}
                    }
                }
            }
        }
    }

    UserContent(
        userViewModel = userViewModel,
        showUserPanelLambda = { showUserPanel },
        onHide = { showUserPanel = false }
    )
}

@Composable
private fun UserContent(
    modifier: Modifier = Modifier,
    userViewModel: UserViewModel,
    showUserPanelLambda:()-> Boolean,
    onHide: () -> Unit
){
    val context = LocalContext.current
    val showUserPanel = showUserPanelLambda()
    AnimatedVisibility(
        modifier = modifier,
        visible = showUserPanel,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
        ) {
            AnimatedVisibility(
                modifier = Modifier
                    .align(Alignment.Center),
                visible = showUserPanel,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut()
            ) {
                UserPanel(
                    modifier = Modifier
                        .padding(12.dp),
                    username = userViewModel.username,
                    face = userViewModel.face,
                    onHide = onHide,
                    onGoMy = {
                        context.startActivity(Intent(context, UserInfoActivity::class.java))
                    },
                    onGoHistory = {
                        context.startActivity(Intent(context, HistoryActivity::class.java))
                    },
                    onGoFavorite = {
                        context.startActivity(Intent(context, FavoriteActivity::class.java))
                    },
                    onGoFollowing = {
                        context.startActivity(
                            Intent(
                                context,
                                FollowingSeasonActivity::class.java
                            )
                        )
                    },
                    onGoLater = {
                        context.startActivity(Intent(context, ToViewActivity::class.java))
                    }
                )
            }
        }
    }
}

@Composable
private fun LeftPlaceHolder(modifier: Modifier = Modifier, onFocus: () -> Unit) {
    Box(
        modifier = modifier
            .width(13.dp)
            .fillMaxHeight()
            .onFocusChanged { focusState ->
                if (focusState.hasFocus) {
                    onFocus.invoke()
                }
            }
            .background(MaterialTheme.colorScheme.background)
            .focusTarget()
            .focusable()
    ) {
        Box(
            modifier = Modifier
                .width(8.dp)
                .fillMaxHeight()
                .background(Color(0xFF1A1A1A))
        )
    }
}

@Composable
private fun TopPlaceHolder(modifier: Modifier = Modifier, onFocus: () -> Unit) {
    Box(
        modifier = modifier
            .height(13.dp)
            .fillMaxWidth()
            .onFocusChanged { focusState ->
                if (focusState.hasFocus) {
                    onFocus.invoke()
                }
            }
            .background(MaterialTheme.colorScheme.background)
            .focusTarget()
            .focusable()
    )
}