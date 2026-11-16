package com.parasaurpal.inkandquill.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectHierarchyRulesTest {
    @Test
    fun enforcesCoreStoryHierarchy() {
        assertTrue(ProjectHierarchyRules.canBeParent("Series", "World"))
        assertTrue(ProjectHierarchyRules.canBeParent("Story", "World"))
        assertFalse(ProjectHierarchyRules.canBeParent("Story", "Series"))
        assertTrue(ProjectHierarchyRules.canBeParent("Book", "Series"))
        assertFalse(ProjectHierarchyRules.canBeParent("Book", "World"))
        assertTrue(ProjectHierarchyRules.canBeParent("Chapter", "Book"))
        assertTrue(ProjectHierarchyRules.canBeParent("Scene", "Chapter"))
        assertFalse(ProjectHierarchyRules.canBeParent("World", "World"))
    }

    @Test
    fun preventsSelfParentingAndDescendantCycles() {
        assertFalse(ProjectHierarchyRules.isValidAssignment("a", "Series", "a", "World", emptySet()))
        assertFalse(ProjectHierarchyRules.isValidAssignment("a", "Series", "b", "World", setOf("b")))
        assertTrue(ProjectHierarchyRules.isValidAssignment("a", "Series", "world", "World", setOf("b")))
    }

    @Test
    fun allowsClearingParentAndLinkingWorldbuildingResources() {
        assertTrue(ProjectHierarchyRules.isValidAssignment("a", "Book", "", null, emptySet()))
        assertTrue(ProjectHierarchyRules.canBeParent("Character", "World"))
        assertTrue(ProjectHierarchyRules.canBeParent("Character", "Series"))
        assertTrue(ProjectHierarchyRules.canBeParent("Location", "Story"))
    }
}
