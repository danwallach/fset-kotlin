package edu.rice.fset

import io.kotest.core.spec.style.FreeSpec

class TreapTests : FreeSpec({
    include(fsetTests("Treap", emptyTreapSet(), emptyTreapSet()))
})
