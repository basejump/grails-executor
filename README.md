Quick Asynchronous Example for those of use with ADHD:
This will fire off a background asynchronous concurrent process. (did I get all the relevant keyword there?)

some domain class


Summary: 
This grails enables the java concurrency Executor Framework into a plugin so your grails app can take advantage of asynchronous (background thread / concurrent) processing. Here are a couple of links to get give you some background information.
http://www.ibm.com/developerworks/java/library/j-jtp1126.html
http://www.vogella.de/articles/JavaConcurrency/article.html
and here are few good write up on groovy concurrency 
http://groovy.codehaus.org/Concurrency+with+Groovy
and a slide show
http://www.slideshare.net/paulk_asert/groovy-and-concurrency-paul-king

Setup:
The plugin sets up a service called executorService so you need do nothing really. It is an implementation of <http://download.oracle.com/javase/6/docs/api/java/util/concurrent/ExecutorService.html> so see that for more info on what you can do with the executorService. It basically wraps another executorService thatby default uses a java <http://download.oracle.com/javase/6/docs/api/java/util/concurrent/Executors.html>Executor to setup the injected service. The default executorService looks like this 
executorService(grails.plugin.executor.SessionBoundExecutorService) { bean->
			bean.destroyMethod = 'destroy'
			sessionFactory = ref("sessionFactory")
			executor = Executors.newCachedThreadPool()
		}

You can override it and inject your own special executor using Executors by either setting up your own bean in conf/spring/resources.groovy or overriding the spring bean settings with your conf/Config.groovy.
beans {
	executorService {
		executor = Executors.newCachedThreadPool(new YourThreadFactory()) //this can be whatever from Executors (don't write your own and pre-optimize)
 	}
}
To me the Config confuses the issue if you can use resources.groovy or doWithSpring in tour plugins

Usage:
You can inject the executorService into any grails aware class. Its just a http://download.oracle.com/javase/6/docs/api/java/util/concurrent/ExecutorService so see the api for what you can do.
remember that a closure is a Runnable<http://download.oracle.com/javase/6/docs/api/java/lang/Runnable.html> so can pass it to any of the methods that accept a runnable. again, good example exists here <http://groovy.codehaus.org/Concurrency+with+Groovy>



in a service

someService {
	def executorService

	def myMethod(){
		do stuff
		executorService.execute({
			calcAging()
		})
	}

	def calcAging(){
		...do long process
	}
	
}

someService {

	def myMethod(){
		..do stuff
		runAsync {
			calcAging()
		}
	}

	def calcAging(){
		...do long process
	}
	
}