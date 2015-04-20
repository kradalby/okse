package no.ntnu.okse;

        import org.ntnunotif.wsnu.base.internal.SoapForwardingHub;
        import org.ntnunotif.wsnu.services.implementations.notificationbroker.NotificationBrokerImpl;
        import org.ntnunotif.wsnu.services.implementations.publisherregistrationmanager.SimplePublisherRegistrationManager;
        import org.ntnunotif.wsnu.services.implementations.subscriptionmanager.SimpleSubscriptionManager;

/**
 * Example of a NotificationBroker with a SubscriptionManager and a PublisherRegistrationManager
 */
public class BrokerWithSubscriptionManagerAndPublisherRegistrationManager {

    private NotificationBrokerImpl broker;
    private SimpleSubscriptionManager subscriptionManager;
    private SimplePublisherRegistrationManager publisherRegistrationManager;

    private final String brokerEndpoint = "myBroker";
    private final String subscriptionManagerEndpoint = "mySubscriptionManager";
    private final String publisherRegistrationManagerEndpoint = "publisherRegistrationManagerEndpoint";

    private SoapForwardingHub myHub;

    /**
     * We can do everything needed in the constructor
     */
    public BrokerWithSubscriptionManagerAndPublisherRegistrationManager() {

        // Instantiate the Web Services
        broker = new NotificationBrokerImpl();
        subscriptionManager = new SimpleSubscriptionManager();
        publisherRegistrationManager = new SimplePublisherRegistrationManager();

        // Quick build the Web services, thus initializing the entire system
        myHub = broker.quickBuild(brokerEndpoint);
        subscriptionManager.quickBuild(subscriptionManagerEndpoint, myHub);
        publisherRegistrationManager.quickBuild(publisherRegistrationManagerEndpoint, myHub);

        // This is the crucial part, we need to add both the subscription manager and the publisher registration manager
        // to the broker. This is necessary to allow the Broker to react to changes in the subscriptions/registrations occurring
        // at the subscription manager and publisher registration manager
        broker.setSubscriptionManager(subscriptionManager);
        broker.setRegistrationManager(publisherRegistrationManager);
    }
}