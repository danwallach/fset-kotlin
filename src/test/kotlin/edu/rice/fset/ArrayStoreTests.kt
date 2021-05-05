package edu.rice.fset

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll

internal val listIntWithIndex = arbitrary { rs ->
    val length = Arb.int(1, 10).next(rs)
    val list = Arb.list(Arb.int(0, 10000), length..length).next(rs)
    val query = Arb.int(0, length - 1).next(rs)
    Pair(list, query)
}
internal val listKVWithIndex = arbitrary { rs ->
    val length = Arb.int(1, 10).next(rs)
    val list = Arb.list(
        Arb.int(0, 10000)
            .map { kv(it.toString(), it.toString().length) },
        length..length
    )
        .next(rs)
    val query = Arb.int(0, length - 1).next(rs)
    Pair(list, query)
}

class ArrayStoreTests : FreeSpec({
    "basic equality" {
        checkAll(listIntWithIndex) { (list, _) ->
            val as1 = list.toArrayStore()
            val as2 = list.toArrayStore()
            list shouldBe as1.toList()
            as1.deepEquals(as2) shouldBe true
            as1.hashCode() shouldBe as2.hashCode()
        }
    }

    "getting" {
        checkAll(listIntWithIndex) { (list, query) ->
            val as1 = list.toArrayStore()
            as1[query] shouldBe list.get(query)
        }
    }

    "withoutIndex" {
        checkAll(listIntWithIndex) { (list, query) ->
            val as1 = list.toArrayStore()
            as1.withoutIndex(query)
                .toList() shouldBe list.filterIndexed { index, _ -> query != index }
        }
    }
    "withoutElement" {
        checkAll(listIntWithIndex) { (list, query) ->
            val as1 = list.toArrayStore()
            // might be multiple copies; we're checking that withoutElement removes the first of them
            val firstOffsetOf = list.indexOf(list[query])
            as1.withoutElement(as1[firstOffsetOf])
                .toList() shouldBe list.filterIndexed { index, _ -> firstOffsetOf != index }
        }
    }
    "updateElement" {
        checkAll(listKVWithIndex) { (list, query) ->
            val as1 = list.toArrayStore()
            val firstOffsetOf = list.indexOf(list[query])
            val updateVal = kv(
                list[query].key,
                -list[query].value
            ) // keys are still equal, values are different
            val as2 = as1.updateElement(updateVal)
            as2.deepEquals(as2) shouldBe true
            as2[firstOffsetOf].key shouldBe as1[firstOffsetOf].key
            as2[firstOffsetOf].value shouldBe -as1[firstOffsetOf].value

            // now, we'll try something that's not there
            as1.updateElement(kv("-5", 5)).deepEquals(as1) shouldBe true
        }
    }
    "contains and find" {
        checkAll(listIntWithIndex) { (list, query) ->
            val as1 = list.toArrayStore()
            as1.contains(list[query]) shouldBe true
            as1.contains(-5) shouldBe false
            as1.find(list[query]) shouldBe list[query]
            as1.find(-5) shouldBe null
        }
    }
    "insert" {
        checkAll(listIntWithIndex) { (list, query) ->
            val as1 = list.toArrayStore()
            val as2 = as1.insert(-5, query)
            as2.size() shouldBe list.size + 1
            as2.toList() shouldBe list.subList(0, query) + (-5) + list.subList(query, list.size)
        }
    }
    "mapIndexed" {
        checkAll(listIntWithIndex) { (list, _) ->
            val as1 = list.toArrayStore()
            val as2 = as1.mapIndexed { index, value -> index + value }
            as2.toList() shouldBe list.mapIndexed { index, value -> index + value }
        }
    }
    "append" {
        checkAll(listIntWithIndex) { (list, _) ->
            val as1 = list.toArrayStore()
            val as2 = as1.append(-5)
            as2.toList() shouldBe list + (-5)
        }
    }
    "flatMap" {
        checkAll(listIntWithIndex) { (list, _) ->
            val as1 = list.toArrayStore()
            as1.flatMap {
                sequenceOf("<", it.toString(), ">")
            }.toList() shouldBe list.flatMap {
                sequenceOf("<", it.toString(), ">")
            }.toList()
        }
    }
    "joinToString" {
        checkAll(listIntWithIndex) { (list, _) ->
            val as1 = list.toArrayStore()
            as1.joinToString(", ") shouldBe list.joinToString(separator = ", ")
        }
    }
    "toSet" {
        checkAll(listIntWithIndex) { (list, _) ->
            val as1 = list.toArrayStore()
            as1.toSet() shouldBe list.toSet()
        }
    }
})
