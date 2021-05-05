package edu.rice.fset

internal sealed interface HamtNode<E : Any>

// This is where the compressed storage happens.
internal data class HamtSparseNode<E : Any>(val bitmap: UInt, val storage: Array<HamtNode<E>>) :
    HamtNode<E>

// If we end up in the case where every bit is full, then we'll simplify to just the
// array and nothing else.
internal data class HamtFullNode<E : Any>(val storage: Array<HamtNode<E>>) : HamtNode<E>

// Common case: storage of a singleton at an HAMT leaf.
internal data class HamtLeafNodeOne<E : Any>(val hash: UInt, val contents: E) : HamtNode<E>

// It's going to be rare that we have more than one value stored at a given hash, so we're not
// going to do anything crazy to optimize this case. Kotlin's List is just Java's ArrayList,
// and adding to it makes a copy first, but we don't care, again, because this is very rate.
internal data class HamtLeafNodeMany<E : Any>(val hash: UInt, val contents: List<E>) : HamtNode<E>

// Because of the compressed storage feature, we're not going to ever have internal nodes
// representing "nothing". This is only going to be part of top-level representation of
// an empty HAMT.
internal class HamtEmptyNode<E : Any> : HamtNode<E> {
    override fun equals(other: Any?) = other === this
    override fun hashCode(): Int = 1
}

internal val emptySingleton = HamtEmptyNode<Any>()

@Suppress("UNCHECKED_CAST")
internal fun <E : Any> emptyHamtNode(): HamtNode<E> = emptySingleton as HamtNode<E>

// With BITS_PER_LEVEL = 5, that means that we could have as many as 2^5 = 32 entries in the
// compressed storage, and the bitmap will have 32 bits in it indicating which bits have entries
// in the storage.

// We're using a clever solution built around popcount, which is sometimes a native machine
// instruction. Hand wavy explanation: say we're looking for the 7th entry. So we take
// 2^7 - 1 = 0000...000111111 (six one bits), and then run popcount on that, bitwise-anded
// with the bitmap, which then tells us how many things are in the storage for the first six
// entries. The thing we're looking for, then, is right afterward.

// Note: the countOneBits() extension function on Int is only currently supported on the JVM.

// Also note: we're working our way up from the little-endian side of the bitmap and the hash.

internal fun <E : Any> sparseNodeOf(element: E, fullHash: UInt, offset: Int = 0): HamtSparseNode<E> {
    val locationOffset = (fullHash shr offset) and LEVEL_MASK
    val locationBit = 1U shl locationOffset.toInt()

    return HamtSparseNode(locationBit, arrayOf(HamtLeafNodeOne(fullHash, element)))
}

internal const val BITS_PER_LEVEL = 5
internal const val MAX_STORAGE_SLOTS = 32 // 2^5
internal const val LEVEL_MASK = 31U // 000...00011111

internal fun sparseLocation(bitmap: UInt, location: Int): Int {
    val locationBit = 1U shl location
    return (bitmap and (locationBit - 1U)).toInt().countOneBits()
}

internal fun sparseBitmapContains(bitmap: UInt, location: Int): Boolean {
    val locationBit = 1U shl location
    return (bitmap and locationBit) != 0U
}

internal fun <E : Any> HamtSparseNode<E>.updateOffset(sparseOffset: Int, updateFunc: (HamtNode<E>) -> HamtNode<E>): HamtSparseNode<E> {
    if (sparseOffset >= storage.size || sparseOffset < 0)
        throw RuntimeException("storage doesn't contain requested offset: $sparseOffset (bitmap: 0x" + "%08x".format(bitmap.toInt()) + ")")

    return HamtSparseNode(
        bitmap,
        storage.mapIndexed {
            index, node ->
            if (index == sparseOffset) updateFunc(node) else node
        }.toTypedArray()
    )
}

internal fun <E : Any> HamtFullNode<E>.updateOffset(location: Int, updateFunc: (HamtNode<E>) -> HamtNode<E>): HamtFullNode<E> {
    if (location >= storage.size || location < 0)
        throw RuntimeException("storage doesn't contain requested offset: $location")

    return HamtFullNode(
        storage.mapIndexed { index, node ->
            if (index == location) updateFunc(node) else node
        }.toTypedArray()
    )
}

internal fun <E : Any> HamtNode<E>.insert(element: E, fullHash: UInt, offset: Int = 0): HamtNode<E> {
    val locationOffset = ((fullHash shr offset) and LEVEL_MASK).toInt()
    val locationBit = 1U shl locationOffset

    return when (this) {
        is HamtEmptyNode -> HamtLeafNodeOne(fullHash, element)
        is HamtLeafNodeOne -> if (hash == fullHash) {
            HamtLeafNodeMany(hash, listOf(contents, element))
        } else {
            sparseNodeOf(element, fullHash, offset)
                .insert(contents, hash, offset)
        }
        is HamtLeafNodeMany -> {
            // Two cases here:
            // 1) the thing we're inserting matches the hash that's already here, so
            //    we just stick it on the list
            // 2) the thing we're inserting doesn't fit here, so we have to upgrade
            //    this node from a leaf node to a sparse node
            if (hash == fullHash) {
                HamtLeafNodeMany(hash, contents + element)
            } else {
                contents.fold(
                    sparseNodeOf(element, fullHash, offset)
                ) { n: HamtNode<E>, e: E ->
                    n.insert(e, hash, offset)
                }
            }
        }
        is HamtFullNode ->
            // We're inserting into a node where every position is already full,
            // so we'll need to copy everything in the array except for the
            // position where we'll need to do a recursive insert.
            updateOffset(locationOffset) { it.insert(element, fullHash, offset + BITS_PER_LEVEL) }

        is HamtSparseNode -> {
            // Three sub-cases:
            // 1) The slot is already full, so we need to insert recursively.
            // 2) The slot is empty
            //    a) After filling the slot, then every slot is full so we need to
            //       upgrade to a HamtFullNode
            //    b) Otherwise, we end up with another HamtSparseNode

            // The bottom two cases are basically the same, so we'll just construct the
            // same array and then depending on its length, decide what to do with it.

            val sparseOffset = sparseLocation(bitmap, locationOffset)
            if (sparseBitmapContains(bitmap, locationOffset)) {
                // case 1
                updateOffset(sparseOffset) { it.insert(element, fullHash, offset + BITS_PER_LEVEL) }
            } else {
                val newStorage = (0..storage.size).map { loc ->
                    when {
                        loc < sparseOffset -> storage[loc]
                        loc > sparseOffset -> storage[loc - 1]
                        else -> HamtLeafNodeOne(fullHash, element)
                    }
                }.toTypedArray()

                if (newStorage.size == MAX_STORAGE_SLOTS) {
                    HamtFullNode(newStorage)
                } else {
                    HamtSparseNode(bitmap or locationBit, newStorage)
                }
            }
        }
    }
}

internal fun <E : Any> HamtNode<E>.remove(element: E, fullHash: UInt, offset: Int = 0): HamtNode<E> {
    val locationOffset = ((fullHash shr offset) and LEVEL_MASK).toInt()
    val locationBit = 1U shl locationOffset

    return when (this) {
        is HamtEmptyNode -> this // nothing to remove!
        is HamtLeafNodeOne -> if (hash == fullHash) {
            emptyHamtNode()
        } else {
            this // nothing to remove!
        }
        is HamtLeafNodeMany -> {
            // So many cases to worry about:
            // 1) the thing we're removing matches the hash that's already here, so
            //    we need to remove it
            //    a) yielding no size change (so it wasn't there)
            //    b) yielding only one result
            //    c) yielding two or more
            // 2) the thing we're removing isn't here, so we're done
            if (hash == fullHash) {
                val newContents = contents.filter { it != element }
                val newSize = newContents.size
                when {
                    newSize == contents.size -> this // nothing to remove!
                    newSize == 1 -> HamtLeafNodeOne(hash, newContents[0])
                    else -> HamtLeafNodeMany(hash, newContents)
                }
            } else {
                this
            }
        }
        is HamtFullNode -> {
            // We're removing from a node where every position is already full,
            // so recursion is going to happen. So many cases!
            // 1) We get back what was already there, so nothing changed.
            // 2) If we get back an empty node, then we need to build a sparse node.
            // 3) Otherwise, we're just replacing what we've already got in the same slot.
            //    so we'll need to copy everything in the array except for the
            //    position where we'll need to place the result.

            val removeNode = storage[locationOffset]
            val newRemoveNode = removeNode.remove(element, fullHash, offset + BITS_PER_LEVEL)

            when {
                newRemoveNode === removeNode -> this // nothing changed!
                newRemoveNode is HamtEmptyNode -> {
                    val newStorage = storage
                        .filterIndexed { index, _ -> index != locationOffset }
                        .toTypedArray()
                    val newBitmap = locationBit.inv()
                    HamtSparseNode(newBitmap, newStorage)
                }
                else -> updateOffset(locationOffset) { newRemoveNode }
            }
        }
        is HamtSparseNode -> {
            // We're removing from a node where we have some but not all positions full,
            // so we have many, many cases.
            // 1) The sub-node slot is empty. Nothing there to remove. We're done.
            // 2) The sub-node slot has something, so we need to do a recursive removal.
            //    a) The result is the same as what's there; nothing got removed. We're done.
            //    b) The result is an empty node, so this slot needs to become empty
            //       1) If this gets us down to a single slot, we might be able to simplify
            //       2) Otherwise, just another sparse node
            //    c) The result is non-empty, so we need to just replace what's in the slot

            if (!sparseBitmapContains(bitmap, locationOffset)) {
                this
            } else {
                val sparseOffset = sparseLocation(bitmap, locationOffset)
                val removeNode = storage[sparseOffset]
                val newRemoveNode = removeNode.remove(element, fullHash, offset + BITS_PER_LEVEL)
                when {
                    removeNode === newRemoveNode -> this // nothing changed!
                    newRemoveNode is HamtEmptyNode -> {
                        val newStorage = storage
                            .filterIndexed { index, _ -> index != sparseOffset }
                            .toTypedArray()
                        when {
                            newStorage.isEmpty() -> emptyHamtNode()
                            newStorage.size == 1 &&
                                (
                                    newRemoveNode is HamtLeafNodeOne ||
                                        newRemoveNode is HamtLeafNodeMany
                                    ) -> newRemoveNode
                            else -> HamtSparseNode(bitmap and locationBit.inv(), newStorage)
                        }
                    }
                    else -> updateOffset(locationOffset) { newRemoveNode }
                }
            }
        }
    }
}

internal fun <E : Any> HamtNode<E>.lookup(hashValue: Int, element: E, offset: Int = 0): E? = when (this) {
    is HamtEmptyNode -> null // shouldn't ever happen
    is HamtLeafNodeOne -> if (this.hash == hashValue.toUInt() && contents == element) contents else null
    is HamtLeafNodeMany -> if (this.hash == hashValue.toUInt()) contents.firstOrNull { it == element } else null
    is HamtFullNode -> {
        val locationOffset = ((hashValue.toUInt() shr offset) and LEVEL_MASK).toInt()
        storage[locationOffset].lookup(hashValue, element, offset + BITS_PER_LEVEL)
    }
    is HamtSparseNode -> {
        val locationOffset = ((hashValue.toUInt() shr offset) and LEVEL_MASK).toInt()
        if (!sparseBitmapContains(bitmap, locationOffset)) {
            null
        } else {
            val sparseOffset = sparseLocation(bitmap, locationOffset)
            storage[sparseOffset].lookup(hashValue, element, offset + BITS_PER_LEVEL)
        }
    }
}

internal fun <E : Any> HamtNode<E>.update(hashValue: Int, element: E, offset: Int = 0): HamtNode<E> = when (this) {
    is HamtEmptyNode -> this // shouldn't ever happen
    is HamtLeafNodeOne -> if (hash == hashValue.toUInt() && contents == element) HamtLeafNodeOne(hash, element) else this
    is HamtLeafNodeMany ->
        if (hash == hashValue.toUInt() && element in contents)
            HamtLeafNodeMany(hash, contents.map { if (it == element) element else it })
        else
            this
    is HamtFullNode -> {
        val locationOffset = ((hashValue.toUInt() shr offset) and LEVEL_MASK).toInt()
        val updateResult = storage[locationOffset].update(hashValue, element, offset + BITS_PER_LEVEL)
        if (updateResult === storage[locationOffset])
            this
        else
            updateOffset(locationOffset) { updateResult }
    }
    is HamtSparseNode -> {
        if (!sparseBitmapContains(bitmap, offset)) {
            this.insert(element, hashValue.toUInt(), offset)
        } else {
            val sparseOffset =
                sparseLocation(bitmap, ((hashValue.toUInt() shr offset) and LEVEL_MASK).toInt())
            val updateResult =
                storage[sparseOffset].update(hashValue, element, offset + BITS_PER_LEVEL)
            if (updateResult === storage[sparseOffset])
                this
            else
                updateOffset(sparseOffset) { updateResult }
        }
    }
}

internal fun <E : Any> HamtNode<E>.storageSequence(): Sequence<Set<E>> = when (this) {
    is HamtEmptyNode -> emptySequence()
    is HamtLeafNodeOne -> sequenceOf(setOf(contents))
    is HamtLeafNodeMany -> sequenceOf(contents.toSet())
    is HamtSparseNode -> storage.asSequence().flatMap { it.storageSequence() }
    is HamtFullNode -> storage.asSequence().flatMap { it.storageSequence() }
}

internal fun <E1 : Any, E2 : Any> equality(tree1: HamtNode<E1>, tree2: HamtNode<E2>): Boolean {
    // CHAMPs have canonical structure, so structural equality implies set equality.
    // This isn't necessarily the case with HAMTs, so we'll just convert to a sequence
    // ordered by hash value and then check equality on that. Note that it's a sequence
    // of unordered sets, where each set corresponds to entries with identical hashes.

    val seq1 = tree1.storageSequence()
    val seq2 = tree2.storageSequence()
    return seq1.zip(seq2).all { (a, b) -> a == b }
}

internal fun <E : Any> HamtNode<E>.nodeDepths(priorDepth: Int = 1): Sequence<Int> = when (this) {
    is HamtEmptyNode -> emptySequence()
    is HamtLeafNodeOne -> sequenceOf(priorDepth)
    is HamtLeafNodeMany -> sequenceOf(priorDepth) // we're counting multiple values with identical hashes as if one
    is HamtSparseNode -> storage.flatMap { it.nodeDepths(priorDepth + 1) }.asSequence()
    is HamtFullNode -> storage.flatMap { it.nodeDepths(priorDepth + 1) }.asSequence()
}

internal fun <E : Any> HamtNode<E>.iterator() = storageSequence().flatMap { it }.iterator()

internal fun <E : Any> HamtNode<E>.debugPrint(depth: Int = 0) {
    val whitespace = " ".repeat(depth * 2)
    when (this) {
        is HamtEmptyNode -> { }
        is HamtLeafNodeOne -> println("%s| leaf: %s".format(whitespace, contents.toString()))
        is HamtLeafNodeMany -> println("%s| leaf: %s".format(whitespace, contents.joinToString(separator = ", ")))
        is HamtSparseNode -> {
            println("%s| sparse (bitmap: 0x%08x)".format(whitespace, bitmap))
            storage.forEach { it.debugPrint(depth + 1) }
        }
        is HamtFullNode -> {
            println("%s| full".format(whitespace))
            storage.forEach { it.debugPrint(depth + 1) }
        }
    }
}

internal data class HamtSet<E : Any>(val tree: HamtNode<E>) : FSet<E> {
    override val size: Int
        get() = tree.storageSequence().fold(0) { sum, s -> sum + s.size }

    override fun equals(other: Any?) = when (other) {
        is HamtSet<*> -> equality(tree, other.tree)
        else -> false
    }

    override fun hashCode() = tree.hashCode()

    override fun isEmpty() = tree is HamtEmptyNode

    override fun iterator(): Iterator<E> = tree.iterator()

    override fun plus(element: E): FSet<E> = HamtSet(
        tree.insert(element, fullHash = element.familyHash1().toUInt())
    )

    override fun addAll(elements: Iterable<E>): FSet<E> = HamtSet(
        elements.fold(tree) { t, e -> t.insert(e, e.familyHash1().toUInt()) }
    )

    override fun minus(element: E): FSet<E> {
        val newTree = tree.remove(element, element.familyHash1().toUInt())
        return if (newTree === tree) this else HamtSet(newTree)
    }

    override fun removeAll(elements: Iterable<E>): FSet<E> {
        val newTree = elements.fold(tree) { t, e -> t.remove(e, e.familyHash1().toUInt()) }
        return if (newTree === tree) this else HamtSet(newTree)
    }

    override fun lookup(element: E): E? = tree.lookup(element.familyHash1(), element)

    override fun update(element: E): FSet<E> {
        val newTree = tree.update(element.familyHash1(), element)
        return if (newTree === tree) this else HamtSet(newTree)
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

fun <E : Any> emptyHamtSet(): FSet<E> = HamtSet(emptyHamtNode())

fun <E : Any> hamtSetOf(vararg elements: E) = elements.asIterable().toHamtSet()

fun <E : Any> Iterable<E>.toHamtSet() = emptyHamtSet<E>().addAll(this)
