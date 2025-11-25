package com.pdf2md.domain.converter

import com.pdf2md.domain.pdf.StructuredElement
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * ListGrouper의 단위 테스트
 *
 * 연속된 리스트 아이템들이 올바르게 그룹화되는지 검증합니다.
 */
class ListGrouperTest : DescribeSpec({

    describe("ListGrouper.groupElements") {

        context("빈 리스트") {
            it("빈 리스트를 입력하면 빈 리스트를 반환해야 함") {
                val result = ListGrouper.groupElements(emptyList())

                result shouldBe emptyList()
            }
        }

        context("연속된 불릿 리스트 아이템") {
            it("연속된 unordered 리스트 아이템들을 하나의 그룹으로 묶어야 함") {
                val elements = listOf(
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
                )

                val result = ListGrouper.groupElements(elements)

                result.size shouldBe 1
                result[0].shouldBeInstanceOf<GroupedElement.GroupedList>()

                val groupedList = result[0] as GroupedElement.GroupedList
                groupedList.ordered shouldBe false
                groupedList.items.size shouldBe 3
                groupedList.items[0].text shouldBe "Item 1"
                groupedList.items[1].text shouldBe "Item 2"
                groupedList.items[2].text shouldBe "Item 3"
            }
        }

        context("연속된 번호 리스트 아이템") {
            it("연속된 ordered 리스트 아이템들을 하나의 그룹으로 묶어야 함") {
                val elements = listOf(
                    StructuredElement.ListItem(
                        level = 0,
                        marker = "1.",
                        text = "First",
                        pageNumber = 1,
                        ordered = true
                    ),
                    StructuredElement.ListItem(
                        level = 0,
                        marker = "2.",
                        text = "Second",
                        pageNumber = 1,
                        ordered = true
                    )
                )

                val result = ListGrouper.groupElements(elements)

                result.size shouldBe 1
                val groupedList = result[0] as GroupedElement.GroupedList
                groupedList.ordered shouldBe true
                groupedList.items.size shouldBe 2
            }
        }

        context("리스트 타입이 변경되는 경우") {
            it("ordered와 unordered가 섞이면 별도의 그룹으로 분리해야 함") {
                val elements = listOf(
                    StructuredElement.ListItem(
                        level = 0,
                        marker = "-",
                        text = "Unordered 1",
                        pageNumber = 1,
                        ordered = false
                    ),
                    StructuredElement.ListItem(
                        level = 0,
                        marker = "-",
                        text = "Unordered 2",
                        pageNumber = 1,
                        ordered = false
                    ),
                    StructuredElement.ListItem(
                        level = 0,
                        marker = "1.",
                        text = "Ordered 1",
                        pageNumber = 1,
                        ordered = true
                    ),
                    StructuredElement.ListItem(
                        level = 0,
                        marker = "2.",
                        text = "Ordered 2",
                        pageNumber = 1,
                        ordered = true
                    )
                )

                val result = ListGrouper.groupElements(elements)

                result.size shouldBe 2

                val firstGroup = result[0] as GroupedElement.GroupedList
                firstGroup.ordered shouldBe false
                firstGroup.items.size shouldBe 2

                val secondGroup = result[1] as GroupedElement.GroupedList
                secondGroup.ordered shouldBe true
                secondGroup.items.size shouldBe 2
            }
        }

        context("다른 요소가 사이에 있는 경우") {
            it("다른 요소로 인해 리스트 그룹이 분리되어야 함") {
                val elements = listOf(
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
                    StructuredElement.Paragraph(
                        text = "Some paragraph",
                        pageNumber = 1
                    ),
                    StructuredElement.ListItem(
                        level = 0,
                        marker = "-",
                        text = "Item 3",
                        pageNumber = 1,
                        ordered = false
                    )
                )

                val result = ListGrouper.groupElements(elements)

                result.size shouldBe 3

                // 첫 번째 리스트 그룹
                val firstGroup = result[0] as GroupedElement.GroupedList
                firstGroup.items.size shouldBe 2

                // 단락
                val paragraph = result[1] as GroupedElement.PassThrough
                paragraph.element.shouldBeInstanceOf<StructuredElement.Paragraph>()
                (paragraph.element as StructuredElement.Paragraph).text shouldBe "Some paragraph"

                // 두 번째 리스트 그룹
                val secondGroup = result[2] as GroupedElement.GroupedList
                secondGroup.items.size shouldBe 1
            }
        }

        context("중첩 리스트") {
            it("레벨이 다른 연속된 아이템들을 중첩 구조로 만들어야 함") {
                val elements = listOf(
                    StructuredElement.ListItem(
                        level = 0,
                        marker = "-",
                        text = "Parent 1",
                        pageNumber = 1,
                        ordered = false
                    ),
                    StructuredElement.ListItem(
                        level = 1,
                        marker = "-",
                        text = "Child 1.1",
                        pageNumber = 1,
                        ordered = false
                    ),
                    StructuredElement.ListItem(
                        level = 1,
                        marker = "-",
                        text = "Child 1.2",
                        pageNumber = 1,
                        ordered = false
                    ),
                    StructuredElement.ListItem(
                        level = 0,
                        marker = "-",
                        text = "Parent 2",
                        pageNumber = 1,
                        ordered = false
                    )
                )

                val result = ListGrouper.groupElements(elements)

                result.size shouldBe 1
                val groupedList = result[0] as GroupedElement.GroupedList
                groupedList.items.size shouldBe 2 // Parent 1, Parent 2

                // Parent 1의 children 확인
                val parent1 = groupedList.items[0]
                parent1.text shouldBe "Parent 1"
                parent1.children.size shouldBe 2
                parent1.children[0].text shouldBe "Child 1.1"
                parent1.children[1].text shouldBe "Child 1.2"

                // Parent 2의 children 확인
                val parent2 = groupedList.items[1]
                parent2.text shouldBe "Parent 2"
                parent2.children.size shouldBe 0
            }
        }

        context("체크박스 리스트") {
            it("체크박스 상태가 올바르게 유지되어야 함") {
                val elements = listOf(
                    StructuredElement.ListItem(
                        level = 0,
                        marker = "-",
                        text = "Done task",
                        pageNumber = 1,
                        ordered = false,
                        checked = true
                    ),
                    StructuredElement.ListItem(
                        level = 0,
                        marker = "-",
                        text = "Pending task",
                        pageNumber = 1,
                        ordered = false,
                        checked = false
                    ),
                    StructuredElement.ListItem(
                        level = 0,
                        marker = "-",
                        text = "Regular item",
                        pageNumber = 1,
                        ordered = false,
                        checked = null
                    )
                )

                val result = ListGrouper.groupElements(elements)

                result.size shouldBe 1
                val groupedList = result[0] as GroupedElement.GroupedList

                groupedList.items[0].checked shouldBe true
                groupedList.items[1].checked shouldBe false
                groupedList.items[2].checked shouldBe null
            }
        }

        context("리스트가 아닌 요소만 있는 경우") {
            it("모든 요소를 PassThrough로 반환해야 함") {
                val elements = listOf(
                    StructuredElement.Paragraph("Para 1", 1),
                    StructuredElement.Heading(1, "Title", 1),
                    StructuredElement.Paragraph("Para 2", 1)
                )

                val result = ListGrouper.groupElements(elements)

                result.size shouldBe 3
                result.all { it is GroupedElement.PassThrough } shouldBe true
            }
        }

        context("단일 리스트 아이템") {
            it("단일 리스트 아이템도 그룹으로 묶어야 함") {
                val elements = listOf(
                    StructuredElement.ListItem(
                        level = 0,
                        marker = "-",
                        text = "Single item",
                        pageNumber = 1,
                        ordered = false
                    )
                )

                val result = ListGrouper.groupElements(elements)

                result.size shouldBe 1
                val groupedList = result[0] as GroupedElement.GroupedList
                groupedList.items.size shouldBe 1
                groupedList.items[0].text shouldBe "Single item"
            }
        }
    }
})
