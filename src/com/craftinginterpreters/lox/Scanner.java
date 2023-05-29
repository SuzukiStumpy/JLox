package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.craftinginterpreters.lox.TokenType.*;

class Scanner
{
    // The source code passed in to the scanner.
    private final String source;

    // The list of tokens that we have parsed.
    private final List<Token> tokens = new ArrayList<>();

    // Position of the scanner for the current token
    private int start = 0;
    private int current = 0;
    private int line = 1;

    // The reserved keywords for the Lox language
    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();
        keywords.put("and", AND);
        keywords.put("class", CLASS);
        keywords.put("else", ELSE);
        keywords.put("false", FALSE);
        keywords.put("for", FOR);
        keywords.put("fun", FUN);
        keywords.put("if", IF);
        keywords.put("nil", NIL);
        keywords.put("or", OR);
        keywords.put("print", PRINT);
        keywords.put("return", RETURN);
        keywords.put("super", SUPER);
        keywords.put("this", THIS);
        keywords.put("true", TRUE);
        keywords.put("var", VAR);
        keywords.put("while", WHILE);
        keywords.put("break", BREAK);
        keywords.put("continue", CONTINUE);
    }

    /**
     * Constructor for the Scanner class.
     *
     * @param source The source code we wish to scan and tokenize
     */
    Scanner(String source)
    {
        this.source = source;
    }

    /**
     * Scan the entire input for tokens.  Wrapper around the individual token
     * scan function.
     *
     * @return In-order List of all the tokens scanned.
     */
    List<Token> scanTokens()
    {
        while (!isAtEnd()) {
            // We are at the beginning of the next lexeme.
            start = current;
            scanToken();
        }

        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    /**
     * Helper function.  Allows us to determine if we're at the end of the input
     *
     * @return true if at the end of the input, false otherwise.
     */
    private boolean isAtEnd()
    {
        return current >= source.length();
    }

    /**
     * Scan an individual token from the current input.
     */
    private void scanToken()
    {
        char c = advance();

        switch (c) {
            case '(': addToken(LEFT_PAREN); break;
            case ')': addToken(RIGHT_PAREN); break;
            case '{': addToken(LEFT_BRACE); break;
            case '}': addToken(RIGHT_BRACE); break;
            case ',': addToken(COMMA); break;
            case '.': addToken(DOT); break;
            case '-': addToken(MINUS); break;
            case '+': addToken(PLUS); break;
            case ';': addToken(SEMICOLON); break;
            case '*': addToken(STAR); break;
            case '?': addToken(QUERY); break;
            case ':': addToken(COLON); break;
            case '!': addToken(match('=') ? BANG_EQUAL : BANG); break;
            case '=': addToken(match('=') ? EQUAL_EQUAL : EQUAL); break;
            case '<': addToken(match('=') ? LESS_EQUAL : LESS); break;
            case '>': addToken(match('=') ? GREATER_EQUAL : GREATER); break;
            case '/':
                if (match('/')) {
                    // A single line comment goes until the end of the line
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else if (match('*')) {
                    multiLineComment();
                } else {
                    addToken(SLASH);
                }
                break;

            case ' ':
            case '\r':
            case '\t':
                // Ignore all whitespace
                break;

            case '\n':
                line++;
                break;

            case '"':
                string();
                break;

            default:
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    Lox.error(line, "Unexpected character.");
                }
                break;
        }
    }

    /**
     * Helper function, returns the next character from the input source.
     *
     * @return The next character read from the input
     */
    private char advance()
    {
        return source.charAt(current++);
    }

    /**
     * Helper function.  Adds a new token to the Tokens list
     *
     * @param type The type (from the TokenType enum) of the token to add
     */
    private void addToken(TokenType type)
    {
        addToken(type, null);
    }

    /**
     * Helper function.  Adds a new token to the Tokens list.
     *
     * @param type    The type of token to add
     * @param literal Any literal text to add to the token
     */
    private void addToken(TokenType type, Object literal)
    {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }

    /**
     * Helper function.  Allows us to match two-character lexemes
     *
     * @param expected The second character expected from the input stream
     * @return true if the character matched, false otherwise.
     */
    private boolean match(char expected)
    {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;

        current++;
        return true;
    }

    /**
     * Looks ahead one character in the input
     *
     * @return the next character (without consuming it)
     */
    private char peek()
    {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    /**
     * Looks at the next but one character in the input
     *
     * @return the next+1 character (without consuming it)
     */
    private char peekNext()
    {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    /**
     * Parses out string literals
     */
    private void string()
    {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++;
            advance();
        }

        if (isAtEnd()) {
            Lox.error(line, "Unterminated string.");
            return;
        }

        advance();  // The closing "

        // Trim the surrounding quotes from the string
        String value = source.substring(start + 1, current - 1);
        addToken(STRING, value);
    }

    /**
     * Determine if the supplied character is a decimal digit
     *
     * @param c The character to test
     * @return true if c is in the range 0-9, false otherwise
     */
    private boolean isDigit(char c)
    {
        return c >= '0' && c <= '9';
    }

    /**
     * Determine if the supplied character is a valid alpha character
     *
     * @param c The character to test
     * @return true if c is in the range: a-z, A-Z or is _
     */
    private boolean isAlpha(char c)
    {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    /**
     * Determine if next character is alphanumeric or otherwise.
     *
     * @param c The character to test
     * @return true is c is either a digit or alpha.  False otherwise
     */
    private boolean isAlphanumeric(char c)
    {
        return (isAlpha(c) || isDigit(c));
    }

    /**
     * Parses out a numeric literal
     */
    private void number()
    {
        while (isDigit(peek())) advance();

        // Look for a fractional part
        if (peek() == '.' && isDigit(peekNext())) {
            advance(); // Consume the '.'

            while (isDigit(peek())) advance();
        }

        addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    /**
     * Looks through the input to round up identifiers (variables, reserved
     * words, etc).
     */
    private void identifier()
    {
        while (isAlphanumeric(peek())) advance();

        String text = source.substring(start, current);
        TokenType type = keywords.get(text);
        if (type == null) type = IDENTIFIER;
        addToken(type);
    }

    /**
     * Process a multi-line c-style comment.  Allows for nesting
     */
    private void multiLineComment()
    {
        int nestingLevel = 1;

        while (!isAtEnd()) {
            if (peek() == '/' && peekNext() == '*') {
                nestingLevel++;
                advance();
            } else if (peek() == '*' && peekNext() == '/') {
                nestingLevel--;
                advance();
                if (nestingLevel == 0) {
                    advance();
                    break;
                }
            }

            if (peek() == '\n') {
                line++;
            }
            advance();
        }
    }
}
