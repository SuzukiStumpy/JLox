package com.craftinginterpreters.lox;

import java.sql.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import static com.craftinginterpreters.lox.TokenType.*;

/**
 * Class to take a list of tokens from the scanned input and put these together
 * into a structure we can use to parse into a sequence of interpretable code
 * elements
 */
public class Parser
{
    // Sentinel class used to help synchronise the parser when we hit an
    // error condition
    private static class ParseError extends RuntimeException {}

    // The list of tokens we need to parse
    private final List<Token> tokens;

    // The index of the token we're currently parsing
    private int current = 0;

    // The current depth of a loop (for processing break/continue statements
    private int loopDepth = 0;

    /**
     * Constructor for the Parser class.  Simply stores the list of tokens
     * for later parsing.
     * @param tokens The list of tokens we wish to parse
     */
    Parser(List<Token> tokens)
    {
        this.tokens = tokens;
    }

    /**
     * Main method to start the parser once it has been instantiated.
     * Implements the 'program' rule from the Lox grammar.
     * @return A list of parsed statements which can be passed to the
     * interpreter
     * @see #expression()
     */
    List<Stmt> parse()
    {
        List<Stmt> statements = new ArrayList<>();

        while(!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }

    /**
     * Implements the 'declaration' rule from the Lox grammar.  If errors are
     * detected, attempts to resynchronise the interpreter to the next valid
     * statement.
     * @return the result of parsing the statement.
     */
    private Stmt declaration()
    {
        try {
            if (match(CLASS)) return classDeclaration();
            if (match(FUN)) return function("function");
            if (match(VAR)) return varDeclaration();
            return statement();
        }
        catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    /**
     * Implements the 'statement' rule from the Lox grammar, or passed
     * through to the expressionStatement executor.
     * @return The result of processing the statement.
     * @see #expression()
     */
    private Stmt statement()
    {
        if (match(PRINT)) return printStatement();
        if (match(LEFT_BRACE)) return new Stmt.Block(block());
        if (match(FOR)) return forStatement();
        if (match(IF)) return ifStatement();
        if (match(WHILE)) return whileStatement();
        if (match(BREAK)) return breakStatement();
        if (match(CONTINUE)) return continueStatement();
        if (match(RETURN)) return returnStatement();

        return expressionStatement();
    }


    /**
     * The next sequence of methods implements the following grammar:
     * <br>
     * <pre>
     *   program     -> declaration* EOF ;
     *   declaration -> classDecl | funDecl | varDecl | statement ;
     *   classDecl   -> "class" IDENTIFIER "{" function* "}" ;
     *   funDecl     -> "fun" function ;
     *   function    -> IDENTIFIER "(" parameters? ")" block ;
     *   parameters  -> IDENTIFIER ("," IDENTIFIER )* ;
     *   varDecl     -> "var" IDENTIFIER ( "=" expression )? ";" ;
     *   statement   -> exprStmt | forStmt | ifStmt | printStmt
     *                  | breakStmt | continueStmt | whileStmt | block
     *                  | returnStmt ;
     *   exprStmt    -> expression ";" ;
     *   forStmt     -> "for" "(" ( varDecl | exprStmt | ";" )
     *                  expression? ";" expression? ")" statement ;
     *   ifStmt      -> "if" "(" expression ")" statement
     *                  ( "else" statement )? ;
     *   printStmt   -> "print" expression ;
     *   whileStmt   -> "while" "(" expression ")" statement ;
     *   block       -> "{" declaration "}" ;
     *   returnStmt  -> "return" expression? ";" ;
     *   expression  -> comma ;
     *   comma       -> assignment ( "," expression )* ;
     *   assignment  -> ( call "." )? IDENTIFIER "=" assignment | ternary ;
     *   ternary     -> logic_or ( "?" expression ":" expression )* ;
     *   logic_or    -> logic_and ( "or" logic_and )* ;
     *   logic_and   -> equality ( "and" equality )* ;
     *   equality    -> comparison ( ("!=" | "==") comparison )* ;
     *   comparison  -> term ( (">" | ">=" | "<" | "<=") term )* ;
     *   term        -> factor ( ("-" | "+") factor )* ;
     *   factor      -> unary ( ("/" | "*") unary )* ;
     *   unary       -> ("!" | "-") unary | call ;
     *   call        -> primary ( "(" arguments? ")" | "." IDENTIFIER )* ;
     *   arguments   -> expression ( "," expression )* ;
     *   primary     -> NUMBER | STRING | "true" | "false" | "nil"
     *                  | "(" expression ")" | IDENTIFIER
     *               // ERROR PRODUCTIONS
     *                  | ("!=" | "==") equality
     *                  | (">" | ">=" | "<" | "<=") comparison
     *                  | ("+") term
     *                  | ("/" | "*") factor ;
     * </pre>
     * @return The syntax tree generated by the method call
     */
    private Expr expression()
    {
        return comma();
    }

    /**
     * @see #expression()
     */
    private Expr comma()
    {
        Expr expr = assignment();
        while (match(COMMA)) {
            Token operator = previous();
            Expr right = expression();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    /**
     * Implement variable assignment expressions.
     * @see #expression()
     * @return The evaluated expression
     */
    private Expr assignment()
    {
        Expr expr = ternary();

        if (match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            } else if (expr instanceof Expr.Get) {
                Expr.Get get = (Expr.Get)expr;
                return new Expr.Set(get.object, get.name, value);
            }

            error(equals, "Invalid assignment target.");
        }

        return expr;
    }

    /**
     * @see #expression()
     */
    private Expr ternary()
    {
        Expr expr = or();

        if (match(QUERY)) {
            Expr branch1 = expression();
            consume(COLON, "Expect ':' after first branch of ternary operator");
            Expr branch2 = expression();
            expr = new Expr.Ternary(expr, branch1, branch2);
        }
        return expr;
    }

    /**
     * @see #expression()
     */
    private Expr or()
    {
        Expr expr = and();

        while (match(OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    /**
     * @see #expression()
     */
    private Expr and()
    {
        Expr expr = equality();

        while (match(AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    /**
     * @see #expression()
     */
    private Expr equality()
    {
        Expr expr = comparison();
        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    /**
     * @see #expression()
     */
    private Expr comparison()
    {
        Expr expr = term();
        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    /**
     * @see #expression()
     */
    private Expr term()
    {
        Expr expr = factor();

        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    /**
     * @see #expression()
     */
    private Expr factor()
    {
        Expr expr = unary();

        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    /**
     * @see #expression()
     */
    private Expr unary()
    {
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return call();
    }

    /**
     * @see #expression()
     */
    private Expr call()
    {
        Expr expr = primary();

        while (true) {
            if (match(LEFT_PAREN)) {
                expr = finishCall(expr);
            } else if (match(DOT)) {
                Token name = consume(IDENTIFIER, "Expect property name after '.'.");
                expr = new Expr.Get(expr, name);
            } else {
                break;
            }
        }
        return expr;
    }

    /**
     * Helper function to process arguments to a function call
     * @param callee The function we're calling
     * @return The fully parsed function call.
     */
    private Expr finishCall(Expr callee)
    {
        List<Expr> arguments = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (arguments.size() >= 255) {
                    error(peek(), "Can't have more than 255 arguments.");
                }
                arguments.add(equality());
            } while (match(COMMA));
        }

        Token paren = consume(RIGHT_PAREN, "Expect ')' after arguments.");

        return new Expr.Call(callee, paren, arguments);
    }

    /**
     * @see #expression()
     */
    private Expr primary()
    {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(THIS)) return new Expr.This(previous());

        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
        }

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        // ERROR PRODUCTIONS
        if (match(BANG_EQUAL, EQUAL_EQUAL)) {
            error(previous(), "Missing left-hand operand.");
            equality();
            return null;
        }

        if (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            error(previous(), "Missing left-hand operand.");
            comparison();
            return null;
        }

        if (match(PLUS)) {
            error(previous(), "Missing left-hand operand.");
            term();
            return null;
        }

        if (match(SLASH, STAR)) {
            error(previous(), "Missing left-hand operand.");
            factor();
            return null;
        }

        // If we get here, then we have an unknown token so report an error
        throw error(peek(), "Expect expression.");
    }

    /**
     * Private helper function which checks the current token in the stream
     * for a match against the expected types.  If it is, consume the token
     * and return true.  Otherwise, leave the token alone and return false.
     * @param types The token types we expect to see
     * @return True if the token is one of the expected types, false
     * otherwise
     */
    private boolean match(TokenType... types) {
        for (TokenType type: types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    /**
     * Checks to see if the next token is of the expected type and consumes
     * it if so.  If not, then reports the syntax error back up the call-chain
     * @param type The type of token we expect to see
     * @param message The error to report if type does not match
     * @return the next token in the sequence
     */
    private Token consume(TokenType type, String message)
    {
        if (check(type)) return advance();

        throw error(peek(), message);
    }

    /**
     * Looks at the current token (but does not consume it) to see if it
     * matches the expected token type.
     * @param type The Token type we expect to find
     * @return True if the token matches the expected type, false otherwise.
     */
    private boolean check(TokenType type)
    {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    /**
     * Consume and return the current token.
     * @return The current token
     */
    private Token advance()
    {
        if (!isAtEnd()) current++;
        return previous();
    }

    /**
     * @return true if the current token is the End of File marker, false
     * otherwise.
     */
    private boolean isAtEnd()
    {
        return peek().type == EOF;
    }

    /**
     * Looks at (and returns) the current token, but does not consume it.
     * @return The current token.
     */
    private Token peek()
    {
        return tokens.get(current);
    }

    /**
     * @return Returns the token before the current one.
     */
    private Token previous()
    {
        return tokens.get(current - 1);
    }

    /**
     * Report a parsing (syntax) error.
     * @param token The token which gave rise to the error
     * @param message The error message to report
     * @return a ParseError object used to unwind the parser to a sensible state
     */
    private ParseError error(Token token, String message)
    {
        Lox.error(token, message);
        return new ParseError();
    }

    /**
     * When an error has been detected, attempts to unwind the stack to get
     * to a known good condition so we can continue parsing.  We do this by
     * discarding tokens until we hit a statement boundary at which point we
     * resume.
     */
    private void synchronize()
    {
        advance();
        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return;

            switch (peek().type) { // Fall through is intentional
                case CLASS, FOR, FUN, IF, PRINT, RETURN, VAR, WHILE -> {
                    return;
                }
            }

            advance();
        }
    }

    //=================== Statement processors =========================

    /**
     * Implementation of the PRINT statement
     * @return The parsed print statement.
     */
    private Stmt printStatement()
    {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    /**
     * Fall through position for statement processing.  Evaluate the
     * expression statement and return it up the chain...
     * @return The parsed expression
     */
    private Stmt expressionStatement()
    {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }

    /**
     * Define a new statement block/scope and return the enclosed statements
     * @return the list of statements parsed within the block
     */
    private List<Stmt> block()
    {
        List<Stmt> statements = new ArrayList<>();

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }

    /**
     * Parse the declaration (and possible initialisation) of a new variable
     * @return The parsed statement
     */
    private Stmt varDeclaration()
    {
        Token name = consume(IDENTIFIER, "Expect variable name.");

        Expr initializer = null;
        if (match(EQUAL)) {
            initializer = expression();
        }

        consume(SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Var(name, initializer);
    }

    /**
     * Parse out an if statement
     * @return the parsed control statement
     */
    private Stmt ifStatement()
    {
        consume(LEFT_PAREN, "Expect '(' after 'if'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after if condition.");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;

        if (match(ELSE)) {
            elseBranch = statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    /**
     * Parse out a while loop
     * @return the parsed while statement
     */
    private Stmt whileStatement()
    {
        consume(LEFT_PAREN, "Expect '(' after 'while'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after while condition.");

        try {
            loopDepth++;
            Stmt body = statement();

            return new Stmt.While(condition, body);
        }
        finally {
            loopDepth--;
        }
    }

    /**
     * Implementation of the for loop.  Repackages tokens to a while loop
     * internally.
     * @return The parsed loop
     */
    private Stmt forStatement()
    {
        consume(LEFT_PAREN, "Expect '(' after 'for'.");

        Stmt initializer;
        if (match(SEMICOLON)) {
            initializer = null;
        } else if (match(VAR)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }

        Expr condition = null;
        if (!check(SEMICOLON)) {
            condition = expression();
        }
        consume(SEMICOLON, "Expect ';' after loop condition.");

        Expr increment = null;
        if (!check(RIGHT_PAREN)) {
            increment = expression();
        }
        consume(RIGHT_PAREN, "Expect ')' after for clauses.");

        try {
            loopDepth++;

            Stmt body = statement();

            if (increment != null) {
                body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(increment)));
            }

            if (condition == null) condition = new Expr.Literal(true);
            body = new Stmt.While(condition, body);

            if (initializer != null) {
                body = new Stmt.Block(Arrays.asList(initializer, body));
            }

            return body;
        }
        finally {
            loopDepth--;
        }
    }

    /**
     * Code to parse out the break statement
     * @return The parsed statement
     */
    private Stmt breakStatement()
    {
        if (loopDepth == 0) {
            error(previous(), "Must be inside a loop to use 'break'.");
        }
        consume(SEMICOLON, "Expect ';' after break.");
        return new Stmt.Break();
    }

    /**
     * Code to parse out the continue statement
     * @return The parsed statement
     */
    private Stmt continueStatement()
    {
        if (loopDepth == 0) {
            error(previous(), "Must be inside a loop to use 'continue'.");
        }
        consume(SEMICOLON, "Expect ';' after continue.");
        return new Stmt.Continue();
    }

    /**
     * Parser for a user defined function.
     * @param kind String denoting the type of construct (function or method)
     * @return The parsed function object.
     */
    private Stmt.Function function(String kind) {
        Token name = consume(IDENTIFIER, "Expect "+ kind +" name.");
        consume(LEFT_PAREN, "Expect '(' after "+ kind +" name.");
        List<Token> parameters = new ArrayList<>();

        if (!check(RIGHT_PAREN)) {
            do {
                if (parameters.size() >= 255) {
                    error(peek(), "Can't have more than 255 parameters.");
                }
                parameters.add(consume(IDENTIFIER, "Expect parameter name."));
            } while (match(COMMA));
        }
        consume(RIGHT_PAREN, "Expect ')' after parameters.");
        consume(LEFT_BRACE, "Expect '{' before "+ kind +" body.");
        List<Stmt> body = block();
        return new Stmt.Function(name, parameters, body);
    }

    /**
     * Support for parsing return statements in functions
     * @return The parsed return (and any associated value)
     */
    private Stmt returnStatement() {
        Token keyword = previous();
        Expr value = null;
        if (!check(SEMICOLON)) {
            value = expression();
        }

        consume(SEMICOLON, "Expect ';' after return value.");
        return new Stmt.Return(keyword, value);
    }

    /**
     * Support for Classes within the language.
     * @return The parsed class
     */
    private Stmt classDeclaration() {
        Token name = consume(IDENTIFIER, "Expect class name.");

        List<Stmt.Function> methods = new ArrayList<>();
        List<Stmt.Function> classMethods = new ArrayList<>();

        consume(LEFT_BRACE, "Expect '{' before class body.");

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            boolean isClassMethod = match(CLASS);
            (isClassMethod ? classMethods : methods).add(function("method"));
        }

        consume(RIGHT_BRACE, "Expect '}' after class body.");

        return new Stmt.Class(name, methods, classMethods);
    }
}
