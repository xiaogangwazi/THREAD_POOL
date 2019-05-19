package ThreadPool1;

public class ThreadTask {
    Thread thread;
    InternalTask internalTask;
    public ThreadTask(Thread thread ,InternalTask internalTask){
        this.internalTask=internalTask;
        this.thread=thread;
    }
}
