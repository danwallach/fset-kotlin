package edu.rice.fset

import kotlin.random.Random

internal data class BenchmarkResult<T>(val seconds: Double, val result: T)

internal fun <T> measure(block: () -> T): BenchmarkResult<T> {
    val start = System.nanoTime()
    val result: T = block()
    val end = System.nanoTime()
    return BenchmarkResult((end - start) / 1_000_000_000.0, result)
}

internal fun benchmark(name: String, size: Int, offsets: List<Int>, emptyIntSet: FSet<Int>) {
    println("----------------- Benchmarking $name (size = $size, iter = ${offsets.size}) -------------------")

    print("Run: ")
    val runtimes: List<BenchmarkResult<String>> = offsets.map { offset ->
        measure {
            print(".")
            val bigSet1 = emptyIntSet.addAll((offset + 1)..(offset + size))
            println("START")
//            bigSet1.debugPrint()
            println(bigSet1.statistics())
            val bigSet2 = bigSet1.removeAll((offset + 1)..(offset + size / 2))
            println("REMOVE")
//            bigSet2.debugPrint()
            println(bigSet2.statistics())
            val bigSet3 = bigSet2.addAll((offset + 1)..(offset + size * 2))
            println("ADD")
//            bigSet3.debugPrint()
            println(bigSet3.statistics())
            val bigSet4 = bigSet3.removeAll((offset + 1)..(offset + size * 2) step 2)
            println("REMOVE AGAIN")
//            bigSet4.debugPrint()

//            val ignored =
//                ((offset + 1)..(offset + size * 2) step 3).map { bigSet4.lookup(it) ?: 0 }.sum()
//            bigSet.debugPrint()

            val stats4 = bigSet4.statistics()
            println(stats4)
            stats4
        }
    }.toList()
    println(" Done.")

    val minTime: Double = runtimes.minOf { it.seconds }
    val maxTime: Double = runtimes.maxOf { it.seconds }
    val avgTime: Double = runtimes.map { it.seconds }.average()

    println("time:  min = %.3fs, max = %.3fs, avg = %.3fs".format(minTime, maxTime, avgTime))
    runtimes.map { println("stats: ${it.result}") }
    println()
}

fun main() {
    val size = 40000
    val iterations = 1
    val offsets = generateSequence {
        Random.nextInt(Short.MAX_VALUE.toInt())
    }.take(iterations).toList()
//    benchmark("Binary Tree", size, offsets, emptyBinaryTreeSet())
//    benchmark("Binary Choice", size, offsets, emptyBinaryChoiceTreeSet())
//    benchmark("Treap", size, offsets, emptyTreapSet())
//    benchmark("HAMT", size, offsets, emptyHamtSet())
    benchmark("HAMT", size * 10, offsets, emptyHamtSet())
    benchmark("Treap", size * 10, offsets, emptyTreapSet())
}
