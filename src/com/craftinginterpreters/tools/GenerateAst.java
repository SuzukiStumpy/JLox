package com.craftinginterpreters.tools;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class GenerateAst
{
    /**
     * Main entrypoint for the AST Generator.
     * @param args Expects a single parameter: The path for the output
     *             class to be saved in.  (Note filename is automatically
     *             set).
     * @throws IOException
     */
    public static void main(String[] args) throws IOException
    {
        if (args.length != 1) {
            System.out.println("Usage: GenerateAst <output_directory>");
            System.exit(64);
        }
        String outputDir = args[0];

        defineAst(outputDir, "Expr", Arrays.asList(
            "Assign   : Token name, Expr value",
            "Binary   : Expr left, Token operator, Expr right",
            "Call     : Expr callee, Token paren, List<Expr> arguments",
            "Get      : Expr object, Token name",
            "Set      : Expr object, Token name, Expr value",
            "This     : Token keyword",
            "Grouping : Expr expression",
            "Literal  : Object value",
            "Unary    : Token operator, Expr right",
            "Logical  : Expr left, Token operator, Expr right",
            "Ternary  : Expr test, Expr trueBranch, Expr falseBranch",
            "Variable : Token name"
        ));

        defineAst(outputDir, "Stmt", Arrays.asList(
            "Block      : List<Stmt> statements",
            "Class      : Token name, List<Stmt.Function> methods, List<Stmt" +
                ".Function> classMethods",
            "Expression : Expr expression",
            "Function   : Token name, List<Token> params, List<Stmt> body",
            "Var        : Token name, Expr initializer",
            "Print      : Expr expression",
            "If         : Expr condition, Stmt thenBranch, Stmt elseBranch",
            "While      : Expr condition, Stmt body",
            "Break      : ",
            "Continue   : ",
            "Return     : Token keyword, Expr value"
        ));
    }

    /**
     * Generate and write the Java class file.
     * @param outputDir The directory to save the file in
     * @param baseName Base name of the Class (and Java file)
     * @param types List of the subclass definitions to add to the file
     * @throws IOException
     */
    @SuppressWarnings("SpellCheckingInspection")
    private static void defineAst(
        String outputDir, String baseName, List<String> types)
        throws IOException
    {
        String path = outputDir +"/"+ baseName +".java";
        PrintWriter writer = new PrintWriter(path, StandardCharsets.UTF_8);

        writer.println("package com.craftinginterpreters.lox;");
        writer.println();
        writer.println("import java.util.List;");
        writer.println();
        writer.println("abstract class "+ baseName +"{");

        defineVisitor(writer, baseName, types);

        // The AST classes
        for (String type: types) {
            String className = type.split(":")[0].trim();
            String fields = type.split(":")[1].trim();
            defineType(writer, baseName, className, fields);
        }

        // The base accept() method
        writer.println();
        writer.println("  abstract <R> R accept(Visitor<R> visitor);");

        writer.println("}");
        writer.close();
    }

    /**
     * Write out the concrete classes for each subtype
     * @param writer The output stream
     * @param baseName Base name of the Abstract parent class
     * @param className Name of the class we are defining
     * @param fieldList List of the fields to add to the class
     */
    private static void defineType(
        PrintWriter writer, String baseName,
        String className, String fieldList)
    {
        writer.println("  static class "+ className +" extends "+ baseName +"{");

        // Store parameters in fields
        String[] fields;
        if (fieldList.isEmpty()) {
            fields = new String[0];
        } else {
            fields = fieldList.split(", ");
        }

        // Fields
        for (String field: fields) {
            writer.println("    final "+ field +";");
        }
        writer.println();

        // Constructor
        writer.println("    "+ className + "("+ fieldList +") {");

        for (String field: fields) {
            String name = field.split(" ")[1];
            writer.println("      this."+ name +" = "+ name +";");
        }
        writer.println("    }");

        // Visitor pattern
        writer.println();
        writer.println("    @Override");
        writer.println("    <R> R accept(Visitor<R> visitor) {");
        writer.println("      return visitor.visit"+ className +
            baseName + "(this);");
        writer.println("    }");

        writer.println("  }");
    }

    /**
     * Write out the definition for the generic Visitor interface
     * @param writer The output stream
     * @param baseName Base name of the Abstract class
     * @param types List of types to add to the interface
     */
    private static void defineVisitor(
        PrintWriter writer, String baseName, List<String> types)
    {
        writer.println("  interface Visitor<R> {");

        for (String type: types) {
            String typeName = type.split(":")[0].trim();
            writer.println("    R visit"+ typeName + baseName +"("+
                typeName +" "+ baseName.toLowerCase() +");");
        }

        writer.println("    }");
    }
}
