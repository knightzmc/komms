package me.bristermitten.komms

import me.bristermitten.komms.argument.ArgumentSnapshot
import me.bristermitten.komms.argument.RealizedArgument
import me.bristermitten.komms.argument.parser.ArgumentParsers
import me.bristermitten.komms.argument.parser.ParseResult
import me.bristermitten.komms.argument.parser.Success
import me.bristermitten.komms.argument.parser.defaults.registerDefaultParsers
import me.bristermitten.komms.command.Command
import me.bristermitten.komms.command.CommandMap
import me.bristermitten.komms.command.RealizedCommand
import me.bristermitten.komms.command.RegisteredCommand
import me.bristermitten.komms.platform.BasePlatform
import me.bristermitten.komms.platform.Platform
import me.bristermitten.komms.sender.Sender
import java.util.*
import kotlin.reflect.full.isSubclassOf

/**
 * @author AlexL
 */
class CommandHandler {
    private val commands = CommandMap()
    val parsers = ArgumentParsers()

    var platform: Platform = BasePlatform


    init {
        parsers.registerDefaultParsers()
    }

    fun <T : Sender<*>> registerCommand(command: Command<T>): RegisteredCommand<*> {
        val name = command.name
        val realizedArguments = command.argumentTypes.map {
            realizeArgument(it)
        }
        val body = command.body
        val realizedCommand = RealizedCommand(
            name, command.senderType, realizedArguments, body
        )

        commands.register(realizedCommand)
        platform.registerPlatformCommand(command)
        return realizedCommand
    }

    private fun <T : Any> realizeArgument(argument: ArgumentSnapshot<T>): RealizedArgument<T> {
        val name = argument.name
        val type = argument.type

        val parser = parsers.resolveParser(type) ?: throw IllegalStateException("No parser for type $type")

        return RealizedArgument(name, type, parser)
    }

    fun <T : Sender<*>> handle(sender: T, input: String) {
        val parts = input.split(" ")
        val name = parts.firstOrNull() ?: return

        val matchingCommands = commands[name.toLowerCase()] ?: return sender.reply("Unknown command $name")
        val args = parts.drop(1)

        for (command in matchingCommands) {
            if (!command.senderType.isSubclassOf(sender::class)) {
                continue
            }

            val parsed = tryParse(args.toList(), command)
            val successes = parsed.filterIsInstance<Success<*>>()
            if (successes.size != parsed.size) {
                continue
            }
            @Suppress("UNCHECKED_CAST")
            (command as RegisteredCommand<T>).execute(sender, successes.map { it.value })
            return
        }

        sender.reply("Illegal arguments $args")
    }

    private fun tryParse(args: List<String>, command: RegisteredCommand<*>): List<ParseResult<*>> {
        val arguments = command.arguments
        val argStack = ArrayDeque(args)

        return arguments.map {
            it.parser.parse(argStack)
        }
    }
}
