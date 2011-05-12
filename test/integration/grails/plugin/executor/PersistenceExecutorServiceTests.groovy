/*
 * Copyright 2010 Robert Fletcher
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

import java.util.concurrent.*
import executor.test.Book
import org.junit.*
import static org.junit.Assert.*

class PersistenceExecutorServiceTests {

	static transactional = false

	def sessionFactory
	def executorService

	@BeforeClass
	static void setUpData() {
		
	}
	
	@Before
	void setup() {

		Book.withNewSession {
			1.upto(5) { new Book(name: "$it").save() }
		}
	}
	
	@After
	void teardown() {
		Book.withNewSession {
			Book.list()*.delete()
		}
	}
	
	@Test
	void testExecute() {
		assertTrue Book.runAsyncFired.get()
		assert 5 == Book.count()

		def latch = new CountDownLatch(1)
		executorService.execute {
			assertEquals(5, Book.count())
			Book.list()*.delete()
			new Book(name: "async book").save()
			latch.countDown()
		}

		waitFor "end of book delete task", latch
		//sleep for a second to wait for the thread to finish and the session to flush
		sleep(1000)
		assertEquals(1, Book.count())
	}
	
	@Test
	void testSubmitCallable() {

		assert 5 == Book.count()

		def latch = new CountDownLatch(1)
		
		def closure = {
			assertEquals(5, Book.count())
			sleep(2000) //give it a couple of seconds in here so we can test stuff
			Book.list()*.delete()
			def book = new Book(name: "async book").save()
			latch.countDown()
			return book
		}
		
		Future future = executorService.submit(closure as Callable)

		//this should fire while the submited callable task is still running still show 5
		assertEquals(5, Book.count())
		
		waitFor "end of callable", latch, 4l
		//just to make sure we are good this thread before the other finishes
		new Book(name: "normal book").save()
		
		def fbook = future.get() //this will sit here and wait for the submited callable to finish and return the value. 
		assertEquals "async book", fbook.name
		assertEquals(2, Book.count())
	}
	
	@Test
	void testSubmitRunnable() {

		def latch = new CountDownLatch(1)

		Future future = executorService.submit( {
			//Book.withTransaction {
				Book.list()*.delete()
			//}
			latch.countDown()
		} as Runnable)

		waitFor "end of runnable", latch
		
		def fbook = future.get() //this will return a null since we sumbmited a runnable
		assertNull fbook
		assertEquals(0, Book.count())
	}
	
	@Test
	void testSubmitRunnableWithReturn() {

		def latch = new CountDownLatch(1)
		def clos = {
			Book.list()*.delete()
			latch.countDown()
		}
		
		Future future = executorService.submit(clos,"nailed it" )
		waitFor "end of runnable", latch
		
		def fbook = future.get() //this will return a null since we sumbmited a runnable
		assertEquals "nailed it", fbook
		assertEquals(0, Book.count())
	
	}
	
	@AfterClass
	static void tearDownData() {
/*		Album.withNewSession { session ->
			Album.list()*.delete()
			session.flush()
		}*/
	}
	
	static void waitFor(String message, CountDownLatch latch, long timeout = 1l) {
		if (!latch.await(timeout, TimeUnit.SECONDS)) {
			fail "Timed out waiting for $message, $latch.count more latch countDown should have run"
		}
	}

}

