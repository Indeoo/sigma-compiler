package org.sigma.semantics;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SymbolTest {

    @Test
    void testCreateSymbolWithPosition() {
        Symbol symbol = new Symbol("x", TypeRegistry.INT, Symbol.SymbolKind.VARIABLE, 10, 5);

        assertEquals("x", symbol.getName());
        assertEquals(TypeRegistry.INT, symbol.getType());
        assertEquals(Symbol.SymbolKind.VARIABLE, symbol.getKind());
        assertEquals(10, symbol.getDefinitionLine());
        assertEquals(5, symbol.getDefinitionCol());
    }

    @Test
    void testCreateSymbolWithoutPosition() {
        Symbol symbol = new Symbol("y", TypeRegistry.DOUBLE, Symbol.SymbolKind.PARAMETER);

        assertEquals("y", symbol.getName());
        assertEquals(TypeRegistry.DOUBLE, symbol.getType());
        assertEquals(Symbol.SymbolKind.PARAMETER, symbol.getKind());
        assertEquals(0, symbol.getDefinitionLine());
        assertEquals(0, symbol.getDefinitionCol());
    }

    @Test
    void testIsVariable() {
        Symbol var = new Symbol("x", TypeRegistry.INT, Symbol.SymbolKind.VARIABLE);
        Symbol param = new Symbol("y", TypeRegistry.INT, Symbol.SymbolKind.PARAMETER);

        assertTrue(var.isVariable());
        assertFalse(param.isVariable());
    }

    @Test
    void testIsConstant() {
        Symbol constant = new Symbol("X", TypeRegistry.INT, Symbol.SymbolKind.CONSTANT);
        Symbol var = new Symbol("y", TypeRegistry.INT, Symbol.SymbolKind.VARIABLE);

        assertTrue(constant.isConstant());
        assertFalse(var.isConstant());
    }

    @Test
    void testIsParameter() {
        Symbol param = new Symbol("x", TypeRegistry.INT, Symbol.SymbolKind.PARAMETER);
        Symbol var = new Symbol("y", TypeRegistry.INT, Symbol.SymbolKind.VARIABLE);

        assertTrue(param.isParameter());
        assertFalse(var.isParameter());
    }

    @Test
    void testIsMethod() {
        Symbol method = new Symbol("foo", TypeRegistry.VOID, Symbol.SymbolKind.METHOD);
        Symbol var = new Symbol("x", TypeRegistry.INT, Symbol.SymbolKind.VARIABLE);

        assertTrue(method.isMethod());
        assertFalse(var.isMethod());
    }

    @Test
    void testIsClass() {
        Symbol cls = new Symbol("Calculator", TypeRegistry.STRING, Symbol.SymbolKind.CLASS);
        Symbol var = new Symbol("x", TypeRegistry.INT, Symbol.SymbolKind.VARIABLE);

        assertTrue(cls.isClass());
        assertFalse(var.isClass());
    }

    @Test
    void testIsField() {
        Symbol field = new Symbol("pi", TypeRegistry.DOUBLE, Symbol.SymbolKind.FIELD);
        Symbol var = new Symbol("x", TypeRegistry.INT, Symbol.SymbolKind.VARIABLE);

        assertTrue(field.isField());
        assertFalse(var.isField());
    }

    @Test
    void testSymbolEquality() {
        Symbol sym1 = new Symbol("x", TypeRegistry.INT, Symbol.SymbolKind.VARIABLE);
        Symbol sym2 = new Symbol("x", TypeRegistry.INT, Symbol.SymbolKind.VARIABLE);
        Symbol sym3 = new Symbol("y", TypeRegistry.INT, Symbol.SymbolKind.VARIABLE);

        assertEquals(sym1, sym2);
        assertNotEquals(sym1, sym3);
    }

    @Test
    void testSymbolEqualityDifferentTypes() {
        Symbol sym1 = new Symbol("x", TypeRegistry.INT, Symbol.SymbolKind.VARIABLE);
        Symbol sym2 = new Symbol("x", TypeRegistry.DOUBLE, Symbol.SymbolKind.VARIABLE);

        assertNotEquals(sym1, sym2);
    }

    @Test
    void testSymbolEqualityDifferentKinds() {
        Symbol sym1 = new Symbol("x", TypeRegistry.INT, Symbol.SymbolKind.VARIABLE);
        Symbol sym2 = new Symbol("x", TypeRegistry.INT, Symbol.SymbolKind.PARAMETER);

        assertNotEquals(sym1, sym2);
    }

    @Test
    void testSymbolHashCode() {
        Symbol sym1 = new Symbol("x", TypeRegistry.INT, Symbol.SymbolKind.VARIABLE);
        Symbol sym2 = new Symbol("x", TypeRegistry.INT, Symbol.SymbolKind.VARIABLE);

        assertEquals(sym1.hashCode(), sym2.hashCode());
    }

    @Test
    void testSymbolToString() {
        Symbol symbol = new Symbol("x", TypeRegistry.INT, Symbol.SymbolKind.VARIABLE, 10, 5);

        String str = symbol.toString();
        assertTrue(str.contains("x"));
        assertTrue(str.contains("int"));
        assertTrue(str.contains("VARIABLE"));
        assertTrue(str.contains("10"));
    }

    @Test
    void testAllSymbolKinds() {
        Symbol var = new Symbol("x", TypeRegistry.INT, Symbol.SymbolKind.VARIABLE);
        Symbol constant = new Symbol("c", TypeRegistry.INT, Symbol.SymbolKind.CONSTANT);
        Symbol param = new Symbol("p", TypeRegistry.INT, Symbol.SymbolKind.PARAMETER);
        Symbol method = new Symbol("m", TypeRegistry.VOID, Symbol.SymbolKind.METHOD);
        Symbol cls = new Symbol("C", TypeRegistry.STRING, Symbol.SymbolKind.CLASS);
        Symbol field = new Symbol("f", TypeRegistry.INT, Symbol.SymbolKind.FIELD);

        assertTrue(var.isVariable());
        assertTrue(constant.isConstant());
        assertTrue(param.isParameter());
        assertTrue(method.isMethod());
        assertTrue(cls.isClass());
        assertTrue(field.isField());
    }

    @Test
    void testSymbolWithDifferentTypes() {
        Symbol intSym = new Symbol("x", TypeRegistry.INT, Symbol.SymbolKind.VARIABLE);
        Symbol doubleSym = new Symbol("y", TypeRegistry.DOUBLE, Symbol.SymbolKind.VARIABLE);
        Symbol stringSym = new Symbol("z", TypeRegistry.STRING, Symbol.SymbolKind.VARIABLE);
        Symbol boolSym = new Symbol("b", TypeRegistry.BOOLEAN, Symbol.SymbolKind.VARIABLE);

        assertEquals(TypeRegistry.INT, intSym.getType());
        assertEquals(TypeRegistry.DOUBLE, doubleSym.getType());
        assertEquals(TypeRegistry.STRING, stringSym.getType());
        assertEquals(TypeRegistry.BOOLEAN, boolSym.getType());
    }
}
