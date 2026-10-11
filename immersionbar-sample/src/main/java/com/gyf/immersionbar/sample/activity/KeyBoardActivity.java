package com.gyf.immersionbar.sample.activity;

import android.content.Context;
import android.os.Build;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.appcompat.app.ActionBar;
import androidx.core.content.ContextCompat;

import com.gyf.immersionbar.BarProperties;
import com.gyf.immersionbar.ImmersionBar;
import com.gyf.immersionbar.Keyboard;
import com.gyf.immersionbar.sample.R;
import com.gyf.immersionbar.sample.databinding.ActivityKeyBoardBinding;

import java.util.Locale;

/**
 * Soft keyboard and bottom input box verification page.
 *
 * @author geyifeng
 * @date 2017/5/8
 */
public class KeyBoardActivity extends BaseActivity {

    private ActivityKeyBoardBinding binding;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_key_board;
    }

    @Override
    protected void initImmersionBar() {
        ImmersionBar.with(this)
                .navigationBarColor(R.color.colorPrimary)
                .titleBar(binding.toolbar)
                .keyboardEnable(true)
                .addOnKeyboardChangedListener(this::renderKeyboardState)
                .addOnBarPropertiesChangedListener(this::renderEnvironment)
                .init();
    }

    @Override
    protected void initView() {
        setSupportActionBar(binding.toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        renderKeyboardState(false, 0);
        binding.tvEnvironment.setText(buildEnvironmentText(null));
    }

    @Override
    protected void setListener() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.switchKeyboardEnable.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ImmersionBar.with(this).keyboardEnable(isChecked).init();
            binding.tvKeyboardMode.setText(isChecked
                    ? getString(R.string.text_keyboard_enable_true)
                    : getString(R.string.text_keyboard_enable_false));
            if (!isChecked) {
                hideKeyboard();
            }
        });
        binding.btnShowKeyboard.setOnClickListener(v -> showKeyboard(binding.editBottom));
        binding.btnHideKeyboard.setOnClickListener(v -> hideKeyboard());
    }

    @Override
    protected void initViewBinding() {
        binding = ActivityKeyBoardBinding.bind(getContentView());
    }

    private void renderKeyboardState(Keyboard keyboard) {
        renderKeyboardState(keyboard.isPopup(), keyboard.getHeight());
    }

    private void renderKeyboardState(boolean isPopup, int keyboardHeight) {
        float density = getResources().getDisplayMetrics().density;
        String state = isPopup ? getString(R.string.text_status_opened) : getString(R.string.text_status_closed);
        binding.tvKeyboardState.setText(getString(R.string.text_keyboard_status_format,
                state, keyboardHeight, keyboardHeight / density));
        binding.tvKeyboardState.setTextColor(ContextCompat.getColor(this,
                isPopup ? R.color.green : R.color.black));
    }

    private void renderEnvironment(BarProperties properties) {
        binding.tvEnvironment.setText(buildEnvironmentText(properties));
    }

    private String buildEnvironmentText(BarProperties properties) {
        String navigationMode = getString(R.string.text_waiting_system_bar_callback);
        int navigationBarHeight = 0;
        if (properties != null) {
            navigationMode = properties.isGestureNavigation()
                    ? getString(R.string.text_gesture_navigation, properties.getNavigationBarType())
                    : getString(R.string.text_classic_navigation, properties.getNavigationBarType());
            navigationBarHeight = properties.getNavigationBarHeight();
        }
        String implementation = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                ? "WindowInsets.Type.ime()"
                : "getWindowVisibleDisplayFrame()";
        return getString(R.string.text_keyboard_sys_info,
                Build.VERSION.RELEASE, Build.VERSION.SDK_INT,
                getApplicationInfo().targetSdkVersion, navigationMode,
                navigationBarHeight, implementation);
    }

    private void showKeyboard(EditText editText) {
        editText.requestFocus();
        editText.postDelayed(() -> {
            InputMethodManager manager =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (manager != null) {
                manager.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 100);
    }

    private void hideKeyboard() {
        InputMethodManager manager =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (manager != null) {
            manager.hideSoftInputFromWindow(binding.getRoot().getWindowToken(), 0);
        }
        binding.line.requestFocus();
    }
}
