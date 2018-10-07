package tga.aws.spring.parameterstore;

/**
 * The library starts too early (in a Spring Boot application components instantiation chain)
 * At the start moment a logger context (slf4j / logback) will not be initialized yet,
 * so we forced to log our messages in a standard output stream directly.
 *
 */
public class SystemOutLogger {

    public void info(String msg){ println("[INFO] ", msg); }
    public void warn(String msg){ println("[WARN] ", msg); }
    public void error(String msg){ println("[ERROR] ", msg); }

    private void println(String level, String msg){
        System.out.print(level);
        System.out.println(msg);
    }




}
