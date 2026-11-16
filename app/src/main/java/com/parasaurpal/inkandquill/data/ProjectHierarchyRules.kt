package com.parasaurpal.inkandquill.data

/** Keeps the core World → Series/Story → Book → Chapter → Scene hierarchy valid. */
object ProjectHierarchyRules {
    private val resourceParentKinds = setOf("World", "Series", "Story", "Book", "Chapter", "Scene")

    fun allowedParentKinds(childKind: String): Set<String> = when (childKind) {
        "World" -> emptySet()
        "Series", "Story" -> setOf("World")
        "Book" -> setOf("Series")
        "Chapter" -> setOf("Book")
        "Scene" -> setOf("Chapter")
        else -> resourceParentKinds
    }

    fun canBeParent(childKind: String, parentKind: String): Boolean =
        parentKind in allowedParentKinds(childKind)

    fun isValidAssignment(
        childId: String,
        childKind: String,
        parentId: String,
        parentKind: String?,
        descendantIds: Set<String>
    ): Boolean {
        if (parentId.isBlank()) return true
        if (parentId == childId || parentId in descendantIds) return false
        return parentKind != null && canBeParent(childKind, parentKind)
    }
}
