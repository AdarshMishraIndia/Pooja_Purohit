package com.poojapurohit.data.model

import com.google.firebase.firestore.DocumentSnapshot

/**
 * Lenient numeric read. Firestore returns integers as Long and may return Double;
 * legacy/admin-written docs have also carried numeric strings (e.g. experience).
 */
internal fun Any?.asIntOrZero(): Int = when (this) {
    is Number -> toInt()
    is String -> toIntOrNull() ?: 0
    else -> 0
}

internal fun DocumentSnapshot.getStringList(field: String): List<String> =
    (get(field) as? List<*>)?.filterIsInstance<String>() ?: emptyList()
