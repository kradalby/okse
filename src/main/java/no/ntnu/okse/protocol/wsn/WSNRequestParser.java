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

/**
 * Created by Aleksander Skraastad (myth) on 3/19/15.
 * <p>
 * okse is licenced under the MIT licence.
 */
public class WSNRequestParser {

    private static Logger log;

    public WSNRequestParser() {
        log = Logger.getLogger(WSNRequestParser.class.getName());
    }

    public WSNInternalMessage parseMessage(WSNInternalMessage message, OutputStream streamToRequestor) {
        // TODO: Write method to parse and locate which service the message is destined for.

        // We set up the initial returnmessage as having no destination, so we can just return it
        // if we cannot locate where it should go, even though it might be syntactically correct.
        WSNInternalMessage returnMessage = new WSNInternalMessage(InternalMessage.STATUS_FAULT |
                InternalMessage.STATUS_FAULT_INVALID_DESTINATION, null);

        boolean foundRecipient = true;

        // Is it just a request message and has no content
        if ((message.statusCode & InternalMessage.STATUS_HAS_MESSAGE) == 0) {
            log.info("Forwarding request-message...");

            if (foundRecipient) {
                // returnMessage = someService.accept(message);
            } else {
                /*
                for (Service s: services) {
                    returnMessage = s.accept(message);
                    if (returnMessage.statusCode & STATUS_FAULT_INVALID_DESTINATION > 0) {
                        continue;
                    } else if (returnMessage.statusCode & STATUS_OK > 0) {
                        break;
                    } else if (returnMessage.statusCode & STATUS_FAULT_INTERNAL_ERROR > 0) {
                        break;
                    }
                    break;
                }
                 */
            }
        }

        // There is content that should be dealt with
        else {
            log.info("Forwarding message with content...");

            try {

                XMLParser.parse(message);

                try {

                    JAXBElement msg = (JAXBElement) message.getMessage();
                    Class messageClass = msg.getDeclaredType();

                    if (messageClass.equals(org.w3._2001._12.soap_envelope.Envelope.class)) {
                        message.setMessage(msg.getValue());
                    }
                } catch (ClassCastException classcastex) {
                    log.error("Failed to cast a message to a SOAP envelope");
                    return new WSNInternalMessage(InternalMessage.STATUS_FAULT | InternalMessage.STATUS_FAULT_INVALID_PAYLOAD, null);
                }
            } catch (JAXBException initialJaxbParsingException) {
                log.error("Parsing error: " + initialJaxbParsingException.getLinkedException().getMessage());

                try {
                    XMLParser.writeObjectToStream(Utilities.createSoapFault("Client", "Invalid formatted message"), streamToRequestor);
                    return new WSNInternalMessage(InternalMessage.STATUS_FAULT | InternalMessage.STATUS_FAULT_INVALID_PAYLOAD, null);
                } catch (JAXBException faultGeneratingJaxbException) {
                    log.error("Failed to generate SOAP fault message: " + faultGeneratingJaxbException.getMessage());
                    return new WSNInternalMessage(InternalMessage.STATUS_FAULT | InternalMessage.STATUS_FAULT_INTERNAL_ERROR, null);
                }
            }
        }

        // Reuse WSNInternalMessage object for efficiency
        message.statusCode = InternalMessage.STATUS_OK |
                InternalMessage.STATUS_HAS_MESSAGE | InternalMessage.STATUS_ENDPOINTREF_IS_SET;

        if (foundRecipient) {
            log.debug("Have recipient service, sending to recipient object");
            // returnMessage = someService.accept(message);
            log.debug("Recieved returnMessage from recipient object");
        } else {
            log.debug("Looking for service to send to...");
            /*
            for (ServiceConnection s: services) {
                log.debug("Attempting to forward request to " + s)
                returnMessage = s.accept(message);

                if((returnMessage.statusCode & STATUS_FAULT_INVALID_DESTINATION) > 0){
                    continue;
                } else if((returnMessage.statusCode & STATUS_OK) > 0){
                    break;
                } else if((returnMessage.statusCode & STATUS_FAULT_INTERNAL_ERROR) > 0){
                    break;
                } else if((returnMessage.statusCode & STATUS_EXCEPTION_SHOULD_BE_HANDLED) > 0){
                    break;
                }
            }
             */
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
                        return new WSNInternalMessage(InternalMessage.STATUS_OK, null);
                    } catch(Exception e) {
                        log.error("Casting the returnMessage to InputStream failed, " +
                                "even though someone set the MESSAGE_IS_INPUTSTREAM flag.");
                        return new WSNInternalMessage(InternalMessage.STATUS_FAULT | InternalMessage.STATUS_FAULT_INTERNAL_ERROR, null);
                    }
                } else {

                    Object messageToParse = wrapInJAXBAcceptedSoapEnvelope(returnMessage.getMessage());

                    /* Try to parse the object directly into the OutputStream passed in */
                    try {

                        XMLParser.writeObjectToStream(messageToParse, streamToRequestor);
                        return new WSNInternalMessage(InternalMessage.STATUS_OK, null);

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

                        return new WSNInternalMessage(InternalMessage.STATUS_FAULT | InternalMessage.STATUS_FAULT_INTERNAL_ERROR, null);
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

                log.debug("Exception thrown up the stack");

                try {

                    Utilities.attemptToParseException((Exception) returnMessage.getMessage(), streamToRequestor);
                    log.debug("Returning parsed error");
                    return new WSNInternalMessage(InternalMessage.STATUS_FAULT, null);

                } catch (IllegalArgumentException e) {

                    log.error("parseMessage(): Error not parseable, the error can not be a wsdl-specified one.");
                    return new WSNInternalMessage(InternalMessage.STATUS_FAULT | InternalMessage.STATUS_FAULT_INVALID_PAYLOAD, null);

                } catch (ClassCastException e) {

                    log.error("parseMessage(): The returned exception is not a subclass of Exception.");
                    return new WSNInternalMessage(InternalMessage.STATUS_FAULT | InternalMessage.STATUS_FAULT_INVALID_PAYLOAD, null);
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
            JAXBElement element = (JAXBElement)o;
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
     * @param message The message to be sent out
     */
    //TODO: Generate meaningful soap headers
    public WSNInternalMessage generateOutgoingMessage(WSNInternalMessage message) {
        Object messageContent = message.getMessage();

        if ((message.statusCode & InternalMessage.STATUS_HAS_MESSAGE) > 0) {

            /* Easy if it already is an inputstream */
            if ((message.statusCode & InternalMessage.STATUS_MESSAGE_IS_INPUTSTREAM) > 0) {

                try {
                    InputStream messageAsStream = (InputStream) messageContent;
                    message.setMessage(messageAsStream);

                    return message;

                } catch(ClassCastException castexception) {

                    log.error("Someone set the RETURNING_MESSAGE_IS_INPUTSTREAM when in fact it wasn't.");
                    log.trace(castexception.getStackTrace());

                    return new WSNInternalMessage(InternalMessage.STATUS_FAULT_INVALID_PAYLOAD | InternalMessage.STATUS_FAULT, null);
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


}