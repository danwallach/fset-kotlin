package edu.rice.fset

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class HamtTests : FreeSpec({
    "sparse location" {
        sparseBitmapContains(0x00000001U, 0) shouldBe true
        sparseLocation(0x00000001U, 0) shouldBe 0
        sparseBitmapContains(0x00000002U, 1) shouldBe true
        sparseLocation(0x00000002U, 1) shouldBe 0

        sparseBitmapContains(0x00000002U, 0) shouldBe false
        sparseLocation(0x00000002U, 0) shouldBe 0
        sparseBitmapContains(0x00000002U, 2) shouldBe false
        sparseLocation(0x00000002U, 2) shouldBe 1
        sparseBitmapContains(0x00000002U, 5) shouldBe false
        sparseLocation(0x00000002U, 5) shouldBe 1

        sparseBitmapContains(0x00000004U, 2) shouldBe true
        sparseLocation(0x00000004U, 2) shouldBe 0
        sparseBitmapContains(0x00000008U, 3) shouldBe true
        sparseLocation(0x00000008U, 3) shouldBe 0
        sparseBitmapContains(0x00000010U, 4) shouldBe true
        sparseLocation(0x00000010U, 4) shouldBe 0
        sparseBitmapContains(0x00000011U, 0) shouldBe true
        sparseLocation(0x00000011U, 0) shouldBe 0
        sparseBitmapContains(0x00000101U, 8) shouldBe true
        sparseLocation(0x00000101U, 8) shouldBe 1
        sparseBitmapContains(0x00000111U, 0) shouldBe true
        sparseLocation(0x00000111U, 0) shouldBe 0
        sparseBitmapContains(0x00000111U, 4) shouldBe true
        sparseLocation(0x00000111U, 4) shouldBe 1
        sparseBitmapContains(0x00000111U, 8) shouldBe true
        sparseLocation(0x00000111U, 8) shouldBe 2

        sparseBitmapContains(0x00000111U, 7) shouldBe false
        sparseLocation(0x00000111U, 7) shouldBe 2
    }
})

class HamtCommonTests : FreeSpec({
    include(fsetTests("HAMT", emptyHamtSet(), emptyHamtSet(), emptyHamtSet()))
})

class HamtChoiceCommonTests : FreeSpec({
    include(
        fsetTests(
            "HAMT Choice",
            emptyHamtChoiceSet(),
            emptyHamtChoiceSet(),
            emptyHamtChoiceSet()
        )
    )
})
