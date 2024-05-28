package dev.edigonzales.avdocs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.shell.command.annotation.CommandScan;

@ImportRuntimeHints(HintsRegistrar.class)
@CommandScan
@SpringBootApplication
public class AvdocsApplication {

	public static void main(String[] args) {
		SpringApplication.run(AvdocsApplication.class, args);
	}

}
