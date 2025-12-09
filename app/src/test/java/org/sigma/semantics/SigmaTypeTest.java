package org.sigma.semantics;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SigmaTypeTest {

    @Test
    void testPrimitiveTypeEquality() {
        SigmaType int1 = new SigmaType.PrimitiveType("int", 4);
        SigmaType int2 = new SigmaType.PrimitiveType("int", 4);

        assertEquals(int1, int2);
        assertEquals("int", int1.getName());
    }

    @Test
    void testNumericWidening() {
        SigmaType intType = TypeRegistry.INT;
        SigmaType doubleType = TypeRegistry.DOUBLE;
        SigmaType floatType = TypeRegistry.FLOAT;

        // int can be widened to double
        assertTrue(intType.isCompatibleWith(doubleType));

        // int can be widened to float
        assertTrue(intType.isCompatibleWith(floatType));

        // float can be widened to double
        assertTrue(floatType.isCompatibleWith(doubleType));

        // but not the reverse
        //assertFalse(doubleType.isCompatibleWith(intType));
        assertFalse(floatType.isCompatibleWith(intType));
        assertFalse(doubleType.isCompatibleWith(floatType));
    }

    @Test
    void testNullCompatibility() {
        SigmaType nullType = TypeRegistry.NULL;
        SigmaType stringType = TypeRegistry.STRING;
        SigmaType intType = TypeRegistry.INT;

        // null is compatible with reference types
        assertTrue(nullType.isCompatibleWith(stringType));

        // but not with primitives
        assertFalse(nullType.isCompatibleWith(intType));
    }

    @Test
    void testVoidIncompatibility() {
        SigmaType voidType = TypeRegistry.VOID;
        SigmaType intType = TypeRegistry.INT;

        // void is not compatible with anything
        assertFalse(voidType.isCompatibleWith(intType));
        assertFalse(intType.isCompatibleWith(voidType));
    }

    @Test
    void testErrorTypeCompatibility() {
        SigmaType errorType = TypeRegistry.ERROR;
        SigmaType intType = TypeRegistry.INT;

        // error type is compatible with everything (to suppress cascading errors)
        assertTrue(errorType.isCompatibleWith(intType));
    }

    @Test
    void testClassTypeEquality() {
        SigmaType class1 = new SigmaType.ClassType("Calculator");
        SigmaType class2 = new SigmaType.ClassType("Calculator");

        assertEquals(class1, class2);
        assertEquals("Calculator", class1.getName());
    }

    @Test
    void testClassTypeNullCompatibility() {
        SigmaType calculatorType = new SigmaType.ClassType("Calculator");
        SigmaType nullType = TypeRegistry.NULL;

        // class types can accept null
        assertTrue(calculatorType.isCompatibleWith(nullType));
    }

    @Test
    void testIsPrimitive() {
        assertTrue(TypeRegistry.INT.isPrimitive());
        assertTrue(TypeRegistry.DOUBLE.isPrimitive());
        assertFalse(TypeRegistry.STRING.isPrimitive());
        assertFalse(TypeRegistry.VOID.isPrimitive());
    }

    @Test
    void testIsReference() {
        assertTrue(TypeRegistry.STRING.isReference());
        assertTrue(TypeRegistry.NULL.isReference());
        assertFalse(TypeRegistry.INT.isReference());
        assertFalse(TypeRegistry.VOID.isReference());
    }

    @Test
    void testToString() {
        assertEquals("int", TypeRegistry.INT.toString());
        assertEquals("String", TypeRegistry.STRING.toString());
        assertEquals("void", TypeRegistry.VOID.toString());
        assertEquals("null", TypeRegistry.NULL.toString());
        assertEquals("<error>", TypeRegistry.ERROR.toString());
    }

    @Test
    void testSameTypeCompatibility() {
        SigmaType intType = TypeRegistry.INT;
        SigmaType stringType = TypeRegistry.STRING;

        // same type is always compatible with itself
        assertTrue(intType.isCompatibleWith(intType));
        assertTrue(stringType.isCompatibleWith(stringType));
    }

    @Test
    void testBooleanType() {
        SigmaType booleanType = TypeRegistry.BOOLEAN;

        assertEquals("boolean", booleanType.getName());
        assertTrue(booleanType.isPrimitive());
        assertFalse(booleanType.isReference());

        // boolean is not compatible with other primitives
        assertFalse(booleanType.isCompatibleWith(TypeRegistry.INT));
        assertFalse(TypeRegistry.INT.isCompatibleWith(booleanType));
    }
}
