package edu.rice.fset

/**
 * What happens when we have multiple values, with the same hash code, that we need to store? These
 * collisions will be rare, but they will happen, so we deal with them using internal lists, but
 * in the more common case of a single object, we don't want to pay that overhead. This code
 * will be helpful for multiple implementations. Of note, we're saving the hash value so we never
 * have to recompute it.
 *
 * Also of note: this code never calls the hashCode() method on anything being stored here. The
 * idea is that objects being stored in a given NodeStorage will always have the same hash, which
 * is computed externally. This allows for external code to tweak how it computes hashes.
 */
internal sealed class NodeStorage<E : Any>(val hashValue: Int)

internal class NodeStorageOne<E : Any>(hashValue: Int, val element: E) : NodeStorage<E>(hashValue) {
    override fun equals(other: Any?) = when (other) {
        is NodeStorageList<*> -> false
        is NodeStorageOne<*> -> other.hashValue == hashValue && other.element == element
        else -> false
    }

    override fun hashCode(): Int = hashValue

    override fun toString() = "NodeStorageOne(hashValue=$hashValue, element=$element)"
}

internal class NodeStorageList<E : Any>(hashValue: Int, val elements: List<E>) :
    NodeStorage<E>(hashValue) {
    override fun equals(other: Any?): Boolean = when (other) {
        is NodeStorageOne<*> -> false
        is NodeStorageList<*> ->
            // this is O(n^2), but n is small, so we don't really care
            other.hashValue == this.hashValue &&
                elements.size == other.elements.size &&
                elements.all {
                    other.elements.contains(
                        it
                    )
                }
        else -> false
    }

    override fun hashCode(): Int = hashValue

    override fun toString() = "NodeStorageList(hashValue=$hashValue, elements=$elements)"
}

internal fun <E : Any> NodeStorage<E>.insert(element: E): NodeStorage<E> =
    if (this.contains(element)) {
        this
    } else {
        when (this) {
            is NodeStorageOne -> NodeStorageList(hashValue, listOf(this.element, element))
            is NodeStorageList -> NodeStorageList(hashValue, this.elements + element)
        }
    }

internal fun <E : Any> nodeStorageOf(
    hashValue: Int,
    element: E,
    priorStorage: NodeStorage<E>? = null
): NodeStorage<E> =
    if (priorStorage == null) {
        NodeStorageOne(hashValue, element)
    } else {
        priorStorage.insert(element)
    }

internal operator fun <E : Any> NodeStorage<E>.get(element: E): E? = when (this) {
    is NodeStorageOne -> if (this.element == element) this.element else null
    is NodeStorageList -> this.elements.find { it == element }
}

internal fun <E : Any> NodeStorage<E>.contains(element: E) = this[element] != null

internal fun <E : Any> NodeStorage<E>.remove(element: E): NodeStorage<E>? = when (this) {
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

internal fun <E : Any> NodeStorage<E>.asSequence() = when (this) {
    is NodeStorageOne -> sequenceOf(element)
    is NodeStorageList -> elements.asSequence()
}

internal fun <E : Any> NodeStorage<E>.iterator() = this.asSequence().iterator()

internal fun <E : Any> NodeStorage<E>.isSingleton() = when (this) {
    is NodeStorageOne -> true
    is NodeStorageList -> false
}

internal val <E : Any> NodeStorage<E>.size: Int
    get() = when (this) {
        is NodeStorageOne -> 1
        is NodeStorageList -> elements.size
    }
