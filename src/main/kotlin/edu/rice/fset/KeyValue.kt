package edu.rice.fset

// General-purpose storage for keys & values, which requires non-null
// keys, but allows for null values. HashCode and equality exclusively
// use the key and ignore the value, making these useful for doing
// queries, updates, and so forth within a functional set, creating
// a functional map.

// The base KeyValue doesn't have a value at all, and is used for queries.
// The extension KeyValuePair is used for KeyValue tuples.

internal open class KeyValue<K : Any, V>(val key: K) {
    override fun hashCode(): Int {
        return key.hashCode()
    }

    override fun equals(other: Any?) = when (other) {
        is KeyValue<*, *> -> key == other.key
        else -> false
    }

    override fun toString(): String {
        return "KeyValue($key)"
    }
}

internal class KeyValuePair<K : Any, V>(key: K, val value: V) : KeyValue<K, V>(key) {
    override fun toString(): String {
        return "KeyValuePair($key, $value)"
    }
}

internal fun <K : Any> key(key: K): KeyValue<K, Any> = KeyValue(key)
internal fun <K : Any, V> kv(key: K, value: V) = KeyValuePair(key, value)
