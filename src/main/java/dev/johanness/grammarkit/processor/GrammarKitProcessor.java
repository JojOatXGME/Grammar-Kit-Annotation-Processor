package dev.johanness.grammarkit.processor;

import com.intellij.core.CoreProjectEnvironment;
import com.intellij.lang.LanguageASTFactory;
import com.intellij.lang.LanguageBraceMatching;
import com.intellij.psi.PsiFile;
import org.intellij.grammar.BnfASTFactory;
import org.intellij.grammar.BnfBraceMatcher;
import org.intellij.grammar.BnfLanguage;
import org.intellij.grammar.BnfParserDefinition;
import org.intellij.grammar.LightPsi;
import org.intellij.grammar.generator.ParserGenerator;
import org.intellij.grammar.java.JavaHelper;
import org.intellij.grammar.psi.BnfFile;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@SupportedAnnotationTypes("*")
@SupportedOptions({GrammarKitProcessor.LEXER_OPTION, GrammarKitProcessor.PARSER_OPTION})
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public final class GrammarKitProcessor extends AbstractProcessor {
  static final String LEXER_OPTION = "lexer";
  static final String PARSER_OPTION = "parser";

  private final Queue<Path> lexers = new ArrayDeque<>();
  private final Queue<Path> parsers = new ArrayDeque<>();

  private BnfParserDefinition parserDefinition;

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    lexers.addAll(parseOption(LEXER_OPTION));
    parsers.addAll(parseOption(PARSER_OPTION));

    LightPsi.init();
    replaceJavaHelper(processingEnv);
    LightPsi.Init.addKeyedExtension(LanguageASTFactory.INSTANCE, BnfLanguage.INSTANCE, new BnfASTFactory(), null);
    LightPsi.Init.addKeyedExtension(LanguageBraceMatching.INSTANCE, BnfLanguage.INSTANCE, new BnfBraceMatcher(), null);

    parserDefinition = new BnfParserDefinition();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    while (!parsers.isEmpty()) {
      try (TempDir tempDir = new TempDir()) {
        Path source = Objects.requireNonNull(parsers.poll());
        PsiFile bnfFile = LightPsi.parseFile(source.toFile(), parserDefinition);
        if (bnfFile instanceof BnfFile) {
          new ParserGenerator(
              (BnfFile) bnfFile,
              source.getParent().toAbsolutePath().toString(),
              tempDir.getPath().toString(),
              ""
          ).generate();

          Files.walkFileTree(tempDir.getPath(), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
              String relative = tempDir.getPath().relativize(file).toString();
              String className = relative.replaceAll("\\.java$", "").replaceAll("[/\\\\]", ".");
              try (InputStream in = Files.newInputStream(file);
                   OutputStream out = processingEnv.getFiler().createSourceFile(className).openOutputStream()) {
                in.transferTo(out);
              }
              return FileVisitResult.CONTINUE;
            }
          });
        }
        else {
          processingEnv.getMessager().printMessage(
              Diagnostic.Kind.ERROR,
              "Invalid BNF file: " + source);
        }
      }
      catch (IOException e) {
        processingEnv.getMessager().printMessage(
            Diagnostic.Kind.ERROR,
            e.toString());
      }
    }
    return false;
  }

  private List<Path> parseOption(String optionName) {
    String value = processingEnv.getOptions().get(optionName);
    if (value == null) {
      return List.of();
    }
    else {
      return Arrays.stream(value.split(Pattern.quote(File.pathSeparator)))
          .filter(Predicate.not(String::isBlank))
          .map(Path::of)
          .collect(Collectors.toList());
    }
  }

  private static void replaceJavaHelper(ProcessingEnvironment processingEnv) {
    Object parsing = getField(LightPsi.class, null, "ourParsing");
    CoreProjectEnvironment proj = getField(parsing.getClass(), parsing, "proj");
    proj.getProject().getPicoContainer().unregisterComponent(JavaHelper.class.getName());
    proj.getProject().registerService(JavaHelper.class, new MyJavaHelper(processingEnv));
  }

  @SuppressWarnings("unchecked")
  private static <T> T getField(Class<?> clazz, Object instance, String name) {
    try {
      Field field = clazz.getDeclaredField(name);
      field.trySetAccessible();
      return (T) field.get(instance);
    }
    catch (IllegalAccessException | NoSuchFieldException e) {
      throw new IllegalStateException(e);
    }
  }
}
