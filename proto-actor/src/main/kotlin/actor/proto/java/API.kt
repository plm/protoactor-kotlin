@file:JvmName("Actors")
@file:JvmMultifileClass
package actor.proto.java

import actor.proto.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.future.asCompletableFuture
import kotlinx.coroutines.experimental.future.await
import java.time.Duration
import java.util.concurrent.CompletableFuture

private val done: CompletableFuture<Void> = CompletableFuture.completedFuture(null)
fun done(): CompletableFuture<Void> = done

fun fromProducer(producer: () -> Actor): Props {
    return actor.proto.fromProducer {
        val actor = producer()
        val ctx = ContextImpl(actor)
        object : actor.proto.Actor  {
            suspend override fun receive(context: actor.proto.Context) {
                actor.receive(ctx.wrap(context)).await()
            }
        }
    }
}

fun fromFunc(receive : (Context) -> CompletableFuture<*>) {
    val actor = object : Actor {
        override fun receive(context: Context): CompletableFuture<*> = receive(context)
    }
    val ctx = ContextImpl(actor)
    object : actor.proto.Actor {
        suspend override fun receive(context: actor.proto.Context) {
            actor.receive(ctx.wrap(context)).await()
        }
    }
}

fun spawn(props: Props): PID {
    val name = ProcessRegistry.nextId()
    return spawnNamed(props, name)
}

fun spawnPrefix(props: Props, prefix: String): PID {
    val name = prefix + ProcessRegistry.nextId()
    return spawnNamed(props, name)
}

fun spawnNamed(props: Props, name: String): PID {
    return props.spawn(name, null)
}

fun send(target: PID, message: Any) = DefaultActorClient.send(target, message)
fun request(target: PID, message: Any, sender: PID) = DefaultActorClient.request(target, message, sender)
fun <T> requestAwait(target: PID, message: Any, timeout: Duration): CompletableFuture<T> {
    val d = async(CommonPool) {
        DefaultActorClient.requestAwait<T>(target, message, timeout)
    }
    return d.asCompletableFuture()
}
