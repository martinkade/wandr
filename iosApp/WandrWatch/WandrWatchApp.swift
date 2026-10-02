import SwiftUI

@main
struct WandrWatchApp: App {
    @State private var manager = WorkoutManager()

    init() {
        WatchConnectivityService.shared.start()
    }

    var body: some Scene {
        WindowGroup {
            RootView(manager: manager)
        }
    }
}
