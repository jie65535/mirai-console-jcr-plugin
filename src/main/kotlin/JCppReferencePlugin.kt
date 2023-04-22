package top.jie65535.jcr

import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.subscribeMessages
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import net.mamoe.mirai.utils.info
import java.io.BufferedReader
import java.io.InputStreamReader

object JCppReferencePlugin : KotlinPlugin(
    JvmPluginDescription(
        id = "top.jie65535.mirai-console-jcr-plugin",
        name = "J Cpp Reference Plugin",
        version = "0.4.0"
    ) {
        author("jie65535")
        info("C/C++ 参考搜索插件")
    }
) {
    private const val cppreferencePrefix = "https://zh.cppreference.com/w/"
    private const val qtDocPrefix = "https://doc.qt.io/qt-5/"
    private const val gccDocPrefix = "https://gcc.gnu.org/onlinedocs/gcc-12.2.0/gcc/"

    /**
     * C索引
     */
    private val indexC by lazy { loadMap("/devhelp-index-c.txt") }

    /**
     * Qt文档索引，按::分割
     */
    private val indexQt by lazy {
        val map = loadMap("/qt-5.13.0.txt")
        splitKeys(map as MutableMap<String, String>)
        map
    }

    /**
     * GCC编译选项索引，不能忽略大小写
     */
    private val indexGcc by lazy { loadMap("/gcc-12-opt-index.txt", false) }

    private fun loadMap(path: String, ignoreCase: Boolean = true): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val stream = this::class.java.getResourceAsStream(path)
        if (stream == null) {
            logger.error("无法找到指定资源：$path")
        } else {
            stream.use {
                val br = BufferedReader(InputStreamReader(stream))
                while (br.ready()) {
                    val line = br.readLine()
                    val s = line.indexOf('\t')
                    if (s > 0) {
                        val key = line.substring(0, s)
                        val url = line.substring(s+1)
                        map.putIfAbsent(key, url)
                    }
                }
            }

            // 如果忽略大小写，则将key的小写形式也加入索引
            if (ignoreCase) {
                // 使用临时的map来存储小写版本
                val lowerMap = mutableMapOf<String, String>()
                for ((key, value) in map.entries) {
                    lowerMap.putIfAbsent(key.lowercase(), value)
                }
                // 插入回返回索引
                for ((key, value) in lowerMap.entries) {
                    map.putIfAbsent(key.lowercase(), value)
                }
            }
        }
        logger.info("\"$path\" ${map.size} keywords loaded")
        return map
    }

    /**
     * 按照 `::` 分割关键字
     */
    private fun splitKeys(map: MutableMap<String, String>) {
        val subMap = mutableMapOf<String, String>()
        for (item in map) {
            val s = item.key.indexOf("::")
            if (s >= 0) {
                val sub = item.key.substring(s+2)
                if (!map.containsKey(sub)) {
                    subMap[sub] = item.value
                }
            }
        }
        if (subMap.isNotEmpty()) {
            splitKeys(subMap)
            for (item in subMap) {
                map.putIfAbsent(item.key, item.value)
            }
        }
    }

    override fun onEnable() {
        logger.info { "Plugin loaded" }
        PluginCommands.register()
        Data.initData()

        val eventChannel = GlobalEventChannel.parentScope(this)
        eventChannel.subscribeMessages {
            startsWith("c ") { checkC(it) }
            startsWith("C ") { checkC(it) }
            startsWith("cpp ") { checkCpp(it) }
            startsWith("CPP ") { checkCpp(it) }
            startsWith("c++ ") { checkCpp(it) }
            startsWith("C++ ") { checkCpp(it) }
            startsWith("qt ") { checkQt(it) }
            startsWith("Qt ") { checkQt(it) }
            startsWith("QT ") { checkQt(it) }
            startsWith("gcc ") { checkGcc(it) }
            startsWith("GCC ") { checkGcc(it) }
        }
    }

    /**
     * 查找C文档
     */
    private suspend fun MessageEvent.checkC(keyword: String) {
        if (keyword.isEmpty()) return
        logger.info("check c \"$keyword\"")
        findAndReply(indexC, keyword, cppreferencePrefix)
    }

    /**
     * 查找C++文档
     */
    private suspend fun MessageEvent.checkCpp(keyword: String) {
        if (keyword.isEmpty()) return
        logger.info("check cpp \"$keyword\"")
        val index = Data.getIndex(keyword)
        if (index != null) {
            subject.sendMessage(message.quote() + cppreferencePrefix + index.link)
        }
    }

    /**
     * 查找Qt文档
     */
    private suspend fun MessageEvent.checkQt(keyword: String) {
        if (keyword.isEmpty()) return
        logger.info("check qt \"$keyword\"")
        findAndReply(indexQt, keyword, qtDocPrefix)
    }

    /**
     * 查找GCC命令行文档
     */
    private suspend fun MessageEvent.checkGcc(keyword: String) {
        if (keyword.isEmpty()) return
        logger.info("check gcc \"$keyword\"")
        if (!findAndReply(indexGcc, keyword, gccDocPrefix, false) && keyword.startsWith('-')) {
            findAndReply(indexGcc, keyword.substring(1), gccDocPrefix, false)
        }
    }

    /**
     * 从索引中查找并拼接前缀回复发送者
     * @param index 索引
     * @param keyword 关键字
     * @param prefix 前缀
     * @param ignoreCase 忽略大小写 默认true
     * @return 返回是否回复
     */
    private suspend fun MessageEvent.findAndReply(index: Map<String, String>, keyword: String, prefix: String, ignoreCase: Boolean = true): Boolean {
        if (keyword.isEmpty()) return false
        val url = find(index, keyword, prefix) ?: if (ignoreCase) find(index, keyword.lowercase(), prefix) else null
        return if (url != null) {
            subject.sendMessage(message.quote() + url)
            true
        } else false
    }

    /**
     * 从索引中查找并拼接前缀返回
     * @param index 索引
     * @param keyword 关键字
     * @param prefix 前缀
     * @return 返回查找结果，未找到返回null
     */
    private fun find(index: Map<String, String>, keyword: String, prefix: String): String? {
        val value = index[keyword]
        return if (value != null) {
            prefix + value
        } else {
            null
        }
    }
}
