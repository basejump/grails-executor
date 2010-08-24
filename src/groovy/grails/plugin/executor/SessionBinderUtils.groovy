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

import org.hibernate.FlushMode
import org.hibernate.SessionFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.orm.hibernate3.SessionFactoryUtils
import org.springframework.orm.hibernate3.SessionHolder
import org.springframework.transaction.support.TransactionSynchronizationManager

/**
 * Utilities methods for working with hibernate session in gorm to bind and unbind sessions to the current thread
*/
class SessionBinderUtils {
	private static Logger log = LoggerFactory.getLogger(SessionBinderUtils)
	
	static boolean bindSession(SessionFactory sessionFactory) {
		SessionHolder sessionHolder = TransactionSynchronizationManager.getResource(sessionFactory)
		if(sessionHolder){
			if(sessionHolder.session.flushMode != FlushMode.MANUAL) {
				sessionHolder.getSession().flush()
			}
			return false
		} 
		else {
			def session = SessionFactoryUtils.getSession(sessionFactory, true)
			session.flushMode = FlushMode.AUTO
			TransactionSynchronizationManager.bindResource(sessionFactory, new SessionHolder(session))
			if (log.isDebugEnabled()) log.debug "bindSession was run on current thread"
			return true
		}
	}

	static void unbindSession(SessionFactory sessionFactory) {
		SessionHolder sessionHolder = TransactionSynchronizationManager.getResource(sessionFactory)
		if (sessionHolder && sessionHolder.session.flushMode != FlushMode.MANUAL) {
			sessionHolder.session.flush()
		}
		TransactionSynchronizationManager.unbindResource(sessionFactory)
		SessionFactoryUtils.releaseSession(sessionHolder.session,sessionFactory)
		if (log.isDebugEnabled()) log.debug "unbindSession was run on current thread"
	}

}