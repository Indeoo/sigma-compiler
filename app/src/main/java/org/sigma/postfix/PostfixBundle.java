package org.sigma.postfix;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Container holding the main Postfix program and auxiliary function modules.
 */
public class PostfixBundle {
    private final PostfixProgram mainProgram;
    private final Map<String, PostfixProgram> functionPrograms;
    private final List<PostfixFunction> functions;

    public PostfixBundle(PostfixProgram mainProgram,
                         Map<String, PostfixProgram> functionPrograms,
                         List<PostfixFunction> functions) {
        this.mainProgram = mainProgram;
        this.functionPrograms = new LinkedHashMap<>(functionPrograms);
        this.functions = List.copyOf(functions);
    }

    public PostfixProgram getMainProgram() {
        return mainProgram;
    }

    public Map<String, PostfixProgram> getFunctionPrograms() {
        return functionPrograms;
    }

    public List<PostfixFunction> getFunctions() {
        return functions;
    }
}
