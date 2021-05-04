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
    fun add(vararg elements: E) = addAll(elements.asIterable())

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
    fun remove(vararg elements: E) = removeAll(elements.asIterable())

    /**
     * Given a desired element, returns that element (or whatever we have that's "equal" to it),
     * or null if it's absent.
     */
    fun lookup(element: E): E?

    /**
     * Given an element E, if it's "equal" to something that's already present, this returns
     * a new set, with the old element replaced by the new one. If the element is not present,
     * the original set is returned.
     */
    fun update(element: E): FSet<E>

    override fun contains(element: E) = lookup(element) != null

    override fun containsAll(elements: Collection<E>) = elements.all { contains(it) }

    /**
     * Returns internal statistics about the performance of the data structure. Useful
     * for printing. Used for internal debugging.
     */
    fun statistics(): String

    /**
     * Prints the tree in a verbose way to stdout. Used for internal debugging.
     */
    fun debugPrint(): Unit
}
