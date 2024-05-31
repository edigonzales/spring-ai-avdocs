package dev.edigonzales.avdocs;

import jakarta.annotation.PostConstruct;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.ParagraphPdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.core.io.FileSystemResource;

@Component
public class ReferenceDocsLoader {

    private static final Logger log = LoggerFactory.getLogger(ReferenceDocsLoader.class);
    private final JdbcClient jdbcClient;
    private final VectorStore vectorStore;
    @Value("classpath:/docs/handbuch-2.6.pdf")
    private Resource pdfResource;

    @Value("${app.docs.location}")
    private String docsLocation;

    public ReferenceDocsLoader(JdbcClient jdbcClient, VectorStore vectorStore) {
        this.jdbcClient = jdbcClient;
        this.vectorStore = vectorStore;
    }

    @PostConstruct
    public void init() {
        Integer count = jdbcClient.sql("select count(*) from vector_store")
                .query(Integer.class)
                .single();

        log.info("Current count of the Vector Store: {}", count);
        if (count == 0) {
            log.info("Loading PDFs into Vector Store");
            
            List<String> files = Stream.of(new File(docsLocation).listFiles())
                .filter(file -> !file.isDirectory())
                .map(File::getAbsolutePath)
                .collect(Collectors.toList());
            
            for(String file : files) {
                log.info("file: " + file);
                if (!file.endsWith("pdf")) {
                    continue;
                }
                
                var config = PdfDocumentReaderConfig.builder()
                        .withPageExtractedTextFormatter(
                                new ExtractedTextFormatter.Builder().withNumberOfBottomTextLinesToDelete(0)
                                        .withNumberOfTopPagesToSkipBeforeDelete(0).build())
                        .withPagesPerDocument(1).build();

                //var pdfReader = new ParagraphPdfDocumentReader(pdfResource, config);
                var pdfReader = new PagePdfDocumentReader(new FileSystemResource(file), config);
                var textSplitter = new TokenTextSplitter();
                vectorStore.accept(textSplitter.apply(pdfReader.get()));
            }
            
//            var config = PdfDocumentReaderConfig.builder()
//                    .withPageExtractedTextFormatter(new ExtractedTextFormatter.Builder().withNumberOfBottomTextLinesToDelete(0)
//                            .withNumberOfTopPagesToSkipBeforeDelete(0)
//                            .build())
//                    .withPagesPerDocument(1)
//                    .build();
//
//            //var pdfReader = new ParagraphPdfDocumentReader(pdfResource, config);
//            var pdfReader = new PagePdfDocumentReader(pdfResource, config);
//            var textSplitter = new TokenTextSplitter();
//            vectorStore.accept(textSplitter.apply(pdfReader.get()));

            log.info("Application is ready");
        }
    }
}
