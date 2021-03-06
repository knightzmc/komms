@file:Suppress("UNCHECKED_CAST")

package me.bristermitten.komms.command

import me.bristermitten.komms.argument.ArgumentSnapshot
import me.bristermitten.komms.sender.LoggingSender
import me.bristermitten.komms.sender.Sender
import me.bristermitten.komms.spec.CommandSpec
import me.bristermitten.komms.spec.SenderSpec
import kotlin.reflect.full.withNullability

/**
 * @author AlexL
 */
fun <T : Sender<*>> command(name: String, body: CommandSpec<T>.() -> Unit) {

}

@JvmName("commandGenericSender")
fun command(name: String, body: CommandSpec<*>.() -> Unit) {

}


/* 0 arguments */
inline fun <reified S : Sender<*>> command(
    name: String,
    crossinline body: S.() -> Unit,
): Command<S> {
    return Command(name, S::class, emptyList()) {
        body()
    }
}

/* 1 argument */
inline fun <reified S : Sender<*>, T : Any> command(
    name: String,
    arg1: ArgumentSnapshot<T>,
    crossinline body: S.(T) -> Unit,
): Command<S> {
    return Command(name, S::class, listOf(arg1)) { args ->
        body(args[0] as T)
    }
}

/* 2 arguments */
inline fun <reified S : Sender<*>, A : Any, B : Any> command(
    name: String,
    arg1: ArgumentSnapshot<A>,
    arg2: ArgumentSnapshot<B>,
    crossinline body: S.(A, B) -> Unit,
): Command<S> {
    return Command(name, S::class, listOf(arg1, arg2)) { args ->
        body(args[0] as A, args[1] as B)
    }
}

fun <T : Any> loggingSenderCommand(
    name: String,
    arg1: ArgumentSnapshot<T>,
    body: LoggingSender.(T) -> Unit,
): Command<LoggingSender> {
    return command(name, arg1, body)
}


@JvmName("stringArg")
fun arg(name: String) = arg<String>(name)

fun vararg(name: String) = arg<List<String>>(name)

inline fun <reified T : Any> arg(name: String) = ArgumentSnapshot<T>(name, T::class)


fun <T: Any> ArgumentSnapshot<T>.optional() = ArgumentSnapshot<T?>(name, klass, type.withNullability(true))


inline fun <reified T: Any> sender() : SenderSpec<T> {
 return object: SenderSpec<T>{}
}
