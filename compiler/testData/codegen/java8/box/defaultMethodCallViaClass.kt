// FILE: Simple.java

interface Simple {
    default String test(String s) {
        return s + "K";
    }

    static String testStatic(String s) {
        return s + "K";
    }
}

// FILE: main.kt
// LANGUAGE_VERSION: 1.1
class Test : Simple {}

fun box(): String {
    val test = Test().test("O")
    if (test != "OK") return "fail $test"

    return Simple.testStatic("O")
}
