// !WITH_NEW_INFERENCE
// NI_EXPECTED_FILE
val <!NI;IMPLICIT_NOTHING_PROPERTY_TYPE!>x<!> get() = <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!>()
val y get() = <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>bar<!>()

fun <E> foo(): E = null!!
fun <E> bar(): List<E> = null!!
