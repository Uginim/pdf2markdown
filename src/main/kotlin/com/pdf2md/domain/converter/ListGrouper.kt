package com.pdf2md.domain.converter

import com.pdf2md.domain.pdf.StructuredElement

/**
 * 연속된 리스트 아이템들을 그룹화하는 유틸리티
 *
 * PDF에서 추출된 개별 ListItem 요소들을 분석하여,
 * 연속된 같은 타입(ordered/unordered)의 리스트 아이템들을
 * 하나의 그룹으로 묶습니다.
 *
 * ## 그룹화 규칙
 * 1. 연속된 ListItem 요소들은 같은 그룹으로 묶임
 * 2. ordered 속성이 다르면 새로운 그룹 시작
 * 3. ListItem이 아닌 요소가 나타나면 현재 그룹 종료
 * 4. 중첩 리스트는 level 속성으로 구분 (향후 확장)
 *
 * ## 사용 예:
 * ```kotlin
 * val elements = listOf(
 *     StructuredElement.ListItem(level = 0, marker = "-", text = "Item 1", pageNumber = 1, ordered = false),
 *     StructuredElement.ListItem(level = 0, marker = "-", text = "Item 2", pageNumber = 1, ordered = false),
 *     StructuredElement.Paragraph("Some text", 1),
 *     StructuredElement.ListItem(level = 0, marker = "1.", text = "First", pageNumber = 1, ordered = true),
 * )
 *
 * val grouped = ListGrouper.groupElements(elements)
 * // 결과: [GroupedList(2개 unordered), Paragraph, GroupedList(1개 ordered)]
 * ```
 */
object ListGrouper {

    /**
     * 요소 리스트를 처리하여 연속된 리스트 아이템들을 그룹화합니다.
     *
     * @param elements 원본 StructuredElement 리스트
     * @return 그룹화된 요소 리스트 (GroupedList 또는 원본 요소)
     */
    fun groupElements(elements: List<StructuredElement>): List<GroupedElement> {
        if (elements.isEmpty()) return emptyList()

        val result = mutableListOf<GroupedElement>()
        var currentListGroup: MutableList<StructuredElement.ListItem>? = null
        var currentOrdered: Boolean? = null
        var currentLevel: Int? = null

        for (element in elements) {
            when (element) {
                is StructuredElement.ListItem -> {
                    // 현재 그룹이 없거나, ordered 타입이 다르거나, 레벨이 0이 아닌데 레벨이 크게 변경된 경우 새 그룹 시작
                    val shouldStartNewGroup = currentListGroup == null ||
                            currentOrdered != element.ordered ||
                            shouldSplitByLevel(currentLevel, element.level)

                    if (shouldStartNewGroup) {
                        // 이전 그룹 저장
                        currentListGroup?.let { group ->
                            result.add(createGroupedList(group, currentOrdered!!))
                        }

                        // 새 그룹 시작
                        currentListGroup = mutableListOf(element)
                        currentOrdered = element.ordered
                        currentLevel = element.level
                    } else {
                        // 현재 그룹에 추가
                        currentListGroup!!.add(element)
                        currentLevel = element.level
                    }
                }
                else -> {
                    // 리스트가 아닌 요소: 현재 그룹 종료
                    currentListGroup?.let { group ->
                        result.add(createGroupedList(group, currentOrdered!!))
                    }
                    currentListGroup = null
                    currentOrdered = null
                    currentLevel = null

                    // 다른 요소는 그대로 PassThrough로 추가
                    result.add(GroupedElement.PassThrough(element))
                }
            }
        }

        // 마지막 그룹 저장
        currentListGroup?.let { group ->
            result.add(createGroupedList(group, currentOrdered!!))
        }

        return result
    }

    /**
     * 레벨 변경으로 인해 그룹을 분리해야 하는지 판단합니다.
     *
     * 기본적으로 같은 그룹 내에서는 레벨 변경을 허용합니다 (중첩 리스트 지원).
     * 단, 레벨 0에서 갑자기 레벨 2 이상으로 점프하는 경우는 새 그룹으로 분리합니다.
     */
    private fun shouldSplitByLevel(currentLevel: Int?, newLevel: Int): Boolean {
        if (currentLevel == null) return false
        // 레벨이 2 이상 점프하면 분리 (예: 0 -> 2)
        return kotlin.math.abs(newLevel - currentLevel) > 1
    }

    /**
     * 리스트 아이템 그룹을 GroupedList로 변환합니다.
     */
    private fun createGroupedList(
        items: List<StructuredElement.ListItem>,
        ordered: Boolean
    ): GroupedElement.GroupedList {
        // 중첩 구조를 처리하여 트리 형태로 변환
        val rootItems = buildNestedStructure(items)

        return GroupedElement.GroupedList(
            items = rootItems,
            ordered = ordered
        )
    }

    /**
     * 평면적인 리스트 아이템 리스트를 중첩 구조로 변환합니다.
     *
     * 예:
     * - Item 1 (level 0)
     *   - Sub 1 (level 1)
     *   - Sub 2 (level 1)
     * - Item 2 (level 0)
     *
     * 위와 같은 구조를 트리 형태로 만듭니다.
     */
    private fun buildNestedStructure(items: List<StructuredElement.ListItem>): List<NestedListItem> {
        if (items.isEmpty()) return emptyList()

        val result = mutableListOf<NestedListItem>()
        val stack = mutableListOf<Pair<Int, MutableList<NestedListItem>>>()

        // 루트 레벨 초기화
        stack.add(-1 to result)

        for (item in items) {
            val currentItem = NestedListItem(
                text = item.text,
                checked = item.checked,
                ordered = item.ordered,
                level = item.level,
                children = mutableListOf()
            )

            // 현재 아이템의 레벨보다 높거나 같은 레벨의 스택 항목을 제거
            while (stack.size > 1 && stack.last().first >= item.level) {
                stack.removeLast()
            }

            // 현재 아이템을 스택 최상위의 children에 추가
            stack.last().second.add(currentItem)

            // 현재 아이템을 스택에 추가 (다음 하위 아이템을 위해)
            stack.add(item.level to currentItem.children)
        }

        return result
    }
}

/**
 * 그룹화된 요소를 나타내는 sealed class
 */
sealed class GroupedElement {
    /**
     * 그룹화된 리스트
     *
     * @property items 중첩 구조의 리스트 아이템들
     * @property ordered 번호 리스트 여부
     */
    data class GroupedList(
        val items: List<NestedListItem>,
        val ordered: Boolean
    ) : GroupedElement()

    /**
     * 리스트가 아닌 요소 (그대로 통과)
     */
    data class PassThrough(
        val element: StructuredElement
    ) : GroupedElement()
}

/**
 * 중첩 가능한 리스트 아이템
 *
 * @property text 아이템 텍스트
 * @property checked 체크박스 상태
 * @property ordered 번호 리스트 여부
 * @property level 중첩 레벨
 * @property children 하위 아이템들
 */
data class NestedListItem(
    val text: String,
    val checked: Boolean?,
    val ordered: Boolean,
    val level: Int,
    val children: MutableList<NestedListItem>
)
