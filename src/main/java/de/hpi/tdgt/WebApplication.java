package de.hpi.tdgt;

import de.hpi.tdgt.test.story.atom.Data_Generation;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.net.URISyntaxException;

@Log4j2
@SpringBootApplication
public class WebApplication {
    public static void main(String[] args) throws URISyntaxException, IOException, InterruptedException {
        if(args.length < 1){
            log.error("Usage: java -jar "+new java.io.File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getName()+" <Path to generated Data>");
            System.exit(1);
        }
        if(args[0].equals("cli")){
            Main.main(args);
        }
        else {
            Data_Generation.outputDirectory = args[0];
            SpringApplication.run(WebApplication.class, args);
        }
    }
}
