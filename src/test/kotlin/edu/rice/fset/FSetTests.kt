package edu.rice.fset

import io.kotest.core.spec.style.freeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.next
import io.kotest.property.checkAll

data class IntWithLameHash(val element: Int) {
    // Exercises that our set implementations still work as sets, even when the underlying
    // hash function gets tons of collisions.
    override fun hashCode() = element % 10

    override fun equals(other: Any?) = when (other) {
        is IntWithLameHash -> element == other.element
        else -> false
    }
}

val intWithLameHashGen = arbitrary { rs ->
    IntWithLameHash(Arb.int(0, 30).next(rs))
}

val listIntWithLameHashGen = arbitrary { rs ->
    Arb.list(intWithLameHashGen).next(rs)
}

fun <E : Any> setsEqual(a: Set<E>, b: Set<E>): Boolean = a.containsAll(b) && b.containsAll(a)

fun fsetTests(
    algorithm: String,
    emptyStringSet: FSet<String>,
    emptyIntSet: FSet<IntWithLameHash>
) = freeSpec {
    algorithm - {
        "basic inserts and membership (string)" {
            checkAll<List<String>> { inputs ->
                val testMe = emptyStringSet.addAll(inputs.asIterable())
                inputs.forEach { i ->
                    val result = testMe.lookup(i)
                    result shouldNotBe null
                    val expected = inputs.filter { it == i }.toSet()
                    setOf(result) shouldBe expected
                }
            }
        }
        "basic inserts and membership (restricted int)" {
            checkAll(listIntWithLameHashGen) { inputs ->
                val testMe = emptyIntSet.addAll(inputs.asIterable())
                inputs.forEach { i ->
                    val result = testMe.lookup(i)
                    result shouldNotBe null
                    val expected = inputs.filter { it == i }.toSet()
                    setOf(result) shouldBe expected
                }
            }
        }
        "queries for missing values (restricted int)" {
            checkAll(listIntWithLameHashGen, intWithLameHashGen) { inputs, other ->
                val testMe = emptyIntSet.addAll(inputs.asIterable())
                val result = testMe.lookup(other)
                if (inputs.contains(other)) {
                    result shouldBe other
                } else {
                    result shouldBe null
                }
            }
        }
        "removing a value and set equality (restricted int)" {
            checkAll(listIntWithLameHashGen, intWithLameHashGen) { inputs, other ->
                val testMe = emptyIntSet.addAll(inputs.asIterable())
                val testMePlus = testMe + other
                val testMinus = testMePlus - other

                // We're testing that we have the expected set behavior, and we're
                // exercising our equals() methods versus the setsEqual() code
                // above that uses Kotlin logic from Container. (Our own equals()
                // methods can take advantage of internal structure and hopefully
                // run faster.)

                if (inputs.contains(other)) {
                    setsEqual(testMe, testMePlus) shouldBe true
                    testMe shouldBe testMePlus
                    val testMeHash = testMe.hashCode()
                    val testMePlusHash = testMePlus.hashCode()
                    testMeHash shouldBe testMePlusHash
                    setsEqual(testMe, testMinus) shouldBe false
                    testMe shouldNotBe testMinus
                } else {
                    setsEqual(testMe, testMinus) shouldBe true
                    testMe shouldBe testMinus
                    val testMeHash = testMe.hashCode()
                    val testMinusHash = testMinus.hashCode()

                    if (testMeHash != testMinusHash) {
                        // slightly more useful debugging output than just a failed assertion
                        println("These should be the same!")
                        testMe.debugPrint()
                        println()
                        testMinus.debugPrint()
                    }
                    testMeHash shouldBe testMinusHash
                    setsEqual(testMe, testMePlus) shouldBe false
                    testMe shouldNotBe testMePlus
                }
            }
        }
        "removing everything, ending up with nothing (strings)" {
            checkAll<List<String>> { inputs ->
                val fullSet = emptyStringSet.addAll(inputs.asIterable())
                val emptySetAgain = fullSet.removeAll(inputs.shuffled().asIterable())
//                println("Stats: ${fullSet.statistics()} -> ${emptySetAgain.statistics()}")
                emptySetAgain shouldBe emptyStringSet
            }
        }
    }
}
