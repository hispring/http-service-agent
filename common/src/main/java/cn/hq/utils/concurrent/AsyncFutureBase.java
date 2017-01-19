package cn.hq.utils.concurrent;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AsyncFutureBase<TSource> implements AsyncFuture<TSource> {

	private static final int UNDONE = 0;
	private static final int SUCCESS = 1;
	private static final int ERROR = 2;

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	private TSource source;

	private volatile int state = UNDONE;
	private volatile Throwable exception;
	
	private volatile String errorCode;

	private CountDownLatch completedLatch = new CountDownLatch(1);

	private CopyOnWriteArrayList<AsyncFutureListener<TSource>> listeners = new CopyOnWriteArrayList<AsyncFutureListener<TSource>>();

	public AsyncFutureBase(TSource source) {
		this.source = source;
	}

	@Override
	public TSource getSource() {
		return source;
	}

	@Override
	public boolean isDone() {
		return state != UNDONE;
	}

	@Override
	public boolean isSuccess() {
		return state == SUCCESS;
	}

	@Override
	public Throwable getException() {
		return exception;
	}
	
	@Override
	public String getErrorCode() {
		return errorCode;
	}

	@Override
	public void await() throws InterruptedException {
		await(-1L, false);
	}

	@Override
	public boolean await(long timeoutMillis) throws InterruptedException {
		return await(timeoutMillis, false);
	}

	@Override
	public void awaitUninterruptibly() {
		try {
			await(-1L, false);
		} catch (InterruptedException e) {
			// swallow InterruptedException;
		}
	}

	@Override
	public boolean awaitUninterruptibly(long timeoutMillis) {
		try {
			return await(timeoutMillis, false);
		} catch (InterruptedException e) {
			// swallow InterruptedException;
			return false;
		}
	}

	private boolean await(long timeoutMillis, boolean interruptibly) throws InterruptedException {
		if (timeoutMillis < 0) {
			timeoutMillis = Long.MAX_VALUE;
		}
		long startTs = System.currentTimeMillis();
		long endTs = startTs;
		long leftTs = timeoutMillis;
		boolean ready = false;
		do {
			try {
				ready = completedLatch.await(leftTs, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				if (interruptibly) {
					throw e;
				}
			}
			if (ready) {
				return ready;
			}
			endTs = System.currentTimeMillis();
			leftTs = timeoutMillis - endTs + startTs;
			if (leftTs <= 0) {
				return isDone();
			}
		} while (true);
	}

	@Override
	public void addListener(AsyncFutureListener<TSource> listener) {
		if (listener == null) {
			throw new IllegalArgumentException("Null listener!");
		}
		listeners.add(listener);
	}

	protected void setSuccess() {
		state = SUCCESS;
		complete();
	}

	protected void setError(Throwable ex) {
		exception = ex;
		state = ERROR;
		complete();
	}
	
	protected void setError(String errorCode) {
		this.errorCode = errorCode;
		state = ERROR;
		complete();
	}

	private void complete() {
		completedLatch.countDown();
		// fire event;
		for (AsyncFutureListener<TSource> asyncFutureListener : listeners) {
			fireCompleted(asyncFutureListener);
		}
	}

	private void fireCompleted(AsyncFutureListener<TSource> asyncFutureListener) {
		try {
			asyncFutureListener.complete(this);
		} catch (Exception e) {
			logger.error("Error occurred on fire completed event to AsyncFutureListener["
					+ asyncFutureListener.getClass().getName() + "]!!!--" + e.getMessage(), e);
		}
	}
}
