import Foundation

enum AppConfig {
    /// Backend API base URL. For real iPhone devices, use your Mac/PC LAN IP instead of localhost.
    static let apiBaseURL = URL(string: "http://localhost:8000")!

    static func websocketURL(from path: String) -> URL? {
        guard var components = URLComponents(url: apiBaseURL, resolvingAgainstBaseURL: false) else {
            return nil
        }
        components.scheme = (components.scheme == "https") ? "wss" : "ws"
        components.path = path.hasPrefix("/") ? path : "/\(path)"
        components.query = nil
        components.fragment = nil
        return components.url
    }
}
