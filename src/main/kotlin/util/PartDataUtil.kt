package util

import io.ktor.http.content.*


inline fun <reified T:PartData> Iterable<PartData>.get(name:String): T? {
    return filterIsInstance<T>().singleOrNull { it.name==name }
}
