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
