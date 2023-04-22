package top.jie65535.jcr

import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand

object PluginCommands : CompositeCommand(
    JCppReferencePlugin, "jcr",
    description = "J CppReference Commands"
) {
    @SubCommand
    @Description("查询帮助")
    suspend fun CommandSender.help() {
        sendMessage("""
            c <keyword>   # 查询C标准库
            cpp <keyword> # 查询C++标准库
            qt <keyword>  # 查询Qt类库
            gcc <keyword> # 查询GCC命令行选项
        """.trimIndent())
    }
}