package dev.edigonzales.avdocs;

import org.jline.utils.Log;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.Generation;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.core.io.Resource;
import org.springframework.shell.command.annotation.Command;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Command
public class SpringAssistantCommand {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    @Value("classpath:/prompts/av-docs.st")
    private Resource avPromptTemplate;

    public SpringAssistantCommand(ChatClient chatClient, VectorStore vectorStore) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
    }

    @Command(command = "q")
    public String question(@DefaultValue(value = "Wie ist die Gebäude-Definition") String message) {
        PromptTemplate promptTemplate = new PromptTemplate(avPromptTemplate);
        Map<String, Object> promptParameters = new HashMap<>();
        promptParameters.put("input", message);
        promptParameters.put("documents", String.join("\n", findSimilarDocuments(message)));

        //System.err.println("Calling chat client");
        Log.info("Calling chat client");
        
        Generation generation = chatClient.call(promptTemplate.create(promptParameters)).getResult();
        ChatGenerationMetadata metadata = generation.getMetadata();
        
        //Log.info(metadata.getContentFilterMetadata());
        
        return generation.getOutput().getContent();
    }

    private List<String> findSimilarDocuments(String message) {
        //System.err.println("Starting similarity search");
        Log.info("Starting similarity search");
        List<Document> similarDocuments = vectorStore.similaritySearch(SearchRequest.query(message).withTopK(4));
        
        for (Document document : similarDocuments) {
            System.err.println("Quelle: " + document.getMetadata());
        }
        //System.err.println("Similar documents found: " + similarDocuments.size());
        Log.info("Similar documents found: " + similarDocuments.size());
        return similarDocuments.stream().map(Document::getContent).toList();
    }

}
