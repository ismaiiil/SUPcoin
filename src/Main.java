public class Main {
    public static void main(String[] args) {
        Thread discoveryThread = new Thread(DiscoveryThread.getInstance());
        discoveryThread.start();

        Thread client = new Thread(new ClientBroadcaster(2));
        client.start();
    }
}
