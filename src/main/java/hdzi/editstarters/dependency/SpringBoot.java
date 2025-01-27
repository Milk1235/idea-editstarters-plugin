package hdzi.editstarters.dependency;

import hdzi.editstarters.version.Version;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SpringBoot {
    private final Version bootVersion;
    private final List<Module> modules;


    public SpringBoot(Version bootVersion, List<Module> modules) {
        this.bootVersion = bootVersion;
        this.modules = modules;
    }
}
