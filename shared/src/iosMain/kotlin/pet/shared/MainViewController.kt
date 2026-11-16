package pet.shared

import androidx.compose.ui.window.ComposeUIViewController
import pet.shared.di.AppContainer
import pet.shared.presentation.compose.ComposeApp
import platform.UIKit.UIViewController

/** Wraps the Compose screen in a UIViewController for SwiftUI. */
fun composeViewController(container: AppContainer): UIViewController =
    ComposeUIViewController { ComposeApp(container) }
