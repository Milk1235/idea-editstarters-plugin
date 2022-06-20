package hdzi.editstarters.initializr;

import lombok.Data;

import java.util.Map;

@Data
public class InitializrResponse {
    private Map<String, InitializrDependency> dependencies;
    private Map<String, InitializrRepository> repositories;
    private Map<String, InitializrBom> boms;
}