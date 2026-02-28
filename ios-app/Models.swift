import Foundation

typealias Seat = String

struct CreateSessionRequest: Encodable {
    let topic: String
    let num_llm_speakers: Int
    let turns_per_speaker: Int
    let max_chars: Int
    let language: String
    let difficulty: String
}

struct CreateSessionResponse: Decodable {
    let session_id: String
    let ws_url: String
}

struct SessionParticipant: Decodable {
    let seat: String
}

struct SessionStateEvent: Decodable {
    let type: String
    let topic: String
    let status: String
    let participants: [SessionParticipant]
    let turn_counts: [String: Int]
    let current_speaker_seat: String?
    let max_chars: Int
}

struct MessageNewEvent: Decodable {
    let type: String
    let message_id: String
    let turn_index: Int
    let seat: String
    let text: String
}

struct TurnRequestHumanEvent: Decodable {
    let type: String
    let timeout_secs: Int?
}

struct MessageTypingEvent: Decodable {
    let type: String
    let seat: String
}

struct SessionFinishedEvent: Decodable {
    let type: String
    let pick_seat: String
    let confidence: Double
    let why: String
}

struct ChatMessage: Identifiable {
    let id: String
    let turnIndex: Int
    let seat: String
    let text: String
}

struct JudgeResult {
    let seat: String
    let confidence: Double
    let reason: String
}

enum OutboundEvent {
    case join(clientID: String)
    case resume(clientID: String, lastSeenMessageID: String?)
    case humanMessage(clientID: String, text: String)

    var jsonString: String {
        switch self {
        case let .join(clientID):
            return "{\"type\":\"session.join\",\"client_id\":\"\(clientID)\"}"
        case let .resume(clientID, lastSeen):
            if let lastSeen {
                return "{\"type\":\"session.resume\",\"client_id\":\"\(clientID)\",\"last_seen_message_id\":\"\(lastSeen)\"}"
            }
            return "{\"type\":\"session.resume\",\"client_id\":\"\(clientID)\",\"last_seen_message_id\":null}"
        case let .humanMessage(clientID, text):
            let escaped = text
                .replacingOccurrences(of: "\\", with: "\\\\")
                .replacingOccurrences(of: "\"", with: "\\\"")
                .replacingOccurrences(of: "\n", with: "\\n")
            return "{\"type\":\"human.message\",\"client_id\":\"\(clientID)\",\"text\":\"\(escaped)\"}"
        }
    }
}
