/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Norwegian Defence Research Establishment / NTNU
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package no.ntnu.okse.protocol.wsn;

import no.ntnu.okse.core.event.PublisherChangeEvent;
import no.ntnu.okse.core.event.listeners.PublisherChangeListener;
import no.ntnu.okse.core.subscription.Publisher;
import no.ntnu.okse.core.subscription.SubscriptionService;
import org.apache.log4j.Logger;
import org.ntnunotif.wsnu.base.util.RequestInformation;
import org.ntnunotif.wsnu.services.general.ExceptionUtilities;
import org.ntnunotif.wsnu.services.implementations.notificationbroker.AbstractNotificationBroker;
import org.ntnunotif.wsnu.services.implementations.publisherregistrationmanager.AbstractPublisherRegistrationManager;
import org.oasis_open.docs.wsn.br_2.DestroyRegistration;
import org.oasis_open.docs.wsn.br_2.DestroyRegistrationResponse;
import org.oasis_open.docs.wsn.brw_2.ResourceNotDestroyedFault;
import org.oasis_open.docs.wsrf.rw_2.ResourceUnknownFault;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.XmlSeeAlso;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Aleksander Skraastad (myth) on 4/20/15.
 * <p>
 * okse is licenced under the MIT licence.
 */
@WebService(targetNamespace = "http://docs.oasis-open.org/wsn/brw-2", name = "PublisherRegistrationManager")
@XmlSeeAlso({org.oasis_open.docs.wsn.t_1.ObjectFactory.class, org.oasis_open.docs.wsn.br_2.ObjectFactory.class, org.oasis_open.docs.wsrf.r_2.ObjectFactory.class, org.oasis_open.docs.wsrf.bf_2.ObjectFactory.class, org.oasis_open.docs.wsn.b_2.ObjectFactory.class})
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
public class WSNRegistrationManager extends AbstractPublisherRegistrationManager implements PublisherChangeListener {

    public static final String WSN_PUBLISHER_TOKEN = "wsn-publisherkey";

    private static Logger log;
    private SubscriptionService _subscriptionService = null;
    private HashMap<String, Publisher> localPublisherMap;
    private HashMap<String, AbstractNotificationBroker.PublisherHandle> localPublisherHandle;

    /**
     * Empty constructor that initializes the log and local mappings
     */
    public WSNRegistrationManager() {
        log = Logger.getLogger(WSNRegistrationManager.class.getName());
        _subscriptionService = null;
        localPublisherMap = new HashMap<>();
        localPublisherHandle = new HashMap<>();
    }

    /**
     * Helper method that initializes this proxy manager with the reference to the OKSE SubscriptionService
     *
     * @param service : SubscriptionService
     */
    public void initCoreSubscriptionService(SubscriptionService service) {
        _subscriptionService = service;
    }

    /**
     * Check if the registrationKey already exists
     *
     * @param registrationKey The registrationKey to check
     * @return True if the registrationKey exists, false otherwise
     */
    public boolean keyExists(String registrationKey) {
        return localPublisherMap.containsKey(registrationKey);
    }

    /**
     * Retrieves a collection of all the registered publisher reference keys
     *
     * @return A collection of publisherReference keys
     */
    public Collection<String> getAllPublishers() {
        return localPublisherMap.keySet();
    }

    /**
     * The main method to add a publisher to the managing system, and the OKSE SubscriptionService
     *
     * @param p         The Publisher object to be added
     * @param pubHandle The WS-Nu PublisherHandle connected to the new Publisher
     */
    public void addPublisher(Publisher p, AbstractNotificationBroker.PublisherHandle pubHandle) {
        _subscriptionService.addPublisher(p);
        localPublisherMap.put(p.getAttribute(WSN_PUBLISHER_TOKEN), p);
        localPublisherHandle.put(p.getAttribute(WSN_PUBLISHER_TOKEN), pubHandle);
    }

    /**
     * Required method from the WS-Nu abstract class, IS NOT USED
     *
     * @param s The Publisher key
     * @param l The TerminationTime in milliseconds since unix epoch
     */
    @Override
    @Deprecated
    public void addPublisher(String s, long l) {
        log.warn("WS-Nu default addPublisher with hashKey and terminationTime called. " +
                "Locate offending method and change to addSubscriber(Publisher p, PublisherHandle pubHandle)");
    }

    /**
     * Get the PublisherHandle of a publisher if it exists in the local publisherhandle map.
     *
     * @param publisher The WSN publisher key associated with this publisher
     * @return The PublisherHandle of the requested publisher if it exists, null otherwise
     */
    public AbstractNotificationBroker.PublisherHandle getPublisherHandle(String publisher) {
        if (localPublisherHandle.containsKey(publisher)) return localPublisherHandle.get(publisher);
        return null;
    }

    /**
     * Get the PublisherHandle of a publisher if it exists in the local publisherhandle map.
     * This method attempts to extract the publisherkey from the Publisher's attribute set
     * and delegates the rest of the work to the String based method with the same name
     *
     * @param publisher The Publisher object we want the PublisherHandle of
     * @return The PublisherHandle of the requested publisher if it exists, null otherwise
     */
    public AbstractNotificationBroker.PublisherHandle getPublisherHandle(Publisher publisher) {
        return getPublisherHandle(publisher.getAttribute(WSN_PUBLISHER_TOKEN));
    }

    /**
     * This method uses the OKSE core subscription service to remove the subscriber,
     * and the awaits the callback from the publisherchangeevent to verify, and then remove from local maps
     *
     * @param p The WS-Nu publisherRegistrationKey representing the publisher to be removed from the manager
     */
    @Override
    public void removePublisher(String p) {
        if (hasPublisher(p)) {
            _subscriptionService.removePublisher(localPublisherMap.get(p));
        }
    }

    /**
     * Check wether or not we have a registered publisher with the provided registrationKey
     *
     * @param p The registrationKey to check existance of
     * @return True if the manager knows the provided registrationKey, false otherwise
     */
    public boolean hasPublisher(String p) {
        return localPublisherMap.containsKey(p);
    }

    /**
     * Generic update method intended to purge expired publishers if they have a terminationTime set
     */
    @Override
    public void update() {
        // This should maybe proxy the periodic update event removing expired subs
    }

    /* Begin Public web service methods */

    /**
     * This method implements the {@link org.oasis_open.docs.wsn.brw_2.PublisherRegistrationManager}'s DestroyRegistration.
     * <p>
     * The method conforms to the standard. Thus, any specifics can be found at
     * <a href="http://docs.oasis-open.org/wsn/wsn-ws_brokered_notification-1.3-spec-os.htm#_Toc133294203"></a>
     * <p>
     * Note that the subscription-reference is contained in the request-url.
     *
     * @param destroyRegistrationRequest The parsed object.
     * @return The DestryoRegistrationtResponse if everything went fine.
     * @throws ResourceNotDestroyedFault This is thrown if either the publisher-reference is ill-formatted,
     *                                   or does not represent an existing publisher registration
     * @throws ResourceUnknownFault      As of 0.3 this is never thrown as WS-Resources is not implemented
     */
    @Override
    @WebResult(name = "DestroyRegistrationResponse", targetNamespace = "http://docs.oasis-open.org/wsn/br-2", partName = "DestroyRegistrationResponse")
    @WebMethod(operationName = "DestroyRegistration")
    public DestroyRegistrationResponse destroyRegistration
    (
            @WebParam(partName = "DestroyRegistrationRequest", name = "DestroyRegistration", targetNamespace = "http://docs.oasis-open.org/wsn/br-2")
            DestroyRegistration destroyRegistrationRequest
    ) throws ResourceNotDestroyedFault, ResourceUnknownFault {
        log.debug("Received DestroyRegistration request");
        RequestInformation requestInformation = connection.getRequestInformation();

        // Iterate over the url parameters to extract the publisherReference
        for (Map.Entry<String, String[]> entry : requestInformation.getParameters().entrySet()) {
            if (!entry.getKey().equals(WSN_PUBLISHER_TOKEN)) {
                continue;
            }

            String pubRef;
            // Check if there have been more than one url-param (which does not conform to the standard)
            if (entry.getValue().length > 1) {

                log.debug("Found more than one param, iterating...");
                pubRef = entry.getValue()[0];

                // Check if we have the publisher in our local map
                if (localPublisherMap.containsKey(pubRef)) {
                    // If we do, remove the Publisher object in the local map from the subscription service
                    // The final removal of the publisherKey from the local map will happen in the
                    // SubscriptionChangeListener callback.
                    log.debug("Found subscriptionReference in local map, passing Subscriber object to SubscriptionService");
                    _subscriptionService.removePublisher(localPublisherMap.get(pubRef));
                    // Return the response
                    return new DestroyRegistrationResponse();
                }

                log.debug("Malformed subscription parameter");
                ExceptionUtilities.throwResourceNotDestroyed("en", "Ill-formated subscription-parameter");

            } else if (entry.getValue().length == 0) {
                log.debug("Missing subscription parameter value");
                ExceptionUtilities.throwResourceNotDestroyed("en", "Subscription-parameter is missing value");
            }

            // Extract and store the publisherReference
            pubRef = entry.getValue()[0];

            /* The publisher is not recognized */
            if (!localPublisherMap.containsKey(pubRef)) {
                log.debug("Publisher not found");
                log.debug("Expected: " + pubRef);
                ExceptionUtilities.throwResourceNotDestroyed("en", "Subscription not found.");
            }

            // Pass the removal operation to the subscriptionService
            // and updating of the local maps will happen on the listener callback
            _subscriptionService.removePublisher(localPublisherMap.get(pubRef));

            // Return the response
            return new DestroyRegistrationResponse();
        }
        ExceptionUtilities.throwResourceNotDestroyed("en", "The registration was not found as any parameter" +
                " in the request-uri. Please send a request on the form: " +
                "\"http://urlofthis.domain/webservice/?" + WSN_PUBLISHER_TOKEN + "=registrationkey");

        return null;
    }

    /* Begin listener support methods */

    /**
     * Event listener support for publisher change events from the OKSE SubscriptionService
     *
     * @param e A PublisherChangeEvent containing the type of event and an associated Publisher object
     */
    @Override
    public void publisherChanged(PublisherChangeEvent e) {
        // Is this event on WSN publisher?
        if (e.getData().getOriginProtocol().equals(WSNotificationServer.getInstance().getProtocolServerType())) {
            // Do we have an unregister?
            if (e.getType().equals(PublisherChangeEvent.Type.UNREGISTER)) {
                // Remove the publisher from maps using its WSN_PUBLISHER_TOKEN hash
                localPublisherMap.remove(e.getData().getAttribute(WSN_PUBLISHER_TOKEN));
                localPublisherHandle.remove(e.getData().getAttribute(WSN_PUBLISHER_TOKEN));
            }
        }
    }
}
