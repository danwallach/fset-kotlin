package edu.rice.fset

import io.kotest.core.spec.style.FreeSpec

class BinaryTreeTests : FreeSpec({
    include(fsetTests<String>("Binary Tree", emptyBinaryTreeSet(), emptyBinaryTreeSet()))
})
