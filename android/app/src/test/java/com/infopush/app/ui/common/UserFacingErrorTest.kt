package com.infopush.app.ui.common

import kotlin.test.Test
import kotlin.test.assertTrue

class UserFacingErrorTest {

    @Test
    fun `format should map 403 probe error with readable hint and raw error`() {
        val raw = "source_probe_http_failed: detail=http_status_403"

        val actual = UserFacingError.format(raw)

        assertTrue(actual.contains("该源拒绝访问(403)"))
        assertTrue(actual.contains("可更换镜像或新的源地址后重试"))
        assertTrue(actual.contains("原始错误：$raw"))
    }

    @Test
    fun `format should map empty feed errors`() {
        val actual = UserFacingError.format("feed_empty")

        assertTrue(actual.contains("该源暂无可解析内容"))
    }

    @Test
    fun `format should map unsupported source errors`() {
        val actual = UserFacingError.format("source_probe_unsupported")

        assertTrue(actual.contains("暂不支持该源格式"))
    }
}
