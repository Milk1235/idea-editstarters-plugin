package hdzi.editstarters.springboot

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DataKeys
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.ThrowableComputable
import hdzi.editstarters.springboot.bean.Dependency
import hdzi.editstarters.springboot.bean.StarterInfo
import hdzi.editstarters.ui.EditStartersDialog
import hdzi.editstarters.ui.InitializrUrlDialog

/**
 * 编辑器
 *
 * Created by taojinhou on 2019/1/11.
 */
abstract class SpringBootEditor(val context: DataContext, dependencies: () -> List<Dependency>) {
    private val existsDependencies: Map<Dependency, Dependency> = dependencies().associateBy({ it }, { it })
    private val springbootDependency = existsDependencies[Dependency("org.springframework.boot", "spring-boot")]

    /**
     * 启动编辑器
     */
    fun edit() {
        if (isSpringBootProject) {
            // 弹出spring initializr地址输入框
            val dialog = InitializrUrlDialog().show()
            // 检查url确定是否点击了ok
            if (dialog.isOK) {
                initSpringInitializr(dialog.url!!)
                EditStartersDialog(this).show()
            }
        } else throw Exception("Not a Spring Boot Project!")
    }

    var springInitializr: SpringInitializr? = null

    val currentVersion: String? = springbootDependency?.version

    /**
     * 判断是否是spring boot项目
     */
    val isSpringBootProject: Boolean = springbootDependency != null

    /**
     * 初始化Initializr
     */
    private fun initSpringInitializr(url: String) =
        ProgressManager.getInstance().runProcessWithProgressSynchronously(ThrowableComputable<Unit, Exception> {
            springInitializr = SpringInitializr(url, currentVersion!!)
            existsDependencies.values.forEach { dep ->
                this.springInitializr!!.addExistsStarter(dep.groupId, dep.artifactId)
            }
        }, "Load ${url}", false, context.getData(DataKeys.PROJECT))

    abstract fun addDependencies(starterInfos: Collection<StarterInfo>)

    abstract fun removeDependencies(starterInfos: Collection<StarterInfo>)
}