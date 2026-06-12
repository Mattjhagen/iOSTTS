// swift-tools-version: 6.0
// Metro Reader — iOS Native Application
// Swift 6, SwiftUI, SwiftData, AVFoundation, NaturalLanguage, PDFKit

import PackageDescription

let package = Package(
    name: "MetroReader",
    platforms: [
        .iOS(.v18),
        .iPadOS(.v18),
    ],
    products: [
        .library(name: "MetroReader", targets: ["MetroReader"]),
    ],
    dependencies: [
        // EPUB parsing: FolioReaderKit alternative — pure Swift EPUB parser
        .package(
            url: "https://github.com/readium/swift-toolkit.git",
            from: "3.0.0"
        ),
        // Zip/archive support for EPUB (which is a ZIP)
        .package(
            url: "https://github.com/weichsel/ZIPFoundation.git",
            from: "0.9.19"
        ),
    ],
    targets: [
        .target(
            name: "MetroReader",
            dependencies: [
                .product(name: "ReadiumShared", package: "swift-toolkit"),
                .product(name: "ReadiumStreamer", package: "swift-toolkit"),
                .product(name: "ReadiumNavigator", package: "swift-toolkit"),
                .product(name: "ZIPFoundation", package: "ZIPFoundation"),
            ],
            path: "MetroReader",
            swiftSettings: [
                .enableExperimentalFeature("StrictConcurrency"),
            ]
        ),
        .testTarget(
            name: "MetroReaderTests",
            dependencies: ["MetroReader"],
            path: "MetroReaderTests"
        ),
    ]
)
