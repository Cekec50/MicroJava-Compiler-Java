# MJ Language Compiler

A comprehensive compiler for the "MJ" programming language, built as a project for a Program Translators course. This compiler processes MJ source code through a full pipeline including lexical analysis, parsing, semantic analysis, and code generation, ultimately producing executable code for a custom virtual machine.

## Tech Stack

*   **Language:** Java
*   **Build Tool:** Apache Ant
*   **Lexical Analysis:** JFlex
*   **Parser Generator:** CUP (Compiler utility for Java)
*   **Runtime Environment:** A custom MJ-Runtime (`mj-runtime-1.1.jar`) is used to execute the compiled object code.
*   **Dependencies:**
    *   Log4j for logging
    *   A custom Symbol Table library

## Features

*   **Lexical Analysis:** Scans MJ source files and converts them into a stream of tokens according to the grammar defined in `spec/mjlexer.flex`.
*   **Syntactic Analysis:** Parses tokens to build an Abstract Syntax Tree (AST) based on the language grammar specified in `spec/mjparser.cup`.
*   **Semantic Analysis:** Traverses the AST to enforce semantic rules, such as type checking and variable declaration checks.
*   **Code Generation:** Translates the validated AST into bytecode, saved as `.obj` files, which can be executed by the MJ runtime environment.

## Getting Started

Follow these instructions to get the compiler up and running on your local machine.

### Prerequisites

*   **Java Development Kit (JDK):** Version 8 or higher.
*   **Apache Ant:** Must be installed and configured in your system's PATH.

You can verify your installations by running:
```sh
java -version
ant -version
```

### Build

To build the compiler, run the default Ant target. This command will generate the lexer and parser from the specification files and then compile all the Java source code.

```sh
ant
```
The compiled classes will be located in the `src` directory alongside the `.java` source files, as per the project's structure.

## Project Structure

The repository is organized as follows:

```
├── build.xml               # The Apache Ant build script
├── config/                 # Configuration files (e.g., for Log4j)
├── lib/                    # All required .jar dependencies
├── logs/                   # Output log files
├── spec/                   # JFlex and CUP grammar specifications
│   ├── mjlexer.flex
│   └── mjparser.cup
├── src/                    # Java source code for the compiler
│   └── rs/ac/bg/etf/pp1/
├── test/                   # Sample MJ program files (.mj and .obj)
└── README.md
```

## Usage

The primary way to use the compiler is through the test classes. To compile a program, you would typically run a test class like `MJParserTest.java`.

To run a compiled MJ program (`.obj` file), you can use the `runObj` Ant target. The default program to run is `test/program.obj`.

```sh
ant runObj
```

This command will first disassemble the object file for inspection and then execute it using the MJ runtime, with `input.txt` provided as standard input.
