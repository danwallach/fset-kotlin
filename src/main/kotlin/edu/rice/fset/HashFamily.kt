package edu.rice.fset

import kotlin.math.ceil
import kotlin.math.sqrt

// We need to be able to leverage the usual hashCode() methods into a *family* of hash
// functions. This is challenging because many core Java classes have really simplistic
// hash functions. Of particular note, Integer.hashCode() just returns the same integer.

// Our solution is that we picked 14 large primes, where the high bits of those primes
// come from their position in the ordering (e.g., prime[0] starts with 0001-bits and
// prime[13] starts with 1110), and we multiply the output of the regular hashCode()
// method to get our new hash values. This means that several of these will be
// negative 32-bit signed integers.

// Why large primes? Because they're relatively prime to the modulus (2^31), we should
// be able to still generate the full set of 32-bit integers without repeats. Why a bunch
// of different large primes spread across this interval? So they'll behave differently
// from one another, while having the desired spreading behavior.

// These are used through the "familyHash" extension functions. Even familyHash1() is going
// to be preferable to the built-in hashCode() function, because even for degenerate cases
// like Integer.hashCode(), it will spread the results out.

internal fun isPrime(n: Long): Boolean {
    if (n == 2L) {
        return true
    }

    if (n and 1L == 0L) {
        // any other even numbers are not prime
        return false
    }

    var q: Long = 3
    val stop = ceil(sqrt(n.toDouble())).toLong()

    while (q < stop) {
        if (n % q == 0L) return false
        q = q + 2
    }
    return true
}

internal fun findPrimeAfter(start: Long): Long {
    var n: Long =
        if ((start and 1L) == 0L) {
            start + 1
        } else {
            start
        }

    while (true) {
        if (isPrime(n)) {
            return n
        }
        n = n + 1
    }
}

internal fun findOurPrimes(): List<Long> {
    val startingPoints: List<Long> = (1L..14L).map {
        // repeat these four bits eight times, giving us a broader distribution of starting points
        (it shl 28) or (it shl 24) or (it shl 20) or (it shl 16) or
                (it shl 12) or (it shl 8) or (it shl 4) or it
    }
    return startingPoints.map { findPrimeAfter(2_654_435_769L + it) }
}

// these values derived using the function above
internal val bigPrimes = longArrayOf(
    2940766931L, 3227098129L, 3513429229L, 3799760401L,
    4086091553L, 4372422701L, 4658753851L, 4945084993L, 5231416147L, 5517747349L, 5804078453L,
    6090409621L, 6376740779L, 6663071941L
)

fun Any.familyHash1(): Int {
    return (this.hashCode() * bigPrimes[0]).toInt()
}

fun Any.familyHash2(): IntArray {
    val oHash = this.hashCode()
    return intArrayOf(
        (oHash * bigPrimes[6]).toInt(),
        (oHash * bigPrimes[13]).toInt()
    )
}

fun Any.familyHash4(): IntArray {
    val oHash = this.hashCode()
    return intArrayOf(
        (oHash * bigPrimes[4]).toInt(),
        (oHash * bigPrimes[7]).toInt(),
        (oHash * bigPrimes[10]).toInt(),
        (oHash * bigPrimes[13]).toInt()
    )
}

fun Any.familyHash8(): IntArray {
    val oHash = this.hashCode()
    return intArrayOf(
        (oHash * bigPrimes[0]).toInt(),
        (oHash * bigPrimes[2]).toInt(),
        (oHash * bigPrimes[4]).toInt(),
        (oHash * bigPrimes[6]).toInt(),
        (oHash * bigPrimes[7]).toInt(),
        (oHash * bigPrimes[9]).toInt(),
        (oHash * bigPrimes[11]).toInt(),
        (oHash * bigPrimes[13]).toInt()
    )
}

fun Any.familyHash14(): IntArray {
    val oHash = this.hashCode()
    return intArrayOf(
        (oHash * bigPrimes[0]).toInt(),
        (oHash * bigPrimes[1]).toInt(),
        (oHash * bigPrimes[2]).toInt(),
        (oHash * bigPrimes[3]).toInt(),
        (oHash * bigPrimes[4]).toInt(),
        (oHash * bigPrimes[5]).toInt(),
        (oHash * bigPrimes[6]).toInt(),
        (oHash * bigPrimes[7]).toInt(),
        (oHash * bigPrimes[8]).toInt(),
        (oHash * bigPrimes[9]).toInt(),
        (oHash * bigPrimes[10]).toInt(),
        (oHash * bigPrimes[11]).toInt(),
        (oHash * bigPrimes[12]).toInt(),
        (oHash * bigPrimes[13]).toInt(),
    )
}

fun main() {
    println("Searching for some big primes")
    val primes = findOurPrimes()
    println("Big primes: " + primes.joinToString(separator = ", "))
}
