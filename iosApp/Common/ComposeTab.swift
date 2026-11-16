import SwiftUI
import Shared

/// Hosts the Compose Multiplatform screen in SwiftUI.
struct ComposeTab: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.composeViewController()
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
