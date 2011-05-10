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

import grails.plugin.spock.*
import spock.lang.*
import spock.util.concurrent.BlockingVariable

import java.util.concurrent.Callable

import executor.test.Book

class AlternativeSyntaxesSpec extends IntegrationSpec {

	// Autowired
	def executorService
	
	def var = new BlockingVariable()

	def "left shift closure"() {
		when:
		executorService << { var.set(1) }
		
		then:
		var.get() == 1
	}

	def "left shift callable"() {
		when:
		executorService << new Callable() { def call() { var.set(1) } }
		
		then:
		var.get() == 1
	}

	def "left shift runnable"() {
		when:
		executorService << new Runnable() { void run() { var.set(1) } }
		
		then:
		var.get() == 1
	}
	
}