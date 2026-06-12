// HomeView.swift
// Metro Reader — Home Screen

import SwiftUI
import SwiftData

struct HomeView: View {

    @Environment(\.modelContext) private var modelContext
    @EnvironmentObject private var appState: AppState
    @Query(sort: \Book.lastOpenedDate, order: .reverse) private var recentBooks: [Book]
    @Query private var allBooks: [Book]

    @State private var selectedBook: Book? = nil

    private var currentlyReading: [Book] {
        recentBooks.filter { $0.progressPercent > 0 && !$0.isFinished }.prefix(6).map { $0 }
    }

    private var recentlyAdded: [Book] {
        allBooks.sorted { $0.addedDate > $1.addedDate }.prefix(6).map { $0 }
    }

    private var totalBooksRead: Int { allBooks.filter { $0.isFinished }.count }
    private var totalBooks: Int { allBooks.count }
    private var totalPagesRead: Int { allBooks.reduce(0) { $0 + Int(Double($1.pageCount) * $1.progressPercent / 100) } }

    var body: some View {
        NavigationStack {
            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 0) {

                    // ── Hero Title ────────────────────────────────────────
                    VStack(alignment: .leading, spacing: 0) {
                        Text("metro")
                            .font(MetroType.display(64, weight: .ultraLight))
                            .tracking(-4)
                            .foregroundStyle(.primary)
                        Text("reader")
                            .font(MetroType.display(64, weight: .ultraLight))
                            .tracking(-4)
                            .foregroundStyle(appState.accentColor)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, 20)
                    .padding(.top, 12)
                    .padding(.bottom, 24)

                    // ── Stats Row ─────────────────────────────────────────
                    if totalBooks > 0 {
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 2) {
                                MetroStatTile(
                                    label: "books",
                                    value: "\(totalBooks)",
                                    color: appState.accentColor
                                )
                                .frame(width: 110, height: 90)

                                MetroStatTile(
                                    label: "finished",
                                    value: "\(totalBooksRead)",
                                    color: appState.accentColor.opacity(0.75)
                                )
                                .frame(width: 110, height: 90)

                                MetroStatTile(
                                    label: "pages read",
                                    value: "\(totalPagesRead)",
                                    color: appState.accentColor.opacity(0.55)
                                )
                                .frame(width: 130, height: 90)
                            }
                            .padding(.horizontal, 20)
                        }
                        .padding(.bottom, 24)
                    }

                    // ── Currently Reading ─────────────────────────────────
                    if !currentlyReading.isEmpty {
                        MetroSectionHeader(title: "currently reading")
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 12) {
                                ForEach(currentlyReading) { book in
                                    CurrentlyReadingCard(book: book, accentColor: appState.accentColor)
                                        .onTapGesture { selectedBook = book }
                                }
                            }
                            .padding(.horizontal, 20)
                            .padding(.vertical, 8)
                        }
                        .padding(.bottom, 16)
                    }

                    // ── Recently Added ────────────────────────────────────
                    if !recentlyAdded.isEmpty {
                        MetroSectionHeader(title: "recently added")
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 12) {
                                ForEach(recentlyAdded) { book in
                                    SmallBookTile(book: book, accentColor: appState.accentColor)
                                        .onTapGesture { selectedBook = book }
                                }
                            }
                            .padding(.horizontal, 20)
                            .padding(.vertical, 8)
                        }
                        .padding(.bottom, 24)
                    }

                    // ── Empty State ───────────────────────────────────────
                    if allBooks.isEmpty {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("your reading journey starts here")
                                .font(MetroType.headline(24, weight: .thin))
                                .foregroundStyle(.primary)
                                .padding(.horizontal, 20)
                            Text("Go to Library to import your first book.")
                                .font(MetroType.bodySans())
                                .foregroundStyle(.secondary)
                                .padding(.horizontal, 20)
                        }
                        .padding(.top, 16)
                    }

                    Spacer(minLength: 80)
                }
            }
            .navigationBarTitleDisplayMode(.inline)
            .navigationDestination(item: $selectedBook) { book in
                ReaderView(book: book)
            }
        }
    }
}

// MARK: - Currently Reading Card
struct CurrentlyReadingCard: View {
    let book: Book
    let accentColor: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            BookCoverView(
                coverData: book.coverData,
                title: book.title,
                accentColor: accentColor.opacity(0.8)
            )
            .frame(width: 140, height: 200)
            .overlay(alignment: .bottom) {
                MetroProgressBar(progress: book.progressPercent / 100, color: accentColor, height: 4)
            }

            VStack(alignment: .leading, spacing: 2) {
                Text(book.title)
                    .font(MetroType.label(13, weight: .medium))
                    .lineLimit(1)
                    .foregroundStyle(.primary)
                Text("\(Int(book.progressPercent))%")
                    .font(MetroType.caption())
                    .foregroundStyle(accentColor)
            }
            .padding(.horizontal, 4)
            .padding(.vertical, 6)
        }
        .frame(width: 140)
    }
}

// MARK: - Small Book Tile
struct SmallBookTile: View {
    let book: Book
    let accentColor: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            BookCoverView(
                coverData: book.coverData,
                title: book.title,
                accentColor: accentColor.opacity(0.7)
            )
            .frame(width: 90, height: 130)

            Text(book.title)
                .font(MetroType.caption())
                .lineLimit(2)
                .foregroundStyle(.primary)
                .frame(width: 90, alignment: .leading)
        }
        .frame(width: 90)
    }
}
