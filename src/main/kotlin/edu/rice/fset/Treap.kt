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

internal data class TreapNode<E : Any> (
    val storage: NodeStorage<E>,
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
        left.hashCode() * 31 + storage.hashCode() * 7 + right.hashCode()
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

internal fun <E : Any> TreapNode<E>.updateLeft(newLeft: Treap<E>): TreapNode<E> =
    TreapNode(storage, newLeft, right)

internal fun <E : Any> TreapNode<E>.updateRight(newRight: Treap<E>): TreapNode<E> =
    TreapNode(storage, left, newRight)

internal fun <E : Any> TreapNode<E>.updateStorage(newStorage: NodeStorage<E>): TreapNode<E> =
    TreapNode(newStorage, left, right)

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
        val lHash = if (lEmpty) Int.MAX_VALUE else (left as TreapNode).storage.hashValue
        val rHash = if (rEmpty) Int.MAX_VALUE else (right as TreapNode).storage.hashValue

        if (lEmpty && rEmpty) {
            this
        } else if (lEmpty) {
            if (rHash < storage.hashValue) {
                rotateLeft()
            } else {
                this
            }
        } else if (rEmpty) {
            if (lHash < storage.hashValue) {
                rotateRight()
            } else {
                this
            }
        } else {
            if (storage.hashValue < lHash && storage.hashValue < rHash) {
                this
            } else if (lHash < storage.hashValue && lHash < rHash) {
                rotateRight()
            } else {
                rotateLeft()
            }
        }
    }
}

internal fun <E : Any> Treap<E>.insert(hashValue: Int, element: E): Treap<E> = when (this) {
    is EmptyTreap -> TreapNode(nodeStorageOf(hashValue, element), this, this)
    is TreapNode -> {
        val localHashValue = storage.hashCode()
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
        val localHashValue = storage.hashCode()
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
                    val lHash = if (lEmpty) Int.MAX_VALUE else (left as TreapNode).storage.hashValue
                    val rHash = if (rEmpty) Int.MAX_VALUE else (right as TreapNode).storage.hashValue
                    when {
                        lEmpty && rEmpty -> emptyTreap()
                        lEmpty -> rotateLeft().remove(hashValue, element)
                        rEmpty || (lHash < rHash) -> rotateRight().remove(hashValue, element)
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
        val localHashValue = storage.hashCode()
        when {
            hashValue < localHashValue -> left.lookup(hashValue)
            hashValue > localHashValue -> right.lookup(hashValue)
            else -> storage.asSequence()
        }
    }
}

internal val emptyTreapSingleton: EmptyTreap<Any> = EmptyTreap()

@Suppress("UNCHECKED_CAST")
internal fun <E : Any> emptyTreap(): Treap<E> = emptyTreapSingleton as Treap<E>

internal fun <E : Any> treapOf(vararg elements: E): Treap<E> =
    elements.fold(emptyTreap<E>()) { t, e -> t.insert(e.hashCode(), e) }

/** Guarantees ordering of hash values, but no guarantees of ordering within a NodeStorage */
internal fun <E : Any> Treap<E>.storageSequence(): Sequence<NodeStorage<E>> = when (this) {
    is EmptyTreap -> sequenceOf()
    is TreapNode ->
        left.storageSequence() + sequenceOf(storage) + right.storageSequence()
}

/** Warning: no ordering guarantees for objects with equal hashcodes */
internal fun <E : Any> Treap<E>.iterator(): Iterator<E> =
    this.storageSequence().flatMap { it.asSequence() }.iterator()

// /////////// Now, a binary tree "set" from the binary tree

internal data class TreapSet<E : Any>(val tree: Treap<E>) : FSet<E> {
    override val size: Int
        get() = tree.size

    override fun equals(other: Any?) = when (other) {
        is TreapSet<*> -> tree == other.tree
        else -> false
    }

    override fun hashCode() = tree.hashCode()

    override fun isEmpty() = tree.isEmpty()

    override fun iterator() = tree.iterator()

    override fun plus(element: E): FSet<E> =
        TreapSet(tree.insert(element.hashCode(), element))

    override fun addAll(elements: Iterable<E>): FSet<E> =
        TreapSet(elements.fold(tree) { t, e -> t.insert(e.hashCode(), e) })

    override fun minus(element: E): FSet<E> =
        TreapSet(tree.remove(element.hashCode(), element))

    override fun removeAll(elements: Iterable<E>): FSet<E> =
        TreapSet(elements.fold(tree) { t, e -> t.remove(e.hashCode(), e) })

    override fun lookup(element: E): E? =
        tree.lookup(element.hashCode())
            .filter { it == element }
            .firstOrNull()

    override fun toString(): String {
        val result = tree.iterator().asSequence().joinToString(separator = ", ")
        return "TreapSet($result)"
    }
}

fun <E : Any> emptyTreapSet(): FSet<E> = TreapSet(emptyTreap())

fun <E : Any> treapSetOf(vararg elements: E): FSet<E> =
    elements.asIterable().toTreapSet()

fun <E : Any> Iterable<E>.toTreapSet(): FSet<E> =
    emptyTreapSet<E>().addAll(this)
