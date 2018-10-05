package tga.aws.spring.parameterstore;

/**
 * Created by grigory@clearscale.net on 10/5/2018.
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
