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

import com.google.common.io.ByteStreams;
import org.apache.log4j.Logger;
import org.ntnunotif.wsnu.base.internal.Hub;
import org.ntnunotif.wsnu.base.internal.ServiceConnection;
import org.ntnunotif.wsnu.base.net.XMLParser;
import org.ntnunotif.wsnu.base.util.InternalMessage;
import org.ntnunotif.wsnu.base.util.Utilities;
import org.xmlsoap.schemas.soap.envelope.Body;
import org.xmlsoap.schemas.soap.envelope.Envelope;
import org.xmlsoap.schemas.soap.envelope.Header;
import org.xmlsoap.schemas.soap.envelope.ObjectFactory;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;

/**
 * Created by Aleksander Skraastad (myth) on 3/19/15.
 * <p>
 * okse is licenced under the MIT licence.
 */
public class WSNRequestParser implements Hub {

    private static Logger log;
    private static WSNotificationServer _protocolServer;

    public WSNRequestParser(WSNotificationServer server) {
        log = Logger.getLogger(WSNRequestParser.class.getName());
        _protocolServer = server;
    }

    public InternalMessage parseMessage(InternalMessage message, OutputStream streamToRequestor) {

        ServiceConnection recipient = findRecipientService(message.getRequestInformation().getRequestURL());

        // We set up the initial returnmessage as having no destination, so we can just return it
        // if we cannot locate where it should go, even though it might be syntactically correct.
        InternalMessage returnMessage = new InternalMessage(InternalMessage.STATUS_FAULT |
                InternalMessage.STATUS_FAULT_INVALID_DESTINATION, null);

        boolean foundRecipient = recipient != null ? true : false;

        // Is it just a request and has no content
        if ((message.statusCode & InternalMessage.STATUS_HAS_MESSAGE) == 0) {
            log.info("Forwarding request...");

            if (foundRecipient) {
                log.info("We found a recipient: " + recipient);
                returnMessage = recipient.acceptMessage(message);
            } else {
                log.info("Did not immediately find a recipient... Looping through services...");
                for (ServiceConnection s : _protocolServer.getServices()) {
                    returnMessage = s.acceptMessage(message);
                    if ((returnMessage.statusCode & InternalMessage.STATUS_FAULT_INVALID_DESTINATION) > 0) {
                        log.info("Found an invalid destination: " + s);
                        continue;
                    } else if ((returnMessage.statusCode & InternalMessage.STATUS_OK) > 0) {
                        log.info("Hooray, found a service that accepted the message: " + s);
                        break;
                    } else if ((returnMessage.statusCode & InternalMessage.STATUS_FAULT_INTERNAL_ERROR) > 0) {
                        log.info("There was an error while trying to get service " + s + " to accept the message.");
                        break;
                    }
                    break;
                }
            }
        }

        // There is content that should be dealt with
        else {
            log.debug("Forwarding message with content...");

            try {

                XMLParser.parse(message);
                log.debug("Successfully parsed message.");

                try {

                    log.debug("Attempting to cast to JAXBElement");
                    JAXBElement msg = (JAXBElement) message.getMessage();
                    Class messageClass = msg.getDeclaredType();

                    if (messageClass.equals(org.w3._2001._12.soap_envelope.Envelope.class)) {
                        message.setMessage(msg.getValue());
                    } else if (messageClass.equals(org.xmlsoap.schemas.soap.envelope.Envelope.class)) {
                        message.setMessage(msg.getValue());
                    }
                } catch (ClassCastException classcastex) {
                    log.error("Failed to cast a message to a SOAP envelope");
                    return new InternalMessage(InternalMessage.STATUS_FAULT | InternalMessage.STATUS_FAULT_INVALID_PAYLOAD, null);
                }
            } catch (JAXBException initialJaxbParsingException) {
                if (initialJaxbParsingException.getLinkedException() != null) {
                    log.error("Parsing error: " + initialJaxbParsingException.getLinkedException().getMessage());
                } else {
                    log.error("Parsing error: " + initialJaxbParsingException);
                }

                try {
                    XMLParser.writeObjectToStream(Utilities.createSoapFault("Client", "Invalid formatted message"), streamToRequestor);
                    return new InternalMessage(InternalMessage.STATUS_FAULT | InternalMessage.STATUS_FAULT_INVALID_PAYLOAD, null);
                } catch (JAXBException faultGeneratingJaxbException) {
                    log.error("Failed to generate SOAP fault message: " + faultGeneratingJaxbException.getMessage());
                    return new InternalMessage(InternalMessage.STATUS_FAULT | InternalMessage.STATUS_FAULT_INTERNAL_ERROR, null);
                }
            }
        }

        // Reuse WSNInternalMessage object for efficiency
        message.statusCode = InternalMessage.STATUS_OK |
                InternalMessage.STATUS_HAS_MESSAGE | InternalMessage.STATUS_ENDPOINTREF_IS_SET;

        if (foundRecipient) {
            log.debug("Have recipient service, sending to recipient object");
            returnMessage = recipient.acceptMessage(message);
            log.debug("Received returnMessage from recipient object");
        } else {
            log.debug("Looking for service to send to...");
            for (ServiceConnection s : _protocolServer.getServices()) {
                log.debug("Attempting to forward request to " + s);
                returnMessage = s.acceptMessage(message);

                if ((returnMessage.statusCode & InternalMessage.STATUS_FAULT_INVALID_DESTINATION) > 0) {
                    continue;
                } else if ((returnMessage.statusCode & InternalMessage.STATUS_OK) > 0) {
                    break;
                } else if ((returnMessage.statusCode & InternalMessage.STATUS_FAULT_INTERNAL_ERROR) > 0) {
                    break;
                } else if ((returnMessage.statusCode & InternalMessage.STATUS_EXCEPTION_SHOULD_BE_HANDLED) > 0) {
                    break;
                }
            }
        }

        /* Everything is processed properly, and we can figure out what to return */
        if ((returnMessage.statusCode & InternalMessage.STATUS_OK) > 0) {
            /* If we have a message we should try and convert it to an inputstream before returning
            * Notably the ApplicationServer does accept other form of messages, but it is more logical to convert
            * it at this point */

            // Do we have a message to handle?
            if ((returnMessage.statusCode & InternalMessage.STATUS_HAS_MESSAGE) > 0) {

                log.debug("Returning message...");
                // Is it an InputStream?
                if ((returnMessage.statusCode & InternalMessage.STATUS_MESSAGE_IS_INPUTSTREAM) > 0) {
                    try {
                        ByteStreams.copy((InputStream) returnMessage.getMessage(), streamToRequestor);
                        return new InternalMessage(InternalMessage.STATUS_OK, null);
                    } catch (Exception e) {
                        log.error("Casting the returnMessage to InputStream failed, " +
                                "even though someone set the MESSAGE_IS_INPUTSTREAM flag.");
                        return new InternalMessage(InternalMessage.STATUS_FAULT | InternalMessage.STATUS_FAULT_INTERNAL_ERROR, null);
                    }
                } else {

                    Object messageToParse = wrapInJAXBAcceptedSoapEnvelope(returnMessage.getMessage());

                    /* Try to parse the object directly into the OutputStream passed in */
                    try {

                        XMLParser.writeObjectToStream(messageToParse, streamToRequestor);
                        return new InternalMessage(InternalMessage.STATUS_OK, null);

                    /* This was not do-able */
                    } catch (JAXBException e) {

                        String faultMessage = e.getClass().getName();
                        faultMessage += e.getMessage() == null ? "" : e.getMessage();
                        Throwable cause = e.getCause();

                        while (cause != null) {
                            faultMessage += "\n\tCaused by: ";
                            faultMessage += cause.getClass().getName() + (cause.getMessage() == null ? "" : cause.getMessage());
                            cause = cause.getCause();
                        }

                        if (e.getLinkedException() != null) {
                            faultMessage += "\n\tWith linked exception:" + e.getLinkedException().getClass().getName();
                            faultMessage += e.getLinkedException().getMessage() == null ? "" : e.getLinkedException().getMessage();
                        }

                        log.error("Unable to marshal returnMessage. Consider converting the " +
                                "message-payload at an earlier point. Reason given:\n\t" + faultMessage);

                        return new InternalMessage(InternalMessage.STATUS_FAULT | InternalMessage.STATUS_FAULT_INTERNAL_ERROR, null);
                    }
                }

            /* We have no message and can just return */
            } else {
                return returnMessage;
            }
        /* We have a fault of some sort, figure out what it is and create proper response */
        } else {

            /* THere is an exception that should be handled */
            if ((returnMessage.statusCode & InternalMessage.STATUS_EXCEPTION_SHOULD_BE_HANDLED) > 0) {

                log.error("Exception thrown up the stack");

                try {

                    Utilities.attemptToParseException((Exception) returnMessage.getMessage(), streamToRequestor);
                    log.debug("Returning parsed error");
                    return new InternalMessage(InternalMessage.STATUS_FAULT, null);

                } catch (IllegalArgumentException e) {
                    log.error("parseMessage(): Error not parseable, the error can not be a wsdl-specified one.");
                    return new InternalMessage(InternalMessage.STATUS_FAULT | InternalMessage.STATUS_FAULT_INVALID_PAYLOAD, null);

                } catch (ClassCastException e) {

                    log.error("parseMessage(): The returned exception is not a subclass of Exception.");
                    return new InternalMessage(InternalMessage.STATUS_FAULT | InternalMessage.STATUS_FAULT_INVALID_PAYLOAD, null);
                }
            /* We have no unhandled exceptions at least */
            } else {
                // Is it an invalid or non-existant endpoint reference requested?
                if ((returnMessage.statusCode & InternalMessage.STATUS_FAULT_INVALID_DESTINATION) > 0) {

                    try {

                        XMLParser.writeObjectToStream(Utilities.createSoapFault("Client", "The message did not contain any information relevant to any web service at this address"), streamToRequestor);
                        return returnMessage;

                    } catch (JAXBException e) {

                        log.error("Something went horribly wrong while creating soap fault.");
                        log.trace(e.getStackTrace());
                    }
                    // If all else fails, generate a generic soap fault message using the Server fault-type
                } else {

                    try {

                        XMLParser.writeObjectToStream(Utilities.createSoapFault("Server", "There was an unexpected error while processing the request."), streamToRequestor);
                        return returnMessage;

                    } catch (JAXBException e) {

                        log.error("Something went horribly wrong while creating soap fault.");
                        log.trace(e.getStackTrace());

                    }
                }
            }
        }

        // We should never reach this section (Just to remove missing return statement warnings).
        returnMessage.statusCode = InternalMessage.STATUS_FAULT_INTERNAL_ERROR;

        return returnMessage;
    }


    /**
     * Takes an object and wraps it in an JAXBElement with declared type Envelope. If it is already a JAXBElement with
     * this declared type, it just returns it. If it is an accepted envelope, it creates the corresponding JAXBElement
     * to wrap it in. If it is something else, it wraps it in an Envelope from namespace
     * http://schemas.xmlsoap.org/soap/envelope/
     *
     * @param o the <code>Object</code> to wrap
     * @return the wrapped JAXBElement
     */
    private Object wrapInJAXBAcceptedSoapEnvelope(Object o) {

        // Check if this is already correct type
        if (o instanceof JAXBElement) {
            JAXBElement element = (JAXBElement) o;
            if (element.getDeclaredType() == Envelope.class ||
                    element.getDeclaredType() == org.w3._2001._12.soap_envelope.Envelope.class)
                return o;
        }

        if (o != null) {
            // Check if it is not already wrapped in an envelope, if so wrap it
            if (!((o instanceof org.w3._2001._12.soap_envelope.Envelope) || o instanceof Envelope)) {
                ObjectFactory factory = new ObjectFactory();
                Envelope env = factory.createEnvelope();
                Body body = factory.createBody();
                body.getAny().add(o);
                env.setBody(body);
                return factory.createEnvelope(env);
            } else if (o instanceof org.w3._2001._12.soap_envelope.Envelope) {
                org.w3._2001._12.soap_envelope.ObjectFactory factory = new org.w3._2001._12.soap_envelope.ObjectFactory();
                return factory.createEnvelope((org.w3._2001._12.soap_envelope.Envelope) o);
            } else if (o instanceof Envelope) {
                ObjectFactory factory = new ObjectFactory();
                return factory.createEnvelope((Envelope) o);
            }
        }

        return null;
    }

    /**
     * Function to accept a message from a local service, and forward it out into the internet.
     * <p>
     *
     * @param message The message to be sent out
     * @return An InternalMessage ready to be processed and sent across the wire.
     */
    public InternalMessage generateOutgoingMessage(InternalMessage message) {
        Object messageContent = message.getMessage();

        if ((message.statusCode & InternalMessage.STATUS_HAS_MESSAGE) > 0) {

            /* Easy if it already is an inputstream */
            if ((message.statusCode & InternalMessage.STATUS_MESSAGE_IS_INPUTSTREAM) > 0) {

                try {
                    InputStream messageAsStream = (InputStream) messageContent;
                    message.setMessage(messageAsStream);

                    return message;

                } catch (ClassCastException castexception) {

                    log.error("Someone set the RETURNING_MESSAGE_IS_INPUTSTREAM when in fact it wasn't.");
                    log.trace(castexception.getStackTrace());

                    return new InternalMessage(InternalMessage.STATUS_FAULT_INVALID_PAYLOAD | InternalMessage.STATUS_FAULT, null);
                }

            } else {

                ObjectFactory factory = new ObjectFactory();
                Envelope envelope = new Envelope();
                Header header = new Header();
                Body body = new Body();

                body.getAny().add(messageContent);
                envelope.setBody(body);
                envelope.setHeader(header);

                InputStream messageAsStream = Utilities.convertUnknownToInputStream(factory.createEnvelope(envelope));
                message.setMessage(messageAsStream);
                message.statusCode = InternalMessage.STATUS_OK |
                        InternalMessage.STATUS_HAS_MESSAGE |
                        InternalMessage.STATUS_MESSAGE_IS_INPUTSTREAM;

                return message;
            }
        /* We have no content, must be a pure request */
        } else {
            return message;
        }
    }

    /**
     * Try to locate a specific ServiceConnection based on the endpoint reference.
     * <p>
     *
     * @param endpointReference: A string representing the endpoint reference.
     * @return The ServiceConnection that matches the specified endpoint reference argument, null if not found.
     */
    public ServiceConnection findRecipientService(String endpointReference) {
        if (endpointReference == null || endpointReference.equals(""))
            return null;

        for (ServiceConnection connection : _protocolServer.getServices()) {

            // Ensure we have connection with endpoint
            if (connection == null || connection.getServiceEndpoint() == null) {
                continue;
            }

            // Try to match the ip in one of two ways
            // Is this first one strictly necessary?
            if (endpointReference.matches("^/?" + connection.getServiceEndpoint().replaceAll("^" + WSNotificationServer.getInstance().getURI(), "") + "(.*)?")) {
                return connection;
            }

            if (endpointReference.matches("/?" + Utilities.stripUrlOfProtocolAndHost(connection.getServiceEndpoint()) + "(.*)") ||
                    ("/" + endpointReference).matches("/?" + Utilities.stripUrlOfProtocolAndHost(connection.getServiceEndpoint()) + "(.*)")) {
                return connection;
            }

        }
        log.debug("Found no matching connection for URL: " + endpointReference);
        return null;
    }

    // HUB OVERRIDES

    @Override
    public InternalMessage acceptNetMessage(InternalMessage internalMessage, OutputStream outputStream) {
        return this.parseMessage(internalMessage, outputStream);
    }

    @Override
    public InternalMessage acceptLocalMessage(InternalMessage internalMessage) {
        return _protocolServer.sendMessage(this.generateOutgoingMessage(internalMessage));
    }

    @Override
    public String getInetAdress() {
        return WSNotificationServer.getInstance().getURI();
    }

    @Override
    public void registerService(ServiceConnection serviceConnection) {
        _protocolServer.registerService(serviceConnection);
    }

    @Override
    public void removeService(ServiceConnection serviceConnection) {
        _protocolServer.removeService(serviceConnection);
    }

    @Override
    public boolean isServiceRegistered(ServiceConnection serviceConnection) {
        return _protocolServer.getServices().contains(serviceConnection);
    }

    @Override
    public Collection<ServiceConnection> getServices() {
        return this._protocolServer.getServices();
    }
}
