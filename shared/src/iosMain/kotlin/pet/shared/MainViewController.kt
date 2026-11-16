package pet.shared

import androidx.compose.ui.window.ComposeUIViewController
import pet.shared.presentation.compose.ComposeApp
import platform.UIKit.UIViewController

/** Entry point for Swift: wraps the Composable into a UIViewController. */
fun composeViewController(): UIViewController = ComposeUIViewController { ComposeApp() }
