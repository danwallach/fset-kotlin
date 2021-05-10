package edu.rice.fset

/**
 * Heuristic to measure the cost of inserting at each different location; we're modeling
 * in a preference for avoiding leaf nodes that already have more than one element in
 * them, and as well, we're modeling that it's cheaper to insert into a sparse node that
 * has an empty slot for you than a full slot.
 */
internal fun <E : Any> HamtNode<E>.insertionCost(
    fullHash: UInt,
    offset: Int = 0
): Int = when (this) {
    is HamtEmptyNode -> 0
    is HamtLeafNodeOne -> 1
    is HamtLeafNodeMany -> 2
    is HamtSparseNode -> {
        val locationOffset = ((fullHash shr offset) and LEVEL_MASK).toInt()
        val sparseOffset = sparseLocation(bitmap, locationOffset)
        if (sparseBitmapContains(bitmap, locationOffset)) {
            2 + storage[sparseOffset].insertionCost(fullHash, offset + BITS_PER_LEVEL)
        } else 1
    }
}

internal fun <E1 : Any, E2 : Any> equalityChoice(tree1: HamtSet<E1>, tree2: HamtSet<E2>): Boolean {
    // This part is a mess, because the hash choices means that the sequences could be quite
    // different from one another.

    // Temporary solution: dump them into a Kotlin (Java) HashSet and let that
    // figure it out.
    val hset1 = kotlin.collections.hashSetOf<E1>()
    val hset2 = kotlin.collections.hashSetOf<E2>()

    tree1.iterator().forEach { hset1.add(it) }
    tree2.iterator().forEach { hset2.add(it) }
    return tree1 == tree2
}

internal fun <E : Any> HamtNode<E>.lookupChoice(element: E): E? {
    val hashes = element.familyHash2()
    val result0 = lookup(element, hashes[0].toUInt())
    if (result0 != null) {
        return result0
    } else {
        return lookup(element, hashes[1].toUInt())
    }
}


internal fun <E : Any> HamtNode<E>.insertChoice(element: E): HamtNode<E> {
    val hashes = element.familyHash2()
    val depth0 = insertionCost(hashes[0].toUInt())
    val depth1 = insertionCost(hashes[1].toUInt())

    return insert(element, fullHash = hashes[if (depth0 < depth1) 0 else 1].toUInt())
}

internal fun <E : Any> HamtNode<E>.updateChoice(element: E): HamtNode<E> {
    val hashes = element.familyHash2()
    var newTree = update(element, hashes[0].toUInt())
    if (newTree === this) {
        newTree = update(element, hashes[1].toUInt())
    }
    return newTree
}

internal fun <E : Any> HamtNode<E>.removeChoice(element: E): HamtNode<E> {
    val hashes = element.familyHash2()
    val result0 = remove(element, fullHash = hashes[0].toUInt())
    if (result0 !== this) {
        return result0
    }
    val result1 = result0.remove(element, fullHash = hashes[0].toUInt())
    return result1
}

internal class HamtChoiceSet<E : Any>(tree: HamtNode<E>) : HamtSet<E>(tree) {
    override fun equals(other: Any?) = when (other) {
        is HamtSet<*> -> equalityChoice(this, other)
        else -> false
    }

    override fun hashCode() = tree.hashCode()

    override fun plus(element: E): FSet<E> = HamtChoiceSet(tree.insertChoice(element))

    override fun addAll(elements: Iterable<E>): FSet<E> = HamtChoiceSet(
        elements.fold(tree) { t, e -> t.insertChoice(e) }
    )

    override fun minus(element: E): FSet<E> {
        val newTree = tree.removeChoice(element)
        return if (newTree === tree) this else HamtChoiceSet(newTree)
    }

    override fun removeAll(elements: Iterable<E>): FSet<E> {
        val newTree = elements.fold(tree) { t, e -> t.removeChoice(e) }
        return if (newTree === tree) this else HamtChoiceSet(newTree)
    }

    override fun lookup(element: E): E? = tree.lookupChoice(element)

    override fun update(element: E): FSet<E> {
        val newTree = tree.updateChoice(element)
        return if (newTree === tree) this else HamtChoiceSet(newTree)
    }
}

fun <E : Any> emptyHamtChoiceSet(): FSet<E> = HamtChoiceSet(emptyHamtNode())

fun <E : Any> hamtChoiceSetOf(vararg elements: E) = elements.asIterable().toHamtChoiceSet()

fun <E : Any> Iterable<E>.toHamtChoiceSet() = emptyHamtChoiceSet<E>().addAll(this)
