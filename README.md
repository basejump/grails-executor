Summary
--------

This grails enables the java concurrency Executor Framework into a plugin so your grails app can take advantage of asynchronous (background thread / concurrent) processing. The main need for this as opposed to just using an [ExecutorService][] from [Executors][] is that we need to wrap the calls so there is a Hibernate session bound to the thread. 
Here are a couple of links to get give you some background information.
http://www.ibm.com/developerworks/java/library/j-jtp1126.html
http://www.vogella.de/articles/JavaConcurrency/article.html
and here are few good write up on groovy concurrency 
http://groovy.codehaus.org/Concurrency+with+Groovy
and a slide show
http://www.slideshare.net/paulk_asert/groovy-and-concurrency-paul-king

Setup
-------

The plugin sets up a service called executorService so you need do nothing really. It is an implementation of an [ExecutorService][] interface so read up on that for more info on what you can do with the executorService. It basically wraps another [ExecutorService][] that by default uses the java [Executors][] utility class to setup the injected service. The default executorService looks like this 

	executorService(grails.plugin.executor.SessionBoundExecutorService) { bean->
		bean.destroyMethod = 'destroy'
		sessionFactory = ref("sessionFactory")
		executor = Executors.newCachedThreadPool()
	}

You can override it and inject your own special executor using Executors by either setting up your own bean in conf/spring/resources.groovy or overriding the spring bean settings with your conf/Config.groovy.
	
	beans {
		executorService {
			//this can be whatever from Executors (don't write your own and pre-optimize)
			executor = Executors.newCachedThreadPool(new YourThreadFactory()) 
 		}
	}

To me the Config confuses the issue if you can use resources.groovy or doWithSpring in your plugins

Usage
------

You can inject the executorService into any bean. Its just an [ExecutorService][] so, again, see the api for more on what you can do. Remember that a closure is a [Runnable](http://download.oracle.com/javase/6/docs/api/java/lang/Runnable.html) so can pass it to any of the methods that accept a runnable. A great example exists [here on the groovy site](http://groovy.codehaus.org/Concurrency+with+Groovy)

The plugin adds shortcut methods to any service/controller/domain objects.
runAsync closure - takes any closure and passes it through to the executorService.execute
callAsync closure - takes any closure that returns a value and passes it through to the executorService.submit . You will get a [Future] back that you 

NOTE ON TRANSACTIONS: keep in mind that this is spinning of a new thread and that any call will be in outside of the any transaction you are in. Use .withTransaction inside your closure, runnable or callable to make your process run in a transaction that is not calling a transactional service method (such as using this in a controller).

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


[ExecutorService]: http://download.oracle.com/javase/6/docs/api/java/util/concurrent/ExecutorService.html
[Executors]: http://download.oracle.com/javase/6/docs/api/java/util/concurrent/Executors.html
[Future]: 
