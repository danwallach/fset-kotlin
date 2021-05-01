package edu.rice.fset

internal sealed interface HamtNode<E: Any>

// This is where the compressed storage happens.
internal data class HamtSparseNode<E: Any>(val bitmap: UInt, val storage: Array<HamtNode<E>>): HamtNode<E>

// If we end up in the case where every bit is full, then we'll simplify to just the
// array and nothing else.
internal data class HamtFullNode<E: Any>(val storage: Array<HamtNode<E>>): HamtNode<E>

// Common case: storage of a singleton at an HAMT leaf.
internal data class HamtLeafNodeOne<E: Any>(val hash: UInt, val contents: E): HamtNode<E>

// It's going to be rare that we have more than one value stored at a given hash, so we're not
// going to do anything crazy to optimize this case. Kotlin's List is just Java's ArrayList,
// and adding to it makes a copy first, but we don't care, again, because this is very rate.
internal data class HamtLeafNodeMany<E: Any>(val hash: UInt, val contents: List<E>): HamtNode<E>

// Because of the compressed storage feature, we're not going to ever have internal nodes
// representing "nothing". This is only going to be part of top-level representation of
// an empty HAMT.
internal class HamtEmptyNode<E: Any> : HamtNode<E>

internal val emptySingleton = HamtEmptyNode<Any>()

@Suppress("UNCHECKED_CAST")
internal fun <E: Any> emptyHamtNode(): HamtNode<E> = emptySingleton as HamtNode<E>

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

internal fun <E: Any> sparseNodeOf(element: E, fullHash: UInt, offset: Int) : HamtSparseNode<E> {
    val locationOffset = (fullHash shr offset) and LEVEL_MASK
    val locationBit = 1U shl locationOffset.toInt()

    return HamtSparseNode(locationBit, arrayOf(HamtLeafNodeOne(fullHash, element)))
}

internal const val BITS_PER_LEVEL = 5
internal const val MAX_STORAGE_SLOTS = 32 // 2^5
internal const val LEVEL_MASK = 31U // 000...00011111

internal fun getSparseStorageIndex(bitmap: UInt, location: Int): Int =
    (((1U shl location) - 1U) and bitmap).toInt().countOneBits()

internal fun <E: Any> HamtFullNode<E>.getNodeEntry(location: Int): HamtNode<E> =
    if (location < 0 || location >= MAX_STORAGE_SLOTS) {
        throw RuntimeException("location $location out of bounds")
    } else {
        storage[location]
    }

internal fun <E: Any> HamtSparseNode<E>.getNodeEntry(location: Int): HamtNode<E> =
    if (location < 0 || location >= MAX_STORAGE_SLOTS) {
        throw RuntimeException("location $location out of bounds")
    } else {
        storage[getSparseStorageIndex(bitmap, location)]
    }

internal fun <E: Any> HamtNode<E>.insert(element: E, fullHash: UInt, offset: Int): HamtNode<E> {
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
                    sparseNodeOf(element, fullHash, offset)) {
                        n: HamtNode<E>, e: E -> n.insert(e, hash, offset)
                }
            }
        }
        is HamtFullNode -> {
            // We're inserting into a node where every position is already full,
            // so we'll need to copy everything in the array except for the
            // position where we'll need to do a recursive insert.
            HamtFullNode(storage.mapIndexed { index, hamtNode ->
                if (index == locationOffset) {
                    hamtNode.insert(element, fullHash, offset + BITS_PER_LEVEL)
                } else {
                    hamtNode
                }
            }.toTypedArray())
        }
        is HamtSparseNode -> {
            // Three sub-cases:
            // 1) The slot is already full, so we need to insert recursively.
            // 2) The slot is empty, but it's about to be full, so we need to switch
            //    to a HamtFullNode.
            // 3) The slot is empty, but we've still got room, so we need to make
            //    another HamtSparseNode.

            // The bottom two cases are basically the same, so we'll just construct the
            // same array and then depending on its length, decide what to do with it.
            if ((bitmap and locationBit) != 0U) {
                // case 1
                val newStorage = (0..storage.size).map { loc ->
                    if (loc == locationOffset) {
                        storage[loc].insert(element, fullHash, offset + BITS_PER_LEVEL)
                    } else {
                        storage[loc]
                    }
                }.toTypedArray()
                HamtSparseNode(bitmap, newStorage)
            } else {
                val newStorage = (0..storage.size).map { loc ->
                    when {
                        loc < locationOffset -> storage[loc]
                        loc > locationOffset -> storage[loc - 1]
                        else -> storage[loc].insert(element, fullHash, offset + BITS_PER_LEVEL)
                    }
                }.toTypedArray()

                if (newStorage.size == MAX_STORAGE_SLOTS) {
                    // case 2
                    HamtFullNode(newStorage)
                } else {
                    // case 3
                    HamtSparseNode(bitmap or locationBit, newStorage)
                }
            }
        }
    }
}
