import SwiftUI

struct HomeView: View {
    @State private var topic = ""
    @State private var llmSpeakers = 1
    @State private var turnsPerSpeaker = 5
    @State private var isLoading = false
    @State private var errorText: String?

    let apiClient: APIClient
    let onSessionCreated: (CreateSessionResponse) -> Void

    var body: some View {
        Form {
            Section("게임 생성") {
                TextField("주제(Topic)", text: $topic)
                Stepper("LLM 화자 수: \(llmSpeakers)", value: $llmSpeakers, in: 1 ... 5)
                Stepper("화자당 턴 수: \(turnsPerSpeaker)", value: $turnsPerSpeaker, in: 1 ... 10)
                Text("max_chars: 160 (고정)")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            Section {
                Button(isLoading ? "생성 중..." : "세션 생성") {
                    Task { await createSession() }
                }
                .disabled(isLoading || topic.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }

            if let errorText {
                Section("오류") {
                    Text(errorText)
                        .foregroundStyle(.red)
                }
            }
        }
        .navigationTitle("Who Is Human")
    }

    private func createSession() async {
        let cleanedTopic = topic.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !cleanedTopic.isEmpty else {
            errorText = "Topic은 필수입니다."
            return
        }

        errorText = nil
        isLoading = true
        defer { isLoading = false }

        do {
            let response = try await apiClient.createSession(
                topic: cleanedTopic,
                llmSpeakers: llmSpeakers,
                turnsPerSpeaker: turnsPerSpeaker
            )
            onSessionCreated(response)
        } catch {
            errorText = error.localizedDescription
        }
    }
}
