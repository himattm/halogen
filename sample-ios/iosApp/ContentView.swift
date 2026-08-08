import UIKit
import SwiftUI
import HalogenSample

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        let bridge = AppleFoundationBridgeImpl.makeIfAvailable()
        return MainViewControllerKt.MainViewController(appleFoundationBridge: bridge)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
    }
}
