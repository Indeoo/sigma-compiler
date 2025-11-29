package org.sigma.semantics;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SymbolTableTest {

    @Test
    void testCreateSymbolTable() {
        SymbolTable table = new SymbolTable();

        assertTrue(table.isGlobalScope());
        assertEquals(Scope.ScopeType.GLOBAL, table.getCurrentScopeType());
        assertEquals(0, table.getScopeDepth());
    }

    @Test
    void testDefineInGlobalScope() {
        SymbolTable table = new SymbolTable();

        boolean success = table.define("x", TypeRegistry.INT, Symbol.SymbolKind.VARIABLE);

        assertTrue(success);
        assertTrue(table.isDefined("x"));

        Symbol found = table.lookup("x");
        assertNotNull(found);
        assertEquals("x", found.getName());
        assertEquals(TypeRegistry.INT, found.getType());
    }

    @Test
    void testDefineWithPosition() {
        SymbolTable table = new SymbolTable();

        boolean success = table.define("x", TypeRegistry.INT, Symbol.SymbolKind.VARIABLE, 10, 5);

        assertTrue(success);
        Symbol found = table.lookup("x");
        assertEquals(10, found.getDefinitionLine());
        assertEquals(5, found.getDefinitionCol());
    }

    @Test
    void testDuplicateDefinitionDetection() {
        SymbolTable table = new SymbolTable();

        table.define("x", TypeRegistry.INT, Symbol.SymbolKind.VARIABLE, 1, 0);
        boolean success = table.define("x", TypeRegistry.DOUBLE, Symbol.SymbolKind.VARIABLE, 2, 0);

        assertFalse(success);
        assertTrue(table.hasErrors());

        var errors = table.getErrors();
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("already defined"));
    }

    @Test
    void testEnterAndExitScope() {
        SymbolTable table = new SymbolTable();

        assertEquals(0, table.getScopeDepth());

        table.enterScope(Scope.ScopeType.METHOD);
        assertEquals(1, table.getScopeDepth());
        assertEquals(Scope.ScopeType.METHOD, table.getCurrentScopeType());

        table.exitScope();
        assertEquals(0, table.getScopeDepth());
        assertEquals(Scope.ScopeType.GLOBAL, table.getCurrentScopeType());
    }

    @Test
    void testCannotExitGlobalScope() {
        SymbolTable table = new SymbolTable();

        assertThrows(IllegalStateException.class, () -> table.exitScope());
    }

    @Test
    void testNestedScopes() {
        SymbolTable table = new SymbolTable();

        table.enterScope(Scope.ScopeType.CLASS);
        table.enterScope(Scope.ScopeType.METHOD);
        table.enterScope(Scope.ScopeType.BLOCK);

        assertEquals(3, table.getScopeDepth());
        assertEquals(Scope.ScopeType.BLOCK, table.getCurrentScopeType());

        table.exitScope();
        assertEquals(Scope.ScopeType.METHOD, table.getCurrentScopeType());

        table.exitScope();
        assertEquals(Scope.ScopeType.CLASS, table.getCurrentScopeType());

        table.exitScope();
        assertTrue(table.isGlobalScope());
    }

    @Test
    void testShadowingInNestedScope() {
        SymbolTable table = new SymbolTable();

        // Define x in global scope as int
        table.define("x", TypeRegistry.INT, Symbol.SymbolKind.VARIABLE);

        // Enter method scope and define x as double
        table.enterScope(Scope.ScopeType.METHOD);
        table.define("x", TypeRegistry.DOUBLE, Symbol.SymbolKind.VARIABLE);

        // From method scope, should find double version
        Symbol found = table.lookup("x");
        assertEquals(TypeRegistry.DOUBLE, found.getType());

        // Exit to global scope
        table.exitScope();

        // Now should find int version
        found = table.lookup("x");
        assertEquals(TypeRegistry.INT, found.getType());
    }

    @Test
    void testLookupInParentScope() {
        SymbolTable table = new SymbolTable();

        // Define in global scope
        table.define("globalVar", TypeRegistry.INT, Symbol.SymbolKind.VARIABLE);

        // Enter method scope
        table.enterScope(Scope.ScopeType.METHOD);

        // Should still be able to find global variable
        assertTrue(table.isDefined("globalVar"));
        assertNotNull(table.lookup("globalVar"));

        // But not locally defined
        assertFalse(table.isDefinedLocal("globalVar"));
        assertNull(table.lookupLocal("globalVar"));
    }

    @Test
    void testIsInMethodScope() {
        SymbolTable table = new SymbolTable();

        assertFalse(table.isInMethodScope());

        table.enterScope(Scope.ScopeType.METHOD);
        assertTrue(table.isInMethodScope());

        table.enterScope(Scope.ScopeType.BLOCK);
        assertTrue(table.isInMethodScope()); // still in method

        table.exitScope();
        table.exitScope();
        assertFalse(table.isInMethodScope());
    }

    @Test
    void testIsInClassScope() {
        SymbolTable table = new SymbolTable();

        assertFalse(table.isInClassScope());

        table.enterScope(Scope.ScopeType.CLASS);
        assertTrue(table.isInClassScope());

        table.enterScope(Scope.ScopeType.METHOD);
        assertTrue(table.isInClassScope()); // still in class

        table.exitScope();
        table.exitScope();
        assertFalse(table.isInClassScope());
    }

    @Test
    void testClearErrors() {
        SymbolTable table = new SymbolTable();

        // Create duplicate definition error
        table.define("x", TypeRegistry.INT, Symbol.SymbolKind.VARIABLE);
        table.define("x", TypeRegistry.DOUBLE, Symbol.SymbolKind.VARIABLE);

        assertTrue(table.hasErrors());

        table.clearErrors();

        assertFalse(table.hasErrors());
        assertEquals(0, table.getErrors().size());
    }

    @Test
    void testComplexScopeHierarchy() {
        SymbolTable table = new SymbolTable();

        // Global scope
        table.define("global", TypeRegistry.INT, Symbol.SymbolKind.VARIABLE);

        // Class scope
        table.enterScope(Scope.ScopeType.CLASS);
        table.define("classField", TypeRegistry.INT, Symbol.SymbolKind.FIELD);

        // Method scope
        table.enterScope(Scope.ScopeType.METHOD);
        table.define("param", TypeRegistry.INT, Symbol.SymbolKind.PARAMETER);

        // Block scope
        table.enterScope(Scope.ScopeType.BLOCK);
        table.define("local", TypeRegistry.INT, Symbol.SymbolKind.VARIABLE);

        // From deepest scope, all should be visible
        assertNotNull(table.lookup("global"));
        assertNotNull(table.lookup("classField"));
        assertNotNull(table.lookup("param"));
        assertNotNull(table.lookup("local"));

        assertEquals(3, table.getScopeDepth());

        // Exit block
        table.exitScope();
        assertNull(table.lookup("local")); // local variable gone
        assertNotNull(table.lookup("param")); // but param still visible

        // Exit method
        table.exitScope();
        assertNull(table.lookup("param")); // param gone
        assertNotNull(table.lookup("classField")); // class field still visible

        // Exit class
        table.exitScope();
        assertNull(table.lookup("classField")); // class field gone
        assertNotNull(table.lookup("global")); // global still visible
    }

    @Test
    void testDefineMultipleSymbols() {
        SymbolTable table = new SymbolTable();

        table.define("x", TypeRegistry.INT, Symbol.SymbolKind.VARIABLE);
        table.define("y", TypeRegistry.DOUBLE, Symbol.SymbolKind.VARIABLE);
        table.define("z", TypeRegistry.STRING, Symbol.SymbolKind.VARIABLE);

        assertTrue(table.isDefined("x"));
        assertTrue(table.isDefined("y"));
        assertTrue(table.isDefined("z"));
        assertFalse(table.isDefined("w"));
    }

    @Test
    void testGetCurrentScope() {
        SymbolTable table = new SymbolTable();

        Scope global = table.getCurrentScope();
        assertNotNull(global);
        assertEquals(Scope.ScopeType.GLOBAL, global.getType());

        table.enterScope(Scope.ScopeType.METHOD);
        Scope method = table.getCurrentScope();
        assertEquals(Scope.ScopeType.METHOD, method.getType());
        assertEquals(global, method.getParent());
    }

    @Test
    void testSymbolTableToString() {
        SymbolTable table = new SymbolTable();

        String str = table.toString();
        assertTrue(str.contains("SymbolTable"));
        assertTrue(str.contains("depth=0"));
    }
}
