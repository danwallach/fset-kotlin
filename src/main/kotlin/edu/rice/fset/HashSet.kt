package edu.rice.fset

import kotlin.collections.HashSet as HashSetMutable

/**
 * This "slow" implementation of FSet is terrible and should only be used for benchmarking.
 * It uses the underlying mutable hashset, which means that most operations are O(n).
 * The only redeeming feature of this implementation is that it's likely to be bug-free,
 * so usable for testing.
 */

data class HashSet<E : Any>(val hashSet: HashSetMutable<E>) : FSet<E> {
    override val size: Int
        get() = hashSet.size

    override fun contains(element: E) = hashSet.contains(element)

    override fun containsAll(elements: Collection<E>) = hashSet.containsAll(elements)

    override fun isEmpty() = hashSet.isEmpty()

    override fun iterator() = hashSet.iterator()

    override fun plus(element: E): FSet<E> {
        val newSet = HashSetMutable(hashSet) // deep copy
        newSet.add(element)
        return HashSet(newSet)
    }

    override fun minus(element: E): FSet<E> {
        val newSet = HashSetMutable(hashSet) // deep copy
        newSet.remove(element)
        return singletonEmptySetFrom(HashSet(newSet))
    }

    override fun lookup(hashValue: Int): Iterator<E> {
        // Normal hash sets don't expose anything quite like this, so we'll
        // have to do something really slow and painful.
        return hashSet.asIterable().filter { it.hashCode() == hashValue }.iterator()
    }
}

/** Creates a "slow" hash set from the given arguments. */
fun <E : Any> slowHashSetOf(vararg elements: E): FSet<E> =
    singletonEmptySetFrom(HashSet(hashSetOf(*elements)))

private val emptyHashSetSingleton: FSet<*> = HashSet(hashSetOf())

/** Creates an empty, "slow" hash set. */
@Suppress("UNCHECKED_CAST")
fun <E : Any> emptySlowHashSet(): FSet<E> = emptyHashSetSingleton as FSet<E>

/** Used to ensure that all "slow" hash sets with no elements become the singleton empty set. */
private fun <E : Any> singletonEmptySetFrom(input: FSet<E>): FSet<E> =
    if (input.isEmpty()) emptySlowHashSet<E>() else input
