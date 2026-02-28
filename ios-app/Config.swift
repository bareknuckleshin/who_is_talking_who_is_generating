import Foundation

enum AppConfig {
    /// Backend API base URL. For real iPhone devices, use your Mac/PC LAN IP instead of localhost.
    static let apiBaseURL = URL(string: "http://localhost:8000")!

    /// Production 배포에서는 반드시 false + HTTPS/WSS를 사용하세요.
    static let allowInsecureWebSocketInDebug = true

    static func websocketURL(from path: String) -> URL? {
        guard var components = URLComponents(url: apiBaseURL, resolvingAgainstBaseURL: false) else {
            return nil
        }

        let secureScheme = (components.scheme == "https") ? "wss" : "ws"
        if secureScheme == "ws", !allowInsecureWebSocketInDebug {
            return nil
        }

        components.scheme = secureScheme
        components.path = path.hasPrefix("/") ? path : "/\(path)"
        components.query = nil
        components.fragment = nil
        return components.url
    }
}
