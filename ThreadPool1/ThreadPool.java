package ThreadPool1;

/**
 * 定义线程池接口
 */
public interface ThreadPool {

    void execute(Runnable runnable);
    int getInitSize();
    int getCoreSize();
    int getMaxSize();
    int getActiveCount();
    boolean isShutDown();
    void shutDown();

}
