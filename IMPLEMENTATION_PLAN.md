# PDF to Markdown 변환기 - 구현 계획서

## 📋 프로젝트 개요

### 목적
PDF 문서를 마크다운 형식으로 변환하는 터미널 기반 CLI 애플리케이케이션

### 기술 스택
- **언어**: Kotlin 1.9+
- **빌드 도구**: Gradle 8.x (Kotlin DSL)
- **CLI 프레임워크**: Clikt 4.x
- **PDF 라이브러리**: Apache PDFBox 3.x
- **로깅**: SLF4J + Logback

---

## 🏗️ 아키텍처 개요

### 레이어 구조
```
┌─────────────────────────────────────┐
│      CLI Layer (Clikt)              │  ← 사용자 입력 처리
├─────────────────────────────────────┤
│    Application Layer                │  ← 비즈니스 로직 조정
├─────────────────────────────────────┤
│  ┌──────────┐  ┌─────────────────┐ │
│  │ PDF      │  │ Markdown        │ │  ← 핵심 도메인 로직
│  │ Reader   │  │ Converter       │ │
│  └──────────┘  └─────────────────┘ │
├─────────────────────────────────────┤
│    Infrastructure Layer             │  ← 파일 I/O, 설정
└─────────────────────────────────────┘
```

### 핵심 모듈

1. **cli** - 명령줄 인터페이스
2. **pdf** - PDF 문서 처리
3. **converter** - 마크다운 변환
4. **output** - 결과 파일 출력
5. **common** - 공통 유틸리티

---

## 📂 디렉토리 구조

```
pdf2markdown/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── .gitignore
├── README.md
├── IMPLEMENTATION_PLAN.md
├── ARCHITECTURE.md
├── LICENSE
└── src/
    ├── main/
    │   ├── kotlin/
    │   │   └── com/pdf2md/
    │   │       ├── Main.kt
    │   │       ├── cli/
    │   │       │   ├── Pdf2MdCommand.kt
    │   │       │   ├── ConvertCommand.kt
    │   │       │   └── VersionCommand.kt
    │   │       ├── pdf/
    │   │       │   ├── PdfReader.kt
    │   │       │   ├── PdfExtractor.kt
    │   │       │   ├── TextElement.kt
    │   │       │   └── DocumentStructure.kt
    │   │       ├── converter/
    │   │       │   ├── MarkdownConverter.kt
    │   │       │   ├── FormattingRules.kt
    │   │       │   ├── HeadingDetector.kt
    │   │       │   └── ListDetector.kt
    │   │       ├── output/
    │   │       │   ├── FileWriter.kt
    │   │       │   └── OutputFormatter.kt
    │   │       └── common/
    │   │           ├── Result.kt
    │   │           ├── Logger.kt
    │   │           └── Extensions.kt
    │   └── resources/
    │       └── logback.xml
    └── test/
        └── kotlin/
            └── com/pdf2md/
                ├── pdf/
                │   └── PdfReaderTest.kt
                ├── converter/
                │   └── MarkdownConverterTest.kt
                └── integration/
                    └── EndToEndTest.kt
```

---

## 🔨 구현 단계

### Phase 1: 프로젝트 초기 설정 (1일)

#### 1.1 Gradle 프로젝트 생성
- [x] `build.gradle.kts` 설정
- [x] `settings.gradle.kts` 설정
- [x] 의존성 추가
  - Clikt
  - PDFBox
  - SLF4J/Logback
  - JUnit 5 (테스트)

#### 1.2 기본 디렉토리 구조 생성
- [x] 패키지 구조 생성
- [x] `.gitignore` 설정
- [x] 라이선스 파일 추가

---

### Phase 2: CLI 인터페이스 구현 (1일)

#### 2.1 Main 진입점
```kotlin
// Main.kt
fun main(args: Array<String>) = Pdf2MdCommand().main(args)
```

#### 2.2 Clikt 명령어 구조
```kotlin
// Pdf2MdCommand.kt
class Pdf2MdCommand : CliktCommand() {
    override fun run() {
        echo("PDF to Markdown Converter")
    }
}

// ConvertCommand.kt
class ConvertCommand : CliktCommand(name = "convert") {
    private val input by argument(help = "Input PDF file")
    private val output by option("-o", "--output", help = "Output markdown file")
    private val extractImages by option("--extract-images").flag(default = false)

    override fun run() {
        // 변환 로직 호출
    }
}
```

#### 2.3 옵션 및 플래그
- `--output, -o`: 출력 파일 경로
- `--extract-images`: 이미지 추출 여부
- `--verbose, -v`: 상세 로그 출력
- `--version`: 버전 정보

---

### Phase 3: PDF 리더 구현 (2-3일)

#### 3.1 PdfReader 클래스
```kotlin
class PdfReader {
    fun loadDocument(filePath: String): Result<PDDocument> {
        return try {
            val document = PDDocument.load(File(filePath))
            Result.Success(document)
        } catch (e: Exception) {
            Result.Error("Failed to load PDF: ${e.message}")
        }
    }
}
```

#### 3.2 PdfExtractor 클래스
```kotlin
class PdfExtractor {
    fun extractText(document: PDDocument): List<TextElement> {
        // PDFBox의 PDFTextStripper 사용
        // 각 페이지별로 텍스트 추출
        // 폰트 크기, 위치 정보 포함
    }

    fun extractStructure(elements: List<TextElement>): DocumentStructure {
        // 제목, 본문, 리스트 등 구조 파악
    }

    fun extractImages(document: PDDocument): List<ImageData> {
        // 이미지 추출 (선택적)
    }
}
```

#### 3.3 데이터 모델
```kotlin
data class TextElement(
    val text: String,
    val fontSize: Float,
    val fontName: String,
    val x: Float,
    val y: Float,
    val pageNumber: Int
)

data class DocumentStructure(
    val headings: List<Heading>,
    val paragraphs: List<Paragraph>,
    val lists: List<ListItem>,
    val images: List<ImageData>
)

data class Heading(
    val level: Int,  // 1-6
    val text: String,
    val pageNumber: Int
)
```

#### 3.4 구현 세부사항
- **텍스트 추출**: `PDFTextStripper` 사용
- **폰트 정보**: `PDFTextStripperByArea` + 커스텀 TextPosition 분석
- **구조 인식**:
  - 폰트 크기 기반 제목 감지
  - 들여쓰기 기반 리스트 감지
  - 줄 간격 기반 단락 구분

---

### Phase 4: 마크다운 변환기 구현 (2-3일)

#### 4.1 MarkdownConverter 클래스
```kotlin
class MarkdownConverter(
    private val rules: FormattingRules
) {
    fun convert(structure: DocumentStructure): String {
        val builder = StringBuilder()

        // 제목 변환
        structure.headings.forEach { heading ->
            builder.append("#".repeat(heading.level))
            builder.append(" ${heading.text}\n\n")
        }

        // 본문 변환
        structure.paragraphs.forEach { para ->
            builder.append("${para.text}\n\n")
        }

        // 리스트 변환
        structure.lists.forEach { list ->
            builder.append("- ${list.text}\n")
        }

        return builder.toString()
    }
}
```

#### 4.2 HeadingDetector
```kotlin
class HeadingDetector {
    fun detectLevel(fontSize: Float, baseFontSize: Float): Int {
        return when {
            fontSize >= baseFontSize * 2.0 -> 1
            fontSize >= baseFontSize * 1.5 -> 2
            fontSize >= baseFontSize * 1.3 -> 3
            fontSize >= baseFontSize * 1.1 -> 4
            else -> 0  // 제목 아님
        }
    }
}
```

#### 4.3 ListDetector
```kotlin
class ListDetector {
    fun detectListItem(text: String, indent: Float): Boolean {
        // 불릿 포인트 감지: -, •, ◦, ▪, 등
        // 번호 리스트 감지: 1., 2., a), b), 등
        // 들여쓰기 감지
    }

    fun getListLevel(indent: Float): Int {
        // 들여쓰기 깊이 계산
    }
}
```

#### 4.4 FormattingRules
```kotlin
data class FormattingRules(
    val preserveLineBreaks: Boolean = true,
    val convertBold: Boolean = true,
    val convertItalic: Boolean = true,
    val extractLinks: Boolean = true,
    val headingPrefix: String = "#",
    val bulletChar: String = "-"
)
```

---

### Phase 5: 파일 출력 구현 (1일)

#### 5.1 FileWriter 클래스
```kotlin
class FileWriter {
    fun write(content: String, outputPath: String): Result<Unit> {
        return try {
            File(outputPath).writeText(content, Charsets.UTF_8)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to write file: ${e.message}")
        }
    }

    fun writeWithImages(
        content: String,
        images: List<ImageData>,
        outputDir: String
    ): Result<Unit> {
        // 마크다운 파일 + 이미지 파일들 저장
        // 이미지 참조 업데이트
    }
}
```

#### 5.2 OutputFormatter
```kotlin
class OutputFormatter {
    fun formatMarkdown(content: String): String {
        // 빈 줄 정리
        // 연속된 공백 제거
        // 마크다운 문법 검증
    }
}
```

---

### Phase 6: 통합 및 에러 처리 (1-2일)

#### 6.1 Result 타입 (Sealed Class)
```kotlin
sealed class Result<out T> {
    data class Success<T>(val value: T) : Result<T>()
    data class Error(val message: String, val cause: Throwable? = null) : Result<Nothing>()

    inline fun <R> map(transform: (T) -> R): Result<R> {
        return when (this) {
            is Success -> Success(transform(value))
            is Error -> this
        }
    }

    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(value)
        return this
    }

    inline fun onError(action: (String) -> Unit): Result<T> {
        if (this is Error) action(message)
        return this
    }
}
```

#### 6.2 에러 처리 전략
- PDF 파일 없음 → 명확한 에러 메시지
- PDF 로드 실패 → 원인 출력 (손상된 파일, 암호화 등)
- 쓰기 권한 없음 → 권한 문제 안내
- 메모리 부족 → 청크 단위 처리 제안

#### 6.3 로깅
```kotlin
// 로깅 레벨
// DEBUG: 상세 처리 과정
// INFO: 주요 단계 진행 상황
// WARN: 경고 (일부 콘텐츠 변환 실패 등)
// ERROR: 치명적 오류
```

---

### Phase 7: 테스트 작성 (2일)

#### 7.1 단위 테스트
```kotlin
class PdfReaderTest {
    @Test
    fun `should load valid PDF document`() {
        // given
        val reader = PdfReader()
        val testFile = "test.pdf"

        // when
        val result = reader.loadDocument(testFile)

        // then
        assertTrue(result is Result.Success)
    }
}
```

#### 7.2 통합 테스트
```kotlin
class EndToEndTest {
    @Test
    fun `should convert PDF to Markdown`() {
        // 전체 변환 프로세스 테스트
    }
}
```

#### 7.3 테스트 커버리지 목표
- 핵심 로직: 80% 이상
- 전체 프로젝트: 70% 이상

---

### Phase 8: 빌드 및 배포 설정 (1일)

#### 8.1 Fat JAR 생성 (Shadow Plugin)
```kotlin
// build.gradle.kts
plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

tasks.shadowJar {
    archiveBaseName.set("pdf2md")
    archiveVersion.set(version.toString())
    archiveClassifier.set("")
    manifest {
        attributes["Main-Class"] = "com.pdf2md.MainKt"
    }
}
```

#### 8.2 실행 스크립트 생성
```bash
#!/bin/bash
# pdf2md.sh
java -jar pdf2md-1.0.0.jar "$@"
```

#### 8.3 Native Image (선택사항 - GraalVM)
- 시작 속도 향상
- 메모리 사용량 감소
- 배포 용량 증가

---

## 🎯 핵심 기술 세부사항

### 1. PDF 텍스트 추출 전략

#### 기본 접근법
```kotlin
fun extractTextWithPosition(document: PDDocument): List<TextElement> {
    val extractor = object : PDFTextStripper() {
        val elements = mutableListOf<TextElement>()

        override fun writeString(text: String, textPositions: List<TextPosition>) {
            textPositions.forEach { pos ->
                elements.add(TextElement(
                    text = pos.unicode,
                    fontSize = pos.fontSize,
                    fontName = pos.font.name,
                    x = pos.x,
                    y = pos.y,
                    pageNumber = currentPageNo
                ))
            }
        }
    }

    extractor.getText(document)
    return extractor.elements
}
```

#### 문제점 및 해결책

**문제 1: 다단 레이아웃**
- PDF가 2단 이상의 칼럼으로 구성된 경우
- 해결: Y 좌표 기반 정렬 → X 좌표로 칼럼 구분

**문제 2: 표 구조**
- 표의 셀 경계 인식
- 해결: 위치 기반 그리드 분석 또는 테두리 라인 감지

**문제 3: 폰트 임베딩**
- 일부 PDF는 폰트가 임베딩되지 않음
- 해결: 대체 폰트 매핑 또는 텍스트만 추출

### 2. 제목 감지 알고리즘

```kotlin
fun detectHeadings(elements: List<TextElement>): List<Heading> {
    // 1. 평균 폰트 크기 계산
    val avgFontSize = elements.map { it.fontSize }.average()

    // 2. 폰트 크기 임계값 설정
    val headingThreshold = avgFontSize * 1.2f

    // 3. 제목 후보 추출
    val candidates = elements.filter { it.fontSize > headingThreshold }

    // 4. 제목 레벨 할당
    return candidates.map { element ->
        val level = calculateHeadingLevel(element.fontSize, avgFontSize)
        Heading(level, element.text, element.pageNumber)
    }
}

fun calculateHeadingLevel(fontSize: Float, baseFontSize: Float): Int {
    val ratio = fontSize / baseFontSize
    return when {
        ratio >= 2.0 -> 1
        ratio >= 1.7 -> 2
        ratio >= 1.4 -> 3
        ratio >= 1.2 -> 4
        ratio >= 1.1 -> 5
        else -> 6
    }
}
```

### 3. 리스트 감지 알고리즘

```kotlin
fun detectLists(elements: List<TextElement>): List<ListItem> {
    val listPattern = Regex("""^(\s*)([-•◦▪▫]|\d+\.|[a-z]\))\s+(.+)$""")

    return elements.mapNotNull { element ->
        listPattern.find(element.text)?.let { match ->
            val (indent, marker, content) = match.destructured
            ListItem(
                level = indent.length / 2,  // 들여쓰기 2칸 = 1레벨
                marker = marker,
                text = content,
                pageNumber = element.pageNumber
            )
        }
    }
}
```

### 4. 이미지 추출

```kotlin
fun extractImages(document: PDDocument, outputDir: String): List<ImageData> {
    val images = mutableListOf<ImageData>()
    var imageCounter = 1

    document.pages.forEachIndexed { pageIndex, page ->
        val resources = page.resources

        resources.xObjectNames.forEach { name ->
            val xObject = resources.getXObject(name)

            if (xObject is PDImageXObject) {
                val imagePath = "$outputDir/image_${imageCounter}.${xObject.suffix}"
                ImageIO.write(xObject.image, xObject.suffix, File(imagePath))

                images.add(ImageData(
                    path = imagePath,
                    pageNumber = pageIndex + 1,
                    altText = "Image $imageCounter"
                ))

                imageCounter++
            }
        }
    }

    return images
}
```

---

## 📊 성능 고려사항

### 메모리 관리
- 큰 PDF (100MB+) 처리 시 메모리 부족 가능
- 해결: 페이지별 스트리밍 처리

```kotlin
fun convertLargePdf(document: PDDocument): String {
    val markdown = StringBuilder()

    document.pages.forEach { page ->
        val pageText = extractPageText(page)
        val pageMarkdown = convertToMarkdown(pageText)
        markdown.append(pageMarkdown)

        // 페이지 처리 후 메모리 해제
        page.clear()
    }

    return markdown.toString()
}
```

### 처리 속도
- 예상: 100페이지 PDF → 5-10초
- 병렬 처리 가능 시 성능 향상

---

## 🚀 향후 개선 사항

### v1.0 (MVP)
- [x] 기본 텍스트 추출
- [x] 제목 감지
- [x] 단락 구분
- [x] CLI 인터페이스

### v1.1
- [ ] 리스트 감지 개선
- [ ] 이미지 추출
- [ ] 표 변환

### v1.2
- [ ] 배치 처리
- [ ] 설정 파일 지원
- [ ] 플러그인 시스템

### v2.0
- [ ] OCR 지원 (스캔 PDF)
- [ ] GUI 버전
- [ ] 웹 서비스 API

---

## 📚 참고 자료

### PDFBox 문서
- https://pdfbox.apache.org/
- PDFTextStripper API
- PDImageXObject API

### Clikt 문서
- https://ajalt.github.io/clikt/
- Commands and subcommands
- Parameters and options

### 마크다운 사양
- CommonMark: https://commonmark.org/
- GFM (GitHub Flavored Markdown)

---

## ✅ 체크리스트

### 개발 시작 전
- [ ] Kotlin 설치 확인 (1.9+)
- [ ] Gradle 설치 확인 (8.x)
- [ ] IDE 설정 (IntelliJ IDEA 권장)
- [ ] Git 저장소 초기화

### 개발 중
- [ ] 단위 테스트 작성
- [ ] 코드 리뷰
- [ ] 문서화 (KDoc)
- [ ] 커밋 메시지 규칙 준수

### 릴리스 전
- [ ] 통합 테스트 통과
- [ ] 성능 테스트
- [ ] 메모리 누수 확인
- [ ] README 작성
- [ ] 라이선스 파일 추가
- [ ] 버전 태깅

---

**다음 문서**: [ARCHITECTURE.md](ARCHITECTURE.md) - 상세 아키텍처 설계
