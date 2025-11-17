# 세분화된 구현 계획

## 개요

전체 프로젝트를 작은 단위로 나누어 단계별로 구현합니다. 각 단계는 독립적으로 테스트 가능하며, 점진적으로 기능을 추가합니다.

---

## 🎯 Phase 0: 프로젝트 초기 설정 (1-2시간)

### Step 0.1: Gradle 프로젝트 기본 설정

**작업 내용**:
```bash
# 1. 디렉토리 구조 확인
pdf2markdown/
├── build.gradle.kts        # 생성 필요
├── settings.gradle.kts     # 생성 필요
├── gradle.properties       # 생성 필요
└── .gitignore             # 생성 필요
```

**파일 생성**:

1. **settings.gradle.kts**
```kotlin
rootProject.name = "pdf2markdown"
```

2. **gradle.properties**
```properties
kotlin.code.style=official
org.gradle.jvmargs=-Xmx2048m
```

3. **build.gradle.kts** (기본 구조만)
```kotlin
plugins {
    kotlin("jvm") version "1.9.21"
    application
}

group = "com.pdf2md"
version = "1.0.0"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)
}
```

**검증**:
```bash
./gradlew build
```

---

### Step 0.2: 의존성 추가

**build.gradle.kts에 의존성 추가**:
```kotlin
dependencies {
    // CLI
    implementation("com.github.ajalt.clikt:clikt:4.2.1")

    // PDF 처리
    implementation("org.apache.pdfbox:pdfbox:3.0.1")

    // 로깅
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("ch.qos.logback:logback-classic:1.4.11")

    // 테스트 - Kotest (Kotlin 네이티브)
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
    testImplementation("io.kotest:kotest-property:5.8.0")
}

tasks.test {
    useJUnitPlatform()
}
```

**검증**:
```bash
./gradlew dependencies --configuration runtimeClasspath
```

---

### Step 0.3: 기본 디렉토리 구조 생성

**디렉토리 생성**:
```bash
mkdir -p src/main/kotlin/com/pdf2md/{cli,application,domain/{pdf,converter},markdown/{ast,builder,renderer},infrastructure,common}
mkdir -p src/main/resources
mkdir -p src/test/kotlin/com/pdf2md/{markdown/{ast,builder,renderer},domain/{pdf,converter}}
mkdir -p src/test/resources
```

**최종 구조**:
```
src/
├── main/
│   ├── kotlin/
│   │   └── com/pdf2md/
│   │       ├── Main.kt
│   │       ├── cli/
│   │       ├── application/
│   │       ├── domain/
│   │       │   ├── pdf/
│   │       │   └── converter/
│   │       ├── markdown/
│   │       │   ├── ast/
│   │       │   ├── builder/
│   │       │   └── renderer/
│   │       ├── infrastructure/
│   │       └── common/
│   └── resources/
│       └── logback.xml
└── test/
    ├── kotlin/
    │   └── com/pdf2md/
    │       └── markdown/
    │           ├── ast/
    │           ├── builder/
    │           └── renderer/
    └── resources/
```

---

## 📦 Phase 1: Markdown AST 모듈 구현 (4-6시간)

### Step 1.1: 기본 인터페이스 및 데이터 클래스 정의

**파일**: `src/main/kotlin/com/pdf2md/markdown/ast/MarkdownNode.kt`

```kotlin
package com.pdf2md.markdown.ast

/**
 * 마크다운 AST의 기본 노드
 */
sealed interface MarkdownNode {
    val type: String
    val position: Position?
        get() = null
}

data class Position(
    val start: Point,
    val end: Point
)

data class Point(
    val line: Int,
    val column: Int,
    val offset: Int
)
```

**테스트**: `src/test/kotlin/com/pdf2md/markdown/ast/MarkdownNodeTest.kt`
```kotlin
package com.pdf2md.markdown.ast

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class MarkdownNodeTest : FunSpec({
    test("Position should be created correctly") {
        val position = Position(
            start = Point(1, 0, 0),
            end = Point(1, 10, 10)
        )

        position.start.line shouldBe 1
        position.end.offset shouldBe 10
    }
})
```

**검증**:
```bash
./gradlew test --tests "com.pdf2md.markdown.ast.MarkdownNodeTest"
```

---

### Step 1.2: 블록 노드 구현

**파일**: `src/main/kotlin/com/pdf2md/markdown/ast/BlockNode.kt`

```kotlin
package com.pdf2md.markdown.ast

sealed interface BlockNode : MarkdownNode

data class Document(
    val children: List<BlockNode> = emptyList(),
    override val position: Position? = null
) : MarkdownNode {
    override val type: String = "root"
}

data class Heading(
    val level: Int,
    val children: List<InlineNode>,
    override val position: Position? = null
) : BlockNode {
    override val type: String = "heading"

    init {
        require(level in 1..6) { "Heading level must be between 1 and 6, got $level" }
    }
}

data class Paragraph(
    val children: List<InlineNode>,
    override val position: Position? = null
) : BlockNode {
    override val type: String = "paragraph"
}

// ... 나머지 블록 노드들
```

**테스트**: `src/test/kotlin/com/pdf2md/markdown/ast/BlockNodeTest.kt`
```kotlin
package com.pdf2md.markdown.ast

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class BlockNodeTest : FunSpec({
    context("Heading") {
        test("should create valid heading") {
            val heading = Heading(1, listOf(Text("Title")))

            heading.type shouldBe "heading"
            heading.level shouldBe 1
            heading.children.size shouldBe 1
        }

        test("should reject invalid heading level") {
            shouldThrow<IllegalArgumentException> {
                Heading(0, listOf(Text("Invalid")))
            }

            shouldThrow<IllegalArgumentException> {
                Heading(7, listOf(Text("Invalid")))
            }
        }
    }

    context("Document") {
        test("should create empty document") {
            val doc = Document()

            doc.type shouldBe "root"
            doc.children.size shouldBe 0
        }

        test("should create document with children") {
            val doc = Document(
                children = listOf(
                    Heading(1, listOf(Text("Title"))),
                    Paragraph(listOf(Text("Content")))
                )
            )

            doc.children.size shouldBe 2
        }
    }
})
```

**진행 순서**:
1. ✅ Heading 구현 및 테스트
2. ✅ Paragraph 구현 및 테스트
3. ⬜ ListNode 구현 및 테스트
4. ⬜ CodeBlock 구현 및 테스트
5. ⬜ Blockquote 구현 및 테스트
6. ⬜ Table 구현 및 테스트

---

### Step 1.3: 인라인 노드 구현

**파일**: `src/main/kotlin/com/pdf2md/markdown/ast/InlineNode.kt`

```kotlin
package com.pdf2md.markdown.ast

sealed interface InlineNode : MarkdownNode

data class Text(
    val value: String,
    override val position: Position? = null
) : InlineNode {
    override val type: String = "text"
}

data class Strong(
    val children: List<InlineNode>,
    override val position: Position? = null
) : InlineNode {
    override val type: String = "strong"
}

// ... 나머지 인라인 노드들
```

**테스트**: `src/test/kotlin/com/pdf2md/markdown/ast/InlineNodeTest.kt`
```kotlin
package com.pdf2md.markdown.ast

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class InlineNodeTest : StringSpec({
    "Text node should store value" {
        val text = Text("Hello")

        text.value shouldBe "Hello"
        text.type shouldBe "text"
    }

    "Strong node should contain children" {
        val strong = Strong(listOf(Text("Bold")))

        strong.children.size shouldBe 1
        (strong.children[0] as Text).value shouldBe "Bold"
    }
})
```

**진행 순서**:
1. ✅ Text 구현 및 테스트
2. ✅ Strong 구현 및 테스트
3. ⬜ Emphasis 구현 및 테스트
4. ⬜ InlineCode 구현 및 테스트
5. ⬜ Link 구현 및 테스트
6. ⬜ Image 구현 및 테스트

---

## 🏗️ Phase 2: Markdown Builder 모듈 구현 (3-4시간)

### Step 2.1: DocumentBuilder 기본 구조

**파일**: `src/main/kotlin/com/pdf2md/markdown/builder/MarkdownBuilder.kt`

```kotlin
package com.pdf2md.markdown.builder

import com.pdf2md.markdown.ast.*

fun markdown(block: DocumentBuilder.() -> Unit): Document {
    return DocumentBuilder().apply(block).build()
}

class DocumentBuilder {
    private val children = mutableListOf<BlockNode>()

    fun heading(level: Int, text: String) {
        children.add(Heading(level, listOf(Text(text))))
    }

    fun paragraph(text: String) {
        children.add(Paragraph(listOf(Text(text))))
    }

    fun build(): Document = Document(children)
}
```

**테스트**: `src/test/kotlin/com/pdf2md/markdown/builder/DocumentBuilderTest.kt`
```kotlin
package com.pdf2md.markdown.builder

import com.pdf2md.markdown.ast.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class DocumentBuilderTest : FunSpec({
    test("should build simple document") {
        val doc = markdown {
            heading(1, "Title")
            paragraph("Content")
        }

        doc.children.size shouldBe 2
        doc.children[0].shouldBeInstanceOf<Heading>()
        doc.children[1].shouldBeInstanceOf<Paragraph>()
    }

    test("should build heading with correct level") {
        val doc = markdown {
            heading(2, "Subtitle")
        }

        val heading = doc.children[0] as Heading
        heading.level shouldBe 2
        (heading.children[0] as Text).value shouldBe "Subtitle"
    }
})
```

---

### Step 2.2: InlineBuilder 구현

**파일**: `src/main/kotlin/com/pdf2md/markdown/builder/InlineBuilder.kt`

```kotlin
package com.pdf2md.markdown.builder

import com.pdf2md.markdown.ast.*

class InlineBuilder {
    private val children = mutableListOf<InlineNode>()

    fun text(value: String) {
        children.add(Text(value))
    }

    fun strong(text: String) {
        children.add(Strong(listOf(Text(text))))
    }

    fun strong(block: InlineBuilder.() -> Unit) {
        val builder = InlineBuilder().apply(block)
        children.add(Strong(builder.build()))
    }

    fun emphasis(text: String) {
        children.add(Emphasis(listOf(Text(text))))
    }

    fun build(): List<InlineNode> = children
}

// DocumentBuilder에 추가
fun DocumentBuilder.paragraph(block: InlineBuilder.() -> Unit) {
    val builder = InlineBuilder().apply(block)
    children.add(Paragraph(builder.build()))
}

fun DocumentBuilder.heading(level: Int, block: InlineBuilder.() -> Unit) {
    val builder = InlineBuilder().apply(block)
    children.add(Heading(level, builder.build()))
}
```

**테스트**: `src/test/kotlin/com/pdf2md/markdown/builder/InlineBuilderTest.kt`
```kotlin
package com.pdf2md.markdown.builder

import com.pdf2md.markdown.ast.*
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class InlineBuilderTest : DescribeSpec({
    describe("InlineBuilder") {
        it("should build inline content") {
            val doc = markdown {
                paragraph {
                    text("Hello ")
                    strong("World")
                    text("!")
                }
            }

            val para = doc.children[0] as Paragraph
            para.children.size shouldBe 3
        }

        it("should support nested strong") {
            val doc = markdown {
                paragraph {
                    strong {
                        text("Bold ")
                        emphasis("and italic")
                    }
                }
            }

            val para = doc.children[0] as Paragraph
            val strong = para.children[0] as Strong
            strong.children.size shouldBe 2
        }
    }
})
```

---

### Step 2.3: ListBuilder 구현

**파일**: `src/main/kotlin/com/pdf2md/markdown/builder/ListBuilder.kt`

```kotlin
package com.pdf2md.markdown.builder

import com.pdf2md.markdown.ast.*

class ListBuilder(private val ordered: Boolean) {
    private val items = mutableListOf<ListItem>()

    fun item(text: String, checked: Boolean? = null) {
        items.add(ListItem(
            checked = checked,
            children = listOf(Paragraph(listOf(Text(text))))
        ))
    }

    fun item(checked: Boolean? = null, block: DocumentBuilder.() -> Unit) {
        val builder = DocumentBuilder().apply(block)
        items.add(ListItem(
            checked = checked,
            children = builder.children
        ))
    }

    fun build(): ListNode = ListNode(ordered, children = items)
}

// DocumentBuilder에 추가
fun DocumentBuilder.list(ordered: Boolean = false, block: ListBuilder.() -> Unit) {
    val builder = ListBuilder(ordered).apply(block)
    children.add(builder.build())
}
```

**테스트**: `src/test/kotlin/com/pdf2md/markdown/builder/ListBuilderTest.kt`

---

## 🎨 Phase 3: Markdown Renderer 구현 (4-5시간)

### Step 3.1: MarkdownRenderer 인터페이스

**파일**: `src/main/kotlin/com/pdf2md/markdown/renderer/MarkdownRenderer.kt`

```kotlin
package com.pdf2md.markdown.renderer

import com.pdf2md.markdown.ast.Document
import com.pdf2md.markdown.ast.MarkdownNode

interface MarkdownRenderer {
    fun render(document: Document): String
    fun renderNode(node: MarkdownNode): String
}

data class RenderOptions(
    val lineEnding: String = "\n",
    val bulletChar: String = "-",
    val orderedListMarker: String = ".",
    val emphasisChar: String = "*",
    val strongChar: String = "**",
    val codeFence: String = "```"
)
```

---

### Step 3.2: GfmRenderer 기본 구현

**파일**: `src/main/kotlin/com/pdf2md/markdown/renderer/GfmRenderer.kt`

```kotlin
package com.pdf2md.markdown.renderer

import com.pdf2md.markdown.ast.*

class GfmRenderer(
    private val options: RenderOptions = RenderOptions()
) : MarkdownRenderer {

    override fun render(document: Document): String {
        return document.children.joinToString(
            options.lineEnding + options.lineEnding
        ) { renderBlock(it) }
    }

    override fun renderNode(node: MarkdownNode): String {
        return when (node) {
            is BlockNode -> renderBlock(node)
            is InlineNode -> renderInline(node)
            is Document -> render(node)
            else -> ""
        }
    }

    private fun renderBlock(node: BlockNode): String {
        return when (node) {
            is Heading -> renderHeading(node)
            is Paragraph -> renderParagraph(node)
            // ... 나머지
        }
    }

    private fun renderHeading(heading: Heading): String {
        val prefix = "#".repeat(heading.level)
        val content = heading.children.joinToString("") { renderInline(it) }
        return "$prefix $content"
    }

    private fun renderParagraph(paragraph: Paragraph): String {
        return paragraph.children.joinToString("") { renderInline(it) }
    }

    private fun renderInline(node: InlineNode): String {
        return when (node) {
            is Text -> node.value
            is Strong -> "${options.strongChar}${node.children.joinToString("") { renderInline(it) }}${options.strongChar}"
            // ... 나머지
        }
    }
}
```

**테스트**: `src/test/kotlin/com/pdf2md/markdown/renderer/GfmRendererTest.kt`
```kotlin
package com.pdf2md.markdown.renderer

import com.pdf2md.markdown.builder.markdown
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class GfmRendererTest : FunSpec({
    val renderer = GfmRenderer()

    test("should render heading") {
        val doc = markdown {
            heading(1, "Title")
        }

        renderer.render(doc) shouldBe "# Title"
    }

    test("should render paragraph") {
        val doc = markdown {
            paragraph("Content")
        }

        renderer.render(doc) shouldBe "Content"
    }

    test("should render multiple blocks with line breaks") {
        val doc = markdown {
            heading(1, "Title")
            paragraph("Content")
        }

        renderer.render(doc) shouldBe "# Title\n\nContent"
    }
})
```

---

### Step 3.3: 점진적 기능 추가

**진행 순서**:
1. ✅ Heading, Paragraph 렌더링
2. ⬜ List 렌더링 (불릿, 번호)
3. ⬜ Inline formatting (Strong, Emphasis, Code)
4. ⬜ Link, Image 렌더링
5. ⬜ Code block 렌더링
6. ⬜ Blockquote 렌더링
7. ⬜ Table 렌더링

**각 기능마다**:
- 구현 → 테스트 작성 → 검증 → 다음 기능

---

## 🌐 Phase 4: HTML Renderer 구현 (3-4시간)

### Step 4.1: HtmlRenderer 기본 구조

**파일**: `src/main/kotlin/com/pdf2md/markdown/renderer/HtmlRenderer.kt`

```kotlin
package com.pdf2md.markdown.renderer

import com.pdf2md.markdown.ast.*

data class HtmlRenderOptions(
    val fullDocument: Boolean = false,
    val title: String? = null,
    val includeDefaultStyles: Boolean = true
)

class HtmlRenderer(
    private val options: HtmlRenderOptions = HtmlRenderOptions()
) : MarkdownRenderer {

    override fun render(document: Document): String {
        val body = document.children.joinToString("\n") { renderBlock(it) }

        return if (options.fullDocument) {
            buildFullDocument(body, options.title)
        } else {
            body
        }
    }

    // ... 구현
}
```

**테스트 주도 개발 (TDD) 방식**:
```kotlin
class HtmlRendererTest : FunSpec({
    // 테스트 먼저 작성
    test("should render heading as h1 tag") {
        val doc = markdown { heading(1, "Title") }
        val renderer = HtmlRenderer()

        renderer.render(doc) shouldBe "<h1>Title</h1>"
    }

    // 구현 → 테스트 통과 → 다음 기능
})
```

---

## 🔄 Phase 5: PDF to AST Converter 구현 (5-6시간)

### Step 5.1: Common 모듈 먼저 구현

**파일**: `src/main/kotlin/com/pdf2md/common/Result.kt`

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
}
```

**테스트**:
```kotlin
class ResultTest : FunSpec({
    test("Success should map value") {
        val result = Result.Success(10)
        val mapped = result.map { it * 2 }

        mapped shouldBe Result.Success(20)
    }

    test("Error should propagate") {
        val result = Result.Error("Failed")
        val mapped = result.map { it }

        mapped shouldBe result
    }
})
```

---

### Step 5.2: PDF DocumentStructure 정의

**파일**: `src/main/kotlin/com/pdf2md/domain/pdf/DocumentStructure.kt`

```kotlin
package com.pdf2md.domain.pdf

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

    // ... 나머지
}

data class DocumentMetadata(
    val title: String?,
    val author: String?
    // ... 나머지
)
```

---

### Step 5.3: PdfToMarkdownAstConverter 구현

**파일**: `src/main/kotlin/com/pdf2md/domain/converter/PdfToMarkdownAstConverter.kt`

```kotlin
package com.pdf2md.domain.converter

import com.pdf2md.domain.pdf.*
import com.pdf2md.markdown.ast.Document
import com.pdf2md.markdown.builder.markdown
import com.pdf2md.common.Result

class PdfToMarkdownAstConverter {
    fun convert(pdfStructure: DocumentStructure): Result<Document> {
        return try {
            val doc = markdown {
                pdfStructure.elements.forEach { element ->
                    when (element) {
                        is StructuredElement.Heading -> {
                            heading(element.level, element.text)
                        }
                        is StructuredElement.Paragraph -> {
                            paragraph(element.text)
                        }
                        // ... 나머지
                    }
                }
            }

            Result.Success(doc)
        } catch (e: Exception) {
            Result.Error("Failed to convert: ${e.message}", e)
        }
    }
}
```

**테스트**:
```kotlin
class PdfToMarkdownAstConverterTest : FunSpec({
    val converter = PdfToMarkdownAstConverter()

    test("should convert simple structure") {
        val pdfStructure = DocumentStructure(
            elements = listOf(
                StructuredElement.Heading(1, "Title", 1),
                StructuredElement.Paragraph("Content", 1)
            ),
            metadata = DocumentMetadata(null, null)
        )

        val result = converter.convert(pdfStructure)

        result.shouldBeInstanceOf<Result.Success<Document>>()
        val doc = (result as Result.Success).value
        doc.children.size shouldBe 2
    }
})
```

---

## 🔌 Phase 6: CLI 구현 (2-3시간)

### Step 6.1: Main 진입점

**파일**: `src/main/kotlin/com/pdf2md/Main.kt`

```kotlin
package com.pdf2md

import com.pdf2md.cli.Pdf2MdCommand

fun main(args: Array<String>) = Pdf2MdCommand().main(args)
```

---

### Step 6.2: Clikt 명령어 구조

**파일**: `src/main/kotlin/com/pdf2md/cli/Pdf2MdCommand.kt`

```kotlin
package com.pdf2md.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands

class Pdf2MdCommand : CliktCommand(
    name = "pdf2md",
    help = "Convert PDF to Markdown/HTML"
) {
    init {
        subcommands(ConvertCommand(), VersionCommand())
    }

    override fun run() = Unit
}
```

**파일**: `src/main/kotlin/com/pdf2md/cli/ConvertCommand.kt`

```kotlin
package com.pdf2md.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.choice

class ConvertCommand : CliktCommand(
    name = "convert",
    help = "Convert PDF to Markdown or HTML"
) {
    private val input by argument(help = "Input PDF file")
        .file(mustExist = true, canBeDir = false)

    private val output by option("-o", "--output")
        .file()

    private val format by option("--format")
        .choice("markdown", "html", "both")
        .default("markdown")

    override fun run() {
        echo("Converting ${input.name} to $format...")
        // TODO: 실제 변환 로직 호출
    }
}
```

---

## 📊 전체 일정 요약

| Phase | 작업 내용 | 예상 시간 | 체크포인트 |
|-------|----------|----------|-----------|
| 0 | 프로젝트 초기 설정 | 1-2h | `./gradlew build` 성공 |
| 1 | Markdown AST | 4-6h | 모든 노드 테스트 통과 |
| 2 | Markdown Builder | 3-4h | DSL 빌더 테스트 통과 |
| 3 | Markdown Renderer | 4-5h | GFM 렌더링 테스트 통과 |
| 4 | HTML Renderer | 3-4h | HTML 렌더링 테스트 통과 |
| 5 | PDF Converter | 5-6h | 통합 테스트 통과 |
| 6 | CLI | 2-3h | CLI 실행 가능 |
| **합계** | | **22-30h** | **완전한 동작** |

---

## ✅ 각 단계별 완료 기준

### Phase 완료 체크리스트

**Phase 0 완료**:
- [ ] `./gradlew build` 성공
- [ ] 모든 의존성 다운로드 완료
- [ ] 디렉토리 구조 생성 완료

**Phase 1 완료**:
- [ ] 모든 블록 노드 타입 구현
- [ ] 모든 인라인 노드 타입 구현
- [ ] 테스트 커버리지 > 80%
- [ ] `./gradlew test` 모두 통과

**Phase 2 완료**:
- [ ] DocumentBuilder 동작
- [ ] InlineBuilder 동작
- [ ] ListBuilder 동작
- [ ] 중첩 빌더 테스트 통과

**Phase 3 완료**:
- [ ] GfmRenderer 모든 노드 렌더링
- [ ] CommonMarkRenderer 구현
- [ ] 렌더링 결과가 예상과 일치

**Phase 4 완료**:
- [ ] HtmlRenderer Fragment 모드
- [ ] HtmlRenderer Full Document 모드
- [ ] HTML 이스케이프 정상 동작

**Phase 5 완료**:
- [ ] PDF → AST 변환 성공
- [ ] 에러 처리 동작
- [ ] 통합 테스트 통과

**Phase 6 완료**:
- [ ] CLI 명령어 실행
- [ ] PDF 파일 변환 성공
- [ ] 결과 파일 생성 확인

---

## 🎯 다음 액션

**지금 바로 시작할 수 있는 단계**:

1. **Step 0.1**: Gradle 파일 생성 (10분)
2. **Step 0.2**: 의존성 추가 (5분)
3. **Step 0.3**: 디렉토리 생성 (5분)
4. **Step 1.1**: MarkdownNode.kt 작성 + 테스트 (30분)

**시작하시겠습니까?** 🚀
