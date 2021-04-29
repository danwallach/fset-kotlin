package edu.rice.fset

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.checkAll
import kotlin.math.sqrt

/**
 * Returns a list of population counts of each bit in the input,
 * in little-endian format, so low-bit goes first in the output list.
 */
fun bitCounts(inputs: List<Int>): List<Int> {
    val results = IntArray(32) { 0 }
    inputs.forEach { i ->
        for (bit in 0..31) {
            if (((1 shl bit) and i) != 0)
                results[bit]++
        }
    }
    return results.toList()
}

fun stddev(inputs: List<Int>): Double {
    val avg = inputs.average()
    return sqrt(inputs.map { (it - avg) * (it - avg) }.sum())
}

class HashFamilyTest : FreeSpec({
    "equality implies hash equality" {
        checkAll<Int, Int> { a, b ->
            val hashA = a.hashCode()
            val hashB = b.hashCode()
            val fHashA = a.familyHash14() // we'll test all 14 hashes
            val fHashB = b.familyHash14()

            if (a == b) {
                hashA shouldBe hashB
                fHashA shouldBe fHashB
            }

            if (hashA != hashB) {
                a shouldNotBe b
                for (i in 0..13)
                    fHashA[i] shouldNotBe fHashB[i]
            }
        }
    }

    "bitCounts basics" {
        val counts = bitCounts(listOf(1, 3, 7, 15, 31))
        counts.subList(0, 6) shouldBe listOf(5, 4, 3, 2, 1, 0)
    }

    "bit distributions of hashes" {
        val inputs = (0..50000).toList()
        val originalDist = bitCounts(inputs)
        val hashDist = bitCounts(inputs.map { it.hashCode() })
        val familyHashes = inputs.map { it.familyHash14() }
        val familyHashDists = (0..13).map { bit ->
            bitCounts(familyHashes.map { it[bit] })
        }

        val originalStddev = stddev(originalDist) // roughly 68372
        val hashStddev = stddev(hashDist) // roughly 68372

        // these range from 3800 through 2200 or so, suggesting we might want to tweak the
        // ones with the worst stddev
        val familyStddevs = familyHashDists.map { stddev(it) }

        familyStddevs.forEach {
            it shouldBeLessThan originalStddev
            it shouldBeLessThan hashStddev
        }
    }
})
