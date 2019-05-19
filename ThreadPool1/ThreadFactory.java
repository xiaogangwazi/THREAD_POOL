package ThreadPool1;

public interface ThreadFactory {
    Thread createThread(Runnable runnable);
}
