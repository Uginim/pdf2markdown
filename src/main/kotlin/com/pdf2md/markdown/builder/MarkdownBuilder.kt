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
 *
 * 마크다운 문서를 Type-safe하게 구축할 수 있는 DSL을 제공합니다.
 */
class DocumentBuilder {
    internal val children = mutableListOf<BlockNode>()

    /**
     * 단순 텍스트 제목을 추가합니다.
     */
    fun heading(level: Int, text: String) {
        children.add(Heading(level, listOf(Text(text))))
    }

    /**
     * 인라인 콘텐츠를 포함하는 제목을 추가합니다.
     */
    fun heading(level: Int, block: InlineBuilder.() -> Unit) {
        val builder = InlineBuilder().apply(block)
        children.add(Heading(level, builder.build()))
    }

    /**
     * 단순 텍스트 단락을 추가합니다.
     */
    fun paragraph(text: String) {
        children.add(Paragraph(listOf(Text(text))))
    }

    /**
     * 인라인 콘텐츠를 포함하는 단락을 추가합니다.
     */
    fun paragraph(block: InlineBuilder.() -> Unit) {
        val builder = InlineBuilder().apply(block)
        children.add(Paragraph(builder.build()))
    }

    /**
     * 리스트를 추가합니다.
     *
     * @param ordered true면 번호 리스트, false면 불릿 리스트
     * @param start 번호 리스트의 시작 번호 (ordered=true일 때만 유효)
     */
    fun list(ordered: Boolean = false, start: Int? = null, block: ListBuilder.() -> Unit) {
        val builder = ListBuilder(ordered).apply(block)
        children.add(builder.build(start))
    }

    /**
     * 코드 블록을 추가합니다.
     */
    fun codeBlock(code: String, lang: String? = null) {
        children.add(CodeBlock(lang = lang, value = code))
    }

    /**
     * 인용구를 추가합니다.
     */
    fun blockquote(block: DocumentBuilder.() -> Unit) {
        val builder = DocumentBuilder().apply(block)
        children.add(Blockquote(builder.children))
    }

    /**
     * 수평선을 추가합니다.
     */
    fun thematicBreak() {
        children.add(ThematicBreak())
    }

    /**
     * 표를 추가합니다.
     */
    fun table(block: TableBuilder.() -> Unit) {
        val builder = TableBuilder().apply(block)
        children.add(builder.build())
    }

    /**
     * HTML 블록을 추가합니다.
     */
    fun html(value: String) {
        children.add(HtmlBlock(value))
    }

    /**
     * 이미 구성된 BlockNode를 추가합니다.
     */
    fun addNode(node: BlockNode) {
        children.add(node)
    }

    /**
     * 문서를 빌드합니다.
     */
    fun build(): Document = Document(children.toList())
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

    fun build(): List<InlineNode> = children.toList()
}

/**
 * 리스트 빌더
 */
class ListBuilder(private val ordered: Boolean) {
    private val items = mutableListOf<ListItem>()

    /**
     * 단순 텍스트 아이템을 추가합니다.
     */
    fun item(text: String, checked: Boolean? = null) {
        items.add(ListItem(
            checked = checked,
            children = listOf(Paragraph(listOf(Text(text))))
        ))
    }

    /**
     * 블록 콘텐츠를 포함하는 아이템을 추가합니다.
     */
    fun item(checked: Boolean? = null, block: DocumentBuilder.() -> Unit) {
        val builder = DocumentBuilder().apply(block)
        items.add(ListItem(
            checked = checked,
            children = builder.children
        ))
    }

    /**
     * 이미 구성된 ListItem을 추가합니다.
     */
    fun addItem(item: ListItem) {
        items.add(item)
    }

    fun build(start: Int? = null): ListNode = ListNode(
        ordered = ordered,
        start = start,
        children = items.toList()
    )
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

    fun build(): Table = Table(alignments.toList(), rows.toList())
}

/**
 * 표 행 빌더
 */
class TableRowBuilder {
    private val cells = mutableListOf<TableCell>()

    fun cell(text: String) {
        cells.add(TableCell(listOf(Text(text))))
    }

    fun cell(block: InlineBuilder.() -> Unit) {
        val builder = InlineBuilder().apply(block)
        cells.add(TableCell(builder.build()))
    }

    fun build(): TableRow = TableRow(cells.toList())
}
