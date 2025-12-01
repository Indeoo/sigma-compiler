// Test complex expressions and control flow

// Var Declaration and assingment
int a;
a = 10;
int b = 5;
int c = 3;

// Constant declaration
final String testString = "STRING";
final int constant = 10;

// Test complex arithmetic
int result = a * b + c - 2;

// Test comparison and logical operators
boolean flag1 = a > b;
boolean flag2 = b <= c;
boolean combined = flag1 && flag2;

c = c + b + a;

// Test if-else
if (c > 5) {
    c = c + 1;
} else {
    c = c - 1;
}


// Test while
while(flag1) {
    if (c > 5) {
        c = c + 1;
    } else {
        c = c - 1;
    }

    c = c + 1;

    flag1 = c > 20;
}

// Groovy-like for loops (typed and untyped)
for (int loopValue in c) {
    c = c + loopValue;
}

for item in testString {
    String loopMessage = item;
}

// Test unary operators
int negated = -a;
boolean inverted = !flag1;

int test(int i) {
    i = i + 1;

    return i;
}

// Class creation
class Calculator {
    double pi = 3.14159;

    double circleArea(double radius) {
        return pi * radius * radius;
    }
}


Calculator calc = new Calculator();

//SEMANTIC TESTS
//// Variable type checks: existing types
//int semanticIntValue = 42;
//double semanticDoubleValue = 3.5;
//float semanticFloatValue = 1;
//String semanticStringValue = "semantic";
//boolean semanticBooleanValue = true;
//String nullableString = null; // Null literal typing
//
//// Undefined type detection for variable declarations
//UndefinedType missingTypeVariable;
//
//// Initializer compatibility with declared type
//int incompatibleInitializer = "oops";
//
//// Duplicate declarations within the same scope
//int duplicatedVariable = 10;
//int duplicatedVariable = 20;
//
//// Literal type inference
//int literalIntValue = 7;
//double literalDoubleValue = 2.25;
//String literalStringValue = "text";
//boolean literalBooleanValue = false;
//
//// Arithmetic operators on numeric types
//double arithmeticMix = literalIntValue + literalDoubleValue - 3 * 2 / 1 % 1;
//double powerChain = literalIntValue ** 2;
//
//// String concatenation via +
//String stringConcatenation = "Result: " + literalIntValue + " / " + semanticStringValue;
//
//// Logical operators && and ||
//boolean logicalCombination = literalBooleanValue && semanticBooleanValue || !literalBooleanValue;
//
//// Comparison operators return boolean
//boolean comparisonResult = literalIntValue < literalDoubleValue && literalIntValue != 0;
//
//// Invalid binary operations
//boolean invalidLogicalOperands = literalIntValue && literalBooleanValue;
//double invalidPowerOperand = literalStringValue ** literalIntValue;
//
//// Unary operators
//int unaryMinusResult = -literalIntValue;
//boolean unaryNotResult = !literalBooleanValue;
//int invalidUnaryMinusOperand = -"text";
//boolean invalidUnaryNotOperand = !literalIntValue;
//
//// Automatic widening conversions (int -> float -> double)
//float widenedIntToFloat = semanticIntValue;
//double widenedFloatToDouble = semanticFloatValue;
//double widenedExpression = literalIntValue + literalDoubleValue;
//
//// Control flow with valid boolean condition
//boolean controlCondition = semanticBooleanValue;
//if (controlCondition) {
//    int thenScopeOnly = 1;
//} else {
//    int thenScopeOnly = 2;
//}
//
//// If condition must be boolean
//if (literalIntValue) {
//    literalIntValue = literalIntValue + 1;
//}
//
//// Scope leak check for then/else branches
//int scopeLeak = thenScopeOnly;
//
//// While conditions must also be boolean
//int loopGuard = 0;
//while (loopGuard < 2) {
//    loopGuard = loopGuard + 1;
//}
//
//while (literalIntValue) {
//    loopGuard = loopGuard + 1;
//}
//
//// Method checks: return type existence and parameter types
//double semanticAverage(double total, int count) {
//    return total / count;
//}
//
//int methodParameterCheck(String label, double value) {
//    return semanticIntValue;
//}
//
//// Undefined return type detection
//UndefinedType methodWithUnknownReturn() {
//    return null;
//}
//
//// Return expression must match declared type
//int invalidReturnMethod() {
//    return "text";
//}
//
//// Duplicate parameter names
//int duplicateParameterMethod(int arg, int arg) {
//    return arg;
//}
//
//// Parameter type existence
//int methodWithUnknownParameter(UndefinedType phantom) {
//    return 0;
//}
//
//// Method call validation: existence and inferred return type
//double averageCallOk = semanticAverage(10.0, 2);
//undefinedUtility(); // Error: method is undefined
//
//// ============================================================
//// Class checks and members
//// ============================================================
//
//// Class registration and field initializers
//class Address {
//    String street = "Unknown street";
//    int number = 0;
//}
//
//// Fields, methods, and parameter typing inside a class
//class Person {
//    Address address = new Address();
//    String name = "Sigma User";
//    int age = 30;
//
//    String describe() {
//        return name + " (" + age + ")";
//    }
//
//    int updateAge(int years) {
//        age = age + years;
//        return age;
//    }
//}
//
//// Duplicate members and invalid field initializers
//class DuplicateMembers {
//    int duplicateField = 1;
//    int duplicateField = 2;
//    double wrongFieldInit = "bad";
//
//    void ping() {}
//    void ping() {}
//}
//
//// Field and parameter type existence inside classes
//class ParameterTypeCheck {
//    UndefinedType phantomField;
//
//    int compute(UndefinedType phantomParam) {
//        return 0;
//    }
//}
//
//// ============================================================
//// Object creation, member access, and method calls
//// ============================================================
//
//Calculator semanticsCalc = new Calculator(); // Valid object creation
//Calculator configuredCalc = new Calculator(semanticIntValue); // Constructor argument typing
//int notAClassInstance = new int(5); // Error: cannot instantiate primitive
//UnknownClass ghostInstance = new UnknownClass(); // Error: class missing
//boolean invalidConstructorTarget = new boolean(true); // Error: primitive instantiation
//
//// Member method call checks
//double circleAreaValue = semanticsCalc.circleArea(3.0);
//semanticsCalc.circleArea(); // Error: wrong argument count
//semanticsCalc.circleArea("radius"); // Error: incompatible argument type
//semanticsCalc.undefinedMethod(); // Error: method not found on class
//semanticIntValue.circleArea(2.0); // Error: target is not a class
//
//// Member field access
//double piFieldCopy = semanticsCalc.pi;
//double missingFieldAccess = semanticsCalc.diameter;
//int invalidFieldTarget = semanticIntValue.pi;
//
//// Object creation for user classes and chained access
//Person person = new Person();
//Person invalidConstructorArgs = new Person("name", 25); // Error: unexpected constructor arguments
//Address addressInstance = new Address();
//
//// Field access validation (including chaining)
//String streetName = person.address.street;
//String invalidChainedAccess = person.address.zipCode;
//
//// Member method calls with argument validation
//String description = person.describe();
//person.describe("extra"); // Error: too many arguments
//int newAge = person.updateAge(2);
//person.updateAge("two"); // Error: wrong argument type
//person.updateAge(); // Error: missing argument
//
//// Member access when the class itself is undefined
//UndefinedClass undefinedHolder;
//double invalidFieldLookup = undefinedHolder.value;
