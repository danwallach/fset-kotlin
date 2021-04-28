package edu.rice.fset

/**
 * General-purpose interface for a "functional" set over objects, assuming that the objects
 * correctly support equality and hashing. Notably absent from this definition is how you
 * go about creating instances of the set in the first place. That will be implementation-specific.
 * Also, multiple objects having the same hash value are supported, but if too many objects
 * share the same hash, performance might well degrade.
 */
interface FSet<E : Any> : Set<E> {
    // TODO: add covariance/contravariance support (makes things uglier)

    /**
     * Adds the given element to the set, returning a new set. The original is unchanged.
     */
    operator fun plus(element: E): FSet<E>

    // Engineering note: we could provide default implementations of addAll and removeAll
    // here that just build on plus and minus, but they'll be faster to do inside.

    /**
     * Adds a number of elements to the set, returning a new set. The original is unchanged.
     */
    fun addAll(elements: Iterable<E>): FSet<E>

    /**
     * Adds a number of elements to the set, returning a new set. The original is unchanged.
     */
    fun addAll(vararg elements: E): FSet<E> = addAll(elements.asIterable())

    /**
     * Subtracts the given element to the set, if it's present, returning a new set.
     * If absent, the original set is returned. The original set is never changed.
     */
    operator fun minus(element: E): FSet<E>

    /**
     * Removes a number of elements from the set, returning a new set. The original is unchanged.
     */
    fun removeAll(elements: Iterable<E>): FSet<E>

    /**
     * Removes a number of elements from the set, returning a new set. The original is unchanged.
     */
    fun removeAll(vararg elements: E): FSet<E> = removeAll(elements.asIterable())

    /**
     * Given the *hash* of the desired element, returns zero or more matching results
     * as an iterator.
     */
    fun lookup(hashValue: Int): Iterator<E>

    override fun contains(element: E): Boolean =
        lookup(element.hashCode())
            .asSequence()
            .firstOrNull { it == element } != null

    override fun containsAll(elements: Collection<E>): Boolean =
        elements.all { contains(it) }
}
