package com.craftinginterpreters.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {
    private static final Interpreter interpreter = new Interpreter();
    static boolean hadError = false;
    static boolean hadRuntimeError = false;

    /**
     * Main entry point for the Lox compiler/interpreter
     *
     * @param args Command-line argument (if any) passed in.  If passed, should
     *             be the path/name of a Lox source file to execute.  If no
     *             parameter passed, then code enters the REPL.  If more than
     *             one parameter is passed, prints a usage message and exits.
     * @throws IOException
     */
    public static void main(String[] args) throws IOException
    {
        if (args.length > 1) {
            System.out.println("Usage: jlox [script]");
            System.exit(64);
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    /**
     * Load and run a Lox program from a file
     *
     * @param path The full path name to the program file to load.
     * @throws IOException
     */
    private static void runFile(String path) throws IOException
    {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));

        // Indicate an error in the return code
        if (hadError) System.exit(65);
        if (hadRuntimeError) System.exit(70);
    }

    /**
     * The REPL for lox.  Accepts commands for immediate execution.  Exits the
     * REPL when a null input is detected (Ctrl-D)
     *
     * @throws IOException
     */
    private static void runPrompt() throws IOException
    {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        for (;;) {
            System.out.print("lox> ");
            String line = reader.readLine();
            if (line == null) break;
            run(line);
            hadError = false;  // Reset the error flag in REPL mode
            hadRuntimeError = false;
        }
    }

    /**
     * The main Lox core.  Actually compiles and executes the passed in code
     *
     * @param source The code we want to execute.
     */
    private static void run(String source)
    {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();

        // Stop if there was a syntax error
        if (hadError) return;

        // Perform static bindings for local variables
        Resolver resolver = new Resolver(interpreter);
        resolver.resolve(statements);

        // Stop if there was a resolution error
        if (hadError) return;

        // Fire up the interpreter with the parsed syntax tree.
        interpreter.interpret(statements);

        // Old code for printing out the syntax tree in the parser.
        //System.out.println(new AstPrinter().print(expression));

        /*  Old code (pre-parser)
        // For the time being, just print the tokens out to console
        for (Token token: tokens) {
            System.out.println(token);
        }
        */

    }

    /**
     * Emits an error message
     *
     * @param line The line number the error occurred at
     * @param message The error message
     */
    static void error(int line, String message)
    {
        report(line, "", message);
    }

    /**
     * Does the actual printing of the error message to the console
     *
     * @param line The line the error occurred at
     * @param where Whereabouts in the code the error occurred
     * @param message The actual error message
     */
    private static void report(int line, String where, String message)
    {
        System.err.println("\n[line "+ line +"] Error "+ where +": "+ message);
        hadError = true;
    }

    /**
     * Static function for reporting errors in the code when read through the
     * parser.
     * @param token The token which caused the error
     * @param message The error message to report
     */
    static void error(Token token, String message)
    {
        if (token.type == TokenType.EOF) {
            report(token.line, "at end", message);
        } else {
            report(token.line, "at '"+ token.lexeme +"'", message);
        }
    }

    /**
     * Static function for reporting runtime errors when read through the
     * interpreter.
     * @param error the error object that was thrown.
     */
    static void runtimeError(RuntimeError error)
    {
        System.err.println("\n"+ error.getMessage() + "\n [Line "+
            error.token.line + "]");
        hadRuntimeError = true;
    }
}
