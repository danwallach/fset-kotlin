package edu.rice

/**
 * General-purpose interface for a "functional" set over objects, assuming that the objects
 * correctly support equality and hashing. Notably absent from this definition is how you
 * go about creating instances of the set in the first place. That will be implementation-specific.
 * Also, multiple objects having the same hash value are supported, but if too many objects
 * share the same hash, performance might well degrade.
 */
interface FSet<E: Any>: Set<E> {
    // TODO: add covariance/contravariance support (makes things uglier)

    /**
     * Adds the given element to the set, returning a new set. The original is unchanged.
     */
    operator fun plus(element: E): FSet<E>

    /**
     * Subtracts the given element to the set, if it's present, returning a new set.
     * If absent, the original set is returned. The original set is never changed.
     */
    operator fun minus(element: E): FSet<E>

    /**
     * Given the *hash* of the desired element, returns zero or more matching results
     * as an iterator.
     */
    fun lookup(hashValue: Int): Iterator<E>
}
