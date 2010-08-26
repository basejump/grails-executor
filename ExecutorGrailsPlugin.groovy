import grails.plugin.executor.SessionBoundExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Callable

class ExecutorGrailsPlugin {
    // the plugin version
    def version = "0.2"
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

	def observe = ["controllers","services"]

	def doWithSpring = {
		executorService(SessionBoundExecutorService) { bean->
			bean.destroyMethod = 'destroy'
			sessionFactory = ref("sessionFactory")
			executor = Executors.newCachedThreadPool()
		}
	}
	
	def addAsyncMethods(application,clazz) {
			clazz.metaClass.runAsync = { Runnable runme ->
				application.mainContext.executorService.execute(runme)
			}
			clazz.metaClass.callAsync = { Closure clos ->
				application.mainContext.executorService.submit(clos as Callable)
			}
			clazz.metaClass.callAsync = { Runnable runme, def returnval ->
				application.mainContext.executorService.submit(runme,returnval)
			}
	}

	def doWithDynamicMethods = { ctx ->
		[application.controllerClasses, application.serviceClasses, application.domainClasses].flatten().each {it->
			addAsyncMethods(application,it)
		}
	}

	def onChange = { event ->
		if (application.isControllerClass(event.source) || application.isServiceClass(event.source)) {
			addAsyncMethods(application,event.source)
		}
	}

	

}
