package grails.plugin.executor
import org.hibernate.SessionFactory
import java.util.concurrent.Callable


/**
* A callable that binds a session  and unbinds after its done
*/
public class SessionBoundCallable<T> implements Callable<T> {
	private final Callable<T> task;
	private final SessionFactory sessionFactory; 
	
	public SessionBoundCallable(Callable task,SessionFactory sessionFactory) {
		if (task == null || sessionFactory == null) throw new NullPointerException();
		this.task = task;
		this.sessionFactory = sessionFactory;
	}
	public T call() {
		SessionBinderUtils.bindSession(sessionFactory);
		T retVal = null;
		try {
			retVal = task.call();
		} finally{
			SessionBinderUtils.unbindSession(sessionFactory);
		}
		return retVal;
	}
}
