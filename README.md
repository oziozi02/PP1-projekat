# PP1-projekat (just Version A!!!)
Compiler for a small, object-oriented language called Microjava(the specifications for the language can be found [here](mikrojava_2025_2026_jan.pdf)).<br/>
Details about the project can be found [here](pp1_2025_2026_jan.pdf).
This compiler has four phases:
1. [Lexer](pp1projekat/workspace/MJCompiler/spec/mjlexer.flex) written using JFlex
2. [Parser](pp1projekat/workspace/MJCompiler/spec/mjparser.cup) written using CUP
3. [Semantic analyzer](pp1projekat/workspace/MJCompiler/src/rs/ac/bg/etf/pp1/SemanticPass.java) which checks the semantic requirements in the project documentation.
4. [Code generator](pp1projekat/workspace/MJCompiler/src/rs/ac/bg/etf/pp1/CodeGenerator.java) which generates the bytecode
