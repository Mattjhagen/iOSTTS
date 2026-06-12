// ContentView.swift
// Metro Reader

import SwiftUI

struct ContentView: View {
    var body: some View {
        RootNavigationView()
    }
}

#Preview {
    ContentView()
        .environmentObject(AppState())
}
