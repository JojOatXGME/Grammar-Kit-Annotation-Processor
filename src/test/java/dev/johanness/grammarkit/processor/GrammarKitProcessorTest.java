package dev.johanness.grammarkit.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.CompilationSubject;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.JavaFileObject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SuppressWarnings("BnfResolve")
final class GrammarKitProcessorTest {
  private Path tempDir;

  @BeforeEach
  void setUp(@TempDir Path tempDir) {
    this.tempDir = tempDir;
  }

  @Test
  void basics() throws IOException {
    JavaFileObject elementTypeClass = createElementTypeClass("org.example.lang1.Lang1ElementType");
    JavaFileObject tokenTypeClass = createElementTypeClass("org.example.lang1.Lang1TokenType");
    Path bnfFile = createBnfFile(
        "lang1.bnf", """
            {
              parserClass="org.example.Lang1Parser"
              extends="com.intellij.extapi.psi.ASTWrapperPsiElement"
              psiClassPrefix="Lang1"
              psiImplClassSuffix="Impl"
              psiPackage="org.example.lang1"
              psiImplPackage="org.example.lang1.impl"
              elementTypeHolderClass="org.example.lang1.Lang1Types"
              elementTypeClass="org.example.lang1.Lang1ElementType"
              tokenTypeClass="org.example.lang1.Lang1TokenType"
              tokens = [ DOT='.']
            }

            lang1File ::= root
            root ::= "."+
            """);

    Compilation compilation = Compiler.javac()
        .withProcessors(new GrammarKitProcessor())
        .withOptions("-Aparser=" + bnfFile, "-source" , "11")
        .compile(elementTypeClass, tokenTypeClass);

    CompilationSubject.assertThat(compilation)
        .succeededWithoutWarnings();
    CompilationSubject.assertThat(compilation)
        .generatedSourceFile("org.example.Lang1Parser");
    CompilationSubject.assertThat(compilation)
        .generatedSourceFile("org.example.lang1.Lang1Types");
    CompilationSubject.assertThat(compilation)
        .generatedSourceFile("org.example.lang1.Lang1Visitor");
    CompilationSubject.assertThat(compilation)
        .generatedSourceFile("org.example.lang1.Lang1Root");
    CompilationSubject.assertThat(compilation)
        .generatedSourceFile("org.example.lang1.impl.Lang1RootImpl");
  }

  @Test
  void usageOfPsiImplUtilClass() throws IOException {
    JavaFileObject elementTypeClass = createElementTypeClass("org.example.lang1.Lang1ElementType");
    JavaFileObject tokenTypeClass = createElementTypeClass("org.example.lang1.Lang1TokenType");

    Path bnfFile = createBnfFile(
        "lang1.bnf", """
            {
              parserClass="org.example.Lang1Parser"
              extends="com.intellij.extapi.psi.ASTWrapperPsiElement"
              psiClassPrefix="Lang1"
              psiImplClassSuffix="Impl"
              psiPackage="org.example.lang1"
              psiImplPackage="org.example.lang1.impl"
              psiImplUtilClass="org.example.lang1.impl.Lang1ImplUtilClass"
              elementTypeHolderClass="org.example.lang1.Lang1Types"
              elementTypeClass="org.example.lang1.Lang1ElementType"
              tokenTypeClass="org.example.lang1.Lang1TokenType"
              tokens = [ DOT='.']
            }
            lang1File ::= root
            root ::= "."+ { methods=[ firstRandomMethod secondRandomMethod ]}
            """);

    JavaFileObject psiImplUtilClass = JavaFileObjects.forSourceString(
        "org.example.lang1.impl.Lang1ImplUtilClass", """
            package org.example.lang1.impl;
            import java.time.LocalDate;
            import java.util.List;
            import org.example.lang1.Lang1Root;
            import org.jetbrains.annotations.NotNull;
            final class Lang1ImplUtilClass {
              static void firstRandomMethod(Lang1Root node) {
              }
              static int secondRandomMethod(Lang1RootImpl node, @NotNull List<LocalDate> someArgument) {
                return 42;
              }
            }
            """);

    Compilation compilation = Compiler.javac()
        .withProcessors(new GrammarKitProcessor())
        .withOptions("-Aparser=" + bnfFile, "-source" , "11")
        .compile(elementTypeClass, tokenTypeClass, psiImplUtilClass);

    CompilationSubject.assertThat(compilation)
        .succeededWithoutWarnings();
    CompilationSubject.assertThat(compilation)
        .generatedSourceFile("org.example.lang1.Lang1Root")
        .containsElementsIn(JavaFileObjects.forSourceString(
            "", """
                package org.example.lang1;
                public interface Lang1Root extends PsiElement {
                  void firstRandomMethod();
                  int secondRandomMethod(@NotNull List<LocalDate> someArgument);
                }
                """));
    CompilationSubject.assertThat(compilation)
        .generatedSourceFile("org.example.lang1.impl.Lang1RootImpl")
        .containsElementsIn(JavaFileObjects.forSourceString(
            "", """
                package org.example.lang1.impl;
                public class Lang1RootImpl extends ASTWrapperPsiElement implements Lang1Root {
                  @Override
                  public void firstRandomMethod() {
                    Lang1ImplUtilClass.firstRandomMethod(this);
                  }
                  @Override
                  public int secondRandomMethod(@NotNull List<LocalDate> someArgument) {
                    return Lang1ImplUtilClass.secondRandomMethod(this, someArgument);
                  }
                }
                """));
  }

  @Test
  void multipleInputs() throws IOException {
    JavaFileObject elementTypeClass = createElementTypeClass("org.example.lang.LangElementType");
    JavaFileObject tokenTypeClass = createElementTypeClass("org.example.lang.LangTokenType");
    Path bnfFile1 = createBnfFile(
        "lang1.bnf", """
            {
              parserClass="org.example.Lang1Parser"
              extends="com.intellij.extapi.psi.ASTWrapperPsiElement"
              psiClassPrefix="Lang1"
              psiImplClassSuffix="Impl"
              psiPackage="org.example.lang"
              psiImplPackage="org.example.lang.impl"
              elementTypeHolderClass="org.example.lang.Lang1Types"
              elementTypeClass="org.example.lang.LangElementType"
              tokenTypeClass="org.example.lang.LangTokenType"
              tokens = [ DOT='.']
            }

            lang1File ::= root
            root ::= "."+
            """);
    Path bnfFile2 = createBnfFile(
        "lang2.bnf", """
            {
              parserClass="org.example.Lang2Parser"
              extends="com.intellij.extapi.psi.ASTWrapperPsiElement"
              psiClassPrefix="Lang2"
              psiImplClassSuffix="Impl"
              psiPackage="org.example.lang"
              psiImplPackage="org.example.lang.impl"
              elementTypeHolderClass="org.example.lang.Lang2Types"
              elementTypeClass="org.example.lang.LangElementType"
              tokenTypeClass="org.example.lang.LangTokenType"
              tokens = [ DOT='.']
            }

            lang1File ::= root
            root ::= "."+
            """);

    Compilation compilation = Compiler.javac()
        .withProcessors(new GrammarKitProcessor())
        .withOptions("-Aparser=" + bnfFile1 + File.pathSeparator + bnfFile2, "-source" , "11")
        .compile(elementTypeClass, tokenTypeClass);

    CompilationSubject.assertThat(compilation)
        .succeededWithoutWarnings();
    CompilationSubject.assertThat(compilation)
        .generatedSourceFile("org.example.Lang1Parser");
    CompilationSubject.assertThat(compilation)
        .generatedSourceFile("org.example.Lang2Parser");
  }

  @Test
  @Disabled("Fails because Lang1RootImpl misses the import for Lang1Parser")
  void ambiguousType() throws IOException {
    // Tests the usage of parameter types which are generated by an annotation
    // processor and are also imported using a wildcard.
    JavaFileObject elementTypeClass = createElementTypeClass("org.example.lang1.Lang1ElementType");
    JavaFileObject tokenTypeClass = createElementTypeClass("org.example.lang1.Lang1TokenType");

    Path bnfFile = createBnfFile(
        "lang1.bnf", """
            {
              parserClass="org.example.Lang1Parser"
              extends="com.intellij.extapi.psi.ASTWrapperPsiElement"
              psiClassPrefix="Lang1"
              psiImplClassSuffix="Impl"
              psiPackage="org.example.lang1"
              psiImplPackage="org.example.lang1.impl"
              psiImplUtilClass="org.example.lang1.impl.Lang1ImplUtilClass"
              elementTypeHolderClass="org.example.lang1.Lang1Types"
              elementTypeClass="org.example.lang1.Lang1ElementType"
              tokenTypeClass="org.example.lang1.Lang1TokenType"
              tokens = [ DOT='.']
            }
            lang1File ::= root
            root ::= child+ { methods=[ randomMethod ]}
            child ::= "."
            """);

    JavaFileObject psiImplUtilClass = JavaFileObjects.forSourceString(
        "org.example.lang1.impl.Lang1ImplUtilClass", """
            package org.example.lang1.impl;
            import org.example.*; // Use wildcard to make resolution ambiguous
            import org.example.lang1.Lang1Root;
            final class Lang1ImplUtilClass {
              static void randomMethod(Lang1Root node, Lang1Parser ambiguousParameter) {
              }
            }
            """);

    Compilation compilation = Compiler.javac()
        .withProcessors(new GrammarKitProcessor())
        .withOptions("-Aparser=" + bnfFile, "-source" , "11")
        .compile(elementTypeClass, tokenTypeClass, psiImplUtilClass);

    CompilationSubject.assertThat(compilation)
        .succeededWithoutWarnings();
    CompilationSubject.assertThat(compilation)
        .generatedSourceFile("org.example.lang1.Lang1Root")
        .containsElementsIn(JavaFileObjects.forSourceString(
            "", """
                package org.example.lang1;
                public interface Lang1Root extends PsiElement {
                  void randomMethod(Lang1Parser ambiguousParameter);
                }
                """));
    CompilationSubject.assertThat(compilation)
        .generatedSourceFile("org.example.lang1.impl.Lang1RootImpl")
        .containsElementsIn(JavaFileObjects.forSourceString(
            "", """
                package org.example.lang1.impl;
                public class Lang1RootImpl extends ASTWrapperPsiElement implements Lang1Root {
                  @Override
                  public void randomMethod(Lang1Parser ambiguousParameter) {
                    Lang1ImplUtilClass.randomMethod(this, ambiguousParameter);
                  }
                }
                """));
  }

  private JavaFileObject createElementTypeClass(String fullName) {
    int lastDot = fullName.lastIndexOf('.');
    String pkg = fullName.substring(0, lastDot);
    String simpleName = fullName.substring(lastDot + 1);

    //@Language("JAVA")
    String source = """
        package %1$s;

        import com.intellij.psi.tree.IElementType;
        import org.jetbrains.annotations.NonNls;
        import org.jetbrains.annotations.NotNull;

        public class %2$s extends IElementType {
            public %2$s(@NotNull @NonNls String debugName) {
                super(debugName, null);
            }
        }
        """.formatted(pkg, simpleName);

    return JavaFileObjects.forSourceString(fullName, source);
  }

  private Path createBnfFile(String name, @Language("BNF") String content) throws IOException {
    Path path = tempDir.resolve(name);
    Files.writeString(path, content);
    return path;
  }
}
