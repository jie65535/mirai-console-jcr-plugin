package top.jie65535.jcr

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
object Data {

    /**
     * 数据
     */
    lateinit var indexes: Array<Index>

    private val Json = Json {
        ignoreUnknownKeys = true
    }

    /**
     * 初始化数据
     * 更新索引：https://cdn.jsdelivr.net/npm/@gytx/cppreference-index/dist/generated.json
     */
    fun initData() {
        val linkMap = JCppReferencePlugin.resolveDataFile("linkmap.json")
        indexes = if (linkMap.exists()) {
            Json.decodeFromString(linkMap.readText())
        } else {
            val resource = this::class.java.getResource("/linkmap.json")
            if (resource == null) {
                JCppReferencePlugin.logger.error("索引资源不存在！请更新索引")
                arrayOf()
            } else {
                Json.decodeFromString(resource.readText())
            }
        }
    }

    /**
     * 获取索引
     */
    fun getIndex(word: String): Index? {
        return indexes.filter { it.name.contains(word) }
            .sortedWith { a, b -> a.name.length - b.name.length }
            .firstOrNull()
    }
}