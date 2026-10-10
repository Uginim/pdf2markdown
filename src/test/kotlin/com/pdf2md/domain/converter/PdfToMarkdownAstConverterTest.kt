package com.pdf2md.domain.converter

import com.pdf2md.common.Result
import com.pdf2md.domain.pdf.DocumentMetadata
import com.pdf2md.domain.pdf.DocumentStructure
import com.pdf2md.domain.pdf.StructuredElement
import com.pdf2md.markdown.ast.*
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * PdfToMarkdownAstConverter의 단위 테스트
 *
 * PDF 구조가 올바르게 Markdown AST로 변환되는지 검증합니다.
 * 특히, 연속된 리스트 아이템들이 하나의 ListNode로 그룹화되는지 확인합니다.
 */
class PdfToMarkdownAstConverterTest : DescribeSpec({

    val converter = PdfToMarkdownAstConverter()

    describe("PdfToMarkdownAstConverter.convert") {

        context("기본 변환") {
            it("제목과 단락을 올바르게 변환해야 함") {
                val structure = DocumentStructure(
                    elements = listOf(
                        StructuredElement.Heading(1, "Title", 1),
                        StructuredElement.Paragraph("Some content", 1)
                    ),
                    metadata = DocumentMetadata.empty()
                )

                val result = converter.convert(structure)

                result.shouldBeInstanceOf<Result.Success<Document>>()
                val doc = (result as Result.Success).value

                doc.children.size shouldBe 2
                doc.children[0].shouldBeInstanceOf<Heading>()
                doc.children[1].shouldBeInstanceOf<Paragraph>()

                val heading = doc.children[0] as Heading
                heading.level shouldBe 1
                (heading.children[0] as Text).value shouldBe "Title"
            }
        }

        context("연속된 리스트 아이템 그룹화") {
            it("연속된 리스트 아이템들을 하나의 ListNode로 그룹화해야 함") {
                val structure = DocumentStructure(
                    elements = listOf(
                        StructuredElement.ListItem(
                            level = 0,
                            marker = "-",
                            text = "Item 1",
                            pageNumber = 1,
                            ordered = false
                        ),
                        StructuredElement.ListItem(
                            level = 0,
                            marker = "-",
                            text = "Item 2",
                            pageNumber = 1,
                            ordered = false
                        ),
                        StructuredElement.ListItem(
                            level = 0,
                            marker = "-",
                            text = "Item 3",
                            pageNumber = 1,
                            ordered = false
                        )
                    ),
                    metadata = DocumentMetadata.empty()
                )

                val result = converter.convert(structure)

                result.shouldBeInstanceOf<Result.Success<Document>>()
                val doc = (result as Result.Success).value

                // 3개의 리스트 아이템이 1개의 ListNode로 그룹화되어야 함
                doc.children.size shouldBe 1
                doc.children[0].shouldBeInstanceOf<ListNode>()

                val listNode = doc.children[0] as ListNode
                listNode.ordered shouldBe false
                listNode.children.size shouldBe 3

                // 각 아이템의 내용 확인
                val item1 = listNode.children[0]
                val item1Para = item1.children[0] as Paragraph
                (item1Para.children[0] as Text).value shouldBe "Item 1"

                val item2 = listNode.children[1]
                val item2Para = item2.children[0] as Paragraph
                (item2Para.children[0] as Text).value shouldBe "Item 2"

                val item3 = listNode.children[2]
                val item3Para = item3.children[0] as Paragraph
                (item3Para.children[0] as Text).value shouldBe "Item 3"
            }

            it("리스트 사이에 다른 요소가 있으면 별도의 ListNode로 분리해야 함") {
                val structure = DocumentStructure(
                    elements = listOf(
                        StructuredElement.ListItem(
                            level = 0,
                            marker = "-",
                            text = "Item 1",
                            pageNumber = 1,
                            ordered = false
                        ),
                        StructuredElement.Paragraph("Separator", 1),
                        StructuredElement.ListItem(
                            level = 0,
                            marker = "-",
                            text = "Item 2",
                            pageNumber = 1,
                            ordered = false
                        )
                    ),
                    metadata = DocumentMetadata.empty()
                )

                val result = converter.convert(structure)

                result.shouldBeInstanceOf<Result.Success<Document>>()
                val doc = (result as Result.Success).value

                doc.children.size shouldBe 3

                // 첫 번째 리스트
                doc.children[0].shouldBeInstanceOf<ListNode>()
                (doc.children[0] as ListNode).children.size shouldBe 1

                // 단락
                doc.children[1].shouldBeInstanceOf<Paragraph>()

                // 두 번째 리스트
                doc.children[2].shouldBeInstanceOf<ListNode>()
                (doc.children[2] as ListNode).children.size shouldBe 1
            }
        }

        context("번호 리스트와 불릿 리스트") {
            it("ordered와 unordered 리스트를 별도로 처리해야 함") {
                val structure = DocumentStructure(
                    elements = listOf(
                        StructuredElement.ListItem(
                            level = 0,
                            marker = "-",
                            text = "Bullet item",
                            pageNumber = 1,
                            ordered = false
                        ),
                        StructuredElement.ListItem(
                            level = 0,
                            marker = "1.",
                            text = "Numbered item",
                            pageNumber = 1,
                            ordered = true
                        )
                    ),
                    metadata = DocumentMetadata.empty()
                )

                val result = converter.convert(structure)

                result.shouldBeInstanceOf<Result.Success<Document>>()
                val doc = (result as Result.Success).value

                doc.children.size shouldBe 2

                val unorderedList = doc.children[0] as ListNode
                unorderedList.ordered shouldBe false

                val orderedList = doc.children[1] as ListNode
                orderedList.ordered shouldBe true
            }
        }

        context("중첩 리스트") {
            it("중첩된 리스트 구조를 올바르게 변환해야 함") {
                val structure = DocumentStructure(
                    elements = listOf(
                        StructuredElement.ListItem(
                            level = 0,
                            marker = "-",
                            text = "Parent",
                            pageNumber = 1,
                            ordered = false
                        ),
                        StructuredElement.ListItem(
                            level = 1,
                            marker = "-",
                            text = "Child",
                            pageNumber = 1,
                            ordered = false
                        )
                    ),
                    metadata = DocumentMetadata.empty()
                )

                val result = converter.convert(structure)

                result.shouldBeInstanceOf<Result.Success<Document>>()
                val doc = (result as Result.Success).value

                doc.children.size shouldBe 1
                val listNode = doc.children[0] as ListNode
                listNode.children.size shouldBe 1

                // Parent 아이템에 Child가 중첩되어 있어야 함
                val parentItem = listNode.children[0]
                // 구조: [Paragraph(Parent), ListNode([Child])]
                parentItem.children.size shouldBe 2

                val parentPara = parentItem.children[0] as Paragraph
                (parentPara.children[0] as Text).value shouldBe "Parent"

                val nestedList = parentItem.children[1] as ListNode
                nestedList.children.size shouldBe 1

                val childItem = nestedList.children[0]
                val childPara = childItem.children[0] as Paragraph
                (childPara.children[0] as Text).value shouldBe "Child"
            }
        }

        context("체크박스 리스트") {
            it("체크박스 상태를 올바르게 변환해야 함") {
                val structure = DocumentStructure(
                    elements = listOf(
                        StructuredElement.ListItem(
                            level = 0,
                            marker = "-",
                            text = "Completed",
                            pageNumber = 1,
                            ordered = false,
                            checked = true
                        ),
                        StructuredElement.ListItem(
                            level = 0,
                            marker = "-",
                            text = "Pending",
                            pageNumber = 1,
                            ordered = false,
                            checked = false
                        )
                    ),
                    metadata = DocumentMetadata.empty()
                )

                val result = converter.convert(structure)

                result.shouldBeInstanceOf<Result.Success<Document>>()
                val doc = (result as Result.Success).value

                val listNode = doc.children[0] as ListNode
                listNode.children[0].checked shouldBe true
                listNode.children[1].checked shouldBe false
            }
        }

        context("다른 요소 타입들") {
            it("코드 블록을 올바르게 변환해야 함") {
                val structure = DocumentStructure(
                    elements = listOf(
                        StructuredElement.CodeBlock(
                            text = "println(\"Hello\")",
                            pageNumber = 1,
                            language = "kotlin"
                        )
                    ),
                    metadata = DocumentMetadata.empty()
                )

                val result = converter.convert(structure)

                result.shouldBeInstanceOf<Result.Success<Document>>()
                val doc = (result as Result.Success).value

                doc.children.size shouldBe 1
                val codeBlock = doc.children[0] as CodeBlock
                codeBlock.value shouldBe "println(\"Hello\")"
                codeBlock.lang shouldBe "kotlin"
            }

            it("인용구를 올바르게 변환해야 함") {
                val structure = DocumentStructure(
                    elements = listOf(
                        StructuredElement.Quote("Famous quote", 1)
                    ),
                    metadata = DocumentMetadata.empty()
                )

                val result = converter.convert(structure)

                result.shouldBeInstanceOf<Result.Success<Document>>()
                val doc = (result as Result.Success).value

                doc.children.size shouldBe 1
                val blockquote = doc.children[0] as Blockquote
                val para = blockquote.children[0] as Paragraph
                (para.children[0] as Text).value shouldBe "Famous quote"
            }

            it("이미지를 올바르게 변환해야 함") {
                val structure = DocumentStructure(
                    elements = listOf(
                        StructuredElement.Image(
                            path = "/images/test.png",
                            altText = "Test image",
                            pageNumber = 1
                        )
                    ),
                    metadata = DocumentMetadata.empty()
                )

                val result = converter.convert(structure)

                result.shouldBeInstanceOf<Result.Success<Document>>()
                val doc = (result as Result.Success).value

                doc.children.size shouldBe 1
                val para = doc.children[0] as Paragraph
                val image = para.children[0] as Image
                image.url shouldBe "/images/test.png"
                image.alt shouldBe "Test image"
            }
        }

        context("메타데이터 처리") {
            it("제목 메타데이터가 있으면 H1으로 추가해야 함") {
                val structure = DocumentStructure(
                    elements = listOf(
                        StructuredElement.Paragraph("Content", 1)
                    ),
                    metadata = DocumentMetadata(
                        title = "Document Title",
                        author = null,
                        subject = null,
                        keywords = null,
                        creator = null,
                        producer = null,
                        creationDate = null,
                        modificationDate = null
                    )
                )

                val result = converter.convert(structure)

                result.shouldBeInstanceOf<Result.Success<Document>>()
                val doc = (result as Result.Success).value

                doc.children.size shouldBe 2

                // 첫 번째는 메타데이터 제목
                val titleHeading = doc.children[0] as Heading
                titleHeading.level shouldBe 1
                (titleHeading.children[0] as Text).value shouldBe "Document Title"

                // 두 번째는 본문
                doc.children[1].shouldBeInstanceOf<Paragraph>()
            }
        }

        context("빈 문서") {
            it("빈 요소 리스트를 변환해도 오류가 발생하지 않아야 함") {
                val structure = DocumentStructure(
                    elements = emptyList(),
                    metadata = DocumentMetadata.empty()
                )

                val result = converter.convert(structure)

                result.shouldBeInstanceOf<Result.Success<Document>>()
                val doc = (result as Result.Success).value
                doc.children.size shouldBe 0
            }
        }
    }
})
