import SwiftUI

@main
struct KMPPetApp: App {
    var body: some Scene {
        WindowGroup {
            TabView {
                ContentView()
                    .tabItem { Label("SwiftUI", systemImage: "swift") }
                ComposeTab()
                    .ignoresSafeArea(.keyboard)
                    .tabItem { Label("Compose", systemImage: "square.stack.3d.up") }
            }
        }
    }
}
