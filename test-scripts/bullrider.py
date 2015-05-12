# -*- encoding: utf-8 -*-

import sys
import time
import requests
from random import shuffle

DEBUG = True
INTERVAL = 0.001
RUNS = 1000
SOAP_HEADER = "application/soap+xml;charset=utf-8"
MODES = {
    "all": "All available",
    "notify": "Notification",
    "massnotify": "Mass Notification",
    "multinotify": "MultiNotification",
    "largenotify": "Large (9MB) Notification",
    "subscribe": "Subscribe",
    "subscribe-xpath": "Subscribe (XPATH)",
    "subscribe-notopic": "Subscribe (No Topic)",
    "subscribe-simpletopic": "Subscribe (SimpleTopic)",
    "subscribe-useraw": "Subscribe (UseRaw=true)",
    "subscribe-fulltopic": "Subscribe (FullTopic)",
    "subscribe-xpathtopic": "Subscribe (XPATH Topic)",
    "unsubscribe": "Unsubscribe",
    "register": "PublisherRegistration",
    "getcurrent": "GetCurrentMessage",
    "renew": "RenewSubscription",
    "pause": "PauseSubscription",
    "resume": "ResumeSubscription",
    "unregister": "DestroyRegistration",
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
<s:Envelope xmlns:ns2="http://www.w3.org/2001/12/soap-envelope"
xmlns:ns3="http://docs.oasis-open.org/wsrf/bf-2"
xmlns:wsa="http://www.w3.org/2005/08/addressing"
xmlns:wsnt="http://docs.oasis-open.org/wsn/b-2"
xmlns:ns6="http://docs.oasis-open.org/wsn/t-1"
xmlns:ns7="http://docs.oasis-open.org/wsn/br-2"
xmlns:s="http://schemas.xmlsoap.org/soap/envelope/"
xmlns:ns9="http://docs.oasis-open.org/wsrf/r-2">
<s:Header>
<wsa:Action>http://docs.oasis-open.org/wsn/bw-2/NotificationConsumer/Notify</wsa:Action>
</s:Header>
<s:Body>
<wsnt:Notify>
<wsnt:NotificationMessage>
<wsnt:Topic Dialect="http://docs.oasis-open.org/wsn/t-1/TopicExpression/Full">%s</wsnt:Topic>
<wsnt:Message xmlns:oxmsg="http://okse.test.message"><Content>%s</Content></wsnt:Message>
</wsnt:NotificationMessage>
</wsnt:Notify>
</s:Body>
</s:Envelope>"""

NOTIFY_MULTIPLE = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<s:Envelope xmlns:ns2="http://www.w3.org/2001/12/soap-envelope" xmlns:ns3="http://docs.oasis-open.org/wsrf/bf-2" xmlns:wsa="http://www.w3.org/2005/08/addressing" xmlns:wsnt="http://docs.oasis-open.org/wsn/b-2" xmlns:ns6="http://docs.oasis-open.org/wsn/t-1" xmlns:ns7="http://docs.oasis-open.org/wsn/br-2" xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ns9="http://docs.oasis-open.org/wsrf/r-2">
<s:Header>
<wsa:Action>http://docs.oasis-open.org/wsn/bw-2/NotificationConsumer/Notify</wsa:Action>
</s:Header>
<s:Body>
<wsnt:Notify>
<wsnt:NotificationMessage>
<wsnt:Topic Dialect="http://docs.oasis-open.org/wsn/t-1/TopicExpression/Concrete">%s</wsnt:Topic>
<wsnt:Message><Content>%s</Content></wsnt:Message>
</wsnt:NotificationMessage>
<wsnt:NotificationMessage>
<wsnt:Topic Dialect="http://docs.oasis-open.org/wsn/t-1/TopicExpression/Simple">%s</wsnt:Topic>
<wsnt:Message><Content>%s</Content></wsnt:Message>
</wsnt:NotificationMessage>
</wsnt:Notify>
</s:Body>
</s:Envelope>"""

NOTIFY_LARGE = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<s:Envelope xmlns:ns2="http://www.w3.org/2001/12/soap-envelope" xmlns:ns3="http://docs.oasis-open.org/wsrf/bf-2" xmlns:wsa="http://www.w3.org/2005/08/addressing" xmlns:wsnt="http://docs.oasis-open.org/wsn/b-2" xmlns:ns6="http://docs.oasis-open.org/wsn/t-1" xmlns:ns7="http://docs.oasis-open.org/wsn/br-2" xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ns9="http://docs.oasis-open.org/wsrf/r-2">
<s:Header>
<wsa:Action>http://docs.oasis-open.org/wsn/bw-2/NotificationConsumer/Notify</wsa:Action>
</s:Header>
<s:Body>
<wsnt:Notify>
<wsnt:NotificationMessage>
<wsnt:Topic Dialect="http://docs.oasis-open.org/wsn/t-1/TopicExpression/Concrete">%s</wsnt:Topic>
<wsnt:Message><Content>%s</Content></wsnt:Message>
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
<ns3:Filter xmlns:ox="http://okse.default.topic"><ns3:TopicExpression Dialect="http://docs.oasis-open.org/wsn/t-1/TopicExpression/Concrete">%s</ns3:TopicExpression></ns3:Filter>
<ns3:InitialTerminationTime>2016-01-01T00:00:00</ns3:InitialTerminationTime>
</ns3:Subscribe>
</ns6:Body>
</ns6:Envelope>
"""

SUBSCRIBE_FULLTOPIC = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<ns6:Envelope xmlns:ns2="http://www.w3.org/2005/08/addressing"
xmlns:ns3="http://docs.oasis-open.org/wsn/b-2"
xmlns:ns4="http://docs.oasis-open.org/wsn/t-1"
xmlns:ns5="http://docs.oasis-open.org/wsrf/bf-2"
xmlns:ns6="http://schemas.xmlsoap.org/soap/envelope/"
xmlns:test="http://test.com"
xmlns:test2="http://test2.com">
<ns6:Header>
<ns2:Action>http://docs.oasis-open.org/wsn/bw-2/NotificationProducer/SubscribeRequest</ns2:Action>
</ns6:Header>
<ns6:Body>
<ns3:Subscribe>
<ns3:ConsumerReference><ns2:Address>%s</ns2:Address></ns3:ConsumerReference>
<ns3:Filter>
<ns3:TopicExpression Dialect="http://docs.oasis-open.org/wsn/t-1/TopicExpression/Full">%s</ns3:TopicExpression></ns3:Filter>
</ns3:Subscribe>
</ns6:Body>
</ns6:Envelope>
"""

SUBSCRIBE_USERAW = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
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
<ns3:SubscriptionPolicy><ns3:UseRaw/></ns3:SubscriptionPolicy>
<ns3:Filter><ns3:TopicExpression Dialect="http://docs.oasis-open.org/wsn/t-1/TopicExpression/Concrete">%s</ns3:TopicExpression></ns3:Filter>
</ns3:Subscribe>
</ns6:Body>
</ns6:Envelope>
"""

SUBSCRIBE_SIMPLETOPIC = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
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
<ns3:Filter><ns3:TopicExpression Dialect="http://docs.oasis-open.org/wsn/t-1/TopicExpression/Simple">%s</ns3:TopicExpression></ns3:Filter>
</ns3:Subscribe>
</ns6:Body>
</ns6:Envelope>
"""

SUBSCRIBE_NOTOPIC = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
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
</ns3:Subscribe>
</ns6:Body>
</ns6:Envelope>
"""

SUBSCRIBE_XPATH = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
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
<ns3:Filter><ns3:TopicExpression Dialect="http://docs.oasis-open.org/wsn/t-1/TopicExpression/Concrete">%s</ns3:TopicExpression>
<ns3:MessageContent Dialect="http://www.w3.org/TR/1999/REC-xpath-19991116">/message[text()="derp"]</ns3:MessageContent>
</ns3:Filter>
</ns3:Subscribe>
</ns6:Body>
</ns6:Envelope>
"""

SUBSCRIBE_XPATHTOPIC = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
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
<ns3:Filter><ns3:TopicExpression Dialect="http://www.w3.org/TR/1999/REC-xpath-19991116">%s</ns3:TopicExpression></ns3:Filter>
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
xmlns:s="http://schemas.xmlsoap.org/soap/envelope/"
xmlns:ox="http://okse.default.topic">
<s:Header><wsa:Action>http://docs.oasis-open.org/wsn/brw-2/RegisterPublisher/RegisterPublisherRequest</wsa:Action>
</s:Header>
<s:Body>
<wsn-br:RegisterPublisher><wsn-br:PublisherReference><wsa:Address>%s</wsa:Address></wsn-br:PublisherReference>
<wsn-br:Topic Dialect="http://docs.oasis-open.org/wsn/t-1/TopicExpression/Concrete">%s</wsn-br:Topic>
<wsn-br:Demand>false</wsn-br:Demand>
<wsn-br:InitialTerminationTime>2016-01-01T14:03:00</wsn-br:InitialTerminationTime>
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
<wsnt:TerminationTime>2016-01-02T00:00:00Z</wsnt:TerminationTime>
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

UNREGISTER = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<s:Envelope xmlns:wsa="http://www.w3.org/2005/08/addressing"
xmlns:wsnt="http://docs.oasis-open.org/wsn/b-2"
xmlns:wsn-br="http://docs.oasis-open.org/wsn/br-2"
xmlns:wsn-bw="http://docs.oasis-open.org/wsn/bw-2"
xmlns:wsn-brw="http://docs.oasis-open.org/wsn/brw-2"
xmlns:wsrf-bf="http://docs.oasis-open.org/wsrf/bf-2"
xmlns:wsrf-bfw="http://docs.oasis-open.org/wsrf/bfw-2"
xmlns:s="http://schemas.xmlsoap.org/soap/envelope/">
<s:Header><wsa:Action>http://docs.oasis-open.org/wsn/brw-2/PublisherRegistrationManager/DestroyRegistrationRequest</wsa:Action>
</s:Header>
<s:Body>
<wsn-br:DestroyRegistration/>
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
    WAN_IP = None

    ### BEGIN Initialization / Constructor

    def __init__(self):
        """
        Call the arg_parse to extract needed input from commandline
        """
        self.arg_parse()
        self.WAN_IP = raw_input("Which IP:Port should be used as host of this machine? (0.0.0.0:8000) ")

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

        return "http://%s:%d%s" % (self.HOST, self.PORT, endpoint)

    def generate_headers(self):
        """
        Generate the dictionary of headers to be sent
        """

        return {"Content-Type": SOAP_HEADER}

    def print_response(self, response):
        """
        Prints the headers and response body of a response
        """
        print "--- RESPONSE CODE -------------------------------------"
        print response.status_code
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

    def generator(self, payload):
        chunks = payload.split("\n")
        size = len(chunks)
        i = 0
        while i < size:
            yield "".join(chunks[i:i+100])
            i = i + 100


    ### BEGIN public API

    def send_notify(self, message):
        """
        Sends a notify to the provided host and port with argument message
        """

        # Generate the payload
        payload = NOTIFY % (self.TOPIC, message)

        # Send the request
        self.send_request(payload)

    def send_notify_multiple(self, message_one, message_two):
        """
        Sends a notify to the provided host and port with two notification
        messages bundled into one Notify request
        """

        # Generate the payload
        payload = NOTIFY_MULTIPLE % (self.TOPIC, message_one, self.TOPIC, message_two)

        # Send the request
        self.send_request(payload)

    def send_notify_large(self):
        """
        Sends a Notification with a large payload
        """

        print "[i] Sending a large Notify"

        bigdata = None

        with open('smallb64data.txt', 'r') as f:
            bigdata = f.read()

        # Generate the payload
        payload = NOTIFY_LARGE % (self.TOPIC, bigdata)

        # Send the request
        self.send_request(self.generator(payload))

    def send_subscription(self):
        """
        Sends a subscription request
        """

        print "[i] Sending a Subscribe request"

        # Generate the payload
        payload = SUBSCRIBE % ('http://' + self.WAN_IP, self.TOPIC)

        # Send the request
        self.send_request(payload)

    def send_subscription_simpletopic(self):
        """
        Sends a subscription request using SimpleTopic (no slashes or subpaths)
        """

        print "[i] Sending a Subscribe request using SimpleTopic expression"

        # Generate the payload
        payload = SUBSCRIBE_SIMPLETOPIC % ('http://' + self.WAN_IP, self.TOPIC)

        self.send_request(payload)

    def send_subscription_fulltopic(self):
        """
        Sends a subscription request using FullTopic
        """

        print "[i] Sending a Subscribe request using FullTopic expression"

        # Generate the payload
        payload = SUBSCRIBE_FULLTOPIC % ('http://' + self.WAN_IP, self.TOPIC)

        # Send the request
        self.send_request(payload)

    def send_subscription_notopic(self):
        """
        Sends a subscription without topic expression
        """

        print "[i] Sending a Subscribe without Topic"

        # Generate payload
        payload = SUBSCRIBE_NOTOPIC % ('http://' + self.WAN_IP)

        # Send the request
        self.send_request(payload)

    def send_subscription_xpathtopic(self):
        """
        Sends a subscription using xpath topic expression"
        """

        print "[i] Sending a Subscribe with XPATH topic expression"

        # Generate the payload
        payload = SUBSCRIBE_XPATHTOPIC % ('http://' + self.WAN_IP, self.TOPIC)

        # Send the request
        self.send_request(payload)

    def send_subscription_xpath(self):
        """
        Sends a subscription with an XPATH expression
        """

        print "[i] Sending a Subscribe with XPATH content filter"

        # Generate payload
        payload = SUBSCRIBE_XPATH % ('http://' + self.WAN_IP, self.TOPIC)

        # Send the request
        self.send_request(payload)

    def send_subscription_useraw(self):
        """
        Sends a subscription with a UseRaw element
        """

        print "[i] Sending a Subscribe with UseRaw element"

        # Generate the payload
        payload = SUBSCRIBE_USERAW % ('http://' + self.WAN_IP, self.TOPIC)

        # Send the request
        self.send_request(payload)

    def send_registration(self):
        """
        Sends a publisher registration request
        """

        print "[i] Sending a PublisherRegistration request"

        # Generate the payload
        payload = REGISTER % (self.WAN_IP, self.TOPIC)

        # Send the request
        self.send_request(payload)

    def send_get_current_message(self):
        """
        Sends a GetCurrentMessage request
        """

        print "[i] Sending a GetCurrentMessage request"

        # Generate the payload
        payload = GET_CURRENT_MESSAGE % (self.TOPIC)

        # Send the request
        self.send_request(payload)

    def send_renew_subscription(self, subscription_reference):
        """
        Sends a Renew request
        """

        print "[i] Sending a RenewSubscription request"

        # Generate the payload
        payload = RENEW

        self.send_request(payload, endpoint="/%s" % subscription_reference)

    def send_pause_subscription(self, subscription_reference):
        """
        Sends a Pause request
        """

        print "[i] Sending a PauseSubscription request"

        # Generate the payload
        payload = PAUSE

        self.send_request(payload, endpoint="/%s" % subscription_reference)

    def send_resume_subscription(self, subscription_reference):
        """
        Sends a Resume request
        """

        print "[i] Sending a ResumeSubscription request"

        # Generate the payload
        payload = RESUME

        self.send_request(payload, endpoint="/%s" % subscription_reference)

    def send_unsubscribe(self, subscription_reference):
        """
        Sends a Unsubscribe request
        """

        print "[i] Sending a Unsubscribe request"

        # Generate the payload
        payload = UNSUBSCRIBE

        self.send_request(payload, endpoint="/%s" % subscription_reference)

    def send_unregister(self, publisher_reference):
        """
        Sends a DestroyRegistration request
        """

        print "[i] Sending a DestroyRegistration request"

        # Generate the payload
        payload = UNREGISTER

        self.send_request(payload, endpoint="/%s" % publisher_reference)


# Start the bruteforcer
if __name__ == "__main__":

    wsn_request = WSNRequest()
    mode = sys.argv[1]
    print "[i] Running in %s mode..." % MODES[mode]

    i = 0

    if mode == 'massnotify':
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

    elif mode == 'notify':
        wsn_request.send_notify("derp")

    elif mode == 'largenotify':
        wsn_request.send_notify_large()

    elif mode == 'multinotify':
        # Shuffle the words
        shuffle(RANDOM_WORDS)
        # Generate the first message
        msg1 = " ".join(RANDOM_WORDS)
        # Shuffle the words once more
        shuffle(RANDOM_WORDS)
        # Generate the second message
        msg2 = " ".join(RANDOM_WORDS)
        # Send the notify
        wsn_request.send_notify_multiple(msg1, msg2)

    elif mode == 'subscribe':
        wsn_request.send_subscription()

    elif mode == 'subscribe-fulltopic':
        wsn_request.send_subscription_fulltopic()

    elif mode == 'subscribe-notopic':
        wsn_request.send_subscription_notopic()

    elif mode == 'subscribe-xpathtopic':
        wsn_request.send_subscription_xpathtopic()

    elif mode == 'subscribe-xpath':
        wsn_request.send_subscription_xpath()

    elif mode == 'subscribe-simpletopic':
        wsn_request.send_subscription_simpletopic()

    elif mode == 'subscribe-useraw':
        wsn_request.send_subscription_useraw()

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

    elif mode == 'unregister':
        publisher_reference = raw_input("Enter the publisher reference: ")
        publisher_reference = publisher_reference.rstrip()
        wsn_request.send_unregister(publisher_reference)

    elif mode == 'all':
        print "[i] Performing all requests in order..."

        wsn_request.send_subscription()
        subscription_reference = raw_input("Enter the subscription reference: ")
        subscription_reference = subscription_reference.rstrip()
        wsn_request.send_registration()
        publisher_reference = raw_input("Enter the publisher reference: ")
        publisher_reference = publisher_reference.rstrip()
        wsn_request.send_notify("Test message that should arrive at consumer address.")
        time.sleep(2)
        wsn_request.send_notify_multiple("Test message 1 (Concrete)", "Test message 2 (Simple)")
        time.sleep(2)
        wsn_request.send_get_current_message()
        time.sleep(2)
        wsn_request.send_subscription_notopic()
        time.sleep(2)
        wsn_request.send_subscription_xpath()
        time.sleep(2)
        wsn_request.send_subscription_xpathtopic()
        time.sleep(2)
        wsn_request.send_subscription_simpletopic()
        time.sleep(2)
        wsn_request.send_subscription_useraw()
        time.sleep(2)
        wsn_request.send_renew_subscription(subscription_reference)
        time.sleep(2)
        wsn_request.send_pause_subscription(subscription_reference)
        time.sleep(2)
        wsn_request.send_notify("Notify sent during paused subscription that should not be recieved.")
        time.sleep(2)
        wsn_request.send_resume_subscription(subscription_reference)
        time.sleep(2)
        wsn_request.send_unsubscribe(subscription_reference)
        time.sleep(2)
        wsn_request.send_unregister(publisher_reference)

    print "[X] Complete."
