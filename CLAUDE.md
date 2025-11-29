# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Sigma is a compiler for a Groovy-like JVM language. The compiler pipeline transforms source code through multiple phases: Lexical Analysis → Parsing → Semantic Analysis → RPN IR Generation → (Future: JVM Bytecode).

**Key characteristics:**
- Source language: Groovy-style with top-level methods, variables, and statements
- Target: JVM bytecode (in progress)
- Pure Java 21 implementation using Gradle

## Build & Development Commands

### Building and Running

```bash
# Build the project
./gradlew build

# Run the compiler (processes app/src/main/resources/source.groovy by default)
./gradlew run

# Run with a specific source file
./gradlew run --args="path/to/file.groovy"
```

### Testing

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "org.sigma.ir.LocalVariableAllocatorTest"

# Run specific test method
./gradlew test --tests "org.sigma.parser.SigmaParserWrapperTest.testValidExpressions"

# Run tests with verbose output (already configured in build.gradle)
./gradlew test --info
```

### Useful Development Tasks

```bash
# Run grammar demo (if ANTLR components are enabled)
./gradlew runGrammarDemo

# Run recursive descent regression tests
./gradlew runRdRegression

# Run lexer demo to inspect tokenization
./gradlew runLexerDemo

# Run test parser
./gradlew runTestParser

# Run AST printer
./gradlew runAstPrinter

# Clean build artifacts
./gradlew clean
```

## Compiler Architecture

### Compilation Pipeline

```
Source Code (.groovy)
    ↓
[LEXER] SigmaLexerWrapper
    → List<SigmaToken>
    ↓
[PARSER] SigmaRecursiveDescentParser (Pure Java, no ANTLR)
    → Ast.CompilationUnit
    ↓
[SEMANTIC ANALYSIS] SemanticAnalyzer (Two-pass)
    → SemanticResult (SymbolTable, Type Info, Errors)
    ↓
[CODE GENERATION] RPNGenerator
    → RPNProgram (RPN Intermediate Representation)
    ↓
[FUTURE] JVM Bytecode Generator
    → .class files
```

### Key Packages

**`org.sigma.lexer`** - Tokenization
- `SigmaLexerWrapper`: Main lexer interface wrapping ANTLR lexer
- `SigmaToken`: Token representation with line/column info
- `TokenType`: Enumeration of all token types

**`org.sigma.parser`** - Parsing (Pure Recursive Descent)
- `SigmaParserWrapper`: High-level parser interface
- `RecursiveDescentParser`: Static entry point maintaining compatibility
- `org.sigma.parser.rd.SigmaRecursiveDescentParser`: Core parser implementation
- `org.sigma.parser.rd.ParserContext`: Manages parsing state, lookahead, backtracking
- `org.sigma.parser.rd.RuleCombinators`: Functional combinators for grammar rules
- **NO ANTLR DEPENDENCY** - Grammar defined in pure Java code

**`org.sigma.parser.Ast`** (or `org.sigma.syntax.parser.Ast`)
- AST node definitions for all language constructs
- `CompilationUnit`: Root AST node containing statements
- Statements: `ClassDeclaration`, `MethodDeclaration`, `VariableDeclaration`, `IfStatement`, `WhileStatement`, etc.
- Expressions: `Binary`, `Unary`, `Call`, `Identifier`, Literals, etc.

**`org.sigma.semantics`** - Semantic Analysis
- `SemanticAnalyzer`: Two-pass analyzer (declaration collection → type checking)
- `SymbolTable`: Manages scopes and symbol resolution
- `SigmaType`: Type system (PrimitiveType, ClassType, VoidType, NullType, ErrorType)
- `TypeRegistry`: Built-in types (int, double, float, boolean, String, void)
- `SemanticError`: Error representation with types and locations

**`org.sigma.ir`** - Intermediate Representation (RPN)
- `RPNGenerator`: Converts AST to RPN instruction stream
- `RPNProgram`: Collection of RPN instructions with metadata
- `RPNInstruction`: Individual instruction with opcode, operand, type, location
- `RPNOpcode`: 28 opcodes (stack operations, arithmetic, control flow, methods, objects)
- `LocalVariableAllocator`: **CRITICAL** - Maps variable names to JVM slot indices

**`org.sigma.CompilerApp`** - Main entry point

### Critical Design Patterns

#### 1. Parser: Pure Recursive Descent (No ANTLR Parse Tree)

The parser builds AST **directly** during parsing using functional combinators:
- Grammar rules defined as `GrammarRule<T>` instances
- Combinators: `choice()`, `optional()`, `zeroOrMore()`, `leftAssociative()`, `rightAssociative()`
- No intermediate CST/parse tree - tokens → AST in one pass
- See `PARSER_ARCHITECTURE.md` for complete details

**When modifying grammar:**
1. Add rule method returning `GrammarRule<AstNode>`
2. Use combinators to compose rule logic
3. Add to appropriate parent rule (e.g., `statement()` or `expression()`)
4. Operator precedence is expressed through rule hierarchy

#### 2. Semantic Analysis: Two-Pass

**Pass 1 (collectDeclarations):** Register all symbols in SymbolTable
- Classes, methods, global variables
- Builds scope hierarchy (GLOBAL → CLASS → METHOD → BLOCK)

**Pass 2 (checkSemantics):** Type checking and validation
- Expression type inference
- Return type validation
- Undefined variable detection
- Type compatibility checking

**Limitations:**
- Method calls: name lookup only, no parameter validation
- No overloading support
- Top-level code is in global scope (no implicit class wrapper yet)

#### 3. RPN IR: Stack-Based with Slot Allocation

**Recent Addition (Phase 1 Complete):**
- `LocalVariableAllocator` maps variable names → JVM slot indices
- Slot 0 reserved for `this` in instance methods
- double/long occupy 2 slots, others occupy 1
- `RPNInstruction` has `slotIndex` field for LOAD/STORE operations
- `RPNGenerator` creates allocator per method, assigns slots during generation

**Pattern for LOAD/STORE:**
```java
RPNInstruction loadInstr = RPNInstruction.load(varName, type, line, col);
if (currentAllocator != null && currentAllocator.hasVariable(varName)) {
    int slot = currentAllocator.getSlot(varName);
    loadInstr.setSlotIndex(slot);
}
emit(loadInstr);
```

### Planned Architecture (Not Yet Implemented)

**AST Transformation Pass** (Planned - see `.claude/plans/jazzy-seeking-wind.md`)
- Insert between Parser and SemanticAnalyzer
- Wrap top-level Groovy code into `Script` class for JVM compatibility
- Top-level methods → instance methods
- Top-level variables → instance fields
- Auto-generate `main()` method

**JVM Bytecode Generation** (Planned)
- Method descriptors (e.g., `(ILjava/lang/String;)V`)
- Max stack depth calculation
- Constant pool generation
- ASM library integration for .class file generation

## Important Files to Read First

1. **`CompilerApp.java`** - Understand the compilation pipeline
2. **`PARSER_ARCHITECTURE.md`** - Parser design and grammar modification guide
3. **`Ast.java`** - All AST node definitions
4. **`SemanticAnalyzer.java`** - How semantic analysis works
5. **`RPNGenerator.java`** - How IR is generated
6. **`LocalVariableAllocator.java`** - JVM slot allocation logic

## Common Patterns

### Adding a New Language Feature

1. **Lexer:** Add token type to `TokenType` enum
2. **Parser:**
   - Add AST node to `Ast.java`
   - Add grammar rule to `SigmaRecursiveDescentParser`
   - Integrate into parent rule (usually `statement()` or `expression()`)
3. **Semantic Analysis:** Add type checking logic to `SemanticAnalyzer`
4. **RPN Generation:** Add opcode to `RPNOpcode`, implement generation in `RPNGenerator`
5. **Tests:** Add unit tests for each phase

### Working with Scopes

```java
symbolTable.enterScope(Scope.ScopeType.METHOD);
try {
    // Analyze method body
} finally {
    symbolTable.exitScope();
}
```

### Type Checking Pattern

```java
SigmaType leftType = inferExpressionType(expr.left);
SigmaType rightType = inferExpressionType(expr.right);

if (!leftType.isCompatibleWith(rightType)) {
    errors.add(new SemanticError(
        SemanticError.SemanticErrorType.TYPE_MISMATCH,
        "Type mismatch: " + leftType + " and " + rightType,
        expr.line, expr.col
    ));
}
```

## Testing Philosophy

- **Lexer tests:** Token sequence validation for various constructs
- **Parser tests:** Valid/invalid syntax, AST structure verification
- **Semantic tests:** Type checking, error detection, symbol resolution
- **RPN tests:** Instruction generation, stack effects, slot allocation
- **Integration tests:** Full pipeline from source to IR

Test files use JUnit 5 with descriptive test names following pattern:
`test<Feature><Scenario>()`

## Known Limitations

1. **Method calls:** No parameter count/type validation (semantic analyzer limitation)
2. **No built-in functions:** `print()`, `println()` not defined - will fail semantic analysis
3. **Top-level code:** Currently parsed as-is, not wrapped in class (JVM incompatible)
4. **No arrays:** Array syntax not implemented
5. **No constructors:** Constructor syntax not yet supported
6. **No exceptions:** No try/catch/throw support
7. **Static vs instance:** No distinction in current implementation

## Project History Context

- Originally used ANTLR for parsing, migrated to pure recursive descent
- Semantic analysis does not currently track local variables in method scopes (known issue)
- Phase 1 of JVM bytecode generation (slot allocation) completed
- AST transformation for top-level code wrapper is planned but not implemented
