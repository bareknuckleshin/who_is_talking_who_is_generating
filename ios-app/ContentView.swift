import SwiftUI

struct ContentView: View {
    @State private var activeSession: CreateSessionResponse?
    private let apiClient = APIClient()

    var body: some View {
        NavigationStack {
            if let activeSession {
                SessionView(sessionID: activeSession.session_id, wsPath: activeSession.ws_url)
                    .toolbar {
                        ToolbarItem(placement: .topBarLeading) {
                            Button("새 게임") {
                                self.activeSession = nil
                            }
                        }
                    }
            } else {
                HomeView(apiClient: apiClient) { response in
                    activeSession = response
                }
            }
        }
    }
}

#Preview {
    ContentView()
}
