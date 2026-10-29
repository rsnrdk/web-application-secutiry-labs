package rsnrdk.websec.async;

/**
 * Stands in for the network round trip of a real external system (SMTP server, pricing API, warehouse API).
 */
final class SimulatedLatency {

    private SimulatedLatency() {
    }

    static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for external system", e);
        }
    }
}
