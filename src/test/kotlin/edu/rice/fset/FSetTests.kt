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
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

data class IntWithLameHash(val element: Int) {
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
                    // hash collisions are basically never going to happen here
                    val desiredHash = i.hashCode()
                    val result = testMe.lookup(desiredHash).asSequence().toSet()
                    val expected = inputs.filter { it.hashCode() == desiredHash }.toSet()
                    assertEquals(result, expected)
                }
            }
        }
        "basic inserts and membership (restricted int)" {
            checkAll(listIntWithLameHashGen) { inputs ->
                val testMe = emptyIntSet.addAll(inputs.asIterable())
                inputs.forEach { i ->
                    // hash collisions are going to happen all the time here
                    val desiredHash = i.hashCode()
                    val result = testMe.lookup(desiredHash).asSequence().toSet()
                    val expected = inputs.filter { it.hashCode() == desiredHash }.toSet()
                    assertEquals(result, expected)
                }
            }
        }
        "queries for missing values (restricted int)" {
            checkAll(listIntWithLameHashGen, intWithLameHashGen) { inputs, other ->
                if (!inputs.contains(other)) {
                    val testMe = emptyIntSet.addAll(inputs.asIterable())
                    val results = testMe.lookup(other.hashCode()).asSequence().toList()
                    results.forEach { i ->
                        assertEquals(i.hashCode(), other.hashCode())
                        assertTrue(inputs.contains(i))
                        assertNotEquals(i, other)
                    }
                }
            }
        }
        "removing a value and set equality (restricted int)" {
            checkAll(listIntWithLameHashGen, intWithLameHashGen) { inputs, other ->
                val testMe = emptyIntSet.addAll(inputs.asIterable())
                val testMePlus = testMe + other
                val testMinus = testMePlus - other

                // we're testing that we have the expected set behavior, and we're
                // exercising our equals() methods versus the setsEqual() code
                // above that uses Kotlin logic from Container.
                if (inputs.contains(other)) {
                    setsEqual(testMe, testMePlus) shouldBe true
                    testMe shouldBe testMePlus
                    testMe.hashCode() shouldBe testMePlus.hashCode()
                    setsEqual(testMe, testMinus) shouldBe false
                    testMe shouldNotBe testMinus
                } else {
                    setsEqual(testMe, testMinus) shouldBe true
                    testMe shouldBe testMinus
                    testMe.hashCode() shouldBe testMinus.hashCode()
                    setsEqual(testMe, testMePlus) shouldBe false
                    testMe shouldNotBe testMePlus
                }
            }
        }
    }
}
