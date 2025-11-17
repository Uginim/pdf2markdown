# Markdown 모듈 설계

## 개요

마크다운 처리를 4개의 독립적인 모듈로 분리하여 재사용성과 테스트 가능성을 높입니다.

```
┌─────────────────────────────────────────────────────────────┐
│                  PDF Processing                             │
│          (PdfReader → DocumentStructure)                    │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│       Module 3: PDF to Markdown AST Converter               │
│      DocumentStructure → Markdown AST                       │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│              Module 1: Markdown AST (Data Model)            │
│  ┌────────┐  ┌──────────┐  ┌────────┐  ┌──────────┐       │
│  │ Document│  │ Heading  │  │  Para  │  │   List   │       │
│  └────────┘  └──────────┘  └────────┘  └──────────┘       │
└─────────────────────┬───────────────────────────────────────┘
                      │
        ┌─────────────┴─────────────┐
        │                           │
        ▼                           ▼
┌───────────────────┐     ┌───────────────────┐
│Module 2: Builder  │     │ Module 4: Renderer│
│                   │     │                   │
│ markdown {        │     │ • PlainText (MD)  │
│   heading(...)    │     │ • HTML            │
│ }                 │     │                   │
└───────────────────┘     └───────────────────┘
        │                           │
        └─────────────┬─────────────┘
                      ▼
              Output (String)
```

---

## 모듈 1: Markdown AST (데이터 모델)

### 설계 원칙

1. **mdast 스펙 준수**: [mdast](https://github.com/syntax-tree/mdast) 표준을 참고
2. **불변성(Immutability)**: 모든 노드는 불변 객체
3. **타입 안전성**: Sealed class로 타입 보장
4. **확장 가능성**: 새로운 노드 타입 추가 용이

### 패키지 구조

```
com.pdf2md.markdown.ast/
├── MarkdownNode.kt          # 기본 노드 인터페이스
├── Document.kt              # 문서 루트
├── BlockNode.kt             # 블록 레벨 노드
├── InlineNode.kt            # 인라인 노드
└── NodeTypes.kt             # 모든 노드 타입 정의
```

### 핵심 인터페이스

```kotlin
package com.pdf2md.markdown.ast

/**
 * 마크다운 AST의 기본 노드
 *
 * mdast의 Node 인터페이스를 기반으로 함
 * @see https://github.com/syntax-tree/mdast#nodes
 */
sealed interface MarkdownNode {
    /**
     * 노드 타입 (예: "heading", "paragraph", "list")
     */
    val type: String

    /**
     * 부모 노드 참조 (선택적)
     */
    val parent: MarkdownNode?
        get() = null

    /**
     * 노드의 위치 정보 (선택적)
     */
    val position: Position?
        get() = null
}

/**
 * 위치 정보
 */
data class Position(
    val start: Point,
    val end: Point
)

data class Point(
    val line: Int,      // 1부터 시작
    val column: Int,    // 1부터 시작
    val offset: Int     // 0부터 시작
)
```

---

### 블록 레벨 노드

```kotlin
package com.pdf2md.markdown.ast

/**
 * 블록 레벨 노드의 기본 인터페이스
 */
sealed interface BlockNode : MarkdownNode

/**
 * 마크다운 문서의 루트 노드
 *
 * @property children 문서의 최상위 블록 노드들
 */
data class Document(
    val children: List<BlockNode> = emptyList(),
    override val position: Position? = null
) : MarkdownNode {
    override val type: String = "root"
}

/**
 * 제목 노드
 *
 * @property level 제목 레벨 (1-6)
 * @property children 인라인 콘텐츠 (보통 텍스트)
 */
data class Heading(
    val level: Int,  // 1-6
    val children: List<InlineNode>,
    override val position: Position? = null
) : BlockNode {
    override val type: String = "heading"

    init {
        require(level in 1..6) { "Heading level must be between 1 and 6" }
    }
}

/**
 * 단락 노드
 *
 * @property children 인라인 콘텐츠
 */
data class Paragraph(
    val children: List<InlineNode>,
    override val position: Position? = null
) : BlockNode {
    override val type: String = "paragraph"
}

/**
 * 리스트 노드
 *
 * @property ordered 번호 리스트 여부
 * @property start 시작 번호 (번호 리스트인 경우)
 * @property spread 리스트 아이템 간 빈 줄 여부
 * @property children 리스트 아이템들
 */
data class ListNode(
    val ordered: Boolean,
    val start: Int? = null,
    val spread: Boolean = false,
    val children: List<ListItem>,
    override val position: Position? = null
) : BlockNode {
    override val type: String = "list"
}

/**
 * 리스트 아이템 노드
 *
 * @property checked 체크박스 상태 (null, true, false)
 * @property spread 아이템 내부에 빈 줄 여부
 * @property children 블록 노드들 (보통 단락)
 */
data class ListItem(
    val checked: Boolean? = null,
    val spread: Boolean = false,
    val children: List<BlockNode>,
    override val position: Position? = null
) : MarkdownNode {
    override val type: String = "listItem"
}

/**
 * 코드 블록 노드
 *
 * @property lang 프로그래밍 언어
 * @property meta 추가 메타데이터
 * @property value 코드 내용
 */
data class CodeBlock(
    val lang: String? = null,
    val meta: String? = null,
    val value: String,
    override val position: Position? = null
) : BlockNode {
    override val type: String = "code"
}

/**
 * 인용구 노드
 *
 * @property children 블록 노드들
 */
data class Blockquote(
    val children: List<BlockNode>,
    override val position: Position? = null
) : BlockNode {
    override val type: String = "blockquote"
}

/**
 * 수평선 노드
 */
data class ThematicBreak(
    override val position: Position? = null
) : BlockNode {
    override val type: String = "thematicBreak"
}

/**
 * 표 노드
 *
 * @property align 각 컬럼의 정렬 (null, left, right, center)
 * @property children 표 행들
 */
data class Table(
    val align: List<Alignment?>,
    val children: List<TableRow>,
    override val position: Position? = null
) : BlockNode {
    override val type: String = "table"
}

enum class Alignment {
    LEFT, RIGHT, CENTER
}

/**
 * 표 행 노드
 *
 * @property children 표 셀들
 */
data class TableRow(
    val children: List<TableCell>,
    override val position: Position? = null
) : MarkdownNode {
    override val type: String = "tableRow"
}

/**
 * 표 셀 노드
 *
 * @property children 인라인 콘텐츠
 */
data class TableCell(
    val children: List<InlineNode>,
    override val position: Position? = null
) : MarkdownNode {
    override val type: String = "tableCell"
}

/**
 * HTML 블록 노드
 *
 * @property value HTML 내용
 */
data class HtmlBlock(
    val value: String,
    override val position: Position? = null
) : BlockNode {
    override val type: String = "html"
}
```

---

### 인라인 노드

```kotlin
package com.pdf2md.markdown.ast

/**
 * 인라인 노드의 기본 인터페이스
 */
sealed interface InlineNode : MarkdownNode

/**
 * 텍스트 노드
 *
 * @property value 텍스트 내용
 */
data class Text(
    val value: String,
    override val position: Position? = null
) : InlineNode {
    override val type: String = "text"
}

/**
 * 강조 노드 (이탤릭)
 *
 * @property children 인라인 콘텐츠
 */
data class Emphasis(
    val children: List<InlineNode>,
    override val position: Position? = null
) : InlineNode {
    override val type: String = "emphasis"
}

/**
 * 강한 강조 노드 (볼드)
 *
 * @property children 인라인 콘텐츠
 */
data class Strong(
    val children: List<InlineNode>,
    override val position: Position? = null
) : InlineNode {
    override val type: String = "strong"
}

/**
 * 인라인 코드 노드
 *
 * @property value 코드 내용
 */
data class InlineCode(
    val value: String,
    override val position: Position? = null
) : InlineNode {
    override val type: String = "inlineCode"
}

/**
 * 링크 노드
 *
 * @property url 링크 URL
 * @property title 링크 타이틀 (선택적)
 * @property children 인라인 콘텐츠 (링크 텍스트)
 */
data class Link(
    val url: String,
    val title: String? = null,
    val children: List<InlineNode>,
    override val position: Position? = null
) : InlineNode {
    override val type: String = "link"
}

/**
 * 이미지 노드
 *
 * @property url 이미지 URL
 * @property title 이미지 타이틀 (선택적)
 * @property alt 대체 텍스트
 */
data class Image(
    val url: String,
    val title: String? = null,
    val alt: String,
    override val position: Position? = null
) : InlineNode {
    override val type: String = "image"
}

/**
 * 줄바꿈 노드
 */
data class Break(
    override val position: Position? = null
) : InlineNode {
    override val type: String = "break"
}

/**
 * 인라인 HTML 노드
 *
 * @property value HTML 내용
 */
data class InlineHtml(
    val value: String,
    override val position: Position? = null
) : InlineNode {
    override val type: String = "html"
}

/**
 * 취소선 노드 (GFM)
 *
 * @property children 인라인 콘텐츠
 */
data class Delete(
    val children: List<InlineNode>,
    override val position: Position? = null
) : InlineNode {
    override val type: String = "delete"
}
```

---

## 모듈 2: Markdown Builder (AST 생성)

### 설계 원칙

1. **DSL 스타일**: Kotlin의 Type-Safe Builder 활용
2. **간결성**: 읽기 쉽고 작성하기 쉬운 API
3. **타입 안전**: 컴파일 타임 검증

### 패키지 구조

```
com.pdf2md.markdown.builder/
├── MarkdownBuilder.kt       # DSL 진입점
├── BlockBuilder.kt          # 블록 빌더
└── InlineBuilder.kt         # 인라인 빌더
```

### 구현

```kotlin
package com.pdf2md.markdown.builder

import com.pdf2md.markdown.ast.*

/**
 * 마크다운 문서 DSL 진입점
 *
 * 사용 예:
 * ```kotlin
 * val doc = markdown {
 *     heading(1, "Title")
 *     paragraph {
 *         text("Hello ")
 *         strong("World")
 *     }
 * }
 * ```
 */
fun markdown(block: DocumentBuilder.() -> Unit): Document {
    return DocumentBuilder().apply(block).build()
}

/**
 * 문서 빌더
 */
class DocumentBuilder {
    private val children = mutableListOf<BlockNode>()

    fun heading(level: Int, text: String) {
        children.add(Heading(level, listOf(Text(text))))
    }

    fun heading(level: Int, block: InlineBuilder.() -> Unit) {
        val builder = InlineBuilder().apply(block)
        children.add(Heading(level, builder.build()))
    }

    fun paragraph(text: String) {
        children.add(Paragraph(listOf(Text(text))))
    }

    fun paragraph(block: InlineBuilder.() -> Unit) {
        val builder = InlineBuilder().apply(block)
        children.add(Paragraph(builder.build()))
    }

    fun list(ordered: Boolean = false, block: ListBuilder.() -> Unit) {
        val builder = ListBuilder(ordered).apply(block)
        children.add(builder.build())
    }

    fun codeBlock(code: String, lang: String? = null) {
        children.add(CodeBlock(lang = lang, value = code))
    }

    fun blockquote(block: DocumentBuilder.() -> Unit) {
        val builder = DocumentBuilder().apply(block)
        children.add(Blockquote(builder.children))
    }

    fun thematicBreak() {
        children.add(ThematicBreak())
    }

    fun table(block: TableBuilder.() -> Unit) {
        val builder = TableBuilder().apply(block)
        children.add(builder.build())
    }

    fun html(value: String) {
        children.add(HtmlBlock(value))
    }

    fun build(): Document = Document(children)
}

/**
 * 인라인 콘텐츠 빌더
 */
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

    fun emphasis(block: InlineBuilder.() -> Unit) {
        val builder = InlineBuilder().apply(block)
        children.add(Emphasis(builder.build()))
    }

    fun code(value: String) {
        children.add(InlineCode(value))
    }

    fun link(url: String, text: String, title: String? = null) {
        children.add(Link(url, title, listOf(Text(text))))
    }

    fun link(url: String, title: String? = null, block: InlineBuilder.() -> Unit) {
        val builder = InlineBuilder().apply(block)
        children.add(Link(url, title, builder.build()))
    }

    fun image(url: String, alt: String, title: String? = null) {
        children.add(Image(url, title, alt))
    }

    fun lineBreak() {
        children.add(Break())
    }

    fun delete(text: String) {
        children.add(Delete(listOf(Text(text))))
    }

    fun html(value: String) {
        children.add(InlineHtml(value))
    }

    fun build(): List<InlineNode> = children
}

/**
 * 리스트 빌더
 */
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

/**
 * 표 빌더
 */
class TableBuilder {
    private val rows = mutableListOf<TableRow>()
    private val alignments = mutableListOf<Alignment?>()

    fun alignment(vararg align: Alignment?) {
        alignments.addAll(align)
    }

    fun row(block: TableRowBuilder.() -> Unit) {
        val builder = TableRowBuilder().apply(block)
        rows.add(builder.build())
    }

    fun build(): Table = Table(alignments, rows)
}

class TableRowBuilder {
    private val cells = mutableListOf<TableCell>()

    fun cell(text: String) {
        cells.add(TableCell(listOf(Text(text))))
    }

    fun cell(block: InlineBuilder.() -> Unit) {
        val builder = InlineBuilder().apply(block)
        cells.add(TableCell(builder.build()))
    }

    fun build(): TableRow = TableRow(cells)
}
```

### 사용 예제

```kotlin
// 예제 1: 간단한 문서
val doc = markdown {
    heading(1, "My Document")
    paragraph("This is a simple paragraph.")
    list {
        item("First item")
        item("Second item")
        item("Third item")
    }
}

// 예제 2: 복잡한 인라인 포맷팅
val doc2 = markdown {
    heading(1) {
        text("Welcome to ")
        strong("Kotlin")
    }

    paragraph {
        text("This is ")
        emphasis("italic")
        text(" and this is ")
        strong("bold")
        text(".")
    }

    paragraph {
        text("Check out ")
        link("https://kotlinlang.org", "Kotlin")
        text("!")
    }
}

// 예제 3: 코드 블록과 표
val doc3 = markdown {
    heading(2, "Code Example")

    codeBlock("""
        fun main() {
            println("Hello, World!")
        }
    """.trimIndent(), lang = "kotlin")

    heading(2, "Comparison Table")

    table {
        alignment(Alignment.LEFT, Alignment.CENTER, Alignment.RIGHT)

        row {
            cell("Feature")
            cell("Kotlin")
            cell("Java")
        }

        row {
            cell("Null Safety")
            cell("✓")
            cell("✗")
        }
    }
}

// 예제 4: 체크리스트
val doc4 = markdown {
    heading(2, "TODO List")

    list {
        item("Write documentation", checked = true)
        item("Implement tests", checked = false)
        item("Deploy to production", checked = false)
    }
}
```

---

## 모듈 3: Markdown Renderer (문자열 출력)

### 설계 원칙

1. **포맷 독립성**: 다양한 마크다운 포맷 지원 (GitHub, CommonMark 등)
2. **설정 가능**: 렌더링 옵션 제공
3. **확장 가능**: 커스텀 렌더러 작성 가능

### 패키지 구조

```
com.pdf2md.markdown.renderer/
├── MarkdownRenderer.kt      # 렌더러 인터페이스
├── GfmRenderer.kt           # GitHub Flavored Markdown
├── CommonMarkRenderer.kt    # CommonMark
└── RenderOptions.kt         # 렌더링 옵션
```

### 구현

```kotlin
package com.pdf2md.markdown.renderer

import com.pdf2md.markdown.ast.*

/**
 * 렌더링 옵션
 */
data class RenderOptions(
    /** 줄바꿈 문자 */
    val lineEnding: String = "\n",

    /** 리스트 마커 */
    val bulletChar: String = "-",

    /** 번호 리스트 마커 */
    val orderedListMarker: String = ".",

    /** 강조 문자 */
    val emphasisChar: String = "*",

    /** 강한 강조 문자 */
    val strongChar: String = "**",

    /** 코드 펜스 문자 */
    val codeFence: String = "```",

    /** 최대 줄 길이 (0 = 제한 없음) */
    val maxLineLength: Int = 0,

    /** 테이블 셀 패딩 */
    val tableCellPadding: Boolean = true,

    /** HTML 이스케이프 여부 */
    val escapeHtml: Boolean = false
)

/**
 * 마크다운 렌더러 인터페이스
 */
interface MarkdownRenderer {
    /**
     * AST를 마크다운 문자열로 렌더링
     */
    fun render(document: Document): String

    /**
     * 개별 노드 렌더링
     */
    fun renderNode(node: MarkdownNode): String
}

/**
 * GitHub Flavored Markdown 렌더러
 */
class GfmRenderer(
    private val options: RenderOptions = RenderOptions()
) : MarkdownRenderer {

    override fun render(document: Document): String {
        return document.children.joinToString(options.lineEnding + options.lineEnding) {
            renderBlock(it)
        }
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
            is ListNode -> renderList(node)
            is CodeBlock -> renderCodeBlock(node)
            is Blockquote -> renderBlockquote(node)
            is ThematicBreak -> renderThematicBreak()
            is Table -> renderTable(node)
            is HtmlBlock -> node.value
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

    private fun renderList(list: ListNode): String {
        return list.children.mapIndexed { index, item ->
            renderListItem(item, list.ordered, index + (list.start ?: 1))
        }.joinToString(options.lineEnding)
    }

    private fun renderListItem(item: ListItem, ordered: Boolean, number: Int): String {
        val marker = if (ordered) {
            "$number${options.orderedListMarker}"
        } else {
            options.bulletChar
        }

        val checkbox = when (item.checked) {
            true -> "[x] "
            false -> "[ ] "
            null -> ""
        }

        val content = item.children.joinToString(options.lineEnding) {
            when (it) {
                is Paragraph -> it.children.joinToString("") { inline -> renderInline(inline) }
                else -> renderBlock(it)
            }
        }

        return "$marker $checkbox$content"
    }

    private fun renderCodeBlock(code: CodeBlock): String {
        val lang = code.lang ?: ""
        return "${options.codeFence}$lang${options.lineEnding}${code.value}${options.lineEnding}${options.codeFence}"
    }

    private fun renderBlockquote(quote: Blockquote): String {
        return quote.children.joinToString(options.lineEnding) { block ->
            "> ${renderBlock(block)}"
        }
    }

    private fun renderThematicBreak(): String {
        return "---"
    }

    private fun renderTable(table: Table): String {
        val result = StringBuilder()

        // 헤더
        if (table.children.isNotEmpty()) {
            result.append(renderTableRow(table.children[0]))
            result.append(options.lineEnding)

            // 구분선
            val separator = table.align.joinToString("|", "|", "|") { align ->
                when (align) {
                    Alignment.LEFT -> ":---"
                    Alignment.RIGHT -> "---:"
                    Alignment.CENTER -> ":---:"
                    null -> "---"
                }
            }
            result.append(separator)
            result.append(options.lineEnding)

            // 본문 행들
            table.children.drop(1).forEach { row ->
                result.append(renderTableRow(row))
                result.append(options.lineEnding)
            }
        }

        return result.toString().trimEnd()
    }

    private fun renderTableRow(row: TableRow): String {
        val cells = row.children.joinToString(" | ") { cell ->
            val content = cell.children.joinToString("") { renderInline(it) }
            if (options.tableCellPadding) " $content " else content
        }
        return "| $cells |"
    }

    private fun renderInline(node: InlineNode): String {
        return when (node) {
            is Text -> escapeText(node.value)
            is Strong -> "${options.strongChar}${node.children.joinToString("") { renderInline(it) }}${options.strongChar}"
            is Emphasis -> "${options.emphasisChar}${node.children.joinToString("") { renderInline(it) }}${options.emphasisChar}"
            is InlineCode -> "`${node.value}`"
            is Link -> renderLink(node)
            is Image -> renderImage(node)
            is Break -> "  ${options.lineEnding}"
            is InlineHtml -> node.value
            is Delete -> "~~${node.children.joinToString("") { renderInline(it) }}~~"
        }
    }

    private fun renderLink(link: Link): String {
        val text = link.children.joinToString("") { renderInline(it) }
        val title = link.title?.let { " \"$it\"" } ?: ""
        return "[$text](${link.url}$title)"
    }

    private fun renderImage(image: Image): String {
        val title = image.title?.let { " \"$it\"" } ?: ""
        return "![${image.alt}](${image.url}$title)"
    }

    private fun escapeText(text: String): String {
        if (!options.escapeHtml) return text

        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }
}

/**
 * CommonMark 렌더러
 */
class CommonMarkRenderer(
    options: RenderOptions = RenderOptions()
) : GfmRenderer(options) {
    // CommonMark는 GFM의 서브셋이므로 대부분 동일
    // 필요시 특정 기능 오버라이드
}

/**
 * HTML 렌더러
 *
 * Markdown AST를 HTML로 변환합니다.
 */
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
            is ListNode -> renderList(node)
            is CodeBlock -> renderCodeBlock(node)
            is Blockquote -> renderBlockquote(node)
            is ThematicBreak -> renderThematicBreak()
            is Table -> renderTable(node)
            is HtmlBlock -> node.value
        }
    }

    private fun renderHeading(heading: Heading): String {
        val content = heading.children.joinToString("") { renderInline(it) }
        val tag = "h${heading.level}"
        return "<$tag>$content</$tag>"
    }

    private fun renderParagraph(paragraph: Paragraph): String {
        val content = paragraph.children.joinToString("") { renderInline(it) }
        return "<p>$content</p>"
    }

    private fun renderList(list: ListNode): String {
        val tag = if (list.ordered) "ol" else "ul"
        val items = list.children.joinToString("\n") { renderListItem(it) }
        val startAttr = if (list.ordered && list.start != null && list.start != 1) {
            " start=\"${list.start}\""
        } else ""

        return "<$tag$startAttr>\n$items\n</$tag>"
    }

    private fun renderListItem(item: ListItem): String {
        val checkbox = when (item.checked) {
            true -> "<input type=\"checkbox\" checked disabled> "
            false -> "<input type=\"checkbox\" disabled> "
            null -> ""
        }

        val content = item.children.joinToString("\n") {
            when (it) {
                is Paragraph -> it.children.joinToString("") { inline -> renderInline(inline) }
                else -> renderBlock(it)
            }
        }

        return "  <li>$checkbox$content</li>"
    }

    private fun renderCodeBlock(code: CodeBlock): String {
        val lang = code.lang?.let { " class=\"language-$it\"" } ?: ""
        val escaped = escapeHtml(code.value)
        return "<pre><code$lang>$escaped</code></pre>"
    }

    private fun renderBlockquote(quote: Blockquote): String {
        val content = quote.children.joinToString("\n") { renderBlock(it) }
        return "<blockquote>\n$content\n</blockquote>"
    }

    private fun renderThematicBreak(): String {
        return "<hr>"
    }

    private fun renderTable(table: Table): String {
        if (table.children.isEmpty()) return ""

        val result = StringBuilder("<table>\n")

        // 헤더
        result.append("  <thead>\n")
        result.append("    ${renderTableRow(table.children[0], table.align, isHeader = true)}\n")
        result.append("  </thead>\n")

        // 본문
        if (table.children.size > 1) {
            result.append("  <tbody>\n")
            table.children.drop(1).forEach { row ->
                result.append("    ${renderTableRow(row, table.align, isHeader = false)}\n")
            }
            result.append("  </tbody>\n")
        }

        result.append("</table>")
        return result.toString()
    }

    private fun renderTableRow(
        row: TableRow,
        alignments: List<Alignment?>,
        isHeader: Boolean
    ): String {
        val tag = if (isHeader) "th" else "td"
        val cells = row.children.mapIndexed { index, cell ->
            val align = alignments.getOrNull(index)
            val alignAttr = when (align) {
                Alignment.LEFT -> " align=\"left\""
                Alignment.RIGHT -> " align=\"right\""
                Alignment.CENTER -> " align=\"center\""
                null -> ""
            }
            val content = cell.children.joinToString("") { renderInline(it) }
            "<$tag$alignAttr>$content</$tag>"
        }.joinToString("")

        return "<tr>$cells</tr>"
    }

    private fun renderInline(node: InlineNode): String {
        return when (node) {
            is Text -> escapeHtml(node.value)
            is Strong -> "<strong>${node.children.joinToString("") { renderInline(it) }}</strong>"
            is Emphasis -> "<em>${node.children.joinToString("") { renderInline(it) }}</em>"
            is InlineCode -> "<code>${escapeHtml(node.value)}</code>"
            is Link -> renderLink(node)
            is Image -> renderImage(node)
            is Break -> "<br>"
            is InlineHtml -> node.value
            is Delete -> "<del>${node.children.joinToString("") { renderInline(it) }}</del>"
        }
    }

    private fun renderLink(link: Link): String {
        val text = link.children.joinToString("") { renderInline(it) }
        val titleAttr = link.title?.let { " title=\"${escapeHtml(it)}\"" } ?: ""
        return "<a href=\"${escapeHtml(link.url)}\"$titleAttr>$text</a>"
    }

    private fun renderImage(image: Image): String {
        val titleAttr = image.title?.let { " title=\"${escapeHtml(it)}\"" } ?: ""
        return "<img src=\"${escapeHtml(image.url)}\" alt=\"${escapeHtml(image.alt)}\"$titleAttr>"
    }

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    private fun buildFullDocument(body: String, title: String?): String {
        return """
            <!DOCTYPE html>
            <html lang="ko">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>${title ?: "Document"}</title>
                ${if (options.includeDefaultStyles) defaultStyles() else ""}
            </head>
            <body>
                $body
            </body>
            </html>
        """.trimIndent()
    }

    private fun defaultStyles(): String {
        return """
            <style>
                body {
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial, sans-serif;
                    line-height: 1.6;
                    max-width: 800px;
                    margin: 0 auto;
                    padding: 20px;
                    color: #333;
                }
                h1, h2, h3, h4, h5, h6 {
                    margin-top: 24px;
                    margin-bottom: 16px;
                    font-weight: 600;
                    line-height: 1.25;
                }
                h1 { font-size: 2em; border-bottom: 1px solid #eaecef; padding-bottom: 0.3em; }
                h2 { font-size: 1.5em; border-bottom: 1px solid #eaecef; padding-bottom: 0.3em; }
                h3 { font-size: 1.25em; }
                code {
                    background-color: #f6f8fa;
                    padding: 0.2em 0.4em;
                    border-radius: 3px;
                    font-family: monospace;
                }
                pre {
                    background-color: #f6f8fa;
                    padding: 16px;
                    border-radius: 6px;
                    overflow-x: auto;
                }
                pre code {
                    background-color: transparent;
                    padding: 0;
                }
                blockquote {
                    border-left: 4px solid #dfe2e5;
                    padding-left: 16px;
                    color: #6a737d;
                    margin-left: 0;
                }
                table {
                    border-collapse: collapse;
                    width: 100%;
                    margin: 16px 0;
                }
                th, td {
                    border: 1px solid #dfe2e5;
                    padding: 6px 13px;
                }
                th {
                    background-color: #f6f8fa;
                    font-weight: 600;
                }
                img {
                    max-width: 100%;
                    height: auto;
                }
                a {
                    color: #0366d6;
                    text-decoration: none;
                }
                a:hover {
                    text-decoration: underline;
                }
            </style>
        """.trimIndent()
    }
}

/**
 * HTML 렌더링 옵션
 */
data class HtmlRenderOptions(
    /** 완전한 HTML 문서 생성 여부 (html, head, body 태그 포함) */
    val fullDocument: Boolean = false,

    /** 문서 제목 (fullDocument가 true일 때만 사용) */
    val title: String? = null,

    /** 기본 CSS 스타일 포함 여부 */
    val includeDefaultStyles: Boolean = true,

    /** 커스텀 CSS */
    val customCss: String? = null
)
```

### 사용 예제

```kotlin
// 1. Markdown 렌더러 (Plain Text)
val doc = markdown {
    heading(1, "Title")
    paragraph("Content")
}

val mdRenderer = GfmRenderer()
val markdown = mdRenderer.render(doc)
println(markdown)
// 출력:
// # Title
//
// Content

// 2. HTML 렌더러 (Fragment)
val htmlRenderer = HtmlRenderer()
val htmlFragment = htmlRenderer.render(doc)
println(htmlFragment)
// 출력:
// <h1>Title</h1>
// <p>Content</p>

// 3. HTML 렌더러 (Full Document)
val fullHtmlRenderer = HtmlRenderer(
    HtmlRenderOptions(
        fullDocument = true,
        title = "My Document",
        includeDefaultStyles = true
    )
)
val fullHtml = fullHtmlRenderer.render(doc)
// 출력: 완전한 HTML 문서 (<!DOCTYPE html>...)

// 4. 커스텀 옵션 (Markdown)
val customRenderer = GfmRenderer(
    RenderOptions(
        bulletChar = "*",
        strongChar = "__",
        lineEnding = "\r\n"
    )
)
val markdown2 = customRenderer.render(doc)
```

---

## 모듈 3: PDF to Markdown AST Converter

### 책임

PDF 문서 구조를 Markdown AST로 변환하는 전용 모듈입니다.

### 패키지 구조

```
com.pdf2md.converter/
├── PdfToMarkdownAstConverter.kt     # 메인 변환기
├── ElementMapper.kt                 # 요소 매핑 로직
└── ListGrouper.kt                   # 리스트 그룹화
```

### 구현

```kotlin
package com.pdf2md.domain.converter

import com.pdf2md.domain.pdf.DocumentStructure
import com.pdf2md.domain.pdf.StructuredElement
import com.pdf2md.markdown.ast.*
import com.pdf2md.markdown.builder.*
import com.pdf2md.markdown.renderer.*
import com.pdf2md.common.Result

/**
 * PDF 구조를 Markdown AST로 변환
 */
class PdfToMarkdownAstConverter {
    fun convert(pdfStructure: DocumentStructure): Result<Document> {
        return try {
            val doc = markdown {
                // 메타데이터 (선택적)
                pdfStructure.metadata.title?.let {
                    heading(1, it)
                }

                // 구조화된 요소 변환
                pdfStructure.elements.forEach { element ->
                    convertElement(element, this)
                }
            }

            Result.Success(doc)
        } catch (e: Exception) {
            Result.Error("Failed to convert to Markdown AST: ${e.message}", e)
        }
    }

    private fun convertElement(element: StructuredElement, builder: DocumentBuilder) {
        when (element) {
            is StructuredElement.Heading -> {
                builder.heading(element.level, element.text)
            }

            is StructuredElement.Paragraph -> {
                builder.paragraph(element.text)
            }

            is StructuredElement.ListItem -> {
                // 리스트는 그룹화 필요 (별도 로직)
                builder.list(element.ordered) {
                    item(element.text, element.checked)
                }
            }

            is StructuredElement.CodeBlock -> {
                builder.codeBlock(element.text, element.language)
            }

            is StructuredElement.Quote -> {
                builder.blockquote {
                    paragraph(element.text)
                }
            }

            is StructuredElement.Image -> {
                builder.paragraph {
                    image(element.path, element.altText)
                }
            }
        }
    }
}

/**
 * 전체 변환 오케스트레이터 (업데이트)
 */
class MarkdownConversionOrchestrator(
    private val astConverter: PdfToMarkdownAstConverter = PdfToMarkdownAstConverter(),
    private val renderer: MarkdownRenderer = GfmRenderer()
) {
    fun convertToMarkdown(pdfStructure: DocumentStructure): Result<String> {
        return astConverter.convert(pdfStructure)
            .map { ast -> renderer.render(ast) }
    }
}
```

### 사용 흐름

#### 예제 1: PDF → Markdown (Plain Text)

```kotlin
fun convertPdfToMarkdown(pdfFile: File): Result<String> {
    // 1. PDF 읽기
    val pdfReader = PdfReader()
    val document = pdfReader.loadDocument(pdfFile).getOrElse { return it }

    // 2. PDF 구조 추출
    val pdfStructure = pdfReader.extractStructure(document).getOrElse { return it }

    // 3. Markdown AST 변환
    val astConverter = PdfToMarkdownAstConverter()
    val markdownAst = astConverter.convert(pdfStructure).getOrElse { return it }

    // 4. Markdown 문자열 렌더링
    val renderer = GfmRenderer()
    val markdown = renderer.render(markdownAst)

    return Result.Success(markdown)
}
```

#### 예제 2: PDF → HTML

```kotlin
fun convertPdfToHtml(pdfFile: File, fullDocument: Boolean = true): Result<String> {
    // 1. PDF 읽기
    val pdfReader = PdfReader()
    val document = pdfReader.loadDocument(pdfFile).getOrElse { return it }

    // 2. PDF 구조 추출
    val pdfStructure = pdfReader.extractStructure(document).getOrElse { return it }

    // 3. Markdown AST 변환
    val astConverter = PdfToMarkdownAstConverter()
    val markdownAst = astConverter.convert(pdfStructure).getOrElse { return it }

    // 4. HTML 렌더링
    val renderer = HtmlRenderer(
        HtmlRenderOptions(
            fullDocument = fullDocument,
            title = pdfStructure.metadata.title,
            includeDefaultStyles = true
        )
    )
    val html = renderer.render(markdownAst)

    return Result.Success(html)
}
```

#### 예제 3: 두 가지 포맷 동시 출력

```kotlin
fun convertPdfToBothFormats(pdfFile: File): Result<ConversionOutput> {
    // 1-3. PDF → AST (공통)
    val pdfReader = PdfReader()
    val document = pdfReader.loadDocument(pdfFile).getOrElse { return it }
    val pdfStructure = pdfReader.extractStructure(document).getOrElse { return it }

    val astConverter = PdfToMarkdownAstConverter()
    val markdownAst = astConverter.convert(pdfStructure).getOrElse { return it }

    // 4. Markdown 렌더링
    val mdRenderer = GfmRenderer()
    val markdown = mdRenderer.render(markdownAst)

    // 5. HTML 렌더링
    val htmlRenderer = HtmlRenderer(
        HtmlRenderOptions(
            fullDocument = true,
            title = pdfStructure.metadata.title
        )
    )
    val html = htmlRenderer.render(markdownAst)

    return Result.Success(ConversionOutput(markdown, html))
}

data class ConversionOutput(
    val markdown: String,
    val html: String
)
```

---

## 테스트 전략

### AST 테스트

```kotlin
class MarkdownAstTest {
    @Test
    fun `should create heading node`() {
        val heading = Heading(1, listOf(Text("Title")))

        assertEquals("heading", heading.type)
        assertEquals(1, heading.level)
        assertEquals("Title", (heading.children[0] as Text).value)
    }

    @Test
    fun `should validate heading level`() {
        assertThrows<IllegalArgumentException> {
            Heading(0, listOf(Text("Invalid")))
        }
    }
}
```

### Builder 테스트

```kotlin
class MarkdownBuilderTest {
    @Test
    fun `should build simple document`() {
        val doc = markdown {
            heading(1, "Title")
            paragraph("Content")
        }

        assertEquals(2, doc.children.size)
        assertTrue(doc.children[0] is Heading)
        assertTrue(doc.children[1] is Paragraph)
    }

    @Test
    fun `should build nested inline content`() {
        val doc = markdown {
            paragraph {
                text("Hello ")
                strong("World")
                text("!")
            }
        }

        val para = doc.children[0] as Paragraph
        assertEquals(3, para.children.size)
    }
}
```

### Renderer 테스트

```kotlin
class GfmRendererTest {
    private val renderer = GfmRenderer()

    @Test
    fun `should render heading`() {
        val doc = markdown {
            heading(1, "Title")
        }

        val output = renderer.render(doc)
        assertEquals("# Title", output)
    }

    @Test
    fun `should render list`() {
        val doc = markdown {
            list {
                item("First")
                item("Second")
            }
        }

        val output = renderer.render(doc)
        assertEquals("- First\n- Second", output)
    }

    @Test
    fun `should render table`() {
        val doc = markdown {
            table {
                alignment(Alignment.LEFT, Alignment.CENTER)

                row {
                    cell("A")
                    cell("B")
                }

                row {
                    cell("1")
                    cell("2")
                }
            }
        }

        val expected = """
            |  A  |  B  |
            |:---|:---:|
            |  1  |  2  |
        """.trimIndent()

        assertEquals(expected, renderer.render(doc))
    }
}
```

---

## HTML Renderer 테스트

```kotlin
class HtmlRendererTest {
    private val renderer = HtmlRenderer()

    @Test
    fun `should render heading to HTML`() {
        val doc = markdown {
            heading(1, "Title")
        }

        val output = renderer.render(doc)
        assertEquals("<h1>Title</h1>", output)
    }

    @Test
    fun `should render paragraph with inline formatting`() {
        val doc = markdown {
            paragraph {
                text("This is ")
                strong("bold")
                text(" and ")
                emphasis("italic")
            }
        }

        val output = renderer.render(doc)
        assertEquals("<p>This is <strong>bold</strong> and <em>italic</em></p>", output)
    }

    @Test
    fun `should render full HTML document`() {
        val doc = markdown {
            heading(1, "Title")
            paragraph("Content")
        }

        val renderer = HtmlRenderer(
            HtmlRenderOptions(
                fullDocument = true,
                title = "Test Doc",
                includeDefaultStyles = false
            )
        )

        val output = renderer.render(doc)

        assertTrue(output.contains("<!DOCTYPE html>"))
        assertTrue(output.contains("<title>Test Doc</title>"))
        assertTrue(output.contains("<h1>Title</h1>"))
    }

    @Test
    fun `should escape HTML entities`() {
        val doc = markdown {
            paragraph("Text with <html> & \"quotes\"")
        }

        val output = renderer.render(doc)
        assertEquals("<p>Text with &lt;html&gt; &amp; &quot;quotes&quot;</p>", output)
    }
}
```

---

## 장점

### 1. 관심사의 분리 (Separation of Concerns)
- **Module 1 - AST**: 데이터 구조만 담당 (불변, 타입 안전)
- **Module 2 - Builder**: AST 생성만 담당 (DSL 스타일)
- **Module 3 - PDF Converter**: PDF → AST 변환만 담당
- **Module 4 - Renderer**: AST → String 출력만 담당

### 2. 재사용성
- **AST**는 다양한 소스에서 사용 가능:
  - PDF 외에도 Word, HTML, 일반 텍스트 등
  - 프로그래매틱 생성 (Builder 사용)
- **Renderer**를 교체하여 다양한 포맷 출력:
  - Markdown (GFM, CommonMark)
  - HTML (Fragment, Full Document)
  - 향후: LaTeX, AsciiDoc, reStructuredText 등

### 3. 다중 포맷 출력
- **하나의 AST로 여러 포맷 동시 생성**:
  ```kotlin
  val ast = pdfToAst(pdf)
  val markdown = GfmRenderer().render(ast)
  val html = HtmlRenderer().render(ast)
  ```
- 변환 로직 중복 없음 (PDF 파싱은 1번만)

### 4. 테스트 용이성
- 각 모듈을 독립적으로 테스트 가능
- Mock 없이 순수 함수 테스트
- AST를 직접 생성하여 렌더러 테스트 가능
- PDF 없이도 마크다운/HTML 생성 테스트 가능

### 5. 표준 준수
- **mdast 스펙** 기반으로 표준에 가까움
- 다른 마크다운 도구와 호환 가능
- CommonMark, GFM 사양 준수

### 6. 확장 가능성
- 새로운 노드 타입 추가 용이 (Sealed class)
- 커스텀 렌더러 작성 가능 (인터페이스 구현)
- 플러그인 시스템 구축 가능
- 커스텀 변환 규칙 추가 가능

### 7. 성능
- AST를 중간 표현으로 사용하여 효율적 처리
- 렌더러는 단순 변환만 수행 (빠름)
- 동일 AST를 여러 번 렌더링 가능 (캐싱 가능)

---

## 다음 단계

### Phase 1: 기초 모듈 구현
1. **Module 1: AST 구현** (기본 노드 타입)
   - BlockNode, InlineNode 정의
   - Document, Heading, Paragraph, List 등
   - 단위 테스트 작성

2. **Module 2: Builder 구현** (DSL)
   - DocumentBuilder, InlineBuilder, ListBuilder
   - Type-safe builder 패턴
   - Builder 테스트

### Phase 2: 렌더러 구현
3. **Module 4: Markdown Renderer 구현**
   - GfmRenderer (GitHub Flavored Markdown)
   - CommonMarkRenderer
   - 렌더러 테스트

4. **Module 4: HTML Renderer 구현**
   - HtmlRenderer (Fragment + Full Document)
   - 스타일링 옵션
   - HTML 렌더러 테스트

### Phase 3: 변환기 구현
5. **Module 3: PDF to AST Converter 구현**
   - PdfToMarkdownAstConverter
   - ElementMapper, ListGrouper
   - 통합 테스트

### Phase 4: 통합 및 최적화
6. **전체 파이프라인 통합**
   - End-to-End 테스트
   - 성능 최적화
   - 에러 처리 개선

7. **CLI 통합**
   - `--format markdown` 옵션
   - `--format html` 옵션
   - `--format both` 옵션 (두 가지 동시 출력)

8. **문서화 및 예제**
   - API 문서
   - 사용 예제
   - 튜토리얼

---

## 참고 자료

- [mdast 스펙](https://github.com/syntax-tree/mdast)
- [CommonMark 스펙](https://spec.commonmark.org/)
- [GitHub Flavored Markdown 스펙](https://github.github.com/gfm/)
- [JetBrains Markdown Parser](https://github.com/JetBrains/markdown)
