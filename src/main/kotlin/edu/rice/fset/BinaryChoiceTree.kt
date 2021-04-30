package edu.rice.fset

import kotlin.random.Random

/**
 * An unbalanced binary tree that switches left or right based on the hash values
 * of the elements rather than relying on elements being comparable. Unlike
 * [BinaryTreeSet], this version uses the "power of choices", by having two
 * separate hash values (via [Any.familyHash2]), allowing each value to have
 * two possible destinations in the tree. Whichever destination results in
 * a lower tree depth will be selected.
 */
internal sealed interface BinaryChoiceTree<E : Any>

internal class EmptyBinaryChoiceTree<E : Any> : BinaryChoiceTree<E> {
    override fun equals(other: Any?) = when (other) {
        is EmptyBinaryChoiceTree<*> -> true
        else -> false
    }

    override fun hashCode() = 1
}

internal data class BinaryChoiceTreeNode<E : Any>(
    val storage: NodeStorage<E>,
    val left: BinaryChoiceTree<E>,
    val right: BinaryChoiceTree<E>
) : BinaryChoiceTree<E> {
    override fun equals(other: Any?) = when (other) {
        is BinaryChoiceTreeNode<*> -> other.storageSequence() == this.storageSequence()
        else -> false
    }

    // Notable: Sequence doesn't give us a useful hash function, but List does,
    // so converting to a list makes this work. This whole thing is more inefficient
    // than something we might cook up by hand, but it's probably not common that
    // we'll be building a set of sets, which is when this might come into play.
    override fun hashCode() = storageSequence().toList().hashCode()
}

internal val <E : Any> BinaryChoiceTree<E>.size: Int
    get() = when (this) {
        is EmptyBinaryChoiceTree -> 0
        is BinaryChoiceTreeNode -> storage.size + left.size + right.size
    }

internal fun <E : Any> BinaryChoiceTree<E>.isEmpty() = when (this) {
    is EmptyBinaryChoiceTree -> true
    is BinaryChoiceTreeNode -> false
}

internal fun <E : Any> BinaryChoiceTreeNode<E>.updateLeft(newLeft: BinaryChoiceTree<E>) =
    BinaryChoiceTreeNode(storage, newLeft, right)

internal fun <E : Any> BinaryChoiceTreeNode<E>.updateRight(newRight: BinaryChoiceTree<E>) =
    BinaryChoiceTreeNode(storage, left, newRight)

internal fun <E : Any> BinaryChoiceTreeNode<E>.updateStorage(newStorage: NodeStorage<E>) =
    BinaryChoiceTreeNode(newStorage, left, right)

internal fun <E : Any> BinaryChoiceTreeNode<E>.rotateRight(): BinaryChoiceTree<E> = when (left) {
    is EmptyBinaryChoiceTree -> throw RuntimeException("cannot rotate right without a left subtree")
    is BinaryChoiceTreeNode -> left.updateRight(this.updateLeft(left.right))
}

internal fun <E : Any> BinaryChoiceTreeNode<E>.rotateLeft(): BinaryChoiceTree<E> = when (right) {
    is EmptyBinaryChoiceTree -> throw RuntimeException("cannot rotate left without a right subtree")
    is BinaryChoiceTreeNode -> right.updateLeft(this.updateRight(right.left))
}

private data class FindResult<E : Any>(val hashValue: Int, val element: E?, val depth: Int)

private fun <E : Any> BinaryChoiceTree<E>.searchForElement(
    hashValue: Int,
    element: E,
    depth: Int = 0,
): FindResult<E> = when (this) {
    is EmptyBinaryChoiceTree -> FindResult(hashValue, null, depth)
    is BinaryChoiceTreeNode -> if (storage.hashValue == hashValue) {
        FindResult(hashValue, storage[element], depth)
    } else if (hashValue < storage.hashValue) {
        left.searchForElement(hashValue, element, depth + 1)
    } else {
        right.searchForElement(hashValue, element, depth + 1)
    }
}

private fun <E : Any> BinaryChoiceTree<E>.searchForElement(element: E) =
    element.familyHash2().map { this.searchForElement(it, element, 0) }

internal fun <E : Any> BinaryChoiceTree<E>.insert(element: E): BinaryChoiceTree<E> {
    // This might seem like unnecessary work, but we know that the element might
    // already be located at any of the possible hash locations, so we have to
    // see if it's already there before attempting to insert it. That means trying
    // every possible path. Only if it's absent do we need to know the shortest path
    // to where it really should be.

    val searchResults = this.searchForElement(element)

    if (searchResults.isEmpty()) {
        throw RuntimeException("shouldn't ever have empty search results")
    }

    val resultsWithElement = searchResults.filter { it.element != null }

    return when (resultsWithElement.size) {
        0 -> {
            val minDepth = searchResults.minOf { it.depth }
            val smallestPath = searchResults.filter { it.depth == minDepth }
            when (smallestPath.size) {
                0 -> throw RuntimeException("should always have at least one element of min size")
                1 -> this.insert(smallestPath[0].hashValue, element)
                // We have multiple choices at the same depth, so we'll pick one at random,
                // since we don't want to have any biases.
                else -> this.insert(smallestPath[Random.nextInt(smallestPath.size)].hashValue, element)
            }
        }
        1 -> this.insert(resultsWithElement[0].hashValue, element)
        else -> throw RuntimeException("shouldn't ever have multiple search results")
    }
}

internal fun <E : Any> BinaryChoiceTree<E>.insert(hashValue: Int, element: E): BinaryChoiceTree<E> =
    when (this) {
        is EmptyBinaryChoiceTree -> BinaryChoiceTreeNode(nodeStorageOf(hashValue, element), this, this)
        is BinaryChoiceTreeNode -> {
            val localHashValue = storage.hashValue
            when {
                hashValue < localHashValue -> updateLeft(left.insert(hashValue, element))
                hashValue > localHashValue -> updateRight(right.insert(hashValue, element))
                else -> updateStorage(storage.insert(element))
            }
        }
    }

internal fun <E : Any> BinaryChoiceTree<E>.remove(element: E): BinaryChoiceTree<E> {
    val searchResults = this.searchForElement(element)

    if (searchResults.isEmpty()) {
        throw RuntimeException("shouldn't ever have empty search results")
    }

    val resultsWithElement = searchResults.filter { it.element != null }

    return when (resultsWithElement.size) {
        0 -> return this // the element we want to remove is absent from this tree
        1 -> this.remove(resultsWithElement[0].hashValue, element)
        else -> throw RuntimeException("shouldn't ever have multiple search results")
    }
}

internal fun <E : Any> BinaryChoiceTree<E>.remove(hashValue: Int, element: E): BinaryChoiceTree<E> =
    when (this) {
        is EmptyBinaryChoiceTree -> this // nothing to remove!
        is BinaryChoiceTreeNode -> {
            val localHashValue = storage.hashValue
            when {
                hashValue < localHashValue -> updateLeft(left.remove(hashValue, element))
                hashValue > localHashValue -> updateRight(right.remove(hashValue, element))
                else -> {
                    // If there's only one thing in the storage, then this whole node
                    // needs to go away. But if there's more than one thing here, we
                    // only need to update the storage and we're still fine.
                    val revisedStorage = storage.remove(element)
                    if (revisedStorage == null) {
                        val lEmpty = left.isEmpty()
                        val rEmpty = right.isEmpty()
                        when {
                            lEmpty && rEmpty -> emptyBinaryChoiceTree()
                            lEmpty -> right
                            rEmpty -> left
                            else -> if (Random.nextBoolean()) {
                                // coin toss to decide which way to rotate
                                rotateRight().remove(hashValue, element)
                            } else {
                                rotateLeft().remove(hashValue, element)
                            }
                        }
                    } else {
                        updateStorage(revisedStorage)
                    }
                }
            }
        }
    }

internal fun <E : Any> BinaryChoiceTree<E>.lookup(element: E): E? =
    element.familyHash2().asIterable().firstNotNullOfOrNull { h -> this.lookup(h, element) }

internal fun <E : Any> BinaryChoiceTree<E>.lookup(hashValue: Int, element: E): E? = when (this) {
    is EmptyBinaryChoiceTree -> null
    is BinaryChoiceTreeNode -> {
        val localHashValue = storage.hashValue
        when {
            hashValue < localHashValue -> left.lookup(hashValue, element)
            hashValue > localHashValue -> right.lookup(hashValue, element)
            else -> storage.get(element)
        }
    }
}

internal val emptyChoiceTreeSingleton: EmptyBinaryChoiceTree<Any> = EmptyBinaryChoiceTree()

@Suppress("UNCHECKED_CAST")
internal fun <E : Any> emptyBinaryChoiceTree(): BinaryChoiceTree<E> = emptyChoiceTreeSingleton as BinaryChoiceTree<E>

/** Guarantees ordering of hash values, but no guarantees of ordering within a NodeStorage */
internal fun <E : Any> BinaryChoiceTree<E>.storageSequence(): Sequence<NodeStorage<E>> = when (this) {
    is EmptyBinaryChoiceTree -> sequenceOf()
    is BinaryChoiceTreeNode ->
        left.storageSequence() + sequenceOf(storage) + right.storageSequence()
}

internal fun <E : Any> BinaryChoiceTree<E>.nodeDepths(priorDepth: Int = 1): Sequence<Int> = when (this) {
    is EmptyBinaryChoiceTree -> sequenceOf()
    is BinaryChoiceTreeNode ->
        left.nodeDepths(priorDepth + 1) +
            sequenceOf(priorDepth) +
            right.nodeDepths(priorDepth + 1)
}

/** Warning: no ordering guarantees for objects with equal hashcodes */
internal fun <E : Any> BinaryChoiceTree<E>.iterator() =
    this.storageSequence().flatMap { it.asSequence() }.iterator()

internal fun <E : Any> BinaryChoiceTree<E>.debugPrint(depth: Int = 0): Unit = when (this) {
    is EmptyBinaryChoiceTree -> { }
    is BinaryChoiceTreeNode -> {
        println("%s| storage: %s".format(" ".repeat(depth * 2), this.storage))
        left.debugPrint(depth + 1)
        right.debugPrint(depth + 1)
    }
}

// /////////// Now, a binary tree "set" from the binary tree

internal data class BinaryChoiceTreeSet<E : Any>(val tree: BinaryChoiceTree<E>) : FSet<E> {
    override val size: Int
        get() = tree.size

    override fun equals(other: Any?) = when (other) {
        is BinaryChoiceTreeSet<*> -> tree == other.tree
        else -> false
    }

    override fun hashCode() = tree.hashCode()

    override fun isEmpty() = tree.isEmpty()

    override fun iterator() = tree.iterator()

    override fun plus(element: E) =
        BinaryChoiceTreeSet(tree.insert(element))

    override fun addAll(elements: Iterable<E>) =
        BinaryChoiceTreeSet(elements.fold(tree) { t, e -> t.insert(e) })

    override fun minus(element: E) =
        BinaryChoiceTreeSet(tree.remove(element))

    override fun removeAll(elements: Iterable<E>) =
        BinaryChoiceTreeSet(elements.fold(tree) { t, e -> t.remove(e) })

    override fun lookup(element: E) = tree.lookup(element)

    override fun toString(): String {
        val result = tree.iterator().asSequence().joinToString(separator = ", ")
        return "BinaryChoiceTreeSet($result)"
    }

    override fun statistics(): String {
        val nodeDepths = tree.nodeDepths().toList()
        val maxDepth = nodeDepths.maxOrNull() ?: 0
        val avgDepth = nodeDepths.average()

        return "nodes: ${nodeDepths.size}, maxDepth: $maxDepth, avgDepth: %.2f".format(avgDepth)
    }

    override fun debugPrint() {
        tree.debugPrint()
    }
}

fun <E : Any> emptyBinaryChoiceTreeSet(): FSet<E> = BinaryChoiceTreeSet(emptyBinaryChoiceTree())

fun <E : Any> binaryChoiceTreeSetOf(vararg elements: E) = elements.asIterable().toBinaryChoiceTreeSet()

fun <E : Any> Iterable<E>.toBinaryChoiceTreeSet() = emptyBinaryChoiceTreeSet<E>().addAll(this)
