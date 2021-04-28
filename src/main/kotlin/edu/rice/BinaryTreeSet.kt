package edu.rice

/**
 * Another slow implementation of functional sets, based on unbalanced binary trees, but
 * using the hash values of the elements rather than relying on elements being comparable.
 */
sealed interface BinaryTree<E: Any> {
    val size: Int
    fun isEmpty(): Boolean
    fun insert(hashValue: Int, element: E): BinaryTree<E>
    fun remove(hashValue: Int, element: E): BinaryTree<E>
    fun lookup(hashValue: Int): Iterator<E>
}

private class EmptyBinaryTree<E: Any>: BinaryTree<E> {
    override val size = 0

    override fun isEmpty() = true

    override fun insert(hashValue: Int, element: E) = BinaryTreeNode(nodeStorageOf(hashValue, element), this, this)

    override fun remove(hashValue: Int, element: E) = this

    override fun lookup(hashValue: Int) = sequenceOf<E>().iterator()

    override fun equals(other: Any?) = this === other  // singleton, so pointer equality is sufficient

    override fun hashCode() = 1  // not really used, but Kotlin complains if it's not here
}


private data class BinaryTreeNode<E: Any>(
    val storage: NodeStorage<E>,
    val left: BinaryTree<E>,
    val right: BinaryTree<E>
): BinaryTree<E> {
    override val size = 1 + left.size + right.size

    override fun isEmpty() = false

    override fun insert(hashValue: Int, element: E): BinaryTree<E> {
        val localHashValue = storage.hashCode()
        return when {
            hashValue < localHashValue -> BinaryTreeNode(storage, left.insert(hashValue, element), right)
            hashValue > localHashValue -> BinaryTreeNode(storage, left, right.insert(hashValue, element))
            else /* hashValue == localHashValue */ -> BinaryTreeNode(storage.insert(element), left, right)
        }
    }

    override fun remove(hashValue: Int, element: E): BinaryTree<E> {
        val localHashValue = storage.hashCode()
        return when {
            hashValue < localHashValue -> BinaryTreeNode(storage, left.remove(hashValue, element), right)
            hashValue > localHashValue -> BinaryTreeNode(storage, left, right.remove(hashValue, element))
            else /* hashValue == localHashValue */ -> {
                val revisedStorage = storage.remove(element)
                if (revisedStorage == null) {
                    rotateRight().remove(hashValue, element)
                } else {
                    BinaryTreeNode(revisedStorage, left, right)
                }
            }
        }
    }

    override fun lookup(hashValue: Int): Iterator<E> {
        val localHashValue = storage.hashCode()
        return when {
            hashValue < localHashValue -> left.lookup(hashValue)
            hashValue > localHashValue -> right.lookup(hashValue)
            else -> storage.iterator()
        }
    }

    override fun equals(other: Any?) = when (other) {
        is BinaryTreeNode<*> -> other.storageSequence() == this.storageSequence()
        else -> false
    }

    override fun hashCode() = storageSequence().hashCode() // a hack, but not often used so good enough
}

private val emptyTreeSingleton: EmptyBinaryTree<Any> = EmptyBinaryTree()

@Suppress("UNCHECKED_CAST")
fun <E: Any> emptyBinaryTree(): BinaryTree<E> = emptyTreeSingleton as BinaryTree<E>

fun <E: Any> binaryTreeOf(vararg elements: E): BinaryTree<E> =
    elements.fold(emptyBinaryTree<E>()) { t, e -> t.insert(e.hashCode(), e) }

/** Guarantees ordering of hash values, but no guarantees of ordering inside any given NodeStorage */
fun <E: Any> BinaryTree<E>.storageSequence(): Sequence<NodeStorage<E>> = when(this) {
    is EmptyBinaryTree -> sequenceOf()
    is BinaryTreeNode ->
        left.storageSequence() + sequenceOf(storage) + right.storageSequence()
}

/** Warning: no ordering guarantees for objects with equal hashcodes */
fun <E: Any> BinaryTree<E>.iterator(): Iterator<E> =
    this.storageSequence().flatMap { it.asSequence() }.iterator()

private fun <E: Any> BinaryTree<E>.rotateRight(): BinaryTree<E> = when(this) {
    is EmptyBinaryTree -> throw RuntimeException("cannot rotate an empty tree")
    is BinaryTreeNode -> when (left) {
        is EmptyBinaryTree -> throw RuntimeException("cannot rotate right without a left subtree")
        is BinaryTreeNode ->
            BinaryTreeNode(
                left.storage,
                left.left,
                BinaryTreeNode(this.storage, left.right, this.right))
    }
}

private fun <E: Any> BinaryTree<E>.rotateLeft(): BinaryTree<E> = when(this) {
    is EmptyBinaryTree -> throw RuntimeException("cannot rotate an empty tree")
    is BinaryTreeNode -> when (right) {
        is EmptyBinaryTree -> throw RuntimeException("cannot rotate left without a right subtree")
        is BinaryTreeNode ->
            BinaryTreeNode(
                right.storage,
                BinaryTreeNode(this.storage, this.left, right.left),
                right.right)
    }
}
