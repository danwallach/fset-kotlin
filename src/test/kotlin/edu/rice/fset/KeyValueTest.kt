package edu.rice.fset

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class KeyValueTest : FreeSpec({
    "equality basics" {
        key("Hello") shouldBe kv("Hello", "World")
        kv("Hello", "World") shouldBe kv("Hello", "Seriously")
        kv("Hello", "World").hashCode() shouldBe kv("Hello", "Seriously").hashCode()
        kv("Hello", "World").toString() shouldNotBe kv("Hello", "Seriously").toString()

        key("Hello") shouldBe kv("Hello", null)
        kv("Hello", "World") shouldBe kv("Hello", 3)
    }
})
