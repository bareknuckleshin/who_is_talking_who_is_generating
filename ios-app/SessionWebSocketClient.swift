import Foundation

@MainActor
final class SessionWebSocketClient: ObservableObject {
    @Published var statusText: String = "연결 안 됨"
    @Published var sessionTopic: String = ""
    @Published var sessionStatus: String = "LOBBY"
    @Published var currentSpeakerSeat: String?
    @Published var maxChars: Int = 160
    @Published var participants: [String] = []
    @Published var messages: [ChatMessage] = []
    @Published var isTyping: Bool = false
    @Published var humanTurnActive: Bool = false
    @Published var judgeResult: JudgeResult?
    @Published var errorText: String?

    private let clientID: String = UUID().uuidString
    private var lastSeenMessageID: String?
    private var socketTask: URLSessionWebSocketTask?
    private var session: URLSession?
    private var reconnectAttempt: Int = 0
    private var closedByUser = false
    private var lastWSURL: URL?

    func connect(wsURL: URL) {
        disconnect()
        closedByUser = false
        statusText = "연결 중..."
        lastWSURL = wsURL

        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 20
        let urlSession = URLSession(configuration: config)
        session = urlSession

        let task = urlSession.webSocketTask(with: wsURL)
        socketTask = task
        task.resume()

        send(lastSeenMessageID == nil
             ? .join(clientID: clientID)
             : .resume(clientID: clientID, lastSeenMessageID: lastSeenMessageID))
        statusText = "연결됨"
        reconnectAttempt = 0
        listen()
    }

    func disconnect() {
        closedByUser = true
        socketTask?.cancel(with: .normalClosure, reason: nil)
        socketTask = nil
        session?.invalidateAndCancel()
        session = nil
        statusText = "연결 안 됨"
    }

    func sendHumanMessage(_ text: String) {
        guard !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }
        send(.humanMessage(clientID: clientID, text: text))
        humanTurnActive = false
    }

    private func send(_ event: OutboundEvent) {
        socketTask?.send(.string(event.jsonString)) { [weak self] error in
            if let error {
                Task { @MainActor in
                    self?.errorText = error.localizedDescription
                }
            }
        }
    }

    private func listen() {
        socketTask?.receive { [weak self] result in
            guard let self else { return }
            switch result {
            case let .failure(error):
                Task { @MainActor in
                    self.errorText = error.localizedDescription
                    self.statusText = "연결 끊김"
                    self.handleReconnectIfNeeded()
                }
            case let .success(message):
                Task { @MainActor in
                    switch message {
                    case let .string(text):
                        self.handleInboundText(text)
                    case let .data(data):
                        if let text = String(data: data, encoding: .utf8) {
                            self.handleInboundText(text)
                        }
                    @unknown default:
                        break
                    }
                    self.listen()
                }
            }
        }
    }

    private func handleReconnectIfNeeded() {
        guard !closedByUser, let socketURL = lastWSURL else { return }
        let delays: [UInt64] = [1, 2, 5]
        let index = min(reconnectAttempt, delays.count - 1)
        let delay = delays[index]
        reconnectAttempt += 1
        statusText = "재연결 \(delay)초 대기"

        Task {
            try? await Task.sleep(nanoseconds: delay * 1_000_000_000)
            if !self.closedByUser {
                self.connect(wsURL: socketURL)
            }
        }
    }

    private func handleInboundText(_ text: String) {
        guard let data = text.data(using: .utf8),
              let root = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any],
              let type = root["type"] as? String
        else {
            return
        }

        switch type {
        case "session.state":
            if let decoded = try? JSONDecoder().decode(SessionStateEvent.self, from: data) {
                sessionTopic = decoded.topic
                sessionStatus = decoded.status
                currentSpeakerSeat = decoded.current_speaker_seat
                participants = decoded.participants.map(\.seat)
                maxChars = decoded.max_chars
            }
        case "turn.request_human":
            humanTurnActive = true
        case "message.typing":
            isTyping = true
        case "message.new":
            if let decoded = try? JSONDecoder().decode(MessageNewEvent.self, from: data) {
                let exists = messages.contains { $0.id == decoded.message_id }
                if !exists {
                    messages.append(
                        ChatMessage(
                            id: decoded.message_id,
                            turnIndex: decoded.turn_index,
                            seat: decoded.seat,
                            text: decoded.text
                        )
                    )
                }
                lastSeenMessageID = decoded.message_id
                isTyping = false
            }
        case "turn.next":
            humanTurnActive = false
        case "session.finished":
            if let decoded = try? JSONDecoder().decode(SessionFinishedEvent.self, from: data) {
                judgeResult = JudgeResult(seat: decoded.pick_seat, confidence: decoded.confidence, reason: decoded.why)
                sessionStatus = "FINISHED"
            }
        default:
            break
        }
    }
}
