import SwiftUI
import Shared

/// Hosts a Compose Multiplatform screen inside SwiftUI.
/// Kotlin gives us a UIViewController; SwiftUI needs a UIViewControllerRepresentable wrapper.
struct ComposeTab: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        // Top-level Kotlin function in MainViewController.kt becomes a static method of `MainViewControllerKt`.
        MainViewControllerKt.composeViewController()
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
