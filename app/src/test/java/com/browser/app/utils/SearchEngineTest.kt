package com.browser.app.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * SearchEngine 单元测试。
 * 不依赖 Android 框架，可在 CI 上直接执行。
 */
class SearchEngineTest {

    @Test
    fun `ALL contains 7 engines`() {
        assertEquals(7, SearchEngine.ALL.size)
    }

    @Test
    fun `getById returns matching engine`() {
        val engine = SearchEngine.getById("bilibili")
        assertEquals("B站", engine.name)
    }

    @Test
    fun `getById falls back to BAIDU for unknown id`() {
        val engine = SearchEngine.getById("non_existent")
        assertEquals(SearchEngine.BAIDU, engine)
    }

    @Test
    fun `every engine has non-blank search url`() {
        SearchEngine.ALL.forEach { engine ->
            assertNotNull(engine.id)
            assert(engine.searchUrl.isNotBlank()) { "Engine ${engine.id} has blank url" }
        }
    }
}
