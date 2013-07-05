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

package grails.plugin.executor

import org.codehaus.groovy.grails.plugins.PluginManagerHolder

import spock.lang.Unroll
import executor.test.Book
import grails.plugin.spock.IntegrationSpec

class DynamicMethodsSpec extends IntegrationSpec {

	// Autowired
	def grailsApplication

	def currentClazz

	@Unroll("Reloading support for #clazz")
	def reloading() {
		setup:
		currentClazz = clazz

		expect:
		artifactHasExecutorMethods

		when:
		reloadClass()

		then:
		!artifactHasExecutorMethods

		when:
		informOfClassChange()

		then:
		artifactHasExecutorMethods

		where:
		clazz << [TestService, TestController]
	}

	def "domain classes"() {
		setup:
		currentClazz = Book

		expect:
		artifactHasExecutorMethods
	}

	protected getArtifactHasExecutorMethods() {
		createArtifact().respondsTo("runAsync", Runnable).size() > 0
	}

	protected createArtifact() {
		loadClass().newInstance()
	}

	protected loadClass() {
		grailsApplication.classLoader.loadClass(currentClazz.name)
	}

	protected reloadClass() {
		currentClazz = grailsApplication.classLoader.reloadClass(currentClazz.name)
	}

	protected informOfClassChange() {
		PluginManagerHolder.pluginManager.informOfClassChange(currentClazz)
	}
}
