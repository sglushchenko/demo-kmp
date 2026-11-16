import SwiftUI
import Shared

/// Hosts the Compose Multiplatform screen in SwiftUI.
struct ComposeTab: UIViewControllerRepresentable {
    let container: AppContainer

    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.composeViewController(container: container)
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
