// TARGET_BACKEND: JVM
// WITH_RUNTIME
// LANGUAGE_VERSION: 1.2
// FILE: test.kt
fun box(): String {
    Foo(logged("i", 1.let { it }), logged("j", 2))

    val result = log.toString()
    if (result != "ij<clinit><init>") return "Fail: '$result'"

    return "OK"
}

// FILE: util.kt
val log = StringBuilder()

fun <T> logged(msg: String, value: T): T {
    log.append(msg)
    return value
}

// FILE: Foo.kt
class Foo(i: Int, j: Int) {
    init {
        log.append("<init>")
    }

    companion object {
        init {
            log.append("<clinit>")
        }
    }
}



