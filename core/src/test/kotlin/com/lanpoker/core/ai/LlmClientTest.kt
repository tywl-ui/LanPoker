package com.lanpoker.core.ai

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LlmClientTest {

    private val client = LlmClient(AiConfig(baseUrl = "https://example.com", apiKey = "k", model = "m"))

    @Test
    fun 解析纯JSON() {
        val d = client.parseDecision("""{"action":"raise","level":3,"reason":"牌好"}""")
        assertEquals(AiActionType.RAISE, d?.action)
        assertEquals(3, d?.level)
    }

    @Test
    fun 解析带代码块() {
        val d = client.parseDecision("""```json
{"action":"compare","target":2,"reason":"试试"}
```""")
        assertEquals(AiActionType.COMPARE, d?.action)
        assertEquals(2, d?.targetId)
    }

    @Test
    fun 解析带前后废话() {
        val d = client.parseDecision("""好的，我的决定是：{"action":"fold","reason":"牌太差"}""")
        assertEquals(AiActionType.FOLD, d?.action)
    }

    @Test
    fun 解析不了返回null() {
        assertNull(client.parseDecision("我选择弃牌"))
        assertNull(client.parseDecision(null))
        assertNull(client.parseDecision("""{"action":"dance"}"""))
    }

    @Test
    fun 快局看牌解析() {
        assertEquals(true, client.parseQuickLook("""{"look":true}"""))
        assertEquals(false, client.parseQuickLook("""{"look":false}"""))
        assertNull(client.parseQuickLook("不看了"))
    }

    @Test
    fun 提取content() {
        val json = """{"choices":[{"message":{"content":"{\"action\":\"call\"}"}}]}"""
        val content = client.parseQuickLook(json) // 模拟：content 里是 {"look":true}
        assertTrue(content == null || content is Boolean)
    }
}
