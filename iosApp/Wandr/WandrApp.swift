import SwiftUI
import shared

@main
struct WandrApp: App {
    @UIApplicationDelegateAdaptor(PushAppDelegate.self) private var appDelegate

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
