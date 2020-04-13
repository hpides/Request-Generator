package de.hpi.tdgt;

import de.hpi.tdgt.controllers.UploadController;
import de.hpi.tdgt.test.story.atom.Data_Generation;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Set;

@Log4j2
@SpringBootApplication
public class WebApplication {
    //this allows the application to be called from all origins
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**").allowedOrigins("*");
            }
        };
    }

    public static void main(String[] args) throws URISyntaxException, IOException, InterruptedException {

        if (args.length < 2) {
            log.error("Usage: java -jar " + new java.io.File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getName() + "[cli] <Path to PDGF dir> <Java 7 interpreter path>");
            System.exit(1);
        }
        if (args[0].equals("cli")) {
            //If we do not start Spring Boot, many properties like logging get half-way initialized. So we start Spring boot and terminate it when the test is done.
            ConfigurableApplicationContext ctx = new SpringApplicationBuilder(WebApplication.class)
                    .web(WebApplicationType.NONE).run();
            Main.main(args);
            int code = SpringApplication.exit(ctx, () -> {
                // return the error code
                return 0;
            });
            System.exit(code);
        } else {
            Data_Generation.outputDirectory = args[0] + File.separator + "output";
            UploadController.PDGF_DIR = args[0];
            UploadController.JAVA_7_DIR = args[1];
            SpringApplication.run(WebApplication.class, args);
        }
    }

}
