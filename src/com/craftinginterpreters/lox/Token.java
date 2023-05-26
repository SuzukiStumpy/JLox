package com.craftinginterpreters.lox;

class Token
{
    final TokenType type;
    final String lexeme;
    final Object literal;
    final int line;

    /**
     * Constructor for Token class.
     *
     * @param type Type of the token represented
     * @param lexeme The actual parsed string for this token
     * @param literal ??
     * @param line The line of source where this token occurred
     */
    Token(TokenType type, String lexeme, Object literal, int line)
    {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
    }

    /**
     * Override of the toString() method so we can print tokens to the console
     * @return String representation of the Token
     */
    public String toString()
    {
        return type +" "+ lexeme +" "+ literal;
    }
}
