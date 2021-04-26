package nl.bioinf.minorapplicationdesign.ontpillen;

import nl.bioinf.minorapplicationdesign.ontpillen.model.web_scraping.WebScrapeExecutor;
import org.apache.juli.logging.Log;
import org.apache.logging.log4j.LogManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Naomi Hindriks
 */
@SpringBootApplication
public class OntpillenApplication {
    private static Logger LOGGER = LoggerFactory.getLogger(OntpillenApplication.class);

    public static void main(String[] args) throws IOException {
        LOGGER.info("Starting the application");
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyApplicationContextConfiguration.class);

        WebScrapeExecutor myWebScraper = ctx.getBean(WebScrapeExecutor.class);
        myWebScraper.runWebcrawlers();

        ctx.close();

        SpringApplication.run(OntpillenApplication.class, args);
    }

}
