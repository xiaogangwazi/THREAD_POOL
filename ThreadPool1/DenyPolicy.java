package ThreadPool1;

/**
 * 定义拒绝策略
 * 实现三种策略
 * 第一种直接丢掉任务
 * 第二种抛出异常
 * 第三种将任务由提供者的线程处理
 */
public interface DenyPolicy {

    public void reject(ThreadPool threadPool,Runnable runnable);


    class DisCardDenyPolicy implements DenyPolicy{

        @Override
        public void reject(ThreadPool threadPool,Runnable runnable) {

        }
    }
    class AbortDenyPolicy implements  DenyPolicy{

        @Override
        public void reject(ThreadPool threadPool, Runnable runnable) {
            new DenyPolicyException("the runnable "+runnable+"will be abort");
        }
    }
    class  RunnerDenyPolicy implements  DenyPolicy{

        @Override
        public void reject(ThreadPool threadPool, Runnable runnable) {
            if(!threadPool.isShutDown()){
                runnable.run();
            }

        }
    }
}
