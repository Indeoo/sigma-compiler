import org.sigma.*;

public class TestSimple {
    public static void main(String[] args) {
        SigmaCompiler compiler = new SigmaCompiler();

        // Simple test
        String code = "println(\"Hello, Sigma!\");";
        System.out.println("Compiling: " + code);

        CompilationResult result = compiler.compile(code);

        if (result.isSuccessful()) {
            System.out.println("Compilation successful! Class: " + result.getClassName());
            System.out.println("Bytecode length: " + result.getBytecode().length);
        } else {
            System.out.println("Compilation failed:");
            System.out.println(result.getAllMessagesAsString());
        }
    }
}