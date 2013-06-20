Summary
--------

This grails plugin incorporates the java concurrency Executor Framework into a plugin so your grails app can take advantage of asynchronous (background thread / concurrent) processing. The main need for this as opposed to just using an [ExecutorService][] from [Executors][] is that we need to wrap the calls so there is a Hibernate or other Data provider session bound to the thread. 
This uses the following pattern to wrap Runnable/Closures so they get a session for whatever Gorm you are using. Hibernate being the default but this is also tested with Mongo (no heavily)  See the info on the [PersistenceContextInterceptor][] grails bean for more info

	//injected spring bean
	PersistenceContextInterceptor persistenceInterceptor
	
	protected wrap(Closure wrapped) {
		persistenceInterceptor.init()
		try {
			wrapped()
		} finally {
			persistenceInterceptor.flush()
			persistenceInterceptor.destroy()
		}
	}

Here are a couple of links to get give you some background information.

<http://www.ibm.com/developerworks/java/library/j-jtp1126.html>  
<http://www.vogella.de/articles/JavaConcurrency/article.html>  

and here are few good write up on groovy concurrency  
<http://groovy.codehaus.org/Concurrency+with+Groovy>  
and a slide show  
<http://www.slideshare.net/paulk_asert/groovy-and-concurrency-paul-king>

Setup
-------

The plugin sets up a Grails service bean called executorService so you need do nothing really. It delegates to an implementation of an Java [ExecutorService][] (not to be confused with a Grails Service) interface so read up on that for more info on what you can do with the executorService. It basically wraps another thread pool [ExecutorService][]. By default it uses the java [Executors][] utility class to setup the injected thread pool ExecutorService implementation. The default Grails executorService config looks like this 

	executorService( grails.plugin.executor.PersistenceContextExecutorWrapper ) { bean->
		bean.destroyMethod = 'destroy'
		persistenceInterceptor = ref("persistenceInterceptor")
		executor = Executors.newCachedThreadPool()
	}

You can override it and inject your own special thread pool executor using [Executors][] by overriding the bean in conf/spring/resources.groovy or the doWithSpring closure in your plugin.
	
	executorService(  grails.plugin.executor.PersistenceContextExecutorWrapper ) { bean->
		bean.destroyMethod = 'destroy' //keep this destroy method so it can try and clean up nicely
		persistenceInterceptor = ref("persistenceInterceptor")
		//this can be whatever from Executors (don't write your own and pre-optimize)
		executor = Executors.newCachedThreadPool(new YourSpecialThreadFactory()) 
	}

Usage
------

You can inject the executorService into any bean. Its a [PersistenceContextExecutorWrapper][] that delegates any calls to a concrete [ExecutorService][] implementation so, again, see the api for more on what you can do. Remember that a [Closure][] is a [Runnable][] so you can pass it to any of the methods that accept a runnable. A great example exists [here on the groovy site](http://groovy.codehaus.org/Concurrency+with+Groovy)

The plugin adds shortcut methods to any service/controller/domain artifacts.

- **runAsync _closure_** - takes any closure and passes it through to the executorService.execute
- **callAsync _closure_** - takes any closure that returns a value and passes it through to the executorService.submit . You will get a [Future] back that you can work with. This will not bind a session in java 1.5 and only works on 1.6 or later

NOTE ON TRANSACTIONS: keep in mind that this is spinning off a new thread and that any call will be outside of the transaction you are in. Use .withTransaction inside your closure, runnable or callable to make your process run in a transaction that is not calling a transactional service method (such as when using this in a controller).

Examples
--------

in a service/domain/controller just pass a Closure or [Runnable] to runAsync

	class someService {

		def myMethod(){
			..do some stuff
			runAsync {
				//this will be in its own trasaction 
				//since each of these service methods are Transactional
				calcAging() 
			}
			.. do some other stuff while aging is calced in background
		}

		def calcAging(){
			...do long process
		}
	}
	
or inject the executorService

	class someService {
		def executorService

		def myMethod(){
			....do stuff
			def future = executorService.submit({
				return calcAging() //you can of course leave out the "return" here
			} as Callable)
			.. do some other stuff while its processing
			//now block and wait with get()
			def aging = future.get()
			..do something
		}

		def calcAging(){
			...do long process
			return agingCalcObject
		}
	
	}
	
or during a domain event
	
	class Book {
		def myNotifyService
		
		String name
		
		def afterInsert(){
			runAsync {
				myNotifyService.informLibraryOfCongress(this)
			}
		}
	}

the callAsync allows you to spin of a process and calls the underlying executorService submit

	class someService {
		def myMethod(){
			....do stuff
			def future = callAsync {
				return calcAging() //you can of course leave out the "return" here
			}
			.. do some other stuff while its processing
			//now block and wait with get()
			def aging = future.get()
			..do something with the aging
		}

		def calcAging(){
			...do stuff
			return agingCalcObject
		}
	}

TODOs
--------

* TODO - setup a wrapper so we can use a [ScheduledExecutorService][] too.


[ExecutorService]: http://download.oracle.com/javase/6/docs/api/java/util/concurrent/ExecutorService.html
[Executors]: http://download.oracle.com/javase/6/docs/api/java/util/concurrent/Executors.html
[Future]: http://download-llnw.oracle.com/javase/6/docs/api/java/util/concurrent/Future.html
[Runnable]: http://download.oracle.com/javase/6/docs/api/java/lang/Runnable.html
[Closure]: http://groovy.codehaus.org/api/groovy/lang/Closure.html
[ScheduledExecutorService]: http://download.oracle.com/javase/6/docs/api/java/util/concurrent/ScheduledExecutorService.html
[PersistenceContextExecutorWrapper]: http://github.com/basejump/grails-executor/blob/master/src/groovy/grails/plugin/executor/PersistenceContextExecutorWrapper.groovy
[PersistenceContextInterceptor]: http://grails.org/doc/latest/api/org/codehaus/groovy/grails/support/PersistenceContextInterceptor.html 
