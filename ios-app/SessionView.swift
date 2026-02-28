import SwiftUI

struct SessionView: View {
    let sessionID: String
    let wsPath: String

    @StateObject private var wsClient = SessionWebSocketClient()
    @State private var messageInput = ""

    var body: some View {
        VStack(spacing: 0) {
            header
            Divider()
            messageList
            Divider()
            inputBar
        }
        .navigationTitle("Session \(sessionID.prefix(6))")
        .navigationBarTitleDisplayMode(.inline)
        .alert("판정 결과", isPresented: .constant(wsClient.judgeResult != nil), presenting: wsClient.judgeResult) { _ in
            Button("확인") { wsClient.judgeResult = nil }
        } message: { result in
            Text("선택 좌석: \(result.seat)\n신뢰도: \(Int(result.confidence * 100))%\n사유: \(result.reason)")
        }
        .onAppear {
            guard let wsURL = AppConfig.websocketURL(from: wsPath) else {
                wsClient.errorText = "WebSocket URL 생성 실패"
                return
            }
            wsClient.connect(wsURL: wsURL)
        }
        .onDisappear {
            wsClient.disconnect()
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(wsClient.sessionTopic.isEmpty ? "대기 중" : wsClient.sessionTopic)
                .font(.headline)
            Text("상태: \(wsClient.sessionStatus) | 연결: \(wsClient.statusText)")
                .font(.caption)
                .foregroundStyle(.secondary)
            if let seat = wsClient.currentSpeakerSeat {
                Text("현재 화자: \(seat)")
                    .font(.caption)
            }
            if wsClient.isTyping {
                Text("누군가 입력 중...")
                    .font(.caption)
                    .foregroundStyle(.blue)
            }
            if wsClient.archivedMessageCount > 0 {
                Text("아카이브된 메시지: \(wsClient.archivedMessageCount)개")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
            if let error = wsClient.errorText {
                Text(error)
                    .font(.caption)
                    .foregroundStyle(.red)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(Color(.secondarySystemBackground))
    }

    private var messageList: some View {
        List(wsClient.messages) { msg in
            VStack(alignment: .leading, spacing: 4) {
                Text("\(msg.turnIndex + 1)턴 · 좌석 \(msg.seat)")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Text(msg.text)
                    .font(.body)
            }
            .padding(.vertical, 4)
        }
        .listStyle(.plain)
    }

    private var inputBar: some View {
        VStack(spacing: 8) {
            HStack {
                TextField("메시지 입력", text: $messageInput, axis: .vertical)
                    .textFieldStyle(.roundedBorder)
                Button("전송") {
                    wsClient.sendHumanMessage(messageInput)
                    messageInput = ""
                }
                .disabled(!wsClient.humanTurnActive || messageInput.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }
            Text(wsClient.humanTurnActive ? "당신 차례입니다." : "다른 화자를 기다리는 중...")
                .font(.caption)
                .foregroundStyle(wsClient.humanTurnActive ? .blue : .secondary)
        }
        .padding(12)
    }
}
