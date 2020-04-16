package de.hpi.tdgt;

import de.hpi.tdgt.controllers.UploadController;
import de.hpi.tdgt.test.story.atom.Data_Generation;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

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

    public static void main(String[] args) throws IOException, InterruptedException {
        ArgumentParser parser = ArgumentParsers.newFor("Request Generator").build()
                .description("Perform tests configured using the frontend.");
        parser.addArgument("PDGFDirectory")
                .metavar("P")
                .type(String.class)
                .help("Root directoy of the PDGF installation");
        parser.addArgument("JAVA7")
                .metavar("J")
                .type(String.class)
                .help("Filepath of a Java 7 JRE.");
        //the whole argument bundle is passed to Spring boot. So we do not have to evaluate this parameter. This is only so it is documented and not rejected as unknown param.
        parser.addArgument("--logging.level.root")
                .type(String.class)
                .help("Logging level. One of debug, info, warn, error, of. Defaults to info.");
        val group = parser.addMutuallyExclusiveGroup("cli").description("Run one test and terminate afterwards (default: Run as webserver)");
        group.addArgument("--load").type(String.class).dest("load").nargs("?").help("If set, one test is loaded from the filesystem.");
        group.addArgument("--restTest").type(String.class).setConst(true).setDefault(false).nargs("?").dest("restTest").help("If set, some static rquests are run.");
        try {
            Namespace res = parser.parseArgs(args);
            Data_Generation.outputDirectory = res.getString("PDGFDirectory") + File.separator + "output";
            UploadController.PDGF_DIR = res.getString("PDGFDirectory");
            UploadController.JAVA_7_DIR = res.getString("JAVA7");
            val load = res.getString("load");
            val restTest = res.getBoolean("restTest");

            if (load != null) {
                //If we do not start Spring Boot, many properties like logging get half-way initialized. So we start Spring boot and terminate it when the test is done.
                ConfigurableApplicationContext ctx = new SpringApplicationBuilder(WebApplication.class)
                        .web(WebApplicationType.NONE).run();
                CLI.loadTest(load);
                int code = SpringApplication.exit(ctx, () -> {
                    // return the error code
                    return 0;
                });
                System.exit(code);
            } else if(restTest){
                CLI.restTest();
            }
            else {
                SpringApplication.run(WebApplication.class, args);
            }
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }
    }

}
