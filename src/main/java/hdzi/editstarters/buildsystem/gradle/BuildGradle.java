package hdzi.editstarters.buildsystem.gradle;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ContainerUtil;
import hdzi.editstarters.buildsystem.ProjectBom;
import hdzi.editstarters.buildsystem.ProjectDependency;
import hdzi.editstarters.buildsystem.ProjectRepository;
import hdzi.editstarters.initializr.InitializrBom;
import hdzi.editstarters.initializr.InitializrRepository;
import hdzi.editstarters.initializr.StarterInfo;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrStatement;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by taojinhou on 2019/1/16.
 */
@SuppressWarnings("ConstantConditions")
public class BuildGradle extends GradleSyntax<GrClosableBlock> {
    private final GroovyFile buildFile;

    private final GroovyPsiElementFactory factory;

    public BuildGradle(Project project, GroovyFile buildFile) {
        this.buildFile = buildFile;
        this.factory = GroovyPsiElementFactory.getInstance(project);
    }


    @Override
    public GrClosableBlock getOrCreateDependenciesTag() {
        return getOrCreateClosure(buildFile, TAG_DEPENDENCY_MANAGEMENT);
    }

    @Override
    public List<ProjectDependency> findAllDependencies(GrClosableBlock dependenciesTag) {
        return PsiTreeUtil.getChildrenOfTypeAsList(dependenciesTag, GrMethodCall.class).stream()
                .map(it -> {
                    GradlePoint gradlePoint = getDependencyGroupArtifact(it);
                    return new ProjectDependency(gradlePoint.getGroupId(), gradlePoint.getArtifactId());
                }).collect(Collectors.toList());
    }


    @Override
    public void createDependencyTag(GrClosableBlock dependenciesTag, StarterInfo info) {
        List<Instruction> instructions = dependencyInstruction(info);
        for (Instruction instruction : instructions) {
            GrStatement statement = factory.createStatementFromText(instruction.toInstString("$inst '$point'"));
            dependenciesTag.addStatementBefore(statement, null);
        }
    }

    @Override
    public GrClosableBlock getOrCreateBomsTag() {
        return getOrCreateClosure(getOrCreateClosure(buildFile, TAG_BOM_MANAGEMENT), TAG_BOM_IMPORT);
    }

    @Override
    public List<ProjectBom> findAllBoms(GrClosableBlock bomsTag) {
        return findAllMethod(bomsTag, TAG_BOM).stream()
                .map(it -> {
                    GradlePoint gradlePoint = splitGroupArtifact(getMethodFirstParam(it));
                    return new ProjectBom(gradlePoint.getGroupId(), gradlePoint.getArtifactId());
                }).collect(Collectors.toList());
    }

    @Override
    public void createBomTag(GrClosableBlock bomsTag, InitializrBom bom) {
        Instruction instruction = bomInstruction(bom);
        GrStatement statement = factory.createStatementFromText(instruction.toInstString("$inst '$point'"));
        bomsTag.addStatementBefore(statement, null);
    }

    @Override
    public GrClosableBlock getOrCreateRepositoriesTag() {
        return getOrCreateClosure(buildFile, TAG_REPOSITORY_MANAGEMENT);
    }


    @Override
    public List<ProjectRepository> findAllRepositories(GrClosableBlock repositoriesTag) {
        return findAllMethod(repositoriesTag, TAG_REPOSITORY).stream()
                .map(it -> {
                    GrMethodCall urlCall = findMethod(it.getClosureArguments()[0], "url");
                    return new ProjectRepository(urlCall != null ? getMethodFirstParam(urlCall) : "");
                }).collect(Collectors.toList());
    }

    public void createRepositoryTag(GrClosableBlock repositoriesTag, InitializrRepository repository) {
        Instruction instruction = repositoryInstruction(repository);
        GrStatement statement = factory.createStatementFromText(instruction.toInstString("$inst { url '$point' }"));
        repositoriesTag.addStatementBefore(statement, null);
    }

    private GrClosableBlock getOrCreateClosure(PsiElement psiElement, String name) {
        GrMethodCall closure = findMethod(psiElement, name);
        if (closure == null) {
            GrStatement statement = factory.createStatementFromText(name + " {\n}");
            if (psiElement instanceof GrClosableBlock) {
                closure = (GrMethodCall) ((GrClosableBlock) psiElement).addStatementBefore(statement, null);
            } else {
                closure = (GrMethodCall) psiElement.add(statement);
            }
        }

        return closure.getClosureArguments()[0];
    }

    private GrMethodCall findMethod(PsiElement psiElement, String name) {
        return PsiTreeUtil.getChildrenOfTypeAsList(psiElement, GrMethodCall.class).stream()
                .filter(it -> Objects.equals(name, it.getInvokedExpression().getText()))
                .findFirst()
                .orElse(null);
    }

    private List<GrMethodCall> findAllMethod(PsiElement psiElement, String name) {
        List<GrMethodCall> closableBlocks = PsiTreeUtil.getChildrenOfTypeAsList(psiElement, GrMethodCall.class);
        return ContainerUtil.findAll(closableBlocks, it -> Objects.equals(name, it.getInvokedExpression().getText()));
    }

    private String getMethodFirstParam(GrMethodCall call) {
        return trimText(call.getArgumentList().getAllArguments()[0].getText());
    }

    private GradlePoint getDependencyGroupArtifact(GrMethodCall call) {
        Map<String, String> namedArguments = Arrays.stream(call.getNamedArguments()).collect(Collectors.toMap(
                it -> trimText(it.getLabel().getText()),
                it -> trimText(it.getExpression().getText())
        ));

        if (namedArguments.isEmpty()) {
            return splitGroupArtifact(getMethodFirstParam(call));
        } else {
            return GradlePoint.of(namedArguments.get("group"), namedArguments.get("name"));
        }
    }

    private String trimText(String s) {
        return trimText(s, '\'', '"');
    }
}
