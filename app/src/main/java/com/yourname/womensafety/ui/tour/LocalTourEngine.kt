package com.yourname.womensafety.ui.tour

import androidx.compose.runtime.compositionLocalOf

/**
 * CompositionLocal that provides the [TourEngine] to any descendant composable.
 *
 * Provided at the root [AppNavGraph] level so that [HelpSupportScreen] and any
 * other screen can access the engine to trigger a tour replay without needing
 * to thread it through every composable in the tree.
 */
val LocalTourEngine = compositionLocalOf<TourEngine?> { null }
