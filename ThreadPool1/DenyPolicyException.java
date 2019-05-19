package ThreadPool1;

public class DenyPolicyException  extends RuntimeException {
    public DenyPolicyException(String message){
        super(message);
    }
}
