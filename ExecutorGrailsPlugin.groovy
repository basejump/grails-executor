import grails.plugin.executor.SessionBoundExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Callable

class ExecutorGrailsPlugin {
    // the plugin version
    def version = "0.1"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "1.2 > *"
    // the other plugins this plugin depends on
    def dependsOn = [:]
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
		"grails-app/domain/**/*","grails-app/views/error.gsp","web-app/**/*"
    ]

    // TODO Fill in these fields
    def author = "Joshua Burnett"
    def authorEmail = "joshua@greenbill.com"
    def title = "Concurrency / asynchronous /background process plugin"
    def description = '''\\ its all concurrent baby.'''

    // URL to the plugin's documentation
    def documentation = "http://github.com/basejump/grails-executor"


	def doWithSpring = {
		executorService(SessionBoundExecutorService) { bean->
			bean.destroyMethod = 'destroy'
			sessionFactory = ref("sessionFactory")
			executor = Executors.newCachedThreadPool()
		}
	}

    def doWithDynamicMethods = { ctx ->
		[application.controllerClasses, application.serviceClasses, application.domainClasses].flatten().each {
			it.metaClass.runAsync = { Runnable runme ->
				application.mainContext.executorService.execute(runme)
			}
			it.metaClass.callAsync = { Closure clos ->
				return application.mainContext.executorService.submit(clos as Callable)
			}
			it.metaClass.callAsync = { Runnable runme, def returnval ->
				return application.mainContext.executorService.submit(runme,returnval)
			}
		}
    }

}
