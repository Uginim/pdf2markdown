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
