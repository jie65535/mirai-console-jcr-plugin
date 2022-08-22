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
        version = "0.2.0"
    ) {
        author("jie65535")
        info("cppreference.com 帮助插件")
    }
) {
    private const val cppreferencePrefix = "https://zh.cppreference.com/w/"

    private fun loadMap(path: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val stream = this::class.java.getResourceAsStream(path)
        if (stream == null) {
            logger.error("资源文件为空")
        } else {
            val br = BufferedReader(InputStreamReader(stream))
            while (br.ready()) {
                val line = br.readLine()
                val s = line.indexOf('\t')
                if (s > 0) {
                    map[line.substring(0, s)] = line.substring(s+1)
                }
            }
        }
        logger.info("\"$path\" ${map.size} keywords loaded")
        return map
    }

    private val indexC by lazy { loadMap("/devhelp-index-c.txt") }
//    private val indexCpp by lazy { loadMap("/devhelp-index-cpp.txt") }

    override fun onEnable() {
        logger.info { "Plugin loaded" }
        PluginCommands.register()
        Data.initData()

        val eventChannel = GlobalEventChannel.parentScope(this)
        eventChannel.subscribeMessages {
            startsWith("c ") { keyword ->
                if (keyword.isEmpty()) return@startsWith
                logger.info("check c \"$keyword\"")
                val link = indexC[keyword]
                if (link != null) {
                    subject.sendMessage(message.quote() + cppreferencePrefix + link)
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
        }
    }
}
