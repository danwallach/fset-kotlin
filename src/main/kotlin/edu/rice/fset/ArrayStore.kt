package edu.rice.fset

/**
 * The ArrayStore value class is a wrapper around basic Java arrays that adds
 * all the functionality that we need in HAMTs and elsewhere. By having all
 * the logic here, the HAMT code gets cleaner. By using a "value class", no
 * extra memory is allocated to use an ArrayStore, yielding a significant
 * speedup relative to ArrayList (maybe 2x).
 */
@JvmInline
internal value class ArrayStore<E : Any>(val contents: Array<Any>)

@Suppress("UNCHECKED_CAST")
internal fun <E : Any> Iterable<E>.toArrayStore(): ArrayStore<E> {
    val list = this.toList()
    val newArray = arrayOfNulls<Any>(list.size)
    var i = 0
    for (e in list) {
        newArray[i++] = e
    }
    return ArrayStore(newArray as Array<Any>)
}

@Suppress("UNCHECKED_CAST")
internal fun <E : Any> ArrayStore<E>.deepEquals(other: ArrayStore<E>): Boolean {
    if (contents.size != other.contents.size) return false
    for (i in 0 until contents.size)
        if (contents[i] != other.contents[i]) return false
    return true
}

@Suppress("UNCHECKED_CAST")
internal fun <E : Any> ArrayStore<E>.withoutIndex(index: Int): ArrayStore<E> {
    // assumption: the index is in bounds for the array
    val newArray = arrayOfNulls<Any>(contents.size - 1)
    for (i in 0 until index) newArray[i] = contents[i]
    for (i in index until contents.size - 1) newArray[i] = contents[i + 1]
    return ArrayStore(newArray as Array<Any>)
}

@Suppress("UNCHECKED_CAST")
internal operator fun <E : Any> ArrayStore<E>.get(index: Int): E = contents[index] as E

@Suppress("UNCHECKED_CAST")
internal fun <E : Any> ArrayStore<E>.withoutElement(element: E): ArrayStore<E> =
    // we'll only remove the first occurance
    if (contains(element)) {
        val newArray = arrayOfNulls<Any>(contents.size - 1)
        var i: Int = 0
        loop1@ while (i < contents.size) {
            if (contents[i] != element) {
                newArray[i] = contents[i]
                i++
            } else {
                i++
                break@loop1
            }
        }
        while (i < contents.size) {
            newArray[i - 1] = contents[i]
            i++
        }
        ArrayStore(newArray as Array<Any>)
    } else {
        this
    }

@Suppress("UNCHECKED_CAST")
internal fun <E : Any> ArrayStore<E>.updateElement(element: E): ArrayStore<E> {
    var updated: Boolean = false
    // we'll only update the first occurance
    val newArray = arrayOfNulls<Any>(contents.size)
    var i: Int = 0
    loop1@ while (i < contents.size) {
        if (contents[i] == element) {
            updated = true
            newArray[i] = element
            i++
            break@loop1
        } else {
            newArray[i] = contents[i]
            i++
        }
    }
    while (i < contents.size) {
        newArray[i] = contents[i]
        i++
    }
    return if (updated) ArrayStore(newArray as Array<Any>) else this
}

@Suppress("UNCHECKED_CAST")
inline internal fun <E : Any> ArrayStore<E>.updateOffset(offset: Int, element: E): ArrayStore<E> {
    val newArray = arrayOfNulls<Any>(contents.size)
    for (i in 0 until contents.size) {
        newArray[i] = if (i == offset) {
            element
        } else {
            contents[i]
        }
    }
    return ArrayStore(newArray as Array<Any>)
}

internal operator fun <E : Any> ArrayStore<E>.contains(element: E) = contents.contains(element)

internal fun <E : Any> ArrayStore<E>.find(element: E): E? {
    for (i in 0 until contents.size)
        if (contents[i] == element) return element
    return null
}

@Suppress("UNCHECKED_CAST")
internal fun <E : Any> ArrayStore<E>.insert(element: E, index: Int): ArrayStore<E> {
    // assumption: the element is not present in the array
    val newArray = arrayOfNulls<Any>(contents.size + 1)
    for (i in 0 until index) newArray[i] = contents[i]
    newArray[index] = element
    for (i in index + 1 until contents.size + 1) newArray[i] = contents[i - 1]
    return ArrayStore(newArray as Array<Any>)
}

@Suppress("UNCHECKED_CAST")
internal fun <E : Any> ArrayStore<E>.mapIndexed(mapFunc: (Int, E) -> E): ArrayStore<E> {
    val newArray = arrayOfNulls<Any>(contents.size)
    for (i in 0 until contents.size)
        newArray[i] = mapFunc(i, contents[i] as E)
    return ArrayStore(newArray as Array<Any>)
}

internal fun <E : Any, R : Any> ArrayStore<E>.flatMap(mapFunc: (E) -> Sequence<R>): Sequence<R> =
    asSequence().flatMap(mapFunc)

internal fun <E : Any> ArrayStore<E>.joinToString(separator: String = "") =
    asSequence().joinToString(separator)

@Suppress("UNCHECKED_CAST")
internal fun <E : Any> ArrayStore<E>.asSequence(): Sequence<E> =
    contents.asSequence() as Sequence<E>

internal fun <E : Any> ArrayStore<E>.toList(): List<E> = asSequence().toList()

internal fun <E : Any> ArrayStore<E>.toSet(): Set<E> = asSequence().toSet()

internal fun <E : Any> ArrayStore<E>.forEach(consumeFunc: (E) -> Unit): Unit =
    asSequence().forEach(consumeFunc)

@Suppress("UNCHECKED_CAST")
internal fun <E : Any> ArrayStore<E>.append(element: E): ArrayStore<E> {
    val newArray = arrayOfNulls<Any>(contents.size + 1)
    for (i in 0 until contents.size)
        newArray[i] = contents[i]
    newArray[contents.size] = element
    return ArrayStore(newArray as Array<Any>)
}

internal fun <E : Any> ArrayStore<E>.size() = contents.size

internal fun <E : Any> ArrayStore<E>.isEmpty() = contents.isEmpty()

@Suppress("UNCHECKED_CAST")
internal fun <E : Any> arrayStoreOne(element: E): ArrayStore<E> {
    val newArray = arrayOfNulls<Any>(1)
    newArray[0] = element
    return ArrayStore(newArray as Array<Any>)
}

@Suppress("UNCHECKED_CAST")
internal fun <E : Any> arrayStoreTwo(e0: E, e1: E): ArrayStore<E> {
    val newArray = arrayOfNulls<Any>(2)
    newArray[0] = e0
    newArray[1] = e1
    return ArrayStore(newArray as Array<Any>)
}