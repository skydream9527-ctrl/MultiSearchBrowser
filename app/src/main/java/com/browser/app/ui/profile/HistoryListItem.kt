package com.browser.app.ui.profile

import com.browser.app.data.entity.HistoryEntity

/**
 * 历史列表的统一项类型：
 * - [Header] 是按天分组的分组标题（今天 / 昨天 / yyyy年MM月dd日）
 * - [Item] 是单条历史记录
 */
sealed class HistoryListItem {

    data class Header(val label: String) : HistoryListItem()

    data class Item(val history: HistoryEntity) : HistoryListItem()
}
