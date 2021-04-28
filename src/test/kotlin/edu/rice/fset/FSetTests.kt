package edu.rice.fset

import io.kotest.core.spec.style.freeSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.next
import io.kotest.property.checkAll

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

fun <E : Any> fsetTests(
    algorithm: String,
    emptyStringSet: FSet<String>,
    emptyIntSet: FSet<IntWithLameHash>
) =
    freeSpec {
        algorithm - {
            "basic inserts and membership (string)" {
                checkAll<List<String>> { inputs ->
                    val testMe = emptyStringSet.addAll(inputs.asIterable())
                    inputs.forEach { i ->
                        // hash collisions are basically never going to happen here
                        val desiredHash = i.hashCode()
                        val result = testMe.lookup(desiredHash).asSequence().toSet()
                        val expected = inputs.filter { it.hashCode() == desiredHash }.toSet()
                        result shouldBe expected
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
                        result shouldBe expected
                    }
                }
            }
        }
    }
