import Foundation

enum APIError: LocalizedError {
    case invalidResponse
    case server(String)

    var errorDescription: String? {
        switch self {
        case .invalidResponse:
            return "서버 응답을 해석할 수 없습니다."
        case let .server(message):
            return message
        }
    }
}

final class APIClient {
    private let session: URLSession

    init(session: URLSession = .shared) {
        self.session = session
    }

    func createSession(topic: String, llmSpeakers: Int, turnsPerSpeaker: Int) async throws -> CreateSessionResponse {
        let payload = CreateSessionRequest(
            topic: topic,
            num_llm_speakers: llmSpeakers,
            turns_per_speaker: turnsPerSpeaker,
            max_chars: 160,
            language: "ko",
            difficulty: "normal"
        )

        var request = URLRequest(url: AppConfig.apiBaseURL.appending(path: "sessions"))
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONEncoder().encode(payload)

        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse else {
            throw APIError.invalidResponse
        }

        if !(200 ..< 300).contains(http.statusCode) {
            let message = String(data: data, encoding: .utf8) ?? "HTTP \(http.statusCode)"
            throw APIError.server(message)
        }

        do {
            return try JSONDecoder().decode(CreateSessionResponse.self, from: data)
        } catch {
            throw APIError.invalidResponse
        }
    }
}
