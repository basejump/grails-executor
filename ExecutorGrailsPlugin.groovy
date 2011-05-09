import grails.plugin.executor.PersistenceContextExecutorWrapper
import java.util.concurrent.Executors
import java.util.concurrent.Callable

class ExecutorGrailsPlugin {

	def version = "0.3-SNAPSHOT"
	def grailsVersion = "1.2 > *"
	def dependsOn = [:]

	def author = "Joshua Burnett"
	def authorEmail = "joshua@greenbill.com"
	def title = "Concurrency / asynchronous /background process plugin"
	def description = "its all concurrent baby."
	def documentation = "http://github.com/basejump/grails-executor"

	def observe = ["controllers","services"]

    def pluginExcludes = [
		"grails-app/**/*", 
		"web-app/**/*"
	]

	def doWithSpring = {
		executorService(PersistenceContextExecutorWrapper) { bean ->
			bean.destroyMethod = 'destroy'
			persistenceInterceptor = ref("persistenceInterceptor")
			executor = Executors.newCachedThreadPool()
		}
	}
	
	def addAsyncMethods(application, clazz) {
		clazz.metaClass.runAsync = { Runnable runme ->
			application.mainContext.executorService.withPersistence(runme)
		}
		clazz.metaClass.callAsync = { Closure clos ->
			application.mainContext.executorService.withPersistence(clos)
		}
		clazz.metaClass.callAsync = { Runnable runme, returnval ->
			application.mainContext.executorService.withPersistence(runme, returnval)
		}
	}

	def doWithDynamicMethods = { ctx ->
		for (artifactClasses in [application.controllerClasses, application.serviceClasses, application.domainClasses]) {
			for (clazz in artifactClasses) {
				addAsyncMethods(application, clazz)
			}
		}
	}

	def onChange = { event ->
		if (application.isControllerClass(event.source) || application.isServiceClass(event.source)) {
			addAsyncMethods(application, event.source)
		}
	}

}
