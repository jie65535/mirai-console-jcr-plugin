package top.jie65535.jcr

import kotlinx.coroutines.runInterruptible
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

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

    private val httpClient by lazy { OkHttpClient() }

    /**
     * 更新数据
     */
    suspend fun updateData() {
        val call = httpClient.newCall(Request.Builder()
            .url("https://cdn.jsdelivr.net/npm/@gytx/cppreference-index/dist/generated.json")
            .build())
        JCppReferencePlugin.logger.info("正在下载索引")
        runInterruptible {
            val response = call.execute()
            if (response.isSuccessful) {
                val json = response.body!!.string()
                indexes = Json.decodeFromString(json)
                // 保存到文件
                JCppReferencePlugin.resolveDataFile("linkmap.json")
                    .writeText(json)
                JCppReferencePlugin.logger.info("索引更新完成")
            } else {
                JCppReferencePlugin.logger.error("下载失败 HTTP Code: ${response.code}")
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