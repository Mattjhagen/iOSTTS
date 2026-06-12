// swift-tools-version: 6.0
// Metro Reader — iOS Native Application
// Swift 6, SwiftUI, SwiftData, AVFoundation, NaturalLanguage, PDFKit

import PackageDescription

let package = Package(
    name: "MetroReader",
    platforms: [
        .iOS(.v18),
    ],
    products: [
        .library(name: "MetroReader", targets: ["MetroReader"]),
    ],
    dependencies: [],
    targets: [
        .target(
            name: "MetroReader",
            dependencies: [],
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
