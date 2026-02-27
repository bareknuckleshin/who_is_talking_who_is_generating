import SwiftUI

struct ContentView: View {
    var body: some View {
        NavigationStack {
            WebGameView(urlString: AppConfig.WEB_APP_URL)
                .navigationTitle("Who Is Talking")
                .navigationBarTitleDisplayMode(.inline)
        }
    }
}

#Preview {
    ContentView()
}
