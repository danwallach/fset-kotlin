package edu.rice.fset

@JvmInline
internal value class ArrayStore<E: Any>(val contents: Array<Any>)

@Suppress("UNCHECKED_CAST")
internal fun <E : Any> ArrayStore<E>.withoutIndex(index: Int) : ArrayStore<E> {
    // assumption: the index is in bounds for the array
    val newArray = arrayOfNulls<Any>(contents.size - 1)
    for (i in 0 until index) newArray[i] = contents[i]
    for (i in index until contents.size) newArray[i - 1] = contents[i]
    return ArrayStore(newArray as Array<Any>)
}

@Suppress("UNCHECKED_CAST")
internal operator fun <E : Any> ArrayStore<E>.get(index: Int): E = contents[index] as E

@Suppress("UNCHECKED_CAST")
internal fun <E : Any> ArrayStore<E>.withoutElement(element: E) : ArrayStore<E> =
    // assumption: the element occurs in the array at most once
    if (contains(element)) {
        val newArray = arrayOfNulls<Any>(contents.size - 1)
        var i: Int = 0
        while (i < contents.size) {
            if(contents[i] != element)
                newArray[i] = contents[i]
            i++
        }
        ArrayStore(newArray as Array<Any>)
    } else {
        this
    }

@Suppress("UNCHECKED_CAST")
internal fun <E : Any> ArrayStore<E>.contains(element: E) = contents.contains(element)

@Suppress("UNCHECKED_CAST")
internal fun <E : Any> ArrayStore<E>.insert(element: E, index: Int) : ArrayStore<E> {
    // assumption: the element is not present in the array
    val newArray = arrayOfNulls<Any>(contents.size + 1)
    for (i in 0 until index) newArray[i] = contents[i]
    newArray[index] = element
    for (i in index + 1 until contents.size) newArray[i] = contents[i - 1]
    return ArrayStore(newArray as Array<Any>)
}