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
import java.nio.file.Files;
import java.util.Set;

@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public final class MyProcessor extends AbstractProcessor {
  private boolean fileCreated;

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    if (!fileCreated) {
      fileCreated = true;
      try (InputStream in = MyProcessor.class.getResourceAsStream("Generated_B.java");
           OutputStream out = processingEnv.getFiler().createSourceFile("myexample.Generated_B").openOutputStream()) {
        assert in != null;
        in.transferTo(out);
      }
      catch (IOException e) {
        processingEnv.getMessager().printMessage(
            Diagnostic.Kind.ERROR,
            e.toString());
      }
    }
    return false;
  }
}
