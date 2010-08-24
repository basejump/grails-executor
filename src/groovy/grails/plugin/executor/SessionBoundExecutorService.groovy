/*
 * Copyright 2010 Joshua Burnett
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package grails.plugin.executor

import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.RunnableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import org.springframework.orm.hibernate3.SessionFactoryUtils
import org.hibernate.SessionFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * An ExecutorService that wraps another executor service and takes care of binding a hibernate session 
 * and does transaction synchronization with TransactionSynchronizationManager. 
 * it basically does this by overriding the call to "newTaskFor" nd "wraps" the runnable/callable in something that is session aware
 * 
 * You must inject a ExecutorService instance. Its recommended to use the convenience methods in Executors.
 * 
 * NOTES: the shutdowns, isShutdown, isTerminated and awaitTermination are just pass throughs to your injected ExecutorService
 * all the rest (submit, invokeAll and invokeAny) it leaves to the AbstractExecutorService which all eventually end up 
 * calling newTaskFor to get a RunnableFuture which will be a SessionFutureTask.
 * your runnable "background" async thread will always get a session bound to it.
 * However, if you inject your own special ExecutorService that does something unique with the submit, invokeAll and invokeAny those methods won't get called. 
 * this is really meant to be used with a ExecutorService that comes with 
 * After I wrote this and scanned the source for the java concurrent it became apparent that this only works with 1.6 since AbstractExecutorService.newTaskFor
 * didn't exist in 1.5
 * 
 */

class SessionBoundExecutorService extends AbstractExecutorService implements ExecutorService{
	private final Logger log = LoggerFactory.getLogger(SessionBoundExecutorService)
	
	ExecutorService executor
	SessionFactory sessionFactory
	
	SessionBoundExecutorService() { }
	
	SessionBoundExecutorService(ExecutorService executor) { 
		this.executor = executor; 
	}
	
	protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
		return new SessionFutureTask<T>(runnable,value,sessionFactory);
	}

	protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
		return new SessionFutureTask<T>(callable,sessionFactory);
	}

	public void execute(Runnable command) { 
		Runnable runMe = command
		if(!(command instanceof SessionFutureTask) && !(command instanceof SessionBoundRunnable)){
			runMe = new SessionBoundRunnable(command,sessionFactory)
		}
		executor.execute(runMe); 
	}

	public void shutdown() { executor.shutdown(); }

	public List<Runnable> shutdownNow() { return executor.shutdownNow(); }

	public boolean isShutdown() { return executor.isShutdown(); }

	public boolean isTerminated() { return executor.isTerminated(); }

	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		return executor.awaitTermination(timeout, unit);
	}
	
	public void destroy() {
		executor.shutdown()
		if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
			log.warn "ExecutorService did not shutdown in 2 seconds. Forcing shutdown of any scheduled tasks"
			executor.shutdownNow()
		}
	}
	

}