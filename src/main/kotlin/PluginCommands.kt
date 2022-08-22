package top.jie65535.jcr

import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand

object PluginCommands : CompositeCommand(
    JCppReferencePlugin, "jcr",
    description = "J CppReference Commands"
) {
    @SubCommand
    @Description("更新索引")
    suspend fun CommandSender.update() {
        Data.updateData()
        sendMessage("OK")
    }
}