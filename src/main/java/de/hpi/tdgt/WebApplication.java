package de.hpi.tdgt;

import de.hpi.tdgt.controllers.UploadController;
import de.hpi.tdgt.test.story.atom.DataGeneration;
import de.hpi.tdgt.test.story.atom.Request;
import de.hpi.tdgt.test.story.atom.assertion.Assertion;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import de.hpi.tdgt.util.*;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

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

        parser.addArgument("--location")
                .type(String.class)
                .help("Location of this node relative to other node. Be careful when using localhost:* hosts. Must be a valid HTTP URL.");

        parser.addArgument("--alternative-output-dir")
                .type(String.class)
                .help("Output directory for CSV files created by PDGF. Defaults to the output directory of the PDGF directory.");
        parser.addArgument("--broker-url")
                .type(String.class)
                .help("Location of the broker. Needs to include protocol (mqtt:// or ws:// or wss://) and port. Defaults to \""+PropertiesReader.getMqttHost()+"\"");
         parser.addArgument("--bufferSize")
		.type(Integer.class)
		.help("Buffer size per file to read in MiB. Defaults to 64.");

         parser.addArgument("--noop")
                 .action(Arguments.storeTrue())
                 .help("Check arguments, but do not start server or CLI-mode test.");
	val group = parser.addMutuallyExclusiveGroup("cli").description("Run one test and terminate afterwards (default: Run as webserver)");
        group.addArgument("--load").type(String.class).dest("load").nargs("?").help("If set, one test is loaded from the filesystem.");
        group.addArgument("--restTest").type(String.class).setConst(true).setDefault(false).nargs("?").dest("restTest").help("If set, some static rquests are run.");
        try {
            //Spring Boot might handle the other params --> ignore those in the list (the unrecognised ones)
            Namespace res = parser.parseKnownArgs(args, new LinkedList<>());
            // the argument parser renames the strings to contain underscores instead of minuses
            DataGeneration.outputDirectory = res.getString("alternative_output_dir") != null? res.getString("alternative_output_dir") :  res.getString("PDGFDirectory") + File.separator + "output";

            UploadController.PDGF_DIR = res.getString("PDGFDirectory");
            UploadController.JAVA_7_DIR = res.getString("JAVA7");
            val load = res.getString("load");
            val restTest = res.getBoolean("restTest");
            //optional
            if(res.getString("location") != null) {
                String[] schemes = {"http", "https"};
                //also allow localhost:* urls
                UrlValidator urlValidator = new UrlValidator(schemes, UrlValidator.ALLOW_LOCAL_URLS);
                if (urlValidator.isValid(res.getString("location"))) {
                    UploadController.LOCATION = res.getString("location");
                } else {
                    log.error("--location: URL is invalid: " + res.getString("location"));
                    System.exit(1);
                }
            }
	    // optional
	    if(res.getInt("bufferSize") != null){
	    		MappedFileReader.setBufferSize(res.getInt("bufferSize") * 1024 * 1024);
	    }
	    // optional
        //note that the name is changed to broker_url by the parser
	    if(res.getString("broker_url") != null){
                PropertiesReader.BROKER_URL = res.getString("broker_url");
	    }
	    if(!res.getBoolean("noop")) {
            if (load != null) {
                //If we do not start Spring Boot, many properties like logging get half-way initialized. So we start Spring boot and terminate it when the test is done.
                ConfigurableApplicationContext ctx = new SpringApplicationBuilder(WebApplication.class)
                        .web(WebApplicationType.NONE).run(args);
                CLI.loadTest(load);
                int code = SpringApplication.exit(ctx, () -> {
                    // return the error code
                    return (Assertion.getOneHasFailed() ? 3 : 0) + (Request.getOneExceededThreshold() ? 2 : 0);
                });
                System.exit(code);
            } else if (restTest) {
                CLI.restTest();
            } else {
                SpringApplication.run(WebApplication.class, args);
            }
        }else{
	        log.info("Not running any test since --noop specified.");
        }
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }
    }

}
