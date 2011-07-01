/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import grails.plugin.executor.PersistenceContextExecutorWrapper
import java.util.concurrent.Executors
import java.util.concurrent.Callable

class ExecutorGrailsPlugin {

	def version = "0.3"
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
