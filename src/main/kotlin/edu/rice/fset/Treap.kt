package edu.rice.fset

internal sealed interface Treap<E : Any>

internal fun treapEquals(t1: Treap<out Any>, t2: Treap<out Any>): Boolean = when {
    // We're going to take advantage of a specific property of treaps where the
    // priorities are based on the hashes -- they have a unique shape, regardless
    // of the construction order. This lends itself to a recursive algorithm
    // that just does structural equality.
    t1 is EmptyTreap && t2 is EmptyTreap -> true
    t1 is TreapNode && t2 is TreapNode ->
        t1.storage == t2.storage &&
            t1.priority == t2.priority &&
            treapEquals(t1.left, t2.left) &&
            treapEquals(t1.right, t2.right)
    else -> false
}

internal class EmptyTreap<E : Any> : Treap<E> {
    override fun equals(other: Any?) = when (other) {
        is Treap<*> -> treapEquals(this, other)
        else -> false
    }

    override fun hashCode() = 1
}

internal data class TreapNode<E : Any>(
    val storage: NodeStorage<E>,
    val priority: Int,
    val left: Treap<E>,
    val right: Treap<E>
) : Treap<E> {
    override fun equals(other: Any?) = when (other) {
        is Treap<*> -> treapEquals(this, other)
        else -> false
    }

    // We're again going to take advantage of the unique shape of these treaps,
    // giving us a more efficient construction for a hash over the treap.
    override fun hashCode() =
        left.hashCode() * 31 + storage.hashValue * 7 + right.hashCode()
}

internal val <E : Any> Treap<E>.size: Int
    get() = when (this) {
        is EmptyTreap -> 0
        is TreapNode -> storage.size + left.size + right.size
    }

internal fun <E : Any> Treap<E>.isEmpty() = when (this) {
    is EmptyTreap -> true
    is TreapNode -> false
}

internal fun <E : Any> TreapNode<E>.updateLeft(newLeft: Treap<E>) =
    TreapNode(storage, priority, newLeft, right)

internal fun <E : Any> TreapNode<E>.updateRight(newRight: Treap<E>) =
    TreapNode(storage, priority, left, newRight)

internal fun <E : Any> TreapNode<E>.updateStorage(newStorage: NodeStorage<E>) =
    TreapNode(newStorage, priority, left, right)

internal fun <E : Any> TreapNode<E>.rotateRight(): Treap<E> = when (left) {
    is EmptyTreap -> throw RuntimeException("cannot rotate right without a left subtree")
    is TreapNode -> left.updateRight(this.updateLeft(left.right))
}

internal fun <E : Any> TreapNode<E>.rotateLeft(): Treap<E> = when (right) {
    is EmptyTreap -> throw RuntimeException("cannot rotate left without a right subtree")
    is TreapNode -> right.updateLeft(this.updateRight(right.left))
}

/**
 * Given a treap node where the tree property is satisfied on the hash values,
 * this will do the necessary rotation to preserve the heap property, but only
 * on this specific node. This is normally called after insertion, on the way
 * back up, from the recursion.
 */
internal fun <E : Any> Treap<E>.treapify(): Treap<E> = when (this) {
    is EmptyTreap -> this
    is TreapNode -> {
        val lEmpty = left.isEmpty()
        val rEmpty = right.isEmpty()
        val lPriority = if (lEmpty) Int.MAX_VALUE else (left as TreapNode).priority
        val rPriority = if (rEmpty) Int.MAX_VALUE else (right as TreapNode).priority

        if (lEmpty && rEmpty) {
            this
        } else if (lEmpty) {
            if (rPriority < priority) {
                rotateLeft()
            } else {
                this
            }
        } else if (rEmpty) {
            if (lPriority < priority) {
                rotateRight()
            } else {
                this
            }
        } else {
            if (priority < lPriority && priority < rPriority) {
                this
            } else if (lPriority < priority && lPriority < rPriority) {
                rotateRight()
            } else {
                rotateLeft()
            }
        }
    }
}

internal fun <E : Any> Treap<E>.insert(hashValue: Int, element: E): Treap<E> = when (this) {
    is EmptyTreap -> TreapNode(nodeStorageOf(hashValue, element), hashValue.familyHash1(), this, this)
    is TreapNode -> {
        val localHashValue = storage.hashValue
        when {
            hashValue < localHashValue -> updateLeft(left.insert(hashValue, element)).treapify()
            hashValue > localHashValue -> updateRight(right.insert(hashValue, element)).treapify()
            else -> updateStorage(storage.insert(element))
        }
    }
}

internal fun <E : Any> Treap<E>.remove(hashValue: Int, element: E): Treap<E> = when (this) {
    is EmptyTreap -> this // nothing to remove!
    is TreapNode -> {
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
                        lEmpty && rEmpty -> emptyTreap()
                        lEmpty -> right
                        rEmpty -> left
                        (left as TreapNode).priority < (right as TreapNode).priority ->
                            rotateRight().remove(hashValue, element)
                        else -> rotateLeft().remove(hashValue, element)
                    }
                } else {
                    updateStorage(revisedStorage)
                }
            }
        }
    }
}

internal fun <E : Any> Treap<E>.lookup(hashValue: Int): Sequence<E> = when (this) {
    is EmptyTreap -> sequenceOf()
    is TreapNode -> {
        val localHashValue = storage.hashValue
        when {
            hashValue < localHashValue -> left.lookup(hashValue)
            hashValue > localHashValue -> right.lookup(hashValue)
            else -> storage.asSequence()
        }
    }
}

// internal fun <E : Any> Treap<E>.validate(lowerBound: Int = Int.MIN_VALUE, uppperBound: Int = Int.MAX_VALUE, priorityU) : Unit = when (this) {
//
// }

internal val emptyTreapSingleton: EmptyTreap<Any> = EmptyTreap()

@Suppress("UNCHECKED_CAST")
internal fun <E : Any> emptyTreap(): Treap<E> = emptyTreapSingleton as Treap<E>

/** Guarantees ordering of hash values, but no guarantees of ordering within a NodeStorage */
internal fun <E : Any> Treap<E>.storageSequence(): Sequence<NodeStorage<E>> = when (this) {
    is EmptyTreap -> sequenceOf()
    is TreapNode ->
        left.storageSequence() + sequenceOf(storage) + right.storageSequence()
}

internal fun <E : Any> Treap<E>.nodeDepths(priorDepth: Int = 1): Sequence<Int> = when (this) {
    is EmptyTreap -> sequenceOf()
    is TreapNode ->
        left.nodeDepths(priorDepth + 1) +
            sequenceOf(priorDepth) +
            right.nodeDepths(priorDepth + 1)
}

/** Warning: no ordering guarantees for objects with equal hashcodes */
internal fun <E : Any> Treap<E>.iterator() =
    this.storageSequence().flatMap { it.asSequence() }.iterator()

internal fun <E : Any> Treap<E>.debugPrint(depth: Int = 0): Unit = when (this) {
    is EmptyTreap -> { }
    is TreapNode -> {
        println("%s| storage: %s, priority: %d".format(" ".repeat(depth * 2), this.storage, this.priority))
        left.debugPrint(depth + 1)
        right.debugPrint(depth + 1)
    }
}

// /////////// Now, a binary tree "set" from the binary tree

internal data class TreapSet<E : Any>(val treap: Treap<E>) : FSet<E> {
    override val size: Int
        get() = treap.size

    override fun equals(other: Any?) = when (other) {
        is TreapSet<*> -> treap == other.treap
        else -> false
    }

    override fun hashCode() = treap.hashCode()

    override fun isEmpty() = treap.isEmpty()

    override fun iterator() = treap.iterator()

    override fun plus(element: E) =
        TreapSet(treap.insert(element.familyHash1(), element))

    override fun addAll(elements: Iterable<E>) =
        TreapSet(elements.fold(treap) { t, e -> t.insert(e.familyHash1(), e) })

    override fun minus(element: E) =
        TreapSet(treap.remove(element.familyHash1(), element))

    override fun removeAll(elements: Iterable<E>) =
        TreapSet(elements.fold(treap) { t, e -> t.remove(e.familyHash1(), e) })

    override fun lookup(element: E) =
        treap.lookup(element.familyHash1())
            .filter { it == element }
            .firstOrNull()

    override fun toString(): String {
        val result = treap.iterator().asSequence().joinToString(separator = ", ")
        return "TreapSet($result)"
    }

    override fun statistics(): String {
        val nodeDepths = treap.nodeDepths().toList()
        val maxDepth = nodeDepths.maxOrNull() ?: 0
        val avgDepth = nodeDepths.average()

        return "nodes: ${nodeDepths.size}, maxDepth: $maxDepth, averageDepth: $avgDepth"
    }

    override fun debugPrint() {
        treap.debugPrint()
    }
}

fun <E : Any> emptyTreapSet(): FSet<E> = TreapSet(emptyTreap())

fun <E : Any> treapSetOf(vararg elements: E) = elements.asIterable().toTreapSet()

fun <E : Any> Iterable<E>.toTreapSet() = emptyTreapSet<E>().addAll(this)
