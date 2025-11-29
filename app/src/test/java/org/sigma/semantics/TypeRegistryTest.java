package org.sigma.semantics;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TypeRegistryTest {

    @Test
    void testBuiltinTypesRegistered() {
        TypeRegistry registry = new TypeRegistry();

        assertTrue(registry.isDefined("int"));
        assertTrue(registry.isDefined("double"));
        assertTrue(registry.isDefined("float"));
        assertTrue(registry.isDefined("boolean"));
        assertTrue(registry.isDefined("String"));
        assertTrue(registry.isDefined("void"));
    }

    @Test
    void testResolveBuiltinType() {
        TypeRegistry registry = new TypeRegistry();

        SigmaType intType = registry.resolve("int");
        assertNotNull(intType);
        assertEquals("int", intType.getName());
        assertTrue(intType instanceof SigmaType.PrimitiveType);
    }

    @Test
    void testResolveUndefinedType() {
        TypeRegistry registry = new TypeRegistry();

        SigmaType unknownType = registry.resolve("UnknownType");
        assertEquals(TypeRegistry.ERROR, unknownType);
    }

    @Test
    void testRegisterCustomClass() {
        TypeRegistry registry = new TypeRegistry();

        registry.registerClass("Calculator");

        assertTrue(registry.isDefined("Calculator"));
        SigmaType calcType = registry.resolve("Calculator");
        assertTrue(calcType instanceof SigmaType.ClassType);
        assertEquals("Calculator", calcType.getName());
    }

    @Test
    void testGetTypeNames() {
        TypeRegistry registry = new TypeRegistry();

        var typeNames = registry.getTypeNames();

        assertTrue(typeNames.contains("int"));
        assertTrue(typeNames.contains("double"));
        assertTrue(typeNames.contains("float"));
        assertTrue(typeNames.contains("boolean"));
        assertTrue(typeNames.contains("String"));
        assertTrue(typeNames.contains("void"));

        // Should have exactly 6 built-in types
        assertEquals(6, typeNames.size());
    }

    @Test
    void testResolveAllBuiltins() {
        TypeRegistry registry = new TypeRegistry();

        // Test that all built-in types resolve correctly
        assertEquals(TypeRegistry.INT, registry.resolve("int"));
        assertEquals(TypeRegistry.DOUBLE, registry.resolve("double"));
        assertEquals(TypeRegistry.FLOAT, registry.resolve("float"));
        assertEquals(TypeRegistry.BOOLEAN, registry.resolve("boolean"));
        assertEquals(TypeRegistry.STRING, registry.resolve("String"));
        assertEquals(TypeRegistry.VOID, registry.resolve("void"));
    }

    @Test
    void testRegisterMultipleClasses() {
        TypeRegistry registry = new TypeRegistry();

        registry.registerClass("Calculator");
        registry.registerClass("Person");
        registry.registerClass("Employee");

        assertTrue(registry.isDefined("Calculator"));
        assertTrue(registry.isDefined("Person"));
        assertTrue(registry.isDefined("Employee"));

        // Should have 6 built-ins + 3 custom = 9 types
        assertEquals(9, registry.getTypeNames().size());
    }

    @Test
    void testNullAndErrorNotRegistered() {
        TypeRegistry registry = new TypeRegistry();

        // null and error are special types, not registered
        assertFalse(registry.isDefined("null"));
        assertFalse(registry.isDefined("<error>"));
    }

    @Test
    void testBuiltinTypeConstants() {
        // Verify built-in type constants are properly initialized
        assertNotNull(TypeRegistry.INT);
        assertNotNull(TypeRegistry.DOUBLE);
        assertNotNull(TypeRegistry.FLOAT);
        assertNotNull(TypeRegistry.BOOLEAN);
        assertNotNull(TypeRegistry.STRING);
        assertNotNull(TypeRegistry.VOID);
        assertNotNull(TypeRegistry.NULL);
        assertNotNull(TypeRegistry.ERROR);

        assertEquals("int", TypeRegistry.INT.getName());
        assertEquals("double", TypeRegistry.DOUBLE.getName());
        assertEquals("float", TypeRegistry.FLOAT.getName());
        assertEquals("boolean", TypeRegistry.BOOLEAN.getName());
        assertEquals("String", TypeRegistry.STRING.getName());
        assertEquals("void", TypeRegistry.VOID.getName());
        assertEquals("null", TypeRegistry.NULL.getName());
        assertEquals("<error>", TypeRegistry.ERROR.getName());
    }

    @Test
    void testPrimitiveTypeSizes() {
        assertEquals(4, ((SigmaType.PrimitiveType) TypeRegistry.INT).getSize());
        assertEquals(8, ((SigmaType.PrimitiveType) TypeRegistry.DOUBLE).getSize());
        assertEquals(4, ((SigmaType.PrimitiveType) TypeRegistry.FLOAT).getSize());
        assertEquals(1, ((SigmaType.PrimitiveType) TypeRegistry.BOOLEAN).getSize());
    }
}
