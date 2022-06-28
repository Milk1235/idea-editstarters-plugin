package hdzi.editstarters.initializr;

import com.google.gson.annotations.SerializedName;
import hdzi.editstarters.dependency.DependencyScope;
import hdzi.editstarters.dependency.Link;
import hdzi.editstarters.dependency.Module;
import hdzi.editstarters.dependency.StarterInfo;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;

@Getter
@Setter
public class InitializrMetadataClient {
    @SerializedName("_link")
    private Links link;

    private Dependencies dependencies;

    public List<Module> getModules(InitializrDependencies initializrDependencies) {
        // 组合 dependencies 和 metaData
        for (StarterInfo starterInfo : this.dependencies) {
            InitializrDependency dependency = initializrDependencies.getDependencies().get(starterInfo.getId());
            if (dependency != null) {
                starterInfo.setGroupId(dependency.getGroupId());
                starterInfo.setArtifactId(dependency.getArtifactId());
                starterInfo.setVersion(dependency.getVersion());
                starterInfo.setScope(DependencyScope.getByScope(dependency.getScope()));

                InitializrBom bom = initializrDependencies.getBoms().get(dependency.getBom());
                if (bom != null) {
                    starterInfo.setBom(bom);
                    for (String rid : bom.getRepositories()) {
                        InitializrRepository repository = initializrDependencies.getRepositories().get(rid);
                        repository.setId(rid);
                        starterInfo.addRepository(repository);
                    }
                }
                InitializrRepository repository = initializrDependencies.getRepositories().get(dependency.getRepository());
                if (repository != null) {
                    repository.setId(dependency.getRepository());
                    starterInfo.addRepository(repository);
                }
            }
        }

        return this.dependencies.values;
    }


    //==================================================================================================================

    @Getter
    @Setter
    public static class Links {
        private Link dependencies;
    }

    @Getter
    @Setter
    public static class Dependencies implements Iterable<StarterInfo> {
        private String type;
        private List<Module> values;

        @NotNull
        @Override
        public Iterator<StarterInfo> iterator() {
            return this.values.stream().flatMap(it -> it.getValues().stream()).iterator();
        }
    }
}
