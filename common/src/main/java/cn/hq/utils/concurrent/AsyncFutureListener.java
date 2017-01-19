package cn.hq.utils.concurrent;

public interface AsyncFutureListener<TSource> {
	
	public void complete(AsyncFuture<TSource> future);
	
}
