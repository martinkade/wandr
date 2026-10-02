import SwiftUI
import shared

@main
struct WandrApp: App {
    init() {
        KoinKt.doInitKoin()
        WatchConnectivityReceiver.instance.activate()
    }

    var body: some Scene {
        WindowGroup {
            RootView()
        }
    }
}
