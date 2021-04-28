package edu.rice.fset

/**
 * What happens when we have multiple values, with the same hash code, that we need to store? These
 * collisions will be rare, but they will happen, so we deal with them using internal lists, but
 * in the more common case of a single object, we don't want to pay that overhead. This code
 * will be helpful for multiple implementations. Of note, we're saving the hash value so we never
 * have to recompute it.
 */
sealed interface NodeStorage<E : Any>

private data class NodeStorageOne<E : Any>(val hashValue: Int, val element: E) : NodeStorage<E> {
    override fun equals(other: Any?) = when (other) {
        is NodeStorageList<*> -> false
        is NodeStorageOne<*> -> other.hashValue == hashValue && other.element == element
        else -> false
    }

    override fun hashCode() = hashValue
}

private data class NodeStorageList<E : Any>(val hashValue: Int, val elements: List<E>) :
    NodeStorage<E> {
    override fun equals(other: Any?): Boolean = when (other) {
        is NodeStorageOne<*> -> false
        is NodeStorageList<*> -> other.hashValue == this.hashValue && elements.all {
            other.elements.contains(
                it
            )
        }
        else -> false
    }

    // don't try to be fancy here: this works
    override fun hashCode() = hashValue
}

fun <E : Any> NodeStorage<E>.insert(element: E): NodeStorage<E> =
    if (this.contains(element)) {
        this
    } else {
        when (this) {
            is NodeStorageOne -> NodeStorageList(hashValue, listOf(this.element, element))
            is NodeStorageList -> NodeStorageList(hashValue, this.elements + element)
        }
    }

fun <E : Any> nodeStorageOf(
    hashValue: Int,
    element: E,
    priorStorage: NodeStorage<E>? = null
): NodeStorage<E> =
    if (priorStorage == null) {
        NodeStorageOne(hashValue, element)
    } else {
        priorStorage.insert(element)
    }

fun <E : Any> NodeStorage<E>.contains(element: E): Boolean = when (this) {
    is NodeStorageOne -> this.element == element
    is NodeStorageList -> this.elements.contains(element)
}

fun <E : Any> NodeStorage<E>.remove(element: E): NodeStorage<E>? = when (this) {
    is NodeStorageOne -> if (this.element == element) null else this
    is NodeStorageList -> {
        val filteredList = elements.filter { it != element }
        if (filteredList.size > 1) {
            NodeStorageList(hashValue, filteredList)
        } else {
            NodeStorageOne(hashValue, filteredList[0])
        }
    }
}

fun <E : Any> NodeStorage<E>.asSequence() = when (this) {
    is NodeStorageOne -> sequenceOf(element)
    is NodeStorageList -> elements.asSequence()
}

fun <E : Any> NodeStorage<E>.iterator() = this.asSequence().iterator()

fun <E : Any> NodeStorage<E>.isSingleton() = when (this) {
    is NodeStorageOne -> true
    is NodeStorageList -> false
}
