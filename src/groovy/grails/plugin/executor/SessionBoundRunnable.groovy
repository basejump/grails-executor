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

import org.hibernate.SessionFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
* A callable that binds a session  and unbinds after its done
*/
public class SessionBoundRunnable implements Runnable {
	private final Runnable task;
	private final SessionFactory sessionFactory; 
	
	public SessionBoundRunnable(Runnable task,SessionFactory sessionFactory) {
		if (task == null || sessionFactory == null) throw new NullPointerException();
		this.task = task;
		this.sessionFactory = sessionFactory;
	}
	
	public void run(){
		SessionBinderUtils.bindSession(sessionFactory);
		try {
			task.run();
		} finally{
			SessionBinderUtils.unbindSession(sessionFactory);
		}
	}
}