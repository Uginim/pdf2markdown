package com.pdf2md.domain.pdf

/**
 * PDF 문서에서 추출된 구조화된 정보
 *
 * @property elements 구조화된 요소들의 리스트
 * @property metadata 문서 메타데이터
 */
data class DocumentStructure(
    val elements: List<StructuredElement>,
    val metadata: DocumentMetadata
)

/**
 * PDF에서 추출된 구조화된 요소의 기본 타입
 *
 * sealed class로 정의하여 모든 가능한 요소 타입을 컴파일 타임에 보장합니다.
 */
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
        val level: Int,
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
     * @property level 중첩 깊이 (0부터 시작)
     * @property marker 리스트 마커 (-, •, 1., a., 등)
     * @property text 아이템 텍스트
     * @property pageNumber 페이지 번호
     * @property ordered 번호 리스트 여부
     * @property checked 체크박스 상태 (null이면 체크박스 없음)
     */
    data class ListItem(
        val level: Int,
        val marker: String,
        override val text: String,
        override val pageNumber: Int,
        val ordered: Boolean,
        val checked: Boolean? = null
    ) : StructuredElement()

    /**
     * 코드 블록 요소
     *
     * @property text 코드 내용
     * @property pageNumber 페이지 번호
     * @property language 프로그래밍 언어 (선택적)
     */
    data class CodeBlock(
        override val text: String,
        override val pageNumber: Int,
        val language: String? = null
    ) : StructuredElement()

    /**
     * 인용구 요소
     *
     * @property text 인용구 텍스트
     * @property pageNumber 페이지 번호
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
     * @property pageNumber 페이지 번호
     */
    data class Image(
        val path: String,
        val altText: String,
        override val pageNumber: Int
    ) : StructuredElement() {
        override val text: String = altText
    }
}

/**
 * PDF 문서 메타데이터
 */
data class DocumentMetadata(
    val title: String?,
    val author: String?,
    val subject: String?,
    val keywords: String?,
    val creator: String?,
    val producer: String?,
    val creationDate: String?,
    val modificationDate: String?
) {
    companion object {
        /**
         * 빈 메타데이터를 생성합니다.
         */
        fun empty() = DocumentMetadata(
            title = null,
            author = null,
            subject = null,
            keywords = null,
            creator = null,
            producer = null,
            creationDate = null,
            modificationDate = null
        )
    }
}
