package edu.rice.fset

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.assertThrows

class HamtTests : FreeSpec({
     "sparse location" {
         sparseLocation(0x00000001U, 0) shouldBe 0
         sparseLocation(0x00000002U, 1) shouldBe 0
         assertThrows<RuntimeException> { sparseLocation(0x00000002U, 0) }

         sparseLocation(0x00000004U, 2) shouldBe 0
         sparseLocation(0x00000008U, 3) shouldBe 0
         sparseLocation(0x00000010U, 4) shouldBe 0
         sparseLocation(0x00000011U, 0) shouldBe 0
         sparseLocation(0x00000101U, 8) shouldBe 1
         sparseLocation(0x00000111U, 0) shouldBe 0
         sparseLocation(0x00000111U, 4) shouldBe 1
         sparseLocation(0x00000111U, 8) shouldBe 2
         assertThrows<RuntimeException> { sparseLocation(0x00000111U, 7) }
     }
})

class HamtCommonTests : FreeSpec({
    include(fsetTests("HAMT", emptyHamtSet(), emptyHamtSet(), emptyHamtSet()))
})