// BookParser.swift
// Metro Reader — Parser Protocol and Implementations

import Foundation
import PDFKit
import UIKit

// MARK: - BookParser Protocol
protocol BookParser: Sendable {
    func supports(format: BookFormat) -> Bool
    func parseMetadata(from url: URL, format: BookFormat) async throws -> BookMetadata
    func parseContent(from url: URL, bookId: UUID) async throws -> BookContent
}

// MARK: - EPUB Parser
final class EPUBParser: BookParser, @unchecked Sendable {

    func supports(format: BookFormat) -> Bool { format == .epub }

    func parseMetadata(from url: URL, format: BookFormat) async throws -> BookMetadata {
        return try await Task.detached(priority: .userInitiated) {
            guard let archive = try? ZipArchive(url: url) else {
                throw ImportError.parseFailure("Cannot open EPUB archive")
            }
            // Read container.xml to find OPF path
            guard let containerData = archive.extract("META-INF/container.xml"),
                  let containerStr = String(data: containerData, encoding: .utf8) else {
                throw ImportError.parseFailure("Missing container.xml")
            }

            let opfPath = Self.extractOPFPath(from: containerStr)
            guard let opfData = archive.extract(opfPath),
                  let opfStr = String(data: opfData, encoding: .utf8) else {
                throw ImportError.parseFailure("Cannot read OPF file")
            }

            var meta = BookMetadata(format: .epub)
            meta.title     = Self.extractOPFValue(opfStr, tag: "dc:title") ?? url.deletingPathExtension().lastPathComponent
            meta.author    = Self.extractOPFValue(opfStr, tag: "dc:creator") ?? ""
            meta.publisher = Self.extractOPFValue(opfStr, tag: "dc:publisher") ?? ""
            meta.language  = Self.extractOPFValue(opfStr, tag: "dc:language") ?? "en"
            meta.isbn      = Self.extractOPFValue(opfStr, tag: "dc:identifier") ?? ""
            meta.description = Self.extractOPFValue(opfStr, tag: "dc:description") ?? ""

            // Cover image
            meta.coverData = Self.extractCover(archive: archive, opfStr: opfStr, opfDir: URL(string: opfPath)?.deletingLastPathComponent().path ?? "")

            return meta
        }.value
    }

    func parseContent(from url: URL, bookId: UUID) async throws -> BookContent {
        return try await Task.detached(priority: .userInitiated) {
            guard let archive = try? ZipArchive(url: url) else {
                throw ImportError.parseFailure("Cannot open EPUB archive")
            }

            guard let containerData = archive.extract("META-INF/container.xml"),
                  let containerStr = String(data: containerData, encoding: .utf8) else {
                throw ImportError.parseFailure("Missing container.xml")
            }

            let opfPath = Self.extractOPFPath(from: containerStr)
            guard let opfData = archive.extract(opfPath),
                  let opfStr = String(data: opfData, encoding: .utf8) else {
                throw ImportError.parseFailure("Cannot read OPF")
            }

            let opfDir = String(opfPath.split(separator: "/").dropLast().joined(separator: "/"))
            let spineItems = Self.extractSpineItems(opfStr: opfStr, opfDir: opfDir)
            let tocEntries = Self.extractNCXToc(archive: archive, opfStr: opfStr, opfDir: opfDir)

            var chapters: [Chapter] = []
            for (index, item) in spineItems.enumerated() {
                guard let data = archive.extract(item.path),
                      let html = String(data: data, encoding: .utf8) ?? String(data: data, encoding: .isoLatin1) else { continue }
                let plain = Self.htmlToPlainText(html)
                let chapter = Chapter(
                    index: index,
                    title: item.title.isEmpty ? "Chapter \(index + 1)" : item.title,
                    content: plain,
                    htmlContent: html
                )
                chapters.append(chapter)
            }

            return BookContent(bookId: bookId, chapters: chapters, tableOfContents: tocEntries)
        }.value
    }

    // MARK: - Private Helpers

    private static func extractOPFPath(from containerXML: String) -> String {
        // Simple regex-free extraction
        if let range = containerXML.range(of: "full-path=\""),
           let end = containerXML[range.upperBound...].range(of: "\"") {
            return String(containerXML[range.upperBound..<end.lowerBound])
        }
        return "OEBPS/content.opf"
    }

    private static func extractOPFValue(_ opf: String, tag: String) -> String? {
        let open = "<\(tag)"
        let close = "</\(tag)>"
        guard let start = opf.range(of: open),
              let contentStart = opf[start.lowerBound...].range(of: ">"),
              let end = opf[contentStart.upperBound...].range(of: close) else { return nil }
        let value = String(opf[contentStart.upperBound..<end.lowerBound])
            .trimmingCharacters(in: .whitespacesAndNewlines)
        return value.isEmpty ? nil : value
    }

    private static func extractCover(archive: ZipArchive, opfStr: String, opfDir: String) -> Data? {
        // Look for cover image reference in OPF manifest
        let patterns = ["cover-image", "cover.jpg", "cover.jpeg", "cover.png"]
        for pattern in patterns {
            if let range = opfStr.range(of: pattern, options: .caseInsensitive) {
                // Find href in surrounding context
                let context = opfStr[max(opfStr.startIndex, opfStr.index(range.lowerBound, offsetBy: -200, limitedBy: opfStr.startIndex) ?? opfStr.startIndex)..<range.upperBound]
                if let hrefRange = context.range(of: "href=\""),
                   let hrefEnd = context[hrefRange.upperBound...].range(of: "\"") {
                    let href = String(context[hrefRange.upperBound..<hrefEnd.lowerBound])
                    let path = opfDir.isEmpty ? href : "\(opfDir)/\(href)"
                    if let data = archive.extract(path) { return data }
                }
            }
        }
        return nil
    }

    private static func extractSpineItems(opfStr: String, opfDir: String) -> [(path: String, title: String)] {
        var items: [(path: String, title: String)] = []
        // Build id->href map from manifest
        var manifest: [String: String] = [:]
        let manifestPattern = "item id=\""
        var searchStr = opfStr
        while let idRange = searchStr.range(of: manifestPattern) {
            let rest = searchStr[idRange.upperBound...]
            guard let idEnd = rest.range(of: "\""),
                  let hrefRange = rest.range(of: "href=\""),
                  let hrefEnd = rest[hrefRange.upperBound...].range(of: "\"") else { break }
            let itemId = String(rest[..<idEnd.lowerBound])
            let href = String(rest[hrefRange.upperBound..<hrefEnd.lowerBound])
            manifest[itemId] = opfDir.isEmpty ? href : "\(opfDir)/\(href)"
            searchStr = String(rest[idEnd.upperBound...])
        }

        // Extract spine order
        if let spineStart = opfStr.range(of: "<spine"),
           let spineEnd = opfStr.range(of: "</spine>") {
            let spineContent = String(opfStr[spineStart.upperBound..<spineEnd.lowerBound])
            var spineSearch = spineContent
            while let idrRange = spineSearch.range(of: "idref=\"") {
                let rest = spineSearch[idrRange.upperBound...]
                guard let idrEnd = rest.range(of: "\"") else { break }
                let idref = String(rest[..<idrEnd.lowerBound])
                if let path = manifest[idref] {
                    items.append((path: path, title: ""))
                }
                spineSearch = String(rest[idrEnd.upperBound...])
            }
        }
        return items
    }

    private static func extractNCXToc(archive: ZipArchive, opfStr: String, opfDir: String) -> [TocEntry] {
        // Find NCX file
        guard let ncxRange = opfStr.range(of: "media-type=\"application/x-dtbncx+xml\""),
              let hrefRange = opfStr[...ncxRange.lowerBound].range(of: "href=\"", options: .backwards),
              let hrefEnd = opfStr[hrefRange.upperBound...].range(of: "\"") else { return [] }
        let ncxHref = String(opfStr[hrefRange.upperBound..<hrefEnd.lowerBound])
        let ncxPath = opfDir.isEmpty ? ncxHref : "\(opfDir)/\(ncxHref)"
        guard let ncxData = archive.extract(ncxPath),
              let ncxStr = String(data: ncxData, encoding: .utf8) else { return [] }

        var entries: [TocEntry] = []
        var index = 0
        var search = ncxStr
        while let navStart = search.range(of: "<navPoint") {
            let rest = search[navStart.upperBound...]
            let labelText: String
            if let labelRange = rest.range(of: "<text>"),
               let labelEnd = rest[labelRange.upperBound...].range(of: "</text>") {
                labelText = String(rest[labelRange.upperBound..<labelEnd.lowerBound])
                    .trimmingCharacters(in: .whitespacesAndNewlines)
            } else {
                labelText = "Chapter \(index + 1)"
            }
            entries.append(TocEntry(title: labelText, chapterIndex: index, level: 0))
            index += 1
            guard let navEnd = rest.range(of: "</navPoint>") else { break }
            search = String(rest[navEnd.upperBound...])
        }
        return entries
    }

    static func htmlToPlainText(_ html: String) -> String {
        // Remove script/style blocks
        var text = html
        for tag in ["script", "style"] {
            while let start = text.range(of: "<\(tag)", options: .caseInsensitive),
                  let end = text.range(of: "</\(tag)>", options: .caseInsensitive) {
                text.removeSubrange(start.lowerBound...end.upperBound)
            }
        }
        // Replace block tags with newlines
        for tag in ["</p>", "</div>", "</br>", "<br>", "<br/>", "</h1>", "</h2>", "</h3>", "</h4>", "</li>"] {
            text = text.replacingOccurrences(of: tag, with: "\n", options: .caseInsensitive)
        }
        // Strip remaining tags
        var result = ""
        var inTag = false
        for char in text {
            if char == "<" { inTag = true }
            else if char == ">" { inTag = false }
            else if !inTag { result.append(char) }
        }
        // Decode common HTML entities
        result = result
            .replacingOccurrences(of: "&amp;", with: "&")
            .replacingOccurrences(of: "&lt;", with: "<")
            .replacingOccurrences(of: "&gt;", with: ">")
            .replacingOccurrences(of: "&quot;", with: "\"")
            .replacingOccurrences(of: "&#39;", with: "'")
            .replacingOccurrences(of: "&nbsp;", with: " ")
            .replacingOccurrences(of: "&mdash;", with: "—")
            .replacingOccurrences(of: "&ndash;", with: "–")
        // Collapse multiple blank lines
        while result.contains("\n\n\n") {
            result = result.replacingOccurrences(of: "\n\n\n", with: "\n\n")
        }
        return result.trimmingCharacters(in: .whitespacesAndNewlines)
    }
}

// MARK: - PDF Parser
final class PDFParser: BookParser, @unchecked Sendable {

    func supports(format: BookFormat) -> Bool { format == .pdf }

    func parseMetadata(from url: URL, format: BookFormat) async throws -> BookMetadata {
        return try await Task.detached(priority: .userInitiated) {
            guard let doc = PDFDocument(url: url) else {
                throw ImportError.parseFailure("Cannot open PDF")
            }
            var meta = BookMetadata(format: .pdf)
            meta.title     = doc.documentAttributes?[PDFDocumentAttribute.titleAttribute] as? String
                             ?? url.deletingPathExtension().lastPathComponent
            meta.author    = doc.documentAttributes?[PDFDocumentAttribute.authorAttribute] as? String ?? ""
            meta.publisher = doc.documentAttributes?[PDFDocumentAttribute.creatorAttribute] as? String ?? ""
            meta.pageCount = doc.pageCount

            // Generate cover thumbnail from first page
            if let page = doc.page(at: 0) {
                let bounds = page.bounds(for: .mediaBox)
                let scale: CGFloat = 200.0 / max(bounds.width, bounds.height)
                let thumbSize = CGSize(width: bounds.width * scale, height: bounds.height * scale)
                let renderer = UIGraphicsImageRenderer(size: thumbSize)
                let img = renderer.image { ctx in
                    UIColor.white.setFill()
                    ctx.fill(CGRect(origin: .zero, size: thumbSize))
                    ctx.cgContext.translateBy(x: 0, y: thumbSize.height)
                    ctx.cgContext.scaleBy(x: scale, y: -scale)
                    page.draw(with: .mediaBox, to: ctx.cgContext)
                }
                meta.coverData = img.jpegData(compressionQuality: 0.7)
            }
            return meta
        }.value
    }

    func parseContent(from url: URL, bookId: UUID) async throws -> BookContent {
        return try await Task.detached(priority: .userInitiated) {
            guard let doc = PDFDocument(url: url) else {
                throw ImportError.parseFailure("Cannot open PDF")
            }
            var chapters: [Chapter] = []
            // Treat every 20 pages as a "chapter"
            let pagesPerChapter = 20
            let totalPages = doc.pageCount
            var chapterIndex = 0

            var i = 0
            while i < totalPages {
                let end = min(i + pagesPerChapter, totalPages)
                var text = ""
                for p in i..<end {
                    if let page = doc.page(at: p) {
                        text += (page.string ?? "") + "\n\n"
                    }
                }
                let chapter = Chapter(
                    index: chapterIndex,
                    title: "Pages \(i + 1)–\(end)",
                    content: text.trimmingCharacters(in: .whitespacesAndNewlines)
                )
                chapters.append(chapter)
                chapterIndex += 1
                i += pagesPerChapter
            }

            // Build TOC from PDF outline
            var toc: [TocEntry] = []
            if let outline = doc.outlineRoot {
                toc = Self.extractOutline(outline, level: 0, chapterSize: pagesPerChapter)
            }

            return BookContent(bookId: bookId, chapters: chapters, tableOfContents: toc)
        }.value
    }

    private static func extractOutline(_ outline: PDFOutline, level: Int, chapterSize: Int) -> [TocEntry] {
        var entries: [TocEntry] = []
        for i in 0..<outline.numberOfChildren {
            guard let child = outline.child(at: i) else { continue }
            let title = child.label ?? "Section"
            let pageIndex = child.destination?.page?.document?.index(for: child.destination!.page!) ?? 0
            let chapterIndex = pageIndex / chapterSize
            let children = extractOutline(child, level: level + 1, chapterSize: chapterSize)
            entries.append(TocEntry(title: title, chapterIndex: chapterIndex, level: level, children: children))
        }
        return entries
    }
}

// MARK: - TXT Parser
final class TxtParser: BookParser, @unchecked Sendable {

    func supports(format: BookFormat) -> Bool { format == .txt }

    func parseMetadata(from url: URL, format: BookFormat) async throws -> BookMetadata {
        var meta = BookMetadata(format: .txt)
        meta.title = url.deletingPathExtension().lastPathComponent
        return meta
    }

    func parseContent(from url: URL, bookId: UUID) async throws -> BookContent {
        return try await Task.detached(priority: .userInitiated) {
            let text = try String(contentsOf: url, encoding: .utf8)
            // Split into chapters at blank-line-separated sections (~2000 words each)
            let paragraphs = text.components(separatedBy: "\n\n")
            let wordsPerChapter = 2000
            var chapters: [Chapter] = []
            var buffer = ""
            var wordCount = 0
            var chapterIndex = 0

            for para in paragraphs {
                buffer += para + "\n\n"
                wordCount += para.split(separator: " ").count
                if wordCount >= wordsPerChapter {
                    chapters.append(Chapter(
                        index: chapterIndex,
                        title: "Section \(chapterIndex + 1)",
                        content: buffer.trimmingCharacters(in: .whitespacesAndNewlines)
                    ))
                    buffer = ""
                    wordCount = 0
                    chapterIndex += 1
                }
            }
            if !buffer.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                chapters.append(Chapter(
                    index: chapterIndex,
                    title: "Section \(chapterIndex + 1)",
                    content: buffer.trimmingCharacters(in: .whitespacesAndNewlines)
                ))
            }
            return BookContent(bookId: bookId, chapters: chapters.isEmpty ? [Chapter(index: 0, title: "Full Text", content: text)] : chapters)
        }.value
    }
}

// MARK: - HTML Parser
final class HTMLParser: BookParser, @unchecked Sendable {

    func supports(format: BookFormat) -> Bool { format == .html }

    func parseMetadata(from url: URL, format: BookFormat) async throws -> BookMetadata {
        var meta = BookMetadata(format: .html)
        if let html = try? String(contentsOf: url, encoding: .utf8) {
            meta.title = EPUBParser.htmlToPlainText(extractTag(html, tag: "title"))
                .trimmingCharacters(in: .whitespacesAndNewlines)
        }
        if meta.title.isEmpty { meta.title = url.deletingPathExtension().lastPathComponent }
        return meta
    }

    func parseContent(from url: URL, bookId: UUID) async throws -> BookContent {
        return try await Task.detached(priority: .userInitiated) {
            let html = try String(contentsOf: url, encoding: .utf8)
            let plain = EPUBParser.htmlToPlainText(html)
            let chapter = Chapter(index: 0, title: "Full Document", content: plain, htmlContent: html)
            return BookContent(bookId: bookId, chapters: [chapter])
        }.value
    }

    private func extractTag(_ html: String, tag: String) -> String {
        guard let start = html.range(of: "<\(tag)"),
              let contentStart = html[start.upperBound...].range(of: ">"),
              let end = html[contentStart.upperBound...].range(of: "</\(tag)>") else { return "" }
        return String(html[contentStart.upperBound..<end.lowerBound])
    }
}

// MARK: - MOBI/AZW3 Parser (basic text extraction)
final class MOBIParser: BookParser, @unchecked Sendable {

    func supports(format: BookFormat) -> Bool { format == .mobi || format == .azw3 }

    func parseMetadata(from url: URL, format: BookFormat) async throws -> BookMetadata {
        return await Task.detached(priority: .userInitiated) {
            var meta = BookMetadata(format: format)
            // MOBI header parsing: read PalmDOC header for title
            guard let data = try? Data(contentsOf: url), data.count > 32 else {
                meta.title = url.deletingPathExtension().lastPathComponent
                return meta
            }
            // PalmDOC name is first 32 bytes
            let nameData = data[0..<32]
            if let name = String(data: nameData, encoding: .utf8)?.trimmingCharacters(in: .init(charactersIn: "\0")) {
                meta.title = name.isEmpty ? url.deletingPathExtension().lastPathComponent : name
            } else {
                meta.title = url.deletingPathExtension().lastPathComponent
            }
            return meta
        }.value
    }

    func parseContent(from url: URL, bookId: UUID) async throws -> BookContent {
        return try await Task.detached(priority: .userInitiated) {
            // Basic MOBI text extraction: read PalmDOC compressed records
            guard let data = try? Data(contentsOf: url) else {
                throw ImportError.parseFailure("Cannot read MOBI file")
            }
            // Extract readable ASCII/UTF-8 text as fallback
            let text = String(data: data, encoding: .utf8)
                ?? String(data: data, encoding: .isoLatin1)
                ?? "Unable to decode MOBI content."
            // Strip binary garbage: keep printable characters
            let cleaned = text.unicodeScalars.filter { $0.value >= 32 || $0.value == 10 || $0.value == 13 }
                .map { Character($0) }
                .reduce("") { $0 + String($1) }
            let chapter = Chapter(index: 0, title: "Book Content", content: cleaned)
            return BookContent(bookId: bookId, chapters: [chapter])
        }.value
    }
}

// MARK: - ZipArchive (lightweight EPUB/ZIP reader)
final class ZipArchive: @unchecked Sendable {
    private let url: URL
    private var entries: [String: Data] = [:]

    init?(url: URL) {
        self.url = url
        guard let data = try? Data(contentsOf: url) else { return nil }
        self.entries = Self.parseZip(data: data)
    }

    func extract(_ path: String) -> Data? {
        return entries[path] ?? entries[path.hasPrefix("/") ? String(path.dropFirst()) : "/\(path)"]
    }

    private static func parseZip(data: Data) -> [String: Data] {
        var result: [String: Data] = [:]
        var offset = 0

        while offset + 30 < data.count {
            // Local file header signature: 0x04034b50
            let sig = data[offset..<offset+4].withUnsafeBytes { $0.load(as: UInt32.self).littleEndian }
            guard sig == 0x04034b50 else { break }

            let compression = data[offset+8..<offset+10].withUnsafeBytes { $0.load(as: UInt16.self).littleEndian }
            let compressedSize = Int(data[offset+18..<offset+22].withUnsafeBytes { $0.load(as: UInt32.self).littleEndian })
            let uncompressedSize = Int(data[offset+22..<offset+26].withUnsafeBytes { $0.load(as: UInt32.self).littleEndian })
            let fileNameLength = Int(data[offset+26..<offset+28].withUnsafeBytes { $0.load(as: UInt16.self).littleEndian })
            let extraLength = Int(data[offset+28..<offset+30].withUnsafeBytes { $0.load(as: UInt16.self).littleEndian })

            let nameStart = offset + 30
            let nameEnd = nameStart + fileNameLength
            guard nameEnd <= data.count else { break }
            let fileName = String(data: data[nameStart..<nameEnd], encoding: .utf8) ?? ""

            let dataStart = nameEnd + extraLength
            let dataEnd = dataStart + compressedSize
            guard dataEnd <= data.count else { break }

            if compression == 0 {
                // Stored (no compression)
                result[fileName] = Data(data[dataStart..<dataEnd])
            } else if compression == 8 {
                // Deflate
                let compressed = Data(data[dataStart..<dataEnd])
                if let decompressed = decompress(data: compressed, expectedSize: uncompressedSize) {
                    result[fileName] = decompressed
                }
            }

            offset = dataEnd
        }
        return result
    }

    private static func decompress(data: Data, expectedSize: Int) -> Data? {
        // Use zlib via NSData
        var decompressed = Data(count: max(expectedSize, data.count * 4))
        let result = decompressed.withUnsafeMutableBytes { destPtr in
            data.withUnsafeBytes { srcPtr in
                // Add zlib header for decompression
                var stream = z_stream()
                stream.next_in = UnsafeMutablePointer(mutating: srcPtr.bindMemory(to: Bytef.self).baseAddress!)
                stream.avail_in = uInt(data.count)
                stream.next_out = destPtr.bindMemory(to: Bytef.self).baseAddress!
                stream.avail_out = uInt(destPtr.count)
                inflateInit2_(&stream, -15, ZLIB_VERSION, Int32(MemoryLayout<z_stream>.size))
                let status = inflate(&stream, Z_FINISH)
                inflateEnd(&stream)
                return status == Z_STREAM_END ? Int(stream.total_out) : -1
            }
        }
        guard result > 0 else { return nil }
        return decompressed.prefix(result)
    }
}

// zlib imports
import zlib
