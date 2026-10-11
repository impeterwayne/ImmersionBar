package com.gyf.immersionbar.sample.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gyf.immersionbar.BarHide
import com.gyf.immersionbar.BarProperties
import com.gyf.immersionbar.ktx.barProperties
import com.gyf.immersionbar.ktx.immersionBar
import com.gyf.immersionbar.sample.AppManager
import com.gyf.immersionbar.sample.R
import com.gyf.immersionbar.sample.utils.ToastUtils

/**
 * Use with Compose
 */
class ComposeActivity : ComponentActivity() {

    private val mBarProperties by lazy { mutableStateOf(barProperties) }
    private var mIsHideStatusBar by mutableStateOf(false)
    private var mIsHideNavigationBar by mutableStateOf(false)
    private var mIsDarkFont by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppManager.getInstance().addActivity(this)
        immersionBar {
            transparentStatusBar()
            navigationBarColor(R.color.btn13)
            statusBarDarkFont(mIsDarkFont)
            addOnBarPropertiesChangedListener {
                mBarProperties.value = it
            }
            addOnStatusBarChangedListener {
                if (it.isFirstCallback.not()) {
                    ToastUtils.show(
                        this@ComposeActivity,
                        getString(
                            R.string.toast_status_bar_state,
                            if (it.isVisible) getString(R.string.text_state_shown) else getString(R.string.text_state_hidden)
                        )
                    )
                }
            }
            addOnNavigationBarChangedListener {
                if (it.isFirstCallback.not()) {
                    ToastUtils.show(
                        this@ComposeActivity,
                        getString(
                            R.string.toast_nav_bar_state,
                            if (it.isVisible) getString(R.string.text_state_shown) else getString(R.string.text_state_hidden)
                        )
                    )
                }
            }
        }
        setContent {
            MaterialTheme {
                Surface(color = Color(0xFFF5F7FA)) {
                    ComposeDemoScreen(
                        barProperties = mBarProperties.value,
                        isHideStatusBar = mIsHideStatusBar,
                        isHideNavigationBar = mIsHideNavigationBar,
                        isDarkFont = mIsDarkFont,
                        onToggleStatusBar = ::toggleStatusBar,
                        onToggleNavigationBar = ::toggleNavigationBar,
                        onToggleStatusFont = ::toggleStatusFont
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AppManager.getInstance().removeActivity(this)
    }

    private fun toggleStatusBar() {
        mIsHideStatusBar = !mIsHideStatusBar
        updateBarVisibility()
    }

    private fun toggleNavigationBar() {
        mIsHideNavigationBar = !mIsHideNavigationBar
        updateBarVisibility()
    }

    private fun updateBarVisibility() {
        immersionBar {
            hideBar(
                when {
                    mIsHideStatusBar && mIsHideNavigationBar -> BarHide.FLAG_HIDE_BAR
                    mIsHideStatusBar -> BarHide.FLAG_HIDE_STATUS_BAR
                    mIsHideNavigationBar -> BarHide.FLAG_HIDE_NAVIGATION_BAR
                    else -> BarHide.FLAG_SHOW_BAR
                }
            )
        }
    }

    private fun toggleStatusFont() {
        mIsDarkFont = !mIsDarkFont
        immersionBar {
            statusBarDarkFont(mIsDarkFont)
        }
    }

    @Composable
    private fun ComposeDemoScreen(
        barProperties: BarProperties,
        isHideStatusBar: Boolean,
        isHideNavigationBar: Boolean,
        isDarkFont: Boolean,
        onToggleStatusBar: () -> Unit,
        onToggleNavigationBar: () -> Unit,
        onToggleStatusFont: () -> Unit
    ) {
        val statusBarHeight = barProperties.statusBarHeightIgnoringVisibility
        val navigationBarHeight = barProperties.navigationBarHeight
        val statusBarHeightDp = with(LocalDensity.current) { statusBarHeight.toDp() }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(Color(0xFFF5F7FA))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorResource(id = R.color.colorPrimary))
                    .padding(top = statusBarHeightDp)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Text(
                    text = getString(R.string.text_compose),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = getString(R.string.text_compose_desc),
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 14.sp
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {
                InfoCard(
                    title = getString(R.string.text_compose_bar_info),
                    rows = listOf(
                        getString(R.string.text_compose_status_bar_height) to "$statusBarHeight px",
                        getString(R.string.text_compose_is_status_bar_visible) to "${barProperties.isStatusBarVisible}",
                        getString(R.string.text_compose_has_nav_bar) to "${barProperties.hasNavigationBar()}",
                        getString(R.string.text_compose_is_nav_bar_visible) to "${barProperties.isNavigationBarVisible}",
                        getString(R.string.text_compose_nav_bar_height) to "$navigationBarHeight px",
                        getString(R.string.text_compose_is_nav_at_bottom) to "${barProperties.isNavigationAtBottom}",
                        getString(R.string.text_compose_is_notch_screen) to "${barProperties.isNotchScreen}",
                        getString(R.string.text_compose_notch_height) to "${barProperties.notchHeight} px"
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                InfoCard(
                    title = getString(R.string.text_compose_action_demo),
                    rows = listOf(
                        getString(R.string.text_compose_status_bar) to if (isHideStatusBar) getString(R.string.text_compose_hidden) else getString(R.string.text_compose_shown),
                        getString(R.string.text_compose_nav_bar) to if (isHideNavigationBar) getString(R.string.text_compose_hidden) else getString(R.string.text_compose_shown),
                        getString(R.string.text_compose_status_bar_font) to if (isDarkFont) getString(R.string.text_compose_dark) else getString(R.string.text_compose_light)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onToggleStatusBar,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = colorResource(id = R.color.btn13),
                        contentColor = Color.White
                    )
                ) {
                    Text(text = if (isHideStatusBar) getString(R.string.text_compose_show_status_bar) else getString(R.string.text_compose_hide_status_bar))
                }

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onToggleNavigationBar,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = colorResource(id = R.color.colorPrimary),
                        contentColor = Color.White
                    )
                ) {
                    Text(text = if (isHideNavigationBar) getString(R.string.text_compose_show_nav_bar) else getString(R.string.text_compose_hide_nav_bar))
                }

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onToggleStatusFont,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = colorResource(id = R.color.btn3),
                        contentColor = Color.White
                    )
                ) {
                    Text(text = if (isDarkFont) getString(R.string.text_compose_switch_light_font) else getString(R.string.text_compose_switch_dark_font))
                }
            }
        }
    }

    @Composable
    private fun InfoCard(title: String, rows: List<Pair<String, String>>) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = title,
                    color = colorResource(id = R.color.colorPrimary),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                rows.forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = row.first,
                            color = Color(0xFF555555),
                            fontSize = 14.sp
                        )
                        Text(
                            text = row.second,
                            color = colorResource(id = R.color.btn3),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
