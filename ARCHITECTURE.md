# PDF to Markdown 변환기 - 아키텍처 설계

## 🏛️ 전체 아키텍처

### 레이어드 아키텍처 (Layered Architecture)

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ CLI Commands │  │ Progress Bar │  │ Error Display│      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    Application Layer                        │
│  ┌──────────────────────────────────────────────┐          │
│  │         ConversionOrchestrator               │          │
│  │  - 변환 프로세스 조정                           │          │
│  │  - 에러 핸들링                                  │          │
│  │  - 로깅                                        │          │
│  └──────────────────────────────────────────────┘          │
└─────────────────────────────────────────────────────────────┘
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                      Domain Layer                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  PdfReader   │  │  Converter   │  │ FileWriter   │      │
│  │              │  │              │  │              │      │
│  │ - Extract    │  │ - Transform  │  │ - Save       │      │
│  │ - Parse      │  │ - Format     │  │ - Validate   │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                  Infrastructure Layer                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │   PDFBox     │  │  File System │  │    Logger    │      │
│  │   Library    │  │   Access     │  │   (SLF4J)    │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
```

---

## 📦 모듈 상세 설계

### 1. CLI Module (Presentation Layer)

#### 책임
- 사용자 입력 파싱
- 명령어 검증
- 결과 출력
- 진행 상황 표시

#### 주요 클래스

**Pdf2MdCommand.kt**
```kotlin
package com.pdf2md.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands

class Pdf2MdCommand : CliktCommand(
    name = "pdf2md",
    help = "Convert PDF documents to Markdown format"
) {
    init {
        subcommands(ConvertCommand(), VersionCommand(), InfoCommand())
    }

    override fun run() {
        // 하위 명령어로 위임
    }
}
```

**ConvertCommand.kt**
```kotlin
package com.pdf2md.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.choice
import java.io.File

class ConvertCommand : CliktCommand(
    name = "convert",
    help = "Convert a PDF file to Markdown"
) {
    // 필수 인자
    private val input by argument(help = "Input PDF file path")
        .file(mustExist = true, canBeDir = false, mustBeReadable = true)

    // 선택 옵션
    private val output by option("-o", "--output", help = "Output markdown file path")
        .file()

    private val extractImages by option("--extract-images", help = "Extract and save images")
        .flag(default = false)

    private val imagesDir by option("--images-dir", help = "Directory to save extracted images")
        .file()

    private val verbose by option("-v", "--verbose", help = "Enable verbose logging")
        .flag(default = false)

    private val format by option("--format", help = "Output format style")
        .choice("github", "commonmark", "strict")
        .default("github")

    private val encoding by option("--encoding", help = "Output file encoding")
        .default("UTF-8")

    override fun run() {
        // 검증
        validateOptions()

        // Orchestrator 호출
        val orchestrator = ConversionOrchestrator(
            config = ConversionConfig(
                extractImages = extractImages,
                imagesDirectory = imagesDir?.absolutePath ?: "./images",
                markdownFormat = format,
                encoding = encoding,
                verbose = verbose
            )
        )

        // 실행
        val result = orchestrator.convert(input, output ?: generateOutputPath(input))

        // 결과 처리
        result.onSuccess {
            echo("✓ Conversion successful: ${it.outputPath}", err = false)
            if (verbose) {
                echo("  Pages: ${it.pageCount}")
                echo("  Images: ${it.imageCount}")
            }
        }.onError { error ->
            echo("✗ Conversion failed: $error", err = true)
        }
    }

    private fun validateOptions() {
        if (extractImages && imagesDir != null) {
            require(imagesDir!!.isDirectory) {
                "Images directory must be a directory"
            }
        }
    }

    private fun generateOutputPath(input: File): File {
        val baseName = input.nameWithoutExtension
        return File(input.parent, "$baseName.md")
    }
}
```

---

### 2. Application Module

#### 책임
- 변환 프로세스 오케스트레이션
- 비즈니스 로직 조정
- 트랜잭션 관리
- 에러 핸들링

#### 주요 클래스

**ConversionOrchestrator.kt**
```kotlin
package com.pdf2md.application

import com.pdf2md.domain.pdf.PdfReader
import com.pdf2md.domain.converter.MarkdownConverter
import com.pdf2md.domain.output.FileWriter
import com.pdf2md.common.Result
import org.slf4j.LoggerFactory
import java.io.File

data class ConversionConfig(
    val extractImages: Boolean = false,
    val imagesDirectory: String = "./images",
    val markdownFormat: String = "github",
    val encoding: String = "UTF-8",
    val verbose: Boolean = false
)

data class ConversionResult(
    val outputPath: String,
    val pageCount: Int,
    val imageCount: Int,
    val processingTimeMs: Long
)

class ConversionOrchestrator(
    private val config: ConversionConfig,
    private val pdfReader: PdfReader = PdfReader(),
    private val markdownConverter: MarkdownConverter = MarkdownConverter(config),
    private val fileWriter: FileWriter = FileWriter()
) {
    private val logger = LoggerFactory.getLogger(ConversionOrchestrator::class.java)

    fun convert(inputFile: File, outputFile: File): Result<ConversionResult> {
        val startTime = System.currentTimeMillis()

        return try {
            logger.info("Starting conversion: ${inputFile.absolutePath}")

            // 1. PDF 로드
            val document = pdfReader.loadDocument(inputFile)
                .getOrElse { error ->
                    return Result.Error("Failed to load PDF: $error")
                }

            logger.debug("PDF loaded successfully, pages: ${document.numberOfPages}")

            // 2. PDF에서 구조 추출
            val structure = pdfReader.extractStructure(document)
                .getOrElse { error ->
                    document.close()
                    return Result.Error("Failed to extract structure: $error")
                }

            logger.debug("Structure extracted: ${structure.elements.size} elements")

            // 3. 이미지 추출 (선택적)
            val images = if (config.extractImages) {
                pdfReader.extractImages(document, config.imagesDirectory)
                    .getOrElse { error ->
                        logger.warn("Failed to extract images: $error")
                        emptyList()
                    }
            } else {
                emptyList()
            }

            logger.debug("Images extracted: ${images.size}")

            // 4. 마크다운 변환
            val markdown = markdownConverter.convert(structure, images)
                .getOrElse { error ->
                    document.close()
                    return Result.Error("Failed to convert to markdown: $error")
                }

            logger.debug("Markdown generated: ${markdown.length} characters")

            // 5. 파일 쓰기
            fileWriter.write(markdown, outputFile, config.encoding)
                .getOrElse { error ->
                    document.close()
                    return Result.Error("Failed to write output file: $error")
                }

            // 6. 정리
            document.close()

            val endTime = System.currentTimeMillis()
            val result = ConversionResult(
                outputPath = outputFile.absolutePath,
                pageCount = document.numberOfPages,
                imageCount = images.size,
                processingTimeMs = endTime - startTime
            )

            logger.info("Conversion completed in ${result.processingTimeMs}ms")
            Result.Success(result)

        } catch (e: Exception) {
            logger.error("Unexpected error during conversion", e)
            Result.Error("Unexpected error: ${e.message}", e)
        }
    }
}
```

---

### 3. Domain Module - PDF Package

#### 책임
- PDF 문서 로드
- 텍스트 및 메타데이터 추출
- 문서 구조 파싱

#### 주요 클래스

**PdfReader.kt**
```kotlin
package com.pdf2md.domain.pdf

import com.pdf2md.common.Result
import org.apache.pdfbox.pdmodel.PDDocument
import org.slf4j.LoggerFactory
import java.io.File

class PdfReader {
    private val logger = LoggerFactory.getLogger(PdfReader::class.java)

    fun loadDocument(file: File): Result<PDDocument> {
        return try {
            val document = PDDocument.load(file)

            // 암호화 확인
            if (document.isEncrypted) {
                try {
                    document.setAllSecurityToBeRemoved(true)
                } catch (e: Exception) {
                    document.close()
                    return Result.Error("PDF is encrypted and cannot be opened")
                }
            }

            Result.Success(document)
        } catch (e: Exception) {
            logger.error("Failed to load PDF document", e)
            Result.Error("Failed to load PDF: ${e.message}", e)
        }
    }

    fun extractStructure(document: PDDocument): Result<DocumentStructure> {
        return try {
            val extractor = PdfExtractor()
            val elements = extractor.extractElements(document)
            val structure = StructureAnalyzer().analyze(elements)

            Result.Success(structure)
        } catch (e: Exception) {
            logger.error("Failed to extract structure", e)
            Result.Error("Failed to extract structure: ${e.message}", e)
        }
    }

    fun extractImages(document: PDDocument, outputDir: String): Result<List<ImageData>> {
        return try {
            val imageExtractor = ImageExtractor()
            val images = imageExtractor.extract(document, outputDir)

            Result.Success(images)
        } catch (e: Exception) {
            logger.error("Failed to extract images", e)
            Result.Error("Failed to extract images: ${e.message}", e)
        }
    }
}
```

**PdfExtractor.kt**
```kotlin
package com.pdf2md.domain.pdf

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.pdfbox.text.TextPosition

class PdfExtractor {
    fun extractElements(document: PDDocument): List<TextElement> {
        val elements = mutableListOf<TextElement>()

        val stripper = object : PDFTextStripper() {
            override fun writeString(text: String, textPositions: MutableList<TextPosition>) {
                textPositions.forEach { pos ->
                    if (pos.unicode.isNotBlank()) {
                        elements.add(TextElement(
                            text = pos.unicode,
                            fontSize = pos.fontSize,
                            fontName = pos.font.name ?: "Unknown",
                            x = pos.x,
                            y = pos.y,
                            width = pos.width,
                            height = pos.height,
                            pageNumber = currentPageNo
                        ))
                    }
                }
            }
        }

        stripper.sortByPosition = true
        stripper.getText(document) // 텍스트 추출 트리거

        return elements
    }
}
```

**DocumentStructure.kt** (Domain Model)
```kotlin
package com.pdf2md.domain.pdf

data class TextElement(
    val text: String,
    val fontSize: Float,
    val fontName: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val pageNumber: Int
)

data class DocumentStructure(
    val elements: List<StructuredElement>,
    val metadata: DocumentMetadata
)

sealed class StructuredElement {
    abstract val pageNumber: Int
    abstract val text: String

    data class Heading(
        val level: Int,
        override val text: String,
        override val pageNumber: Int
    ) : StructuredElement()

    data class Paragraph(
        override val text: String,
        override val pageNumber: Int
    ) : StructuredElement()

    data class ListItem(
        val level: Int,
        val marker: String,
        override val text: String,
        override val pageNumber: Int,
        val ordered: Boolean
    ) : StructuredElement()

    data class CodeBlock(
        override val text: String,
        override val pageNumber: Int,
        val language: String? = null
    ) : StructuredElement()

    data class Quote(
        override val text: String,
        override val pageNumber: Int
    ) : StructuredElement()

    data class Image(
        val path: String,
        val altText: String,
        override val pageNumber: Int
    ) : StructuredElement() {
        override val text: String = altText
    }
}

data class DocumentMetadata(
    val title: String?,
    val author: String?,
    val subject: String?,
    val keywords: String?,
    val creator: String?,
    val producer: String?,
    val creationDate: String?,
    val modificationDate: String?
)

data class ImageData(
    val path: String,
    val pageNumber: Int,
    val altText: String,
    val width: Int,
    val height: Int
)
```

**StructureAnalyzer.kt**
```kotlin
package com.pdf2md.domain.pdf

class StructureAnalyzer {
    fun analyze(elements: List<TextElement>): DocumentStructure {
        val structuredElements = mutableListOf<StructuredElement>()

        // 1. 평균 폰트 크기 계산
        val avgFontSize = elements.map { it.fontSize }.average().toFloat()

        // 2. 그룹화 (같은 줄 요소들)
        val lines = groupIntoLines(elements)

        // 3. 각 줄 분석 및 구조화
        lines.forEach { line ->
            val element = classifyLine(line, avgFontSize)
            structuredElements.add(element)
        }

        // 4. 연속된 단락 병합
        val merged = mergeParagraphs(structuredElements)

        return DocumentStructure(
            elements = merged,
            metadata = DocumentMetadata(
                title = null, // TODO: PDF 메타데이터에서 추출
                author = null,
                subject = null,
                keywords = null,
                creator = null,
                producer = null,
                creationDate = null,
                modificationDate = null
            )
        )
    }

    private fun groupIntoLines(elements: List<TextElement>): List<List<TextElement>> {
        if (elements.isEmpty()) return emptyList()

        val lines = mutableListOf<MutableList<TextElement>>()
        var currentLine = mutableListOf(elements.first())

        elements.drop(1).forEach { element ->
            val lastElement = currentLine.last()

            // 같은 줄인지 확인 (Y 좌표가 비슷하면)
            if (Math.abs(element.y - lastElement.y) < lastElement.height * 0.5) {
                currentLine.add(element)
            } else {
                lines.add(currentLine)
                currentLine = mutableListOf(element)
            }
        }

        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }

        return lines
    }

    private fun classifyLine(
        line: List<TextElement>,
        avgFontSize: Float
    ): StructuredElement {
        val text = line.joinToString("") { it.text }
        val fontSize = line.maxOf { it.fontSize }
        val pageNumber = line.first().pageNumber
        val indent = line.first().x

        // 제목 감지
        val headingLevel = detectHeadingLevel(fontSize, avgFontSize)
        if (headingLevel > 0) {
            return StructuredElement.Heading(headingLevel, text.trim(), pageNumber)
        }

        // 리스트 감지
        val listItem = detectListItem(text, indent)
        if (listItem != null) {
            return listItem
        }

        // 코드 블록 감지 (고정폭 폰트)
        if (isMonospaceFont(line.first().fontName)) {
            return StructuredElement.CodeBlock(text, pageNumber)
        }

        // 기본: 단락
        return StructuredElement.Paragraph(text.trim(), pageNumber)
    }

    private fun detectHeadingLevel(fontSize: Float, baseFontSize: Float): Int {
        val ratio = fontSize / baseFontSize
        return when {
            ratio >= 2.0 -> 1
            ratio >= 1.7 -> 2
            ratio >= 1.4 -> 3
            ratio >= 1.2 -> 4
            ratio >= 1.1 -> 5
            else -> 0  // 제목 아님
        }
    }

    private fun detectListItem(text: String, indent: Float): StructuredElement.ListItem? {
        // 불릿 리스트
        val bulletPattern = Regex("""^(\s*)([-•◦▪▫○●])\s+(.+)$""")
        bulletPattern.find(text)?.let { match ->
            val (_, marker, content) = match.destructured
            return StructuredElement.ListItem(
                level = (indent / 20).toInt(),  // 들여쓰기 20pt = 1레벨
                marker = marker,
                text = content.trim(),
                pageNumber = 0,  // 나중에 설정
                ordered = false
            )
        }

        // 번호 리스트
        val numberedPattern = Regex("""^(\s*)(\d+|\w)[.)])\s+(.+)$""")
        numberedPattern.find(text)?.let { match ->
            val (_, marker, content) = match.destructured
            return StructuredElement.ListItem(
                level = (indent / 20).toInt(),
                marker = marker,
                text = content.trim(),
                pageNumber = 0,
                ordered = true
            )
        }

        return null
    }

    private fun isMonospaceFont(fontName: String): Boolean {
        val monospaceFonts = listOf("Courier", "Consolas", "Monaco", "Monospace")
        return monospaceFonts.any { fontName.contains(it, ignoreCase = true) }
    }

    private fun mergeParagraphs(
        elements: List<StructuredElement>
    ): List<StructuredElement> {
        if (elements.isEmpty()) return emptyList()

        val merged = mutableListOf<StructuredElement>()
        var currentParagraph: StructuredElement.Paragraph? = null

        elements.forEach { element ->
            when (element) {
                is StructuredElement.Paragraph -> {
                    if (currentParagraph != null) {
                        // 연속된 단락 병합
                        currentParagraph = StructuredElement.Paragraph(
                            text = "${currentParagraph!!.text} ${element.text}",
                            pageNumber = currentParagraph!!.pageNumber
                        )
                    } else {
                        currentParagraph = element
                    }
                }
                else -> {
                    // 다른 요소 만나면 현재 단락 저장
                    currentParagraph?.let { merged.add(it) }
                    currentParagraph = null
                    merged.add(element)
                }
            }
        }

        // 마지막 단락 추가
        currentParagraph?.let { merged.add(it) }

        return merged
    }
}
```

---

### 4. Domain Module - Converter Package

#### 책임
- 구조화된 요소를 마크다운으로 변환
- 포맷팅 규칙 적용
- 출력 스타일 관리

**MarkdownConverter.kt**
```kotlin
package com.pdf2md.domain.converter

import com.pdf2md.domain.pdf.DocumentStructure
import com.pdf2md.domain.pdf.StructuredElement
import com.pdf2md.domain.pdf.ImageData
import com.pdf2md.common.Result

class MarkdownConverter(
    private val config: ConversionConfig
) {
    fun convert(
        structure: DocumentStructure,
        images: List<ImageData> = emptyList()
    ): Result<String> {
        return try {
            val builder = StringBuilder()

            // 메타데이터 추가 (선택적)
            if (config.includeMetadata) {
                appendMetadata(builder, structure.metadata)
            }

            // 요소별 변환
            structure.elements.forEach { element ->
                val markdown = convertElement(element)
                builder.append(markdown)
                builder.append("\n\n")
            }

            // 정리
            val markdown = builder.toString()
                .replace(Regex("\n{3,}"), "\n\n")  // 과도한 빈 줄 제거
                .trim()

            Result.Success(markdown)
        } catch (e: Exception) {
            Result.Error("Failed to convert to markdown: ${e.message}", e)
        }
    }

    private fun convertElement(element: StructuredElement): String {
        return when (element) {
            is StructuredElement.Heading -> convertHeading(element)
            is StructuredElement.Paragraph -> convertParagraph(element)
            is StructuredElement.ListItem -> convertListItem(element)
            is StructuredElement.CodeBlock -> convertCodeBlock(element)
            is StructuredElement.Quote -> convertQuote(element)
            is StructuredElement.Image -> convertImage(element)
        }
    }

    private fun convertHeading(heading: StructuredElement.Heading): String {
        val prefix = "#".repeat(heading.level)
        return "$prefix ${heading.text}"
    }

    private fun convertParagraph(paragraph: StructuredElement.Paragraph): String {
        return paragraph.text
    }

    private fun convertListItem(item: StructuredElement.ListItem): String {
        val indent = "  ".repeat(item.level)
        val marker = if (item.ordered) "1." else "-"
        return "$indent$marker ${item.text}"
    }

    private fun convertCodeBlock(code: StructuredElement.CodeBlock): String {
        val language = code.language ?: ""
        return "```$language\n${code.text}\n```"
    }

    private fun convertQuote(quote: StructuredElement.Quote): String {
        return "> ${quote.text}"
    }

    private fun convertImage(image: StructuredElement.Image): String {
        return "![${image.altText}](${image.path})"
    }

    private fun appendMetadata(builder: StringBuilder, metadata: DocumentMetadata) {
        builder.append("---\n")
        metadata.title?.let { builder.append("title: $it\n") }
        metadata.author?.let { builder.append("author: $it\n") }
        metadata.subject?.let { builder.append("subject: $it\n") }
        builder.append("---\n\n")
    }
}
```

---

### 5. Common Module

**Result.kt** (에러 처리)
```kotlin
package com.pdf2md.common

sealed class Result<out T> {
    data class Success<T>(val value: T) : Result<T>()
    data class Error(val message: String, val cause: Throwable? = null) : Result<Nothing>()

    inline fun <R> map(transform: (T) -> R): Result<R> {
        return when (this) {
            is Success -> Success(transform(value))
            is Error -> this
        }
    }

    inline fun <R> flatMap(transform: (T) -> Result<R>): Result<R> {
        return when (this) {
            is Success -> transform(value)
            is Error -> this
        }
    }

    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(value)
        return this
    }

    inline fun onError(action: (String, Throwable?) -> Unit): Result<T> {
        if (this is Error) action(message, cause)
        return this
    }

    fun getOrElse(default: (String) -> T): T {
        return when (this) {
            is Success -> value
            is Error -> default(message)
        }
    }

    fun getOrNull(): T? {
        return when (this) {
            is Success -> value
            is Error -> null
        }
    }
}
```

---

## 🔄 데이터 플로우

```
[PDF File]
    │
    ▼
┌───────────────┐
│  PdfReader    │  PDDocument 생성
│  loadDocument │
└───────┬───────┘
        │
        ▼
┌───────────────┐
│  PdfExtractor │  List<TextElement> 추출
│ extractElements│
└───────┬───────┘
        │
        ▼
┌───────────────────┐
│ StructureAnalyzer │  DocumentStructure 생성
│     analyze       │
└─────────┬─────────┘
          │
          ▼
┌──────────────────┐
│MarkdownConverter │  String (Markdown) 생성
│     convert      │
└─────────┬────────┘
          │
          ▼
┌──────────────┐
│  FileWriter  │  파일 시스템에 저장
│    write     │
└──────────────┘
```

---

## ⚙️ 의존성 주입

### 수동 DI (Constructor Injection)

```kotlin
// 의존성 조립
class DependencyContainer {
    // Infrastructure
    val logger = LoggerFactory.getLogger("PDF2MD")

    // Domain
    val pdfReader = PdfReader()
    val markdownConverter = MarkdownConverter(config)
    val fileWriter = FileWriter()

    // Application
    val orchestrator = ConversionOrchestrator(
        config = config,
        pdfReader = pdfReader,
        markdownConverter = markdownConverter,
        fileWriter = fileWriter
    )
}

// 사용
fun main(args: Array<String>) {
    val container = DependencyContainer()
    Pdf2MdCommand(container.orchestrator).main(args)
}
```

---

## 🧪 테스트 전략

### 단위 테스트
- 각 클래스 독립적으로 테스트
- Mock 사용 (외부 의존성)

### 통합 테스트
- 전체 플로우 테스트
- 실제 PDF 파일 사용

### 테스트 더블
```kotlin
class MockPdfReader : PdfReader() {
    override fun loadDocument(file: File): Result<PDDocument> {
        // 테스트용 문서 반환
    }
}
```

---

이 아키텍처는 **SOLID 원칙**을 따르며, **확장 가능**하고 **테스트 가능**한 구조입니다.
