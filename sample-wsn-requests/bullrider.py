# -*- encoding: utf-8 -*-

import sys
import time
import requests
from random import shuffle

DEBUG = True
INTERVAL = 1
RUNS = 10
SOAP_HEADER = "application/soap+xml;charset=utf-8"
MODES = {
    "full": "All available",
    "notify": "Notification",
    "subscribe": "Subscription",
    "register": "PublisherRegistration",
    "getcurrent": "GetCurrentMessage",
    "renew": "RenewSubscription",
    "pause": "PauseSubscription",
    "resume": "ResumeSubscription",
}

RANDOM_WORDS = [
    "why",
    "are",
    "snakes",
    "people",
    "from",
    "orderly",
    "beer",
    "goose",
    "travel",
]

NOTIFY = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<s:Envelope xmlns:ns2="http://www.w3.org/2001/12/soap-envelope" xmlns:ns3="http://docs.oasis-open.org/wsrf/bf-2" xmlns:wsa="http://www.w3.org/2005/08/addressing" xmlns:wsnt="http://docs.oasis-open.org/wsn/b-2" xmlns:ns6="http://docs.oasis-open.org/wsn/t-1" xmlns:ns7="http://docs.oasis-open.org/wsn/br-2" xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ns9="http://docs.oasis-open.org/wsrf/r-2">
<s:Header>
<wsa:Action>http://docs.oasis-open.org/wsn/bw-2/NotificationConsumer/Notify</wsa:Action>
</s:Header>
<s:Body>
<wsnt:Notify>
<wsnt:NotificationMessage>
<wsnt:Topic Dialect="http://docs.oasis-open.org/wsn/t-1/TopicExpression/Concrete">%s</wsnt:Topic>
<wsnt:Message><npex:NotifyContent xmlns:npex="http://brute.force/test/">%s</npex:NotifyContent></wsnt:Message>
</wsnt:NotificationMessage>
</wsnt:Notify>
</s:Body>
</s:Envelope>"""

SUBSCRIBE = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<ns6:Envelope xmlns:ns2="http://www.w3.org/2005/08/addressing"
xmlns:ns3="http://docs.oasis-open.org/wsn/b-2"
xmlns:ns4="http://docs.oasis-open.org/wsn/t-1"
xmlns:ns5="http://docs.oasis-open.org/wsrf/bf-2"
xmlns:ns6="http://schemas.xmlsoap.org/soap/envelope/">
<ns6:Header>
<ns2:Action>http://docs.oasis-open.org/wsn/bw-2/NotificationProducer/SubscribeRequest</ns2:Action>
</ns6:Header>
<ns6:Body>
<ns3:Subscribe>
<ns3:ConsumerReference><ns2:Address>%s</ns2:Address></ns3:ConsumerReference>
<ns3:Filter><ns3:TopicExpression Dialect="http://docs.oasis-open.org/wsn/t-1/TopicExpression/Concrete">%s</ns3:TopicExpression></ns3:Filter>
</ns3:Subscribe>
</ns6:Body>
</ns6:Envelope>
"""

REGISTER = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<s:Envelope xmlns:wsa="http://www.w3.org/2005/08/addressing"
xmlns:wsn-b="http://docs.oasis-open.org/wsn/b-2"
xmlns:wsn-br="http://docs.oasis-open.org/wsn/br-2"
xmlns:wsn-bw="http://docs.oasis-open.org/wsn/bw-2"
xmlns:wsn-brw="http://docs.oasis-open.org/wsn/brw-2"
xmlns:wsrf-bf="http://docs.oasis-open.org/wsrf/bf-2"
xmlns:wsrf-bfw="http://docs.oasis-open.org/wsrf/bfw-2"
xmlns:s="http://schemas.xmlsoap.org/soap/envelope/">
<s:Header><wsa:Action>http://docs.oasis-open.org/wsn/brw-2/RegisterPublisher/RegisterPublisherRequest</wsa:Action>
</s:Header>
<s:Body>
<wsn-br:RegisterPublisher><wsn-br:PublisherReference><wsa:Address>%s</wsa:Address></wsn-br:PublisherReference>
<wsn-br:Topic Dialect="http://docs.oasis-open.org/wsn/t-1/TopicExpression/Concrete">%s</wsn-br:Topic>
<wsn-br:Demand>false</wsn-br:Demand>
<wsn-br:InitialTerminationTime>2016-01-01T00:00:00.00000Z</wsn-br:InitialTerminationTime>
</wsn-br:RegisterPublisher>
</s:Body>
</s:Envelope>
"""

GET_CURRENT_MESSAGE = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<s:Envelope xmlns:wsa="http://www.w3.org/2005/08/addressing"
xmlns:wsnt="http://docs.oasis-open.org/wsn/b-2"
xmlns:wsn-br="http://docs.oasis-open.org/wsn/br-2"
xmlns:wsn-bw="http://docs.oasis-open.org/wsn/bw-2"
xmlns:wsn-brw="http://docs.oasis-open.org/wsn/brw-2"
xmlns:wsrf-bf="http://docs.oasis-open.org/wsrf/bf-2"
xmlns:wsrf-bfw="http://docs.oasis-open.org/wsrf/bfw-2"
xmlns:s="http://schemas.xmlsoap.org/soap/envelope/">
<s:Header><wsa:Action>http://docs.oasis-open.org/wsn/bw-2/NotificationProducer/GetCurrentMessageRequest</wsa:Action>
</s:Header>
<s:Body>
<wsnt:GetCurrentMessage>
<wsnt:Topic Dialect="http://docs.oasis-open.org/wsn/t-1/TopicExpression/Concrete">%s</wsnt:Topic>
</wsnt:GetCurrentMessage>
</s:Body>
</s:Envelope>
"""

RENEW = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<s:Envelope xmlns:wsa="http://www.w3.org/2005/08/addressing"
xmlns:wsnt="http://docs.oasis-open.org/wsn/b-2"
xmlns:wsn-br="http://docs.oasis-open.org/wsn/br-2"
xmlns:wsn-bw="http://docs.oasis-open.org/wsn/bw-2"
xmlns:wsn-brw="http://docs.oasis-open.org/wsn/brw-2"
xmlns:wsrf-bf="http://docs.oasis-open.org/wsrf/bf-2"
xmlns:wsrf-bfw="http://docs.oasis-open.org/wsrf/bfw-2"
xmlns:s="http://schemas.xmlsoap.org/soap/envelope/">
<s:Header><wsa:Action>http://docs.oasis-open.org/wsn/bw-2/SubscriptionManager/RenewRequest</wsa:Action>
</s:Header>
<s:Body>
<wsnt:Renew>
<wsnt:TerminationTime>2016-01-02T00:00:00.00000Z</wsnt:TerminationTime>
</wsnt:Renew>
</s:Body>
</s:Envelope>
"""

PAUSE = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<s:Envelope xmlns:wsa="http://www.w3.org/2005/08/addressing"
xmlns:wsnt="http://docs.oasis-open.org/wsn/b-2"
xmlns:wsn-br="http://docs.oasis-open.org/wsn/br-2"
xmlns:wsn-bw="http://docs.oasis-open.org/wsn/bw-2"
xmlns:wsn-brw="http://docs.oasis-open.org/wsn/brw-2"
xmlns:wsrf-bf="http://docs.oasis-open.org/wsrf/bf-2"
xmlns:wsrf-bfw="http://docs.oasis-open.org/wsrf/bfw-2"
xmlns:s="http://schemas.xmlsoap.org/soap/envelope/">
<s:Header><wsa:Action>http://docs.oasis-open.org/wsn/bw-2/SubscriptionManager/PauseSubscriptionRequest</wsa:Action>
</s:Header>
<s:Body>
<wsnt:PauseSubscription/>
</s:Body>
</s:Envelope>
"""

RESUME = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<s:Envelope xmlns:wsa="http://www.w3.org/2005/08/addressing"
xmlns:wsnt="http://docs.oasis-open.org/wsn/b-2"
xmlns:wsn-br="http://docs.oasis-open.org/wsn/br-2"
xmlns:wsn-bw="http://docs.oasis-open.org/wsn/bw-2"
xmlns:wsn-brw="http://docs.oasis-open.org/wsn/brw-2"
xmlns:wsrf-bf="http://docs.oasis-open.org/wsrf/bf-2"
xmlns:wsrf-bfw="http://docs.oasis-open.org/wsrf/bfw-2"
xmlns:s="http://schemas.xmlsoap.org/soap/envelope/">
<s:Header><wsa:Action>http://docs.oasis-open.org/wsn/bw-2/SubscriptionManager/ResumeSubscriptionRequest</wsa:Action>
</s:Header>
<s:Body>
<wsnt:ResumeSubscription/>
</s:Body>
</s:Envelope>
"""

UNSUBSCRIBE = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<s:Envelope xmlns:wsa="http://www.w3.org/2005/08/addressing"
xmlns:wsnt="http://docs.oasis-open.org/wsn/b-2"
xmlns:wsn-br="http://docs.oasis-open.org/wsn/br-2"
xmlns:wsn-bw="http://docs.oasis-open.org/wsn/bw-2"
xmlns:wsn-brw="http://docs.oasis-open.org/wsn/brw-2"
xmlns:wsrf-bf="http://docs.oasis-open.org/wsrf/bf-2"
xmlns:wsrf-bfw="http://docs.oasis-open.org/wsrf/bfw-2"
xmlns:s="http://schemas.xmlsoap.org/soap/envelope/">
<s:Header><wsa:Action>http://docs.oasis-open.org/wsn/bw-2/SubscriptionManager/UnsubscribeRequest</wsa:Action>
</s:Header>
<s:Body>
<wsnt:Unsubscribe/>
</s:Body>
</s:Envelope>
"""




class WSNRequest(object):
    """
    The Notify class extracts info from command line during startup
    """

    MODE = None
    HOST = None
    PORT = None
    TOPIC = None

    ### BEGIN Initialization / Constructor

    def __init__(self):
        """
        Call the arg_parse to extract needed input from commandline
        """
        self.arg_parse()

    ### BEGIN Helper methods

    def arg_parse(self):
        """
        Extract needed startup info from commandline
        """

        if len(sys.argv) != 5:
            print "Invalid arguments: python %s <mode> <host> <port> <topic>" % sys.argv[0]
            exit(1)
        try:
            self.MODE = sys.argv[1]
            self.HOST = sys.argv[2]
            self.PORT = int(sys.argv[3])
            self.TOPIC = sys.argv[4]
        except ValueError:
            print "Invalid arguments: python %s <mode> <host> <port> <topic>" % sys.argv[0]
            exit(1)

    def generate_url(self, endpoint):
        """
        Constructs a W3C URL
        """

        return "http://%s:%d" % (self.HOST, self.PORT)

    def generate_headers(self):
        """
        Generate the dictionary of headers to be sent
        """

        return {"Content-Type": SOAP_HEADER}

    def print_response(self, response):
        """
        Prints the headers and response body of a response
        """

        print "--- RESPONSE HEADERS ----------------------------------"
        print response.headers
        print "--- RESPONSE BODY -------------------------------------"
        print response.text
        print "--- RESPONSE END --------------------------------------"

    def send_request(self, payload, endpoint="/"):
        """
        Sends the provided payload and prints response if DEBUG is True
        """

        response = requests.post(self.generate_url(endpoint), headers=self.generate_headers(), data=payload)

        if (DEBUG):
            self.print_response(response)

    ### BEGIN public API

    def send_notify(self, message):
        """
        Sends a notify to the provided host and port with argument message
        """

        # Generate the payload
        payload = NOTIFY % (self.TOPIC, message)

        # Send the request
        self.send_request(payload)

    def send_subscription(self):
        """
        Sends a subscription request
        """

        print "[i] Sending a %s request..." % MODES[sys.argv[1]]

        # Generate the payload
        payload = SUBSCRIBE % ('http://localhost:8081', self.TOPIC)

        # Send the request
        self.send_request(payload)

    def send_registration(self):
        """
        Sends a publisher registration request
        """

        print "[i] Sending a %s request..." % MODES[sys.argv[1]]

        # Generate the payload
        payload = REGISTER % ('http://localhost:61000', self.TOPIC)

        # Send the request
        self.send_request(payload)

    def send_get_current_message(self):
        """
        Sends a GetCurrentMessage request
        """

        print "[i] Sending a %s request..." % MODES[sys.argv[1]]

        # Generate the payload
        payload = GET_CURRENT_MESSAGE % (self.TOPIC)

        # Send the request
        self.send_request(payload)

    def send_renew_subscription(self, subscription_reference):
        """
        Sends a Renew request
        """

        print "[i] Sending a %s request..." % MODES[sys.argv[1]]

        # Generate the payload
        payload = RENEW

        self.send_request(payload, endpoint="/%s" % subscription_reference)

    def send_pause_subscription(self, subscription_reference):
        """
        Sends a Pause request
        """

        print "[i] Sending a %s request..." % MODES[sys.argv[1]]

        # Generate the payload
        payload = PAUSE

        self.send_request(payload, endpoint="/%s" % subscription_reference)

    def send_resume_subscription(self, subscription_reference):
        """
        Sends a Resume request
        """

        print "[i] Sending a %s request..." % MODES[sys.argv[1]]

        # Generate the payload
        payload = RESUME

        self.send_request(payload, endpoint="/%s" % subscription_reference)

    def send_unsubscribe(self, subscription_reference):
        """
        Sends a Unsubscribe request
        """

        print "[i] Sending a %s request..." % MODES[sys.argv[1]]

        # Generate the payload
        payload = UNSUBSCRIBE

        self.send_request(payload, endpoint="/%s" % subscription_reference)


# Start the bruteforcer
if __name__ == "__main__":

    wsn_request = WSNRequest()
    mode = sys.argv[1]
    print "[i] Running in %s mode..." % MODES[mode]

    i = 0

    if mode == 'notify':
        while i < RUNS:
            print "[%d] Sending Notify..." % (i + 1)
            # Shuffle the random words
            shuffle(RANDOM_WORDS)
            # Create the message
            message = " ".join(RANDOM_WORDS)
            # Send the notify
            wsn_request.send_notify(message)
            # Increment the run counter
            i += 1
            # Sleep for INTERVAL
            time.sleep(INTERVAL)

    elif mode == 'subscribe':
        wsn_request.send_subscription()

    elif mode == 'register':
        wsn_request.send_registration()

    elif mode == 'getcurrent':
        wsn_request.send_get_current_message()

    elif mode == 'renew':
        subscription_reference = raw_input("Enter the subscription reference: ")
        subscription_refecence = subscription_reference.rstrip()
        wsn_request.send_renew_subscription(subscription_reference)

    elif mode == 'pause':
        subscription_reference = raw_input("Enter the subscription reference: ")
        subscription_reference = subscription_reference.rstrip()
        wsn_request.send_pause_subscription(subscription_reference)

    elif mode == 'resume':
        subscription_reference = raw_input("Enter the subscription reference: ")
        subscription_reference = subscription_reference.rstrip()
        wsn_request.send_resume_subscription(subscription_reference)

    elif mode == 'unsubscribe':
        subscription_reference = raw_input("Enter the subscription reference: ")
        subscription_reference = subscription_reference.rstrip()
        wsn_request.send_unsubscribe(subscription_reference)

    elif mode == 'all':
        print "[i] Performing all requests in order..."

        wsn_request.send_subscription()
        time.sleep(2)
        subscription_reference = raw_input("Enter the subscription reference: ")
        subscription_reference = subscription_reference.rstrip()
        wsn_request.send_registration()
        time.sleep(2)
        wsn_request.send_notify("Test Message")
        time.sleep(2)
        wsn_request.send_get_current_message()
        time.sleep(2)
        wsn_request.send_renew_subscription(subscription_reference)
        time.sleep(2)
        wsn_request.send_pause_subscription(subscription_reference)
        time.sleep(2)
        wsn_request.send_notify("Notify sent during paused subscription")
        time.sleep(2)
        wsn_request.send_resume_subscription(subscription_reference)
        time.sleep(2)
        wsn_request.send_unsubscribe(subscription_reference)

    print "[X] Complete."
