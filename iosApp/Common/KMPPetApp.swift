import SwiftUI
import Shared

@main
struct KMPPetApp: App {
    private let container = AppContainer()

    var body: some Scene {
        WindowGroup {
            TabView {
                ContentView(container: container)
                    .tabItem { Label("SwiftUI", systemImage: "swift") }
                ComposeTab(container: container)
                    .ignoresSafeArea(.keyboard)
                    .tabItem { Label("Compose", systemImage: "square.stack.3d.up") }
            }
        }
    }
}
