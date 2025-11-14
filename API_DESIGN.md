# PDF to Markdown 변환기 - API 설계서

## 📘 CLI API

### 기본 사용법

```bash
# 기본 변환
pdf2md convert input.pdf

# 출력 파일 지정
pdf2md convert input.pdf -o output.md

# 이미지 추출 포함
pdf2md convert input.pdf --extract-images --images-dir ./images

# 상세 로그
pdf2md convert input.pdf -v

# 버전 확인
pdf2md version

# 도움말
pdf2md --help
pdf2md convert --help
```

### 명령어 목록

#### `convert` - PDF를 마크다운으로 변환

**구문**
```bash
pdf2md convert <INPUT_FILE> [OPTIONS]
```

**인자**
- `INPUT_FILE` (필수): 변환할 PDF 파일 경로

**옵션**
| 옵션 | 짧은 형식 | 타입 | 기본값 | 설명 |
|------|----------|------|--------|------|
| `--output` | `-o` | String | `<입력파일명>.md` | 출력 마크다운 파일 경로 |
| `--extract-images` | - | Flag | `false` | 이미지 추출 활성화 |
| `--images-dir` | - | String | `./images` | 추출된 이미지 저장 디렉토리 |
| `--format` | - | Choice | `github` | 마크다운 포맷 스타일 (`github`, `commonmark`, `strict`) |
| `--encoding` | - | String | `UTF-8` | 출력 파일 인코딩 |
| `--include-metadata` | - | Flag | `false` | YAML 프론트매터로 메타데이터 포함 |
| `--verbose` | `-v` | Flag | `false` | 상세 로그 출력 |
| `--quiet` | `-q` | Flag | `false` | 오류만 출력 |

**예제**
```bash
# 1. 기본 변환
pdf2md convert document.pdf

# 2. 모든 기능 활성화
pdf2md convert research.pdf \
  -o research.md \
  --extract-images \
  --images-dir ./assets/images \
  --format github \
  --include-metadata \
  --verbose

# 3. 여러 파일 변환 (bash 루프 사용)
for file in *.pdf; do
  pdf2md convert "$file" -o "${file%.pdf}.md"
done
```

**종료 코드**
- `0`: 성공
- `1`: 일반 오류
- `2`: 파일을 찾을 수 없음
- `3`: 파일 읽기 권한 없음
- `4`: 파일 쓰기 권한 없음
- `5`: PDF 파싱 오류
- `10`: 잘못된 인자

---

#### `version` - 버전 정보 출력

**구문**
```bash
pdf2md version
```

**출력 예제**
```
pdf2md version 1.0.0
Kotlin: 1.9.21
PDFBox: 3.0.1
Java: 17.0.9
```

---

#### `info` - PDF 파일 정보 출력

**구문**
```bash
pdf2md info <PDF_FILE>
```

**출력 예제**
```
File: document.pdf
Size: 2.5 MB
Pages: 42
Title: My Document
Author: John Doe
Created: 2024-01-15
Encrypted: No
```

---

## 🔧 프로그래매틱 API (내부)

### 1. PdfReader API

```kotlin
class PdfReader {
    /**
     * PDF 문서를 로드합니다.
     *
     * @param file PDF 파일
     * @return PDDocument를 감싼 Result
     * @throws 없음 - Result.Error로 반환
     */
    fun loadDocument(file: File): Result<PDDocument>

    /**
     * PDF에서 문서 구조를 추출합니다.
     *
     * @param document PDF 문서
     * @return DocumentStructure를 감싼 Result
     */
    fun extractStructure(document: PDDocument): Result<DocumentStructure>

    /**
     * PDF에서 이미지를 추출합니다.
     *
     * @param document PDF 문서
     * @param outputDir 이미지 저장 디렉토리
     * @return 이미지 메타데이터 리스트를 감싼 Result
     */
    fun extractImages(
        document: PDDocument,
        outputDir: String
    ): Result<List<ImageData>>

    /**
     * PDF 메타데이터를 추출합니다.
     *
     * @param document PDF 문서
     * @return DocumentMetadata를 감싼 Result
     */
    fun extractMetadata(document: PDDocument): Result<DocumentMetadata>
}
```

**사용 예제**
```kotlin
val reader = PdfReader()
val result = reader.loadDocument(File("input.pdf"))

result.onSuccess { document ->
    val structure = reader.extractStructure(document).getOrNull()
    println("Extracted ${structure?.elements?.size} elements")
    document.close()
}.onError { error, cause ->
    println("Error: $error")
}
```

---

### 2. MarkdownConverter API

```kotlin
data class ConversionConfig(
    val extractImages: Boolean = false,
    val imagesDirectory: String = "./images",
    val markdownFormat: String = "github",
    val encoding: String = "UTF-8",
    val verbose: Boolean = false,
    val includeMetadata: Boolean = false
)

class MarkdownConverter(
    private val config: ConversionConfig
) {
    /**
     * 문서 구조를 마크다운으로 변환합니다.
     *
     * @param structure 문서 구조
     * @param images 이미지 데이터 (선택적)
     * @return 마크다운 문자열을 감싼 Result
     */
    fun convert(
        structure: DocumentStructure,
        images: List<ImageData> = emptyList()
    ): Result<String>

    /**
     * 단일 요소를 마크다운으로 변환합니다.
     *
     * @param element 구조화된 요소
     * @return 마크다운 문자열
     */
    fun convertElement(element: StructuredElement): String
}
```

**사용 예제**
```kotlin
val config = ConversionConfig(
    extractImages = true,
    markdownFormat = "github"
)
val converter = MarkdownConverter(config)

val markdown = converter.convert(structure, images)
markdown.onSuccess { md ->
    println("Generated ${md.length} characters")
}
```

---

### 3. FileWriter API

```kotlin
class FileWriter {
    /**
     * 마크다운 콘텐츠를 파일에 씁니다.
     *
     * @param content 마크다운 문자열
     * @param outputFile 출력 파일
     * @param encoding 파일 인코딩
     * @return Unit을 감싼 Result
     */
    fun write(
        content: String,
        outputFile: File,
        encoding: String = "UTF-8"
    ): Result<Unit>

    /**
     * 마크다운과 이미지를 함께 저장합니다.
     *
     * @param content 마크다운 문자열
     * @param images 이미지 데이터
     * @param outputFile 출력 파일
     * @param imagesDir 이미지 디렉토리
     * @return Unit을 감싼 Result
     */
    fun writeWithImages(
        content: String,
        images: List<ImageData>,
        outputFile: File,
        imagesDir: File
    ): Result<Unit>
}
```

---

### 4. ConversionOrchestrator API

```kotlin
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
    /**
     * PDF를 마크다운으로 변환합니다 (전체 프로세스).
     *
     * @param inputFile 입력 PDF 파일
     * @param outputFile 출력 마크다운 파일
     * @return ConversionResult를 감싼 Result
     */
    fun convert(
        inputFile: File,
        outputFile: File
    ): Result<ConversionResult>

    /**
     * 배치 변환을 수행합니다.
     *
     * @param inputFiles 입력 PDF 파일 리스트
     * @param outputDir 출력 디렉토리
     * @return 각 파일의 변환 결과 리스트
     */
    fun convertBatch(
        inputFiles: List<File>,
        outputDir: File
    ): List<Result<ConversionResult>>
}
```

**사용 예제**
```kotlin
val config = ConversionConfig(extractImages = true)
val orchestrator = ConversionOrchestrator(config)

val result = orchestrator.convert(
    inputFile = File("input.pdf"),
    outputFile = File("output.md")
)

result.onSuccess { conversionResult ->
    println("✓ Converted ${conversionResult.pageCount} pages")
    println("✓ Extracted ${conversionResult.imageCount} images")
    println("✓ Time: ${conversionResult.processingTimeMs}ms")
}
```

---

## 📊 데이터 모델 API

### DocumentStructure

```kotlin
/**
 * PDF 문서의 구조화된 표현
 *
 * @property elements 구조화된 요소 리스트 (순서 보장)
 * @property metadata 문서 메타데이터
 */
data class DocumentStructure(
    val elements: List<StructuredElement>,
    val metadata: DocumentMetadata
)
```

---

### StructuredElement (Sealed Class)

```kotlin
sealed class StructuredElement {
    abstract val pageNumber: Int
    abstract val text: String

    /**
     * 제목 요소
     *
     * @property level 제목 레벨 (1-6)
     * @property text 제목 텍스트
     * @property pageNumber 페이지 번호
     */
    data class Heading(
        val level: Int,  // 1-6
        override val text: String,
        override val pageNumber: Int
    ) : StructuredElement()

    /**
     * 단락 요소
     *
     * @property text 단락 텍스트
     * @property pageNumber 페이지 번호
     */
    data class Paragraph(
        override val text: String,
        override val pageNumber: Int
    ) : StructuredElement()

    /**
     * 리스트 아이템 요소
     *
     * @property level 리스트 깊이 (0부터 시작)
     * @property marker 리스트 마커 (-, •, 1., a. 등)
     * @property text 아이템 텍스트
     * @property ordered 번호 리스트 여부
     */
    data class ListItem(
        val level: Int,
        val marker: String,
        override val text: String,
        override val pageNumber: Int,
        val ordered: Boolean
    ) : StructuredElement()

    /**
     * 코드 블록 요소
     *
     * @property text 코드 텍스트
     * @property language 프로그래밍 언어 (선택적)
     */
    data class CodeBlock(
        override val text: String,
        override val pageNumber: Int,
        val language: String? = null
    ) : StructuredElement()

    /**
     * 인용구 요소
     */
    data class Quote(
        override val text: String,
        override val pageNumber: Int
    ) : StructuredElement()

    /**
     * 이미지 요소
     *
     * @property path 이미지 파일 경로
     * @property altText 대체 텍스트
     */
    data class Image(
        val path: String,
        val altText: String,
        override val pageNumber: Int
    ) : StructuredElement() {
        override val text: String = altText
    }
}
```

**사용 예제**
```kotlin
when (element) {
    is StructuredElement.Heading -> {
        println("Heading level ${element.level}: ${element.text}")
    }
    is StructuredElement.Paragraph -> {
        println("Paragraph: ${element.text}")
    }
    is StructuredElement.ListItem -> {
        val indent = "  ".repeat(element.level)
        println("${indent}${element.marker} ${element.text}")
    }
    is StructuredElement.CodeBlock -> {
        println("Code (${element.language}): ${element.text}")
    }
    is StructuredElement.Quote -> {
        println("> ${element.text}")
    }
    is StructuredElement.Image -> {
        println("![${element.altText}](${element.path})")
    }
}
```

---

### TextElement

```kotlin
/**
 * PDF에서 추출된 원시 텍스트 요소
 *
 * @property text 텍스트 내용
 * @property fontSize 폰트 크기
 * @property fontName 폰트 이름
 * @property x X 좌표
 * @property y Y 좌표
 * @property width 너비
 * @property height 높이
 * @property pageNumber 페이지 번호
 */
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
```

---

### ImageData

```kotlin
/**
 * 추출된 이미지 정보
 *
 * @property path 저장된 이미지 파일 경로
 * @property pageNumber 이미지가 위치한 페이지
 * @property altText 이미지 대체 텍스트
 * @property width 이미지 너비 (픽셀)
 * @property height 이미지 높이 (픽셀)
 */
data class ImageData(
    val path: String,
    val pageNumber: Int,
    val altText: String,
    val width: Int,
    val height: Int
)
```

---

### DocumentMetadata

```kotlin
/**
 * PDF 문서 메타데이터
 *
 * 모든 필드는 nullable (PDF에 따라 없을 수 있음)
 */
data class DocumentMetadata(
    val title: String?,
    val author: String?,
    val subject: String?,
    val keywords: String?,
    val creator: String?,      // PDF 생성 애플리케이션
    val producer: String?,     // PDF 프로듀서
    val creationDate: String?,
    val modificationDate: String?
)
```

---

## 🛠️ Result API (에러 처리)

```kotlin
sealed class Result<out T> {
    data class Success<T>(val value: T) : Result<T>()
    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : Result<Nothing>()

    // 변환 함수
    inline fun <R> map(transform: (T) -> R): Result<R>
    inline fun <R> flatMap(transform: (T) -> Result<R>): Result<R>

    // 콜백 함수
    inline fun onSuccess(action: (T) -> Unit): Result<T>
    inline fun onError(action: (String, Throwable?) -> Unit): Result<T>

    // 값 추출
    fun getOrElse(default: (String) -> T): T
    fun getOrNull(): T?
    fun getOrThrow(): T  // Success가 아니면 예외 발생
}
```

**사용 예제**
```kotlin
// 체이닝
pdfReader.loadDocument(file)
    .flatMap { doc -> pdfReader.extractStructure(doc) }
    .map { structure -> converter.convert(structure) }
    .onSuccess { markdown -> fileWriter.write(markdown, outputFile) }
    .onError { error, cause ->
        logger.error("Conversion failed: $error", cause)
    }

// 패턴 매칭
when (val result = pdfReader.loadDocument(file)) {
    is Result.Success -> {
        val document = result.value
        // 처리
    }
    is Result.Error -> {
        println("Error: ${result.message}")
        result.cause?.printStackTrace()
    }
}
```

---

## 🔌 확장 API (플러그인 시스템 - 향후)

```kotlin
/**
 * 커스텀 변환 규칙 인터페이스
 */
interface ConversionRule {
    fun canHandle(element: TextElement): Boolean
    fun convert(element: TextElement): StructuredElement
}

/**
 * 커스텀 포맷터 인터페이스
 */
interface MarkdownFormatter {
    fun formatHeading(heading: StructuredElement.Heading): String
    fun formatParagraph(paragraph: StructuredElement.Paragraph): String
    fun formatListItem(item: StructuredElement.ListItem): String
    // ...
}

/**
 * 플러그인 등록
 */
class PluginRegistry {
    fun registerRule(rule: ConversionRule)
    fun registerFormatter(formatter: MarkdownFormatter)
}
```

---

## 📝 설정 파일 API (향후)

```yaml
# .pdf2md.yml
conversion:
  extract_images: true
  images_dir: ./assets
  format: github
  encoding: UTF-8

rules:
  heading_detection:
    enabled: true
    size_threshold: 1.2
  list_detection:
    enabled: true
    indent_size: 20

output:
  include_metadata: true
  preserve_line_breaks: true
  max_line_length: 80
```

```kotlin
data class Config(
    val conversion: ConversionSettings,
    val rules: RuleSettings,
    val output: OutputSettings
)

class ConfigLoader {
    fun load(path: String = ".pdf2md.yml"): Config
    fun loadFromJson(path: String): Config
}
```

---

## 🎨 출력 포맷

### GitHub Flavored Markdown (기본)
```markdown
# Heading 1
## Heading 2

Paragraph text with **bold** and *italic*.

- List item 1
- List item 2
  - Nested item

| Column 1 | Column 2 |
|----------|----------|
| Cell 1   | Cell 2   |

```python
code block
```

![Image](./images/image_1.png)
```

### CommonMark
- 표준 마크다운 사양 준수
- 확장 기능 없음

### Strict Mode
- 가장 엄격한 마크다운 사양
- 최대 호환성 보장

---

## 🚦 에러 코드

| 코드 | 상수 | 설명 |
|------|------|------|
| 0 | `SUCCESS` | 성공 |
| 1 | `GENERAL_ERROR` | 일반 오류 |
| 2 | `FILE_NOT_FOUND` | 파일을 찾을 수 없음 |
| 3 | `NO_READ_PERMISSION` | 읽기 권한 없음 |
| 4 | `NO_WRITE_PERMISSION` | 쓰기 권한 없음 |
| 5 | `PDF_PARSE_ERROR` | PDF 파싱 오류 |
| 6 | `ENCRYPTED_PDF` | 암호화된 PDF |
| 7 | `CORRUPTED_PDF` | 손상된 PDF |
| 8 | `OUT_OF_MEMORY` | 메모리 부족 |
| 9 | `CONVERSION_ERROR` | 변환 오류 |
| 10 | `INVALID_ARGUMENT` | 잘못된 인자 |

```kotlin
enum class ExitCode(val code: Int) {
    SUCCESS(0),
    GENERAL_ERROR(1),
    FILE_NOT_FOUND(2),
    NO_READ_PERMISSION(3),
    NO_WRITE_PERMISSION(4),
    PDF_PARSE_ERROR(5),
    ENCRYPTED_PDF(6),
    CORRUPTED_PDF(7),
    OUT_OF_MEMORY(8),
    CONVERSION_ERROR(9),
    INVALID_ARGUMENT(10);

    fun exit(): Nothing {
        kotlin.system.exitProcess(code)
    }
}
```

---

## 📚 사용 시나리오

### 시나리오 1: 기본 변환
```kotlin
fun basicConversion() {
    val config = ConversionConfig()
    val orchestrator = ConversionOrchestrator(config)

    val result = orchestrator.convert(
        inputFile = File("document.pdf"),
        outputFile = File("document.md")
    )

    when (result) {
        is Result.Success -> println("Success!")
        is Result.Error -> println("Error: ${result.message}")
    }
}
```

### 시나리오 2: 이미지 포함 변환
```kotlin
fun conversionWithImages() {
    val config = ConversionConfig(
        extractImages = true,
        imagesDirectory = "./assets/images"
    )
    val orchestrator = ConversionOrchestrator(config)

    orchestrator.convert(
        File("presentation.pdf"),
        File("presentation.md")
    ).onSuccess { result ->
        println("Converted with ${result.imageCount} images")
    }
}
```

### 시나리오 3: 메타데이터 포함
```kotlin
fun conversionWithMetadata() {
    val config = ConversionConfig(
        includeMetadata = true
    )
    val orchestrator = ConversionOrchestrator(config)

    orchestrator.convert(
        File("research.pdf"),
        File("research.md")
    )
}
```

### 시나리오 4: 배치 변환
```kotlin
fun batchConversion() {
    val config = ConversionConfig()
    val orchestrator = ConversionOrchestrator(config)

    val pdfFiles = File("./pdfs")
        .listFiles { file -> file.extension == "pdf" }
        ?.toList() ?: emptyList()

    val results = orchestrator.convertBatch(
        inputFiles = pdfFiles,
        outputDir = File("./markdown")
    )

    val succeeded = results.count { it is Result.Success }
    println("Converted $succeeded of ${results.size} files")
}
```

---

이 API 설계는 **타입 안전성**, **에러 처리**, **확장성**을 중심으로 설계되었습니다.
