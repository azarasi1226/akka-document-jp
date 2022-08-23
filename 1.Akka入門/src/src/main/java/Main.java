import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

public class Main {
    public static void main(String[] args) {
        Logger logger = LoggerFactory.getLogger(Main.class);

        logger.info("{}",    "HOGE");
        logger.info("{} {}", "HOGE", "FUGA");
        logger.info("{} {}", "HOGE");
        logger.info("{}",    "HOGE", "FUGA");
        logger.info("\\{}",  "HOGE");
        logger.info("{ }",   "HOGE");
        System.out.println("aaa");
    }
}
