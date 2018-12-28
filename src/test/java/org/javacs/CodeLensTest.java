package org.javacs;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.ExecutionException;
import org.javacs.lsp.*;
import org.junit.Test;

public class CodeLensTest {

    private static final JavaLanguageServer server = LanguageServerFixture.getJavaLanguageServer();

    private List<? extends CodeLens> lenses(String file) {
        var uri = FindResource.uri(file);
        var params = new CodeLensParams(new TextDocumentIdentifier(uri.toString()));
        try {
            var lenses = server.getTextDocumentService().codeLens(params).get();
            var resolved = new ArrayList<CodeLens>();
            for (var lens : lenses) {
                if (lens.getCommand() == null) {
                    var gson = new Gson();
                    var data = lens.getData();
                    var dataJson = gson.toJsonTree(data);
                    lens.setData(dataJson);
                    lens = server.getTextDocumentService().resolveCodeLens(lens).get();
                }
                resolved.add(lens);
            }
            return resolved;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private List<String> commands(List<? extends CodeLens> lenses) {
        var commands = new ArrayList<String>();
        for (var lens : lenses) {
            var command = new StringJoiner(", ");
            for (var arg : lens.getCommand().getArguments()) {
                command.add(Objects.toString(arg));
            }
            commands.add(command.toString());
        }
        return commands;
    }

    private List<String> titles(List<? extends CodeLens> lenses) {
        var titles = new ArrayList<String>();
        for (var lens : lenses) {
            var line = lens.getRange().getStart().getLine() + 1;
            var title = lens.getCommand().getTitle();
            titles.add(line + ":" + title);
        }
        return titles;
    }

    @Test
    public void testMethods() {
        var lenses = lenses("/org/javacs/example/HasTest.java");
        assertThat(lenses, not(empty()));

        var commands = commands(lenses);
        assertThat(commands, hasItem(containsString("HasTest, null")));
        assertThat(commands, hasItem(containsString("HasTest, testMethod")));
        assertThat(commands, hasItem(containsString("HasTest, otherTestMethod")));
    }

    @Test
    public void constructorReferences() {
        var lenses = lenses("/org/javacs/example/ConstructorRefs.java");
        assertThat(lenses, not(empty()));

        var titles = titles(lenses);
        assertThat(titles, hasItem("4:2 references"));
        assertThat(titles, hasItem("6:2 references"));
    }

    @Test
    public void enumConstants() {
        var lenses = lenses("/org/javacs/example/DontShowEnumConstRefs.java");
        var titles = titles(lenses);
        assertThat(titles, not(hasItem("4:0 references")));
    }
}
