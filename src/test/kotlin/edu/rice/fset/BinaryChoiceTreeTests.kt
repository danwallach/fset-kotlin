package edu.rice.fset

import io.kotest.core.spec.style.FreeSpec

class BinaryChoiceTreeTests : FreeSpec({
    include(fsetTests("Binary Choice Tree (2)", emptyBinaryTreeSet(), emptyBinaryTreeSet()))
})
