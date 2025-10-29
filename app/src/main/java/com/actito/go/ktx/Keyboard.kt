//  Copyright 2021 Christian Schmitz
//  SPDX-License-Identifier: Apache-2.0

package com.actito.go.ktx

import android.app.Activity
import android.view.View
import android.view.View.OnApplyWindowInsetsListener
import android.view.ViewTreeObserver.OnGlobalFocusChangeListener
import android.view.Window
import android.view.WindowInsets
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodManager.HIDE_IMPLICIT_ONLY
import androidx.core.content.getSystemService
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type.ime
import androidx.core.view.WindowInsetsCompat.toWindowInsetsCompat

/**
 * Sets the [OnApplyWindowInsetsListener] of decor view to trigger
 * [onKeyboardVisibilityChanged] with the visibility state of the IME.
 *
 * Use [delegate] to invoke another [OnApplyWindowInsetsListener].
 *
 * @see WindowInsetsCompat.isVisible
 * @see WindowInsetsCompat.Type.ime
 */
public inline fun Activity.setOnKeyboardVisibilityChangeListener(
    delegate: OnApplyWindowInsetsListener,
    crossinline onKeyboardVisibilityChanged: (isVisible: Boolean) -> Unit,
): Unit = setOnKeyboardVisibilityChangeListener(
    delegate::onApplyWindowInsets,
    onKeyboardVisibilityChanged,
)

/**
 * Sets the [OnApplyWindowInsetsListener] of decor view to trigger
 * [onKeyboardVisibilityChanged] with the visibility state of the IME.
 *
 * Use [onApplyWindowInsets] to add another [OnApplyWindowInsetsListener].
 * It defaults to [View.onApplyWindowInsets].
 *
 * @see WindowInsetsCompat.isVisible
 * @see WindowInsetsCompat.Type.ime
 */
public inline fun Activity.setOnKeyboardVisibilityChangeListener(
    crossinline onApplyWindowInsets: (View, WindowInsets) -> WindowInsets =
        View::onApplyWindowInsets,
    crossinline onKeyboardVisibilityChanged: (isVisible: Boolean) -> Unit,
): Unit = window.decorView.setOnApplyWindowInsetsListener { view, insets ->
    val insetsCompat = toWindowInsetsCompat(insets, view)
    onKeyboardVisibilityChanged(insetsCompat.isVisible(ime()))
    onApplyWindowInsets(view, insets)
}

/**
 * Configures the [Activity.window][Activity.getWindow] to hide the soft input method
 * when the [predicate] returns `true` for the `newView`.
 *
 * The [imm] is resolved with [getSystemService] when not provided.
 *
 * Set [isHideImplicitOnly] to `true` to set the [HIDE_IMPLICIT_ONLY]
 * flag for [InputMethodManager.hideSoftInputFromWindow].
 *
 * @throws NullPointerException when [InputMethodManager] is not found.
 */
public inline fun Activity.hideKeyboardOnFocusChange(
    isHideImplicitOnly: Boolean = false,
    imm: InputMethodManager = getSystemService()!!,
    crossinline predicate: (newView: View) -> Boolean,
): OnGlobalFocusChangeListener = window.hideKeyboardOnFocusChange(
    imm,
    isHideImplicitOnly,
    predicate,
)

/**
 * Configures the [Window] to hide the soft input method
 * when the [predicate] returns `true` for the `newView`.
 *
 * Set [isHideImplicitOnly] to `true` to set the [HIDE_IMPLICIT_ONLY]
 * flag for [InputMethodManager.hideSoftInputFromWindow].
 */
public inline fun Window.hideKeyboardOnFocusChange(
    imm: InputMethodManager,
    isHideImplicitOnly: Boolean = false,
    crossinline predicate: (newView: View) -> Boolean,
): OnGlobalFocusChangeListener = with(attributes.token) {
    OnGlobalFocusChangeListener { oldView, newView ->
        if (newView?.let(predicate) != false) imm.hideSoftInputFromWindow(
            (oldView ?: newView)?.windowToken ?: this,
            if (isHideImplicitOnly) HIDE_IMPLICIT_ONLY else 0,
        )
    }
}.also(decorView.viewTreeObserver::addOnGlobalFocusChangeListener)
