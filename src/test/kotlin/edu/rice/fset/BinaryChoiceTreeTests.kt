package edu.rice.fset

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

class BinaryChoiceTreeTests : FreeSpec({
    "standard tests" - {
        include(fsetTests("Binary Choice Tree (2)", emptyBinaryTreeSet(), emptyBinaryTreeSet()))
    }
     "search finds the closest" {
     val v1 = 1
     val h1 = v1.familyHash2()  // h1 = [363786555, -1926862651]
     val v2 = 2
     val h2 = v2.familyHash2()  // h2 = [727573110, 441241994]
     val v3 = 3
     val h3 = v3.familyHash2() // h3 = [1091359665, -1485620657]

     val t1 = BinaryChoiceTreeNode(NodeStorageOne(h1[0], v1), emptyBinaryChoiceTree(), emptyBinaryChoiceTree())
     val t2 = t1.insert(h2[0], v2)
     val t3 = t2.insert(h3[0], v3)

     // okay, this should give a tree like so:
     //                   [363786555 (1)]
     //         ---                             [727573110 (2)]
     //                                     ---                  [1091359665 (3)]

     (t3 as BinaryChoiceTreeNode).storage.hashValue shouldBe 363786555
     t3.left.isEmpty() shouldBe true
     t3.right.isEmpty() shouldBe false
     (t3.right as BinaryChoiceTreeNode).storage.hashValue shouldBe 727573110
     t3.right.left.isEmpty() shouldBe true
     t3.right.right.isEmpty() shouldBe false
     (t3.right.right as BinaryChoiceTreeNode).storage.hashValue shouldBe 1091359665
     t3.right.right.left.isEmpty() shouldBe true
     t3.right.right.right.isEmpty() shouldBe true

     // now we've chosen something where the "power of choices" should send it to the left
     // rather than to the right
     val v5 = 5
     val h5 = v5.familyHash2() // h5 = [1_818_932_775, -1_044_378_663]
     val t5 = t3.insert(v5)

     (t5 as BinaryChoiceTreeNode).storage.hashValue shouldBe 363786555
     t5.left.isEmpty() shouldBe false
     (t5.left as BinaryChoiceTreeNode).storage.hashValue shouldBe -1044378663

     // the rest should be the same as above
     t5.right shouldBeSameInstanceAs t3.right
     }
})
