package coordinator;

public class RetryUtil {
    public static void retryWithSleep(RunnableWithException action) {
        while (true) {
            try {
                action.run();
                break;
            } catch (Exception e) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt(); // Preserve interrupt status
                    throw new RuntimeException("Retry interrupted", ie);
                }
                System.out.println("Retrying due to exception: " + e.getMessage());
            }
        }
    }

    @FunctionalInterface
    public interface RunnableWithException {
        void run() throws Exception;
    }
}
