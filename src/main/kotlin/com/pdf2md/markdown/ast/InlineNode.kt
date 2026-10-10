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
