package com.pdf2md.domain.converter

import com.pdf2md.domain.pdf.DocumentStructure
import com.pdf2md.domain.pdf.StructuredElement
import com.pdf2md.markdown.ast.*
import com.pdf2md.markdown.builder.*
import com.pdf2md.common.Result

/**
 * PDF 구조를 Markdown AST로 변환하는 컨버터
 *
 * PDF에서 추출된 DocumentStructure를 Markdown AST Document로 변환합니다.
 * 특히, 연속된 ListItem 요소들을 ListGrouper를 통해 하나의 ListNode로 그룹화합니다.
 *
 * ## 변환 흐름
 * ```
 * DocumentStructure (PDF 추출 결과)
 *         │
 *         ▼
 * ListGrouper.groupElements()
 *         │
 *         ▼
 * List<GroupedElement>
 *         │
 *         ▼
 * convertGroupedElement()
 *         │
 *         ▼
 * Document (Markdown AST)
 * ```
 *
 * ## 사용 예:
 * ```kotlin
 * val converter = PdfToMarkdownAstConverter()
 * val result = converter.convert(pdfStructure)
 *
 * result.onSuccess { document ->
 *     val renderer = GfmRenderer()
 *     println(renderer.render(document))
 * }
 * ```
 */
class PdfToMarkdownAstConverter {

    /**
     * PDF 구조를 Markdown AST로 변환합니다.
     *
     * @param pdfStructure PDF에서 추출된 문서 구조
     * @return 변환된 Markdown AST Document, 또는 에러
     */
    fun convert(pdfStructure: DocumentStructure): Result<Document> {
        return try {
            // 1. 리스트 아이템들을 그룹화
            val groupedElements = ListGrouper.groupElements(pdfStructure.elements)

            // 2. DSL을 사용하여 문서 빌드
            val doc = markdown {
                // 메타데이터의 제목이 있으면 H1으로 추가
                pdfStructure.metadata.title?.let { title ->
                    heading(1, title)
                }

                // 그룹화된 요소들을 변환
                groupedElements.forEach { groupedElement ->
                    convertGroupedElement(groupedElement, this)
                }
            }

            Result.Success(doc)
        } catch (e: Exception) {
            Result.Error("Failed to convert to Markdown AST: ${e.message}", e)
        }
    }

    /**
     * 그룹화된 요소를 Markdown AST 노드로 변환합니다.
     */
    private fun convertGroupedElement(
        groupedElement: GroupedElement,
        builder: DocumentBuilder
    ) {
        when (groupedElement) {
            is GroupedElement.GroupedList -> {
                // 그룹화된 리스트를 하나의 ListNode로 변환
                convertGroupedList(groupedElement, builder)
            }
            is GroupedElement.PassThrough -> {
                // 리스트가 아닌 요소는 기존 방식으로 변환
                convertElement(groupedElement.element, builder)
            }
        }
    }

    /**
     * 그룹화된 리스트를 ListNode로 변환합니다.
     *
     * 중첩 리스트 구조를 올바르게 처리합니다.
     */
    private fun convertGroupedList(
        groupedList: GroupedElement.GroupedList,
        builder: DocumentBuilder
    ) {
        builder.list(ordered = groupedList.ordered) {
            groupedList.items.forEach { nestedItem ->
                addNestedListItem(nestedItem, this)
            }
        }
    }

    /**
     * 중첩된 리스트 아이템을 재귀적으로 추가합니다.
     */
    private fun addNestedListItem(
        nestedItem: NestedListItem,
        listBuilder: ListBuilder
    ) {
        if (nestedItem.children.isEmpty()) {
            // 하위 아이템이 없으면 단순 아이템으로 추가
            listBuilder.item(nestedItem.text, nestedItem.checked)
        } else {
            // 하위 아이템이 있으면 블록 콘텐츠로 추가
            listBuilder.item(checked = nestedItem.checked) {
                // 현재 아이템의 텍스트
                paragraph(nestedItem.text)

                // 하위 리스트 추가
                list(ordered = nestedItem.children.firstOrNull()?.ordered ?: false) {
                    nestedItem.children.forEach { child ->
                        addNestedListItem(child, this)
                    }
                }
            }
        }
    }

    /**
     * 개별 StructuredElement를 Markdown AST 노드로 변환합니다.
     *
     * 이 메서드는 리스트가 아닌 요소들을 처리합니다.
     * (리스트는 ListGrouper를 통해 별도로 처리됨)
     */
    private fun convertElement(
        element: StructuredElement,
        builder: DocumentBuilder
    ) {
        when (element) {
            is StructuredElement.Heading -> {
                builder.heading(element.level, element.text)
            }

            is StructuredElement.Paragraph -> {
                builder.paragraph(element.text)
            }

            is StructuredElement.ListItem -> {
                // 이 경우는 ListGrouper를 거치지 않고 직접 호출된 경우
                // 단일 아이템 리스트로 처리 (폴백 동작)
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
