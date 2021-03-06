package org.javacs;

import com.sun.source.tree.*;
import com.sun.source.util.*;
import java.util.*;
import javax.lang.model.element.*;

class WarnUnused extends TreePathScanner<Void, Void> {
    private final Trees trees;
    // TODO ignore writes when calculating used
    private final Set<Element> declared = new HashSet<>(), used = new HashSet<>();

    WarnUnused(JavacTask task) {
        this.trees = Trees.instance(task);
    }

    Set<Element> notUsed() {
        declared.removeAll(used);
        return declared;
    }

    Element current() {
        return trees.getElement(getCurrentPath());
    }

    boolean isPrivate(VariableTree t) {
        return t.getModifiers().getFlags().contains(Modifier.PRIVATE);
    }

    boolean isPrivate(MethodTree t) {
        return t.getModifiers().getFlags().contains(Modifier.PRIVATE);
    }

    boolean isPrivate(ClassTree t) {
        return t.getModifiers().getFlags().contains(Modifier.PRIVATE);
    }

    boolean isLocal(VariableTree t) {
        var parent = getCurrentPath().getParentPath().getLeaf();
        return !(parent instanceof ClassTree)
                && !(parent instanceof MethodTree) // TODO hint for unused parameters
                && !(parent instanceof LambdaExpressionTree);
    }

    boolean isEmptyConstructor(MethodTree t) {
        return t.getParameters().isEmpty() && t.getReturnType() == null;
    }

    @Override
    public Void visitVariable(VariableTree t, Void __) {
        if (isPrivate(t) || isLocal(t)) {
            declared.add(current());
        }
        return super.visitVariable(t, null);
    }

    @Override
    public Void visitMethod(MethodTree t, Void __) {
        if (isPrivate(t) && !isEmptyConstructor(t)) {
            declared.add(current());
        }
        return super.visitMethod(t, null);
    }

    @Override
    public Void visitClass(ClassTree t, Void __) {
        if (isPrivate(t)) {
            declared.add(current());
        }
        return super.visitClass(t, null);
    }

    @Override
    public Void visitIdentifier(IdentifierTree t, Void __) {
        used.add(current());
        return super.visitIdentifier(t, null);
    }

    @Override
    public Void visitMemberSelect(MemberSelectTree t, Void __) {
        used.add(current());
        return super.visitMemberSelect(t, null);
    }

    @Override
    public Void visitMemberReference(MemberReferenceTree t, Void __) {
        used.add(current());
        return super.visitMemberReference(t, null);
    }

    @Override
    public Void visitNewClass(NewClassTree t, Void __) {
        used.add(current());
        return super.visitNewClass(t, null);
    }
}
