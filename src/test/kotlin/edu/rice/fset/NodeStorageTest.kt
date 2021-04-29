package edu.rice.fset

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.checkAll
import io.kotest.property.forAll

class NodeStorageTest : FreeSpec({
    "NodeStorageOne" - {
        "remove elem gives null" {
            checkAll<String> { s -> nodeStorageOf(s.hashCode(), s).remove(s) shouldBe null }
        }
        "simple equality" {
            checkAll<String> { s ->
                nodeStorageOf(
                    s.hashCode(),
                    s
                ) shouldBe nodeStorageOf(s.hashCode(), s)
            }
        }
        "singleton" {
            forAll<String> { s -> nodeStorageOf(s.hashCode(), s).isSingleton() }
        }
        "remove non-present elem is no-op" {
            checkAll<String, String> { s1, s2 ->
                if (s1 != s2) {
                    val storage = nodeStorageOf(s1.hashCode(), s1)
                    storage.remove(s2) shouldBe storage
                }
            }
        }
        "one-elem contains its element" {
            forAll<String> { s -> nodeStorageOf(s.hashCode(), s).contains(s) }
        }
        "one-elem contains doesn't contain other element" {
            forAll<String, String> { s1, s2 ->
                s1 == s2 || !nodeStorageOf(
                    s1.hashCode(),
                    s1
                ).contains(s2)
            }
        }
        "iterator" {
            forAll<String> { s -> nodeStorageOf(s.hashCode(), s).iterator().next() == s }
        }
        "insert existing value changes nothing" {
            checkAll<String> { s ->
                val storage = nodeStorageOf(s.hashCode(), s)
                storage.insert(s) shouldBe storage
            }
        }
    }
    "NodeStorageList" - {
        "remove one yields singleton" {
            checkAll<String, String> { s1, s2 ->
                if (s1 != s2) {
                    val store1 = nodeStorageOf(s1.hashCode(), s1)
                    val store2 = nodeStorageOf(s1.hashCode(), s2) // yes, s1.hashCode()
                    store1.insert(s2).remove(s2) shouldBe store1
                    store2.insert(s1).remove(s1) shouldBe store2
                }
            }
        }
        "insert existing value changes nothing" {
            checkAll<String, String> { s1, s2 ->
                if (s1 != s2) {
                    val storage = nodeStorageOf(s1.hashCode(), s1).insert(s2)
                    storage.insert(s2) shouldBe storage
                    storage.insert(s1) shouldBe storage
                }
            }
        }
        "simple equality" {
            checkAll<String, String, Int> { s1, s2, h ->
                nodeStorageOf(h, s1).insert(s2) shouldBe nodeStorageOf(h, s2).insert(s1)
            }
        }
        "singleton" {
            checkAll<String, String, Int> { s1, s2, h ->
                if (s1 != s2) {
                    nodeStorageOf(h, s1).insert(s2).isSingleton() shouldBe false
                }
            }
        }
        "remove non-present elem is no-op" {
            checkAll<String, String, String, Int> { s1, s2, s3, h ->
                if (s1 != s3 && s2 != s3) {
                    val storage = nodeStorageOf(h, s1).insert(s2)
                    storage.remove(s3) shouldBe storage
                }
            }
        }
        "two-elem contains its elements" {
            checkAll<String, String, Int> { s1, s2, h ->
                val storage = nodeStorageOf(h, s1).insert(s2)
                storage.contains(s1) shouldBe true
                storage.contains(s2) shouldBe true
            }
        }
        "two-elem contains or not" {
            checkAll<String, String, String, Int> { s1, s2, s3, h ->
                val storage = nodeStorageOf(h, s1).insert(s2)
                storage.contains(s3) shouldBe (s1 == s3 || s2 == s3)
            }
        }
        "iterator" {
            checkAll<String, String, Int> { s1, s2, h ->
                if (s1 != s2) {
                    val storage = nodeStorageOf(h, s1).insert(s2)
                    val result = storage.iterator().asSequence().toSet()
                    result shouldBe setOf(s1, s2)
                }
            }
        }
    }
})
