package executor.test
import java.util.concurrent.atomic.AtomicBoolean

class Book {
	static AtomicBoolean runAsyncFired = new AtomicBoolean(false) 
	
	String name
	static constraints = {
	}

	def afterInsert(){
		runAsync {
			runAsyncFired.set(true)
		}
	}
}
