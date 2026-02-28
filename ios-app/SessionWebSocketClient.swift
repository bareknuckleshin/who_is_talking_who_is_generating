import Foundation

private actor MessageArchiveStore {
    private let fileURL: URL

    init(filename: String = "chat_archive.jsonl") {
        let baseDirectory = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first
            ?? URL(filePath: NSTemporaryDirectory())
        fileURL = baseDirectory.appending(path: filename)
    }

    func append(_ messages: [ChatMessage]) {
        guard !messages.isEmpty else { return }

        do {
            let encoder = JSONEncoder()
            let lines = try messages.map { message -> String in
                let data = try encoder.encode(message)
                guard let jsonLine = String(data: data, encoding: .utf8) else {
                    throw CocoaError(.coderInvalidValue)
                }
                return jsonLine
            }

            let payload = (lines.joined(separator: "\n") + "\n").data(using: .utf8) ?? Data()
            if FileManager.default.fileExists(atPath: fileURL.path()) {
                let handle = try FileHandle(forWritingTo: fileURL)
                try handle.seekToEnd()
                try handle.write(contentsOf: payload)
                try handle.close()
            } else {
                try payload.write(to: fileURL, options: .atomic)
            }
        } catch {
            // 보조 캐시 기능이므로 실패해도 앱 동작은 유지
        }
    }
}

private final class WebSocketAsyncStreamClient {
    private let wsURL: URL
    private let session: URLSession
    private var socketTask: URLSessionWebSocketTask?

    init(wsURL: URL) {
        self.wsURL = wsURL
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 30
        session = URLSession(configuration: config)
    }

    func connect() {
        let task = session.webSocketTask(with: wsURL)
        socketTask = task
        task.resume()
    }

    func disconnect() {
        socketTask?.cancel(with: .normalClosure, reason: nil)
        socketTask = nil
        session.invalidateAndCancel()
    }

    func send(_ event: OutboundEvent) async throws {
        guard let socketTask else {
            throw URLError(.networkConnectionLost)
        }
        try await socketTask.send(.string(event.jsonString))
    }

    func streamMessages() -> AsyncThrowingStream<String, Error> {
        AsyncThrowingStream { continuation in
            guard let socketTask else {
                continuation.finish(throwing: URLError(.networkConnectionLost))
                return
            }

            func receiveNext() {
                socketTask.receive { result in
                    switch result {
                    case let .success(message):
                        switch message {
                        case let .string(text):
                            continuation.yield(text)
                        case let .data(data):
                            if let text = String(data: data, encoding: .utf8) {
                                continuation.yield(text)
                            }
                        @unknown default:
                            break
                        }
                        receiveNext()
                    case let .failure(error):
                        continuation.finish(throwing: error)
                    }
                }
            }

            receiveNext()
        }
    }
}

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
    @Published var archivedMessageCount: Int = 0

    private let clientID: String = UUID().uuidString
    private let archiveStore = MessageArchiveStore()
    private let maxInMemoryMessages: Int = 150
    private let archiveBatchSize: Int = 50

    private var lastSeenMessageID: String?
    private var lastSequenceID: Int?
    private var reconnectAttempt: Int = 0
    private var closedByUser = false
    private var lastWSURL: URL?

    private var socketClient: WebSocketAsyncStreamClient?
    private var receiveTask: Task<Void, Never>?

    func connect(wsURL: URL) {
        disconnect()
        closedByUser = false
        lastWSURL = wsURL
        statusText = "연결 중..."
        errorText = nil

        let socketClient = WebSocketAsyncStreamClient(wsURL: wsURL)
        socketClient.connect()
        self.socketClient = socketClient

        receiveTask = Task { [weak self] in
            guard let self else { return }
            await self.runReceiveLoop(using: socketClient)
        }
    }

    func disconnect() {
        closedByUser = true
        receiveTask?.cancel()
        receiveTask = nil

        socketClient?.disconnect()
        socketClient = nil
        statusText = "연결 안 됨"
    }

    func sendHumanMessage(_ text: String) {
        let cleaned = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !cleaned.isEmpty else { return }
        humanTurnActive = false

        Task { [weak self] in
            guard let self else { return }
            do {
                try await self.socketClient?.send(.humanMessage(clientID: self.clientID, text: cleaned))
            } catch {
                await self.setError(error.localizedDescription)
            }
        }
    }

    private func runReceiveLoop(using socketClient: WebSocketAsyncStreamClient) async {
        do {
            try await socketClient.send(lastSeenMessageID == nil
                                        ? .join(clientID: clientID)
                                        : .resume(clientID: clientID, lastSeenMessageID: lastSeenMessageID, lastSequenceID: lastSequenceID))
            await MainActor.run {
                statusText = "연결됨"
                reconnectAttempt = 0
            }

            for try await text in socketClient.streamMessages() {
                if Task.isCancelled { break }
                await processInboundText(text)
            }
        } catch {
            if Task.isCancelled { return }
            await setError(error.localizedDescription)
            await handleReconnectIfNeeded()
        }
    }

    private func setError(_ message: String) {
        errorText = message
        statusText = "연결 끊김"
    }

    private func handleReconnectIfNeeded() async {
        guard !closedByUser, let socketURL = lastWSURL else { return }

        let delaySeconds = min(30.0, pow(2.0, Double(reconnectAttempt)))
        reconnectAttempt += 1
        statusText = "재연결 대기: \(Int(delaySeconds))초"

        do {
            try await Task.sleep(nanoseconds: UInt64(delaySeconds * 1_000_000_000))
        } catch {
            return
        }

        if !closedByUser {
            connect(wsURL: socketURL)
        }
    }

    nonisolated private func decodeEventType(from text: String) -> String? {
        guard let data = text.data(using: .utf8),
              let root = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any],
              let type = root["type"] as? String
        else {
            return nil
        }
        return type
    }

    private func processInboundText(_ text: String) async {
        guard let data = text.data(using: .utf8), let type = decodeEventType(from: text) else {
            return
        }

        switch type {
        case "session.state":
            if let event = try? JSONDecoder().decode(SessionStateEvent.self, from: data) {
                sessionTopic = event.topic
                sessionStatus = event.status
                currentSpeakerSeat = event.current_speaker_seat
                participants = event.participants.map(\.seat)
                maxChars = event.max_chars
                if let sequenceID = event.sequence_id {
                    lastSequenceID = sequenceID
                }
            }

        case "turn.request_human":
            humanTurnActive = true

        case "message.typing":
            isTyping = true

        case "message.new":
            if let event = try? JSONDecoder().decode(MessageNewEvent.self, from: data) {
                upsertMessage(id: event.message_id, turnIndex: event.turn_index, seat: event.seat, appendText: event.text, replace: true)
                lastSeenMessageID = event.message_id
                if let sequenceID = event.sequence_id {
                    lastSequenceID = sequenceID
                }
                isTyping = false
                trimMessagesIfNeeded()
            }

        case "message.delta":
            if let event = try? JSONDecoder().decode(MessageDeltaEvent.self, from: data) {
                upsertMessage(id: event.message_id, turnIndex: event.turn_index, seat: event.seat, appendText: event.delta, replace: false)
                lastSeenMessageID = event.message_id
                if let sequenceID = event.sequence_id {
                    lastSequenceID = sequenceID
                }
                trimMessagesIfNeeded()
            }

        case "turn.next":
            humanTurnActive = false

        case "session.finished":
            if let event = try? JSONDecoder().decode(SessionFinishedEvent.self, from: data) {
                judgeResult = JudgeResult(seat: event.pick_seat, confidence: event.confidence, reason: event.why)
                sessionStatus = "FINISHED"
                if let sequenceID = event.sequence_id {
                    lastSequenceID = sequenceID
                }
            }

        default:
            break
        }
    }

    private func upsertMessage(id: String, turnIndex: Int, seat: String, appendText: String, replace: Bool) {
        if let index = messages.firstIndex(where: { $0.id == id }) {
            if replace {
                messages[index].text = appendText
            } else {
                messages[index].text += appendText
            }
        } else {
            messages.append(
                ChatMessage(
                    id: id,
                    turnIndex: turnIndex,
                    seat: seat,
                    text: appendText
                )
            )
        }
    }

    private func trimMessagesIfNeeded() {
        let overflow = messages.count - maxInMemoryMessages
        guard overflow > 0 else { return }

        let archiveCount = min(max(overflow, archiveBatchSize), messages.count)
        let archived = Array(messages.prefix(archiveCount))
        messages.removeFirst(archiveCount)
        archivedMessageCount += archiveCount

        Task {
            await archiveStore.append(archived)
        }
    }
}
