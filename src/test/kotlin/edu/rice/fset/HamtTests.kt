package edu.rice.fset

import io.kotest.core.spec.style.FreeSpec

class HamtTests : FreeSpec({
    include(fsetTests("HAMT", emptyHamtSet(), emptyHamtSet(), emptyHamtSet()))
})