package org.sigma.semantics;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ScopeTest {

    @Test
    void testCreateGlobalScope() {
        Scope global = new Scope(Scope.ScopeType.GLOBAL);

        assertEquals(Scope.ScopeType.GLOBAL, global.getType());
        assertNull(global.getParent());
        assertEquals(0, global.getSymbolCount());
    }

    @Test
    void testCreateNestedScope() {
        Scope global = new Scope(Scope.ScopeType.GLOBAL);
        Scope method = new Scope(global, Scope.ScopeType.METHOD);

        assertEquals(Scope.ScopeType.METHOD, method.getType());
        assertEquals(global, method.getParent());
    }

    @Test
    void testDefineAndLookupLocal() {
        Scope scope = new Scope(Scope.ScopeType.GLOBAL);
        Symbol symbol = new Symbol("x", TypeRegistry.INT, Symbol.SymbolKind.VARIABLE, 1, 0);

        scope.define(symbol);

        Symbol found = scope.lookupLocal("x");
        assertNotNull(found);
        assertEquals("x", found.getName());
        assertEquals(TypeRegistry.INT, found.getType());
    }

    @Test
    void testLookupParentChain() {
        Scope global = new Scope(Scope.ScopeType.GLOBAL);
        Scope method = new Scope(global, Scope.ScopeType.METHOD);
        Scope block = new Scope(method, Scope.ScopeType.BLOCK);

        // Define symbol in global scope
        Symbol globalVar = new Symbol("x", TypeRegistry.INT, Symbol.SymbolKind.VARIABLE);
        global.define(globalVar);

        // Should be able to find it from nested scope
        Symbol found = block.lookup("x");
        assertNotNull(found);
        assertEquals("x", found.getName());
    }

    @Test
    void testLookupLocalDoesNotSearchParent() {
        Scope global = new Scope(Scope.ScopeType.GLOBAL);
        Scope method = new Scope(global, Scope.ScopeType.METHOD);

        Symbol globalVar = new Symbol("x", TypeRegistry.INT, Symbol.SymbolKind.VARIABLE);
        global.define(globalVar);

        // lookupLocal should not find it in method scope
        assertNull(method.lookupLocal("x"));

        // but lookup should find it
        assertNotNull(method.lookup("x"));
    }

    @Test
    void testShadowing() {
        Scope global = new Scope(Scope.ScopeType.GLOBAL);
        Scope method = new Scope(global, Scope.ScopeType.METHOD);

        // Define x in global scope as int
        Symbol globalX = new Symbol("x", TypeRegistry.INT, Symbol.SymbolKind.VARIABLE);
        global.define(globalX);

        // Define x in method scope as double (shadows global)
        Symbol methodX = new Symbol("x", TypeRegistry.DOUBLE, Symbol.SymbolKind.VARIABLE);
        method.define(methodX);

        // From method scope, should find method's x (double)
        Symbol found = method.lookup("x");
        assertEquals(TypeRegistry.DOUBLE, found.getType());

        // From global scope, should find global x (int)
        Symbol foundGlobal = global.lookup("x");
        assertEquals(TypeRegistry.INT, foundGlobal.getType());
    }

    @Test
    void testIsDefinedLocal() {
        Scope global = new Scope(Scope.ScopeType.GLOBAL);
        Scope method = new Scope(global, Scope.ScopeType.METHOD);

        Symbol globalVar = new Symbol("x", TypeRegistry.INT, Symbol.SymbolKind.VARIABLE);
        global.define(globalVar);

        assertTrue(global.isDefinedLocal("x"));
        assertFalse(method.isDefinedLocal("x"));
    }

    @Test
    void testIsDefined() {
        Scope global = new Scope(Scope.ScopeType.GLOBAL);
        Scope method = new Scope(global, Scope.ScopeType.METHOD);

        Symbol globalVar = new Symbol("x", TypeRegistry.INT, Symbol.SymbolKind.VARIABLE);
        global.define(globalVar);

        assertTrue(global.isDefined("x"));
        assertTrue(method.isDefined("x")); // found in parent
        assertFalse(method.isDefined("y")); // not found anywhere
    }

    @Test
    void testGetSymbols() {
        Scope scope = new Scope(Scope.ScopeType.GLOBAL);

        Symbol x = new Symbol("x", TypeRegistry.INT, Symbol.SymbolKind.VARIABLE);
        Symbol y = new Symbol("y", TypeRegistry.DOUBLE, Symbol.SymbolKind.VARIABLE);

        scope.define(x);
        scope.define(y);

        var symbols = scope.getSymbols();
        assertEquals(2, symbols.size());
        assertTrue(symbols.containsKey("x"));
        assertTrue(symbols.containsKey("y"));
    }

    @Test
    void testGetSymbolCount() {
        Scope scope = new Scope(Scope.ScopeType.GLOBAL);

        assertEquals(0, scope.getSymbolCount());

        scope.define(new Symbol("x", TypeRegistry.INT, Symbol.SymbolKind.VARIABLE));
        assertEquals(1, scope.getSymbolCount());

        scope.define(new Symbol("y", TypeRegistry.DOUBLE, Symbol.SymbolKind.VARIABLE));
        assertEquals(2, scope.getSymbolCount());
    }

    @Test
    void testLookupNonExistent() {
        Scope scope = new Scope(Scope.ScopeType.GLOBAL);

        assertNull(scope.lookup("nonexistent"));
        assertNull(scope.lookupLocal("nonexistent"));
    }

    @Test
    void testMultipleLevelNesting() {
        Scope global = new Scope(Scope.ScopeType.GLOBAL);
        Scope class1 = new Scope(global, Scope.ScopeType.CLASS);
        Scope method = new Scope(class1, Scope.ScopeType.METHOD);
        Scope block1 = new Scope(method, Scope.ScopeType.BLOCK);
        Scope block2 = new Scope(block1, Scope.ScopeType.BLOCK);

        // Define at each level
        global.define(new Symbol("a", TypeRegistry.INT, Symbol.SymbolKind.VARIABLE));
        class1.define(new Symbol("b", TypeRegistry.INT, Symbol.SymbolKind.FIELD));
        method.define(new Symbol("c", TypeRegistry.INT, Symbol.SymbolKind.PARAMETER));
        block1.define(new Symbol("d", TypeRegistry.INT, Symbol.SymbolKind.VARIABLE));
        block2.define(new Symbol("e", TypeRegistry.INT, Symbol.SymbolKind.VARIABLE));

        // From deepest scope, should find all
        assertNotNull(block2.lookup("a"));
        assertNotNull(block2.lookup("b"));
        assertNotNull(block2.lookup("c"));
        assertNotNull(block2.lookup("d"));
        assertNotNull(block2.lookup("e"));

        // But only 'e' should be local to block2
        assertNotNull(block2.lookupLocal("e"));
        assertNull(block2.lookupLocal("a"));
        assertNull(block2.lookupLocal("b"));
        assertNull(block2.lookupLocal("c"));
        assertNull(block2.lookupLocal("d"));
    }
}
