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

import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.ExecutorService

import org.codehaus.groovy.grails.support.PersistenceContextInterceptor

/**
 * Wraps an ExecutorService, overriding the submitting methods to have the work done in a 
 * persistence context (via the persistenceInterceptor) and adds new methods that make it possible 
 * to still do work without opening a persistence context
 */
class PersistenceContextExecutorWrapper {
    
	// Autowired
	@Delegate ExecutorService executor
	PersistenceContextInterceptor persistenceInterceptor
	
	void execute(Runnable command) {
		executor.execute(inPersistence(command))
	}

	void executeWithoutSession(Runnable command) {
		executor.execute(command)
	}

	void executeWithoutPersistence(Runnable command) {
		executor.execute(command)
	}
	
	public <T> Future<T> submit(Callable<T> task) {
		executor.submit(inPersistence(task))
	}

	public <T> Future<T> submitWithoutSession(Callable<T> task) {
		executor.submit(task)
	}

	public <T> Future<T> submitWithoutPersistence(Callable<T> task) {
		executor.submit(task)
	}
	
	Future<?> submit(Runnable task) {
		executor.submit(inPersistence(task))
	}

	Future<?> submitWithoutSession(Runnable task) {
		executor.submit(task)
	}

	Future<?> submitWithoutPersistence(Runnable task) {
		executor.submit(task)
	}
	
	public <T> Future<T> submit(Runnable task, T result) {
		executor.submit(inPersistence(task), result)
	}

	public <T> Future<T> submitWithoutSession(Runnable task, T result) {
		executor.submit(task, result)
	}

	public <T> Future<T> submitWithoutPersistence(Runnable task, T result) {
		executor.submit(task, result)
	}
	
	Future withSession(Closure task) {
		withPersistence(task)
	}
	
	Future withPersistence(Closure task) {
		executor.submit(inPersistence((Closure)task))
	}
	
	Future withoutSession(Closure task) {
		executor.withoutPersistence(task)
	}

	Future withoutPersistence(Closure task) {
		executor.submit(task as Callable)
	}

	Future leftShift(Closure task) {
		withPersistence(task)
	}
	
	Callable inPersistence(Closure task) {
		inPersistence(task as Callable)
	}

	Callable inPersistence(Callable task) {
		if (persistenceInterceptor == null) {
			throw new IllegalStateException("Unable to create persistence context wrapped callable because persistenceInterceptor is null")
		}
		
		new PersistenceContextCallableWrapper(persistenceInterceptor, task)
	}

	Runnable inPersistence(Runnable task) {
		if (persistenceInterceptor == null) {
			throw new IllegalStateException("Unable to create persistence context wrapped runnable because persistenceInterceptor is null")
		}
		
		new PersistenceContextRunnableWrapper(persistenceInterceptor, task)
	}
	
	public void destroy() {
		executor.shutdown()
		if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
			log.warn "ExecutorService did not shutdown in 2 seconds. Forcing shutdown of any scheduled tasks"
			executor.shutdownNow()
		}
	}

}