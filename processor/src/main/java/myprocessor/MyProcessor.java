package myprocessor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public final class MyProcessor extends AbstractProcessor {
  private boolean fileCreated;

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    if (!fileCreated) {
      fileCreated = true;
      createSourceFile("generated.Generated_B", "Generated_B.java");
    }
    return false;
  }

  private void createSourceFile(String fullName, String sourceFile) {
    try (InputStream in = MyProcessor.class.getResourceAsStream(sourceFile);
         OutputStream out = processingEnv.getFiler().createSourceFile(fullName).openOutputStream()) {
      assert in != null;
      in.transferTo(out);
    }
    catch (IOException e) {
      processingEnv.getMessager().printMessage(
          Diagnostic.Kind.ERROR,
          e.toString());
    }
  }
}
