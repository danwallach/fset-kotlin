package edu.rice.fset

/**
 * An unbalanced binary tree that switches left or right based on the hash values
 * of the elements rather than relying on elements being comparable.
 */
internal sealed interface BinaryTree<E : Any>

internal class EmptyBinaryTree<E : Any> : BinaryTree<E> {
    override fun equals(other: Any?) = when (other) {
        is EmptyBinaryTree<*> -> true
        else -> false
    }

    override fun hashCode() = 1
}

internal data class BinaryTreeNode<E : Any>(
    val storage: NodeStorage<E>,
    val left: BinaryTree<E>,
    val right: BinaryTree<E>
) : BinaryTree<E> {
    override fun equals(other: Any?) = when (other) {
        is BinaryTreeNode<*> -> other.storageSequence() == this.storageSequence()
        else -> false
    }

    // a hack, but not often used so good enough
    override fun hashCode() = storageSequence().hashCode()
}

internal val <E : Any> BinaryTree<E>.size: Int
    get() = when (this) {
        is EmptyBinaryTree -> 0
        is BinaryTreeNode -> storage.size + left.size + right.size
    }

internal fun <E : Any> BinaryTree<E>.isEmpty() = when (this) {
    is EmptyBinaryTree -> true
    is BinaryTreeNode -> false
}

internal fun <E : Any> BinaryTreeNode<E>.updateLeft(newLeft: BinaryTree<E>): BinaryTreeNode<E> =
    BinaryTreeNode(storage, newLeft, right)

internal fun <E : Any> BinaryTreeNode<E>.updateRight(newRight: BinaryTree<E>): BinaryTreeNode<E> =
    BinaryTreeNode(storage, left, newRight)

internal fun <E : Any> BinaryTreeNode<E>.updateStorage(newStorage: NodeStorage<E>): BinaryTreeNode<E> =
    BinaryTreeNode(newStorage, left, right)

internal fun <E : Any> BinaryTreeNode<E>.rotateRight(): BinaryTree<E> = when (left) {
    is EmptyBinaryTree -> throw RuntimeException("cannot rotate right without a left subtree")
    is BinaryTreeNode -> left.updateRight(this.updateLeft(left.right))
}

internal fun <E : Any> BinaryTreeNode<E>.rotateLeft(): BinaryTree<E> = when (right) {
    is EmptyBinaryTree -> throw RuntimeException("cannot rotate left without a right subtree")
    is BinaryTreeNode -> right.updateLeft(this.updateRight(right.left))
}

internal fun <E : Any> BinaryTree<E>.insert(hashValue: Int, element: E): BinaryTree<E> = when (this) {
    is EmptyBinaryTree -> BinaryTreeNode(nodeStorageOf(hashValue, element), this, this)
    is BinaryTreeNode -> {
        val localHashValue = storage.hashCode()
        when {
            hashValue < localHashValue -> updateLeft(left.insert(hashValue, element))
            hashValue > localHashValue -> updateRight(right.insert(hashValue, element))
            else -> updateStorage(storage.insert(element))
        }
    }
}

internal fun <E : Any> BinaryTree<E>.remove(hashValue: Int, element: E): BinaryTree<E> = when (this) {
    is EmptyBinaryTree -> this // nothing to remove!
    is BinaryTreeNode -> {
        val localHashValue = storage.hashCode()
        when {
            hashValue < localHashValue -> updateLeft(left.remove(hashValue, element))
            hashValue > localHashValue -> updateRight(right.remove(hashValue, element))
            else -> {
                // If there's only one thing in the storage, then this whole node
                // needs to go away, so we need to rotate the tree. But if there's
                // more than one thing here, we only need to update the storage
                // and we're still fine.
                val revisedStorage = storage.remove(element)
                if (revisedStorage == null) {
                    rotateRight().remove(hashValue, element)
                } else {
                    updateStorage(revisedStorage)
                }
            }
        }
    }
}

internal fun <E : Any> BinaryTree<E>.lookup(hashValue: Int): Iterator<E> = when (this) {
    is EmptyBinaryTree -> sequenceOf<E>().iterator()
    is BinaryTreeNode -> {
        val localHashValue = storage.hashCode()
        when {
            hashValue < localHashValue -> left.lookup(hashValue)
            hashValue > localHashValue -> right.lookup(hashValue)
            else -> storage.iterator()
        }
    }
}

internal val emptyTreeSingleton: EmptyBinaryTree<Any> = EmptyBinaryTree()

@Suppress("UNCHECKED_CAST")
internal fun <E : Any> emptyBinaryTree(): BinaryTree<E> = emptyTreeSingleton as BinaryTree<E>

internal fun <E : Any> binaryTreeOf(vararg elements: E): BinaryTree<E> =
    elements.fold(emptyBinaryTree<E>()) { t, e -> t.insert(e.hashCode(), e) }

/** Guarantees ordering of hash values, but no guarantees of ordering within a NodeStorage */
internal fun <E : Any> BinaryTree<E>.storageSequence(): Sequence<NodeStorage<E>> = when (this) {
    is EmptyBinaryTree -> sequenceOf()
    is BinaryTreeNode ->
        left.storageSequence() + sequenceOf(storage) + right.storageSequence()
}

/** Warning: no ordering guarantees for objects with equal hashcodes */
internal fun <E : Any> BinaryTree<E>.iterator(): Iterator<E> =
    this.storageSequence().flatMap { it.asSequence() }.iterator()

// /////////// Now, a binary tree "set" from the binary tree

internal data class BinaryTreeSet<E : Any>(val tree: BinaryTree<E>) : FSet<E> {
    override val size: Int
        get() = tree.size

    override fun equals(other: Any?) = when (other) {
        is BinaryTreeSet<*> -> tree == other.tree
        else -> false
    }

    override fun hashCode() = tree.hashCode()

    override fun isEmpty() = tree.isEmpty()

    override fun iterator() = tree.iterator()

    override fun plus(element: E): FSet<E> =
        BinaryTreeSet(tree.insert(element.hashCode(), element))

    override fun addAll(elements: Iterable<E>): FSet<E> =
        BinaryTreeSet(elements.fold(tree) { t, e -> t.insert(e.hashCode(), e) })

    override fun minus(element: E): FSet<E> =
        BinaryTreeSet(tree.remove(element.hashCode(), element))

    override fun removeAll(elements: Iterable<E>): FSet<E> =
        BinaryTreeSet(elements.fold(tree) { t, e -> t.remove(e.hashCode(), e) })

    override fun lookup(hashValue: Int): Iterator<E> = tree.lookup(hashValue)
}

fun <E : Any> emptyBinaryTreeSet(): FSet<E> = BinaryTreeSet(emptyBinaryTree())

fun <E : Any> binaryTreeSetOf(vararg elements: E): FSet<E> =
    elements.asIterable().toBinaryTreeSet()

fun <E : Any> Iterable<E>.toBinaryTreeSet(): FSet<E> =
    BinaryTreeSet(this.fold(emptyBinaryTree()) { t, e -> t.insert(e.hashCode(), e) })
