package com.craftinginterpreters.lox;

import java.util.List;

/**
 * Exception class.  Thrown within the interpreter when a runtime error is
 * detected.
 */
class RuntimeError extends RuntimeException
{
    final Token token;

    RuntimeError(Token token, String message)
    {
        super(message);
        this.token = token;
    }
}

/**
 * Implementation of the Interpreter for executing the parsed syntax tree.
 * Contains methods for interpreting all the various syntax tree node types
 */
class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void>
{
    // Storage for program symbol tables/variables
    private final Environment environment = new Environment();
    /**
     * The public interface to the interpreter.  Ingests a list of statements
     * and executes each in sequence.
     * @param statements A list of statements to execute
     */
    void interpret(List<Stmt> statements)
    {
        try {
            for (Stmt statement : statements)
            {
                execute(statement);
            }
        }
        catch (RuntimeError error)
        {
            Lox.runtimeError(error);
        }
    }

    /**
     * Execute an individual statement by passing the interpreter to it's
     * accept method.
     * @param statement the statement to execute.
     */
    private void execute(Stmt statement)
    {
        statement.accept(this);
    }

    /**
     * Helper method.  Takes the result of interpreting an expresion and
     * converts it to a printable string for output.
     * @param object the result to parse
     * @return a string representation of the passed object
     */
    private String stringify(Object object)
    {
        if (object == null) return "nil";

        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }
        return object.toString();
    }

    /**
     * Simple evaluation of literal syntax objects.  Easy to evaluate since
     * the actual value of the literal was stored when we initially parsed
     * the input source text.
     * @param expr The literal object to evaluate
     * @return the value stored in the literal.
     */
    @Override
    public Object visitLiteralExpr(Expr.Literal expr)
    {
        return expr.value;
    }

    /**
     * Process a parenthesised expression.
     * @param expr the expression to process
     * @return the value of the evaluated expression
     */
    @Override
    public Object visitGroupingExpr(Expr.Grouping expr)
    {
        return evaluate(expr.expression);
    }

    /**
     * Process a unary expression.
     * @param expr the expression to process
     * @return the value of the evaluated expression
     */
    @Override
    public Object visitUnaryExpr(Expr.Unary expr)
    {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case BANG -> {
                return !isTruthy(right);
            }
            case MINUS -> {
                checkNumberOperand(expr.operator, right);
                return -(double) right;
            }
        }

        // Unreachable
        return null;
    }

    /**
     * Process a variable expression.  Look up the value from the symbol table
     * and parse it.
     * @param expr The expression to interpret
     * @return The value of the processed expression
     */
    @Override
    public Object visitVariableExpr(Expr.Variable expr)
    {
        return environment.get(expr.name);
    }

    /**
     * Processes variable (re)assignment expressions/statements.
     * @param expr The assignment expression t
     * @return the value assigned.
     */
    @Override
    public Object visitAssignExpr(Expr.Assign expr)
    {
        Object value = evaluate(expr.value);
        environment.assign(expr.name, value);
        return value;
    }

    /**
     * Evaluate a binary expression
     * @param expr The expression to evaluate
     * @return The resultant evaluation
     */
    @Override
    public Object visitBinaryExpr(Expr.Binary expr)
    {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case MINUS -> {
                checkNumberOperands(expr.operator, left, right);
                return (double) left - (double) right;
            }
            case PLUS -> {
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                }
                if (left instanceof String || right instanceof String) {
                    return stringify(left) + stringify(right);
                }

                // If we fail any of the above, throw a runtime error.
                throw new RuntimeError(expr.operator, "Operands must be two " + "numbers or a combination of numbers and strings.");
            }
            case SLASH -> {
                checkNumberOperands(expr.operator, left, right);
                if ((double) right == 0.0) {
                    throw new RuntimeError(expr.operator, "Division by zero.");
                }
                return (double) left / (double) right;
            }
            case STAR -> {
                checkNumberOperands(expr.operator, left, right);
                return (double) left * (double) right;
            }
            case GREATER -> {
                checkNumberOperands(expr.operator, left, right);
                return (double) left > (double) right;
            }
            case GREATER_EQUAL -> {
                checkNumberOperands(expr.operator, left, right);
                return (double) left >= (double) right;
            }
            case LESS -> {
                checkNumberOperands(expr.operator, left, right);
                return (double) left < (double) right;
            }
            case LESS_EQUAL -> {
                checkNumberOperands(expr.operator, left, right);
                return (double) left <= (double) right;
            }
            case BANG_EQUAL -> {
                return !isEqual(left, right);
            }
            case EQUAL_EQUAL -> {
                return isEqual(left, right);
            }
        }

        // Unreachable
        return null;
    }

    /**
     * Evaluate the ternary expression.  For this, we need to evaluate first
     * the truthiness of the test expression, then return either the first or
     * second subexpression evaluation.
     * @param expr The expression to evaluate
     * @return The value of the respective subexpression
     */
    public Object visitTernaryExpr(Expr.Ternary expr)
    {
        Object test = evaluate(expr.test);
        Object value;
        if (isTruthy(test)) {
            value = evaluate(expr.trueBranch);
        } else {
            value = evaluate(expr.falseBranch);
        }

        if (value instanceof String) {
            return (String)value;
        } else if (value instanceof Double) {
            return (double)value;
        }

        // Throw a runtime error if we get here...
        throw new RuntimeError(
            new Token(TokenType.QUERY, "?:", "ternary", 0),
            "Invalid expression detected."
        );
    }

    /**
     * Helper for evaluation of a passed in expression.  Simply passes the
     * expression back into the interpreter.
     * @param expr The expression to evaluate
     * @return The evaluated result
     */
    private Object evaluate(Expr expr)
    {
        return expr.accept(this);
    }

    /**
     * Helper function to determine whether an expression is truthy or not.
     * Boolean values take their respective true/false values as expected.
     * Nil values are falsey.  Everything else is truthy
     * @param object The expression to evaluate
     * @return boolean true if expression is truthy, false otherwise.
     */
    private boolean isTruthy(Object object)
    {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean)object;
        return true;
    }

    /**
     * Helper function to determine if two objects are equal.  If both are
     * null, then they're classed as equal, otherwise, if the first is null,
     * automatically returns false (so we don't get NullPointerExceptions in
     * our interpreter)
     * @param a the first object to check
     * @param b the second object to check
     * @return true if the objects are equal, false otherwise.
     */
    private boolean isEqual(Object a, Object b)
    {
        if (a == null && b == null) return true;
        if (a == null) return false;

        return a.equals(b);
    }

    /**
     * Tests whether an operand is numeric or not.  Throws a runtime error if
     * not
     * @param operator The operator we are working with (passed to the error)
     * @param operand The operand we are testing.
     */
    private void checkNumberOperand(Token operator, Object operand)
    {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    /**
     * Tests whether operands are numeric.  Used within binary expressions.
     * @see #checkNumberOperand(Token, Object)
     * @param operator The token in the expression being evaluated
     * @param left The left hand operand
     * @param right The right hand operand
     */
    private void checkNumberOperands(Token operator, Object left,
                                     Object right)
    {
        if (left instanceof Double && right instanceof Double) {
            return;
        }
        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    //=============== Statement processors ======================

    /**
     * Interprets an expression statement.
     * @param stmt the statement to interpret
     */
    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt)
    {
        evaluate(stmt.expression);
        return null;
    }

    /**
     * Interprets a print statement.
     * @param stmt the print statement to interpret
     */
    @Override
    public Void visitPrintStmt(Stmt.Print stmt)
    {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    /**
     * Interpret variable declarations
     * @param stmt the statement to interpret
     */
    @Override
    public Void visitVarStmt(Stmt.Var stmt)
    {
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }
        environment.define(stmt.name.lexeme, value);
        return null;
    }

}
