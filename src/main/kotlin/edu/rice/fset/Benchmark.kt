package edu.rice.fset

import kotlin.random.Random

internal data class BenchmarkResult<T>(val seconds: Double, val result: T)

internal fun <T> measure(block: () -> T): BenchmarkResult<T> {
    val start = System.nanoTime()
    val result: T = block()
    val end = System.nanoTime()
    return BenchmarkResult((end - start) / 1_000_000_000.0, result)
}

internal fun benchmark(name: String, size: Int, iterations: Int, emptyIntSet: FSet<Int>) {
    println("----------------- Benchmarking $name (size = $size, iter = $iterations) -------------------")

    print("Run: ")
    val runtimes: List<BenchmarkResult<String>> = (1..iterations).map {
        measure {
            print(".")
            val offset = Random.nextInt(Short.MAX_VALUE.toInt())
            val bigSet1 = emptyIntSet.addAll((offset + 1)..(offset + size))
//            println("START")
//            bigSet1.debugPrint()
            val bigSet2 = bigSet1.removeAll((offset + 1)..(offset + size / 2))
//            println("REMOVE")
//            bigSet2.debugPrint()
            val bigSet3 = bigSet2.addAll((offset + 1)..(offset + size * 2))
//            println("ADD")
//            bigSet3.debugPrint()
            val bigSet4 = bigSet3.removeAll((offset + 1)..(offset + size * 2) step 2)
//            println("REMOVE AGAIN")
//            bigSet4.debugPrint()

            val ignored = ((offset + 1)..(offset + size * 2) step 3).map { bigSet4.lookup(it) ?: 0 }.sum()
//            bigSet.debugPrint()
            bigSet4.statistics()
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
    val size = 20000
    val iterations = 10
    benchmark("Treap", size, iterations, emptyTreapSet())
    benchmark("Binary Tree", size, iterations, emptyBinaryTreeSet())
    benchmark("Binary Choice", size, iterations, emptyBinaryChoiceTreeSet())
}
