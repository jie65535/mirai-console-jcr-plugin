package top.jie65535.jcr

import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.subscribeMessages
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import net.mamoe.mirai.utils.info
import java.io.BufferedReader
import java.io.InputStreamReader

object JCppReferencePlugin : KotlinPlugin(
    JvmPluginDescription(
        id = "top.jie65535.mirai-console-jcr-plugin",
        name = "J Cpp Reference Plugin",
        version = "0.3.0"
    ) {
        author("jie65535")
        info("C++ 参考搜索插件")
    }
) {
    private const val cppreferencePrefix = "https://zh.cppreference.com/w/"
    private const val qtDocPrefix = "https://doc.qt.io/qt-5/"

    private fun loadMap(path: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val stream = this::class.java.getResourceAsStream(path)
        if (stream == null) {
            logger.error("资源文件为空")
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
                        val lowerKey = key.lowercase()
                        map.putIfAbsent(lowerKey, url)
                    }
                }
            }
            splitKeys(map)
        }
        logger.info("\"$path\" ${map.size} keywords loaded")
        return map
    }

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

    private val indexC by lazy { loadMap("/devhelp-index-c.txt") }
    private val indexQt by lazy { loadMap("/qt-5.13.0.txt") }

    override fun onEnable() {
        logger.info { "Plugin loaded" }
        PluginCommands.register()
        Data.initData()

        val eventChannel = GlobalEventChannel.parentScope(this)
        eventChannel.subscribeMessages {

            startsWith("c ") { keyword ->
                if (keyword.isEmpty()) return@startsWith
                logger.info("check c \"$keyword\"")
                var cLink = indexC[keyword]
                if (cLink != null) {
                    subject.sendMessage(message.quote() + cppreferencePrefix + cLink)
                } else {
                    cLink = indexC[keyword.lowercase()]
                    if (cLink != null) {
                        subject.sendMessage(message.quote() + cppreferencePrefix + cLink)
                    }
                }
            }

            startsWith("cpp ") { keyword ->
                if (keyword.isEmpty()) return@startsWith
                logger.info("check cpp \"$keyword\"")
                val index = Data.getIndex(keyword)
                if (index != null) {
                    subject.sendMessage(message.quote() + cppreferencePrefix + index.link)
                }
            }

            startsWith("qt ") { keyword ->
                if (keyword.isEmpty()) return@startsWith
                logger.info("check qt \"$keyword\"")
                var qtLink = indexQt[keyword]
                if (qtLink != null) {
                    subject.sendMessage(message.quote() + qtDocPrefix + qtLink)
                } else {
                    qtLink = indexQt[keyword.lowercase()]
                    if (qtLink != null) {
                        subject.sendMessage(message.quote() + qtDocPrefix + qtLink)
                    }
                }
            }
        }
    }
}
