package com.bise.omr.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Persists Template objects to disk as JSON files, and loads them back.
 * Default storage directory is "templates/" in the application's working directory.
 */
public class TemplateRepository {

    private static final Logger log = LoggerFactory.getLogger(TemplateRepository.class);

    private final Path storageDir;
    private final ObjectMapper mapper;

    public TemplateRepository() {
        this(Paths.get("templates"));
    }

    public TemplateRepository(Path storageDir) {
        this.storageDir = storageDir;
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);

        try {
            Files.createDirectories(storageDir);
        } catch (IOException ex) {
            log.error("Failed to create template directory", ex);
        }
    }

    public File save(Template template) throws IOException {
        if (!template.validate()) {
            throw new IllegalArgumentException("Template is invalid: code and fields are required");
        }
        File out = storageDir.resolve(template.getTemplateCode() + ".json").toFile();
        mapper.writeValue(out, template);
        log.info("Saved template {} → {}", template.getTemplateCode(), out.getAbsolutePath());
        return out;
    }

    public Template load(File file) throws IOException {
        Template t = mapper.readValue(file, Template.class);
        log.info("Loaded template {} from {}", t.getTemplateCode(), file.getName());
        return t;
    }

    public List<Template> listAll() {
        List<Template> templates = new ArrayList<>();
        try (Stream<Path> paths = Files.list(storageDir)) {
            paths.filter(p -> p.toString().endsWith(".json"))
                 .forEach(p -> {
                     try { templates.add(load(p.toFile())); }
                     catch (IOException ex) { log.warn("Skipping invalid template: {}", p); }
                 });
        } catch (IOException ex) {
            log.error("Failed to list templates", ex);
        }
        return templates;
    }
}
