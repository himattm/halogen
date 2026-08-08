import Foundation
import HalogenSample
#if canImport(FoundationModels)
import FoundationModels
#endif

/// Swift implementation of the Kotlin `AppleFoundationBridge` protocol.
///
/// `FoundationModels` is a Swift-only framework (iOS 26+), so this bridge
/// has to live in the iOS app rather than in the KMP module. The Kotlin
/// `AppleFoundationProvider` adapts the callback methods below into the
/// suspend-based `HalogenLlmProvider` contract.
@objc public final class AppleFoundationBridgeImpl: NSObject, AppleFoundationBridge {

    /// Returns a bridge only when the on-device model is currently
    /// `.available`. Callers should treat a `nil` result as "unsupported"
    /// and fall back to a different provider.
    @objc public static func makeIfAvailable() -> AppleFoundationBridgeImpl? {
        if #available(iOS 26.0, *) {
            #if canImport(FoundationModels)
            switch SystemLanguageModel.default.availability {
            case .available:
                return AppleFoundationBridgeImpl()
            default:
                return nil
            }
            #else
            return nil
            #endif
        }
        return nil
    }

    public func generate(
        prompt: String,
        completion: @escaping (String?, NSError?) -> Void
    ) {
        if #available(iOS 26.0, *) {
            #if canImport(FoundationModels)
            Task {
                do {
                    let session = LanguageModelSession()
                    let response = try await session.respond(to: prompt)
                    completion(response.content, nil)
                } catch {
                    completion(nil, error as NSError)
                }
            }
            return
            #endif
        }
        completion(
            nil,
            NSError(
                domain: "halogen.apple.foundation",
                code: -1,
                userInfo: [NSLocalizedDescriptionKey: "FoundationModels requires iOS 26+"]
            )
        )
    }

    public func availability(completion: @escaping (String, String?) -> Void) {
        if #available(iOS 26.0, *) {
            #if canImport(FoundationModels)
            switch SystemLanguageModel.default.availability {
            case .available:
                completion("available", nil)
                return
            case .unavailable(let reason):
                switch reason {
                case .modelNotReady:
                    completion("initializing", "modelNotReady")
                case .deviceNotEligible:
                    completion("unavailable", "deviceNotEligible")
                case .appleIntelligenceNotEnabled:
                    completion("unavailable", "appleIntelligenceNotEnabled")
                @unknown default:
                    completion("unavailable", "unknown")
                }
                return
            @unknown default:
                completion("unavailable", "unknown")
                return
            }
            #endif
        }
        completion("unavailable", "ios<26")
    }
}
