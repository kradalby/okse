# -*- encoding: utf-8 -*-

import sys
import time
import requests
from random import shuffle

DEBUG = False
INTERVAL = 0.1
RUNS = 50
SOAP_HEADER = "application/soap+xml;charset=utf-8"

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

class WSNRequest(object):
    """
    The Notify class extracts info from command line during startup
    """

    HOST = None
    PORT = None
    TOPIC = None

    def __init__(self):
        """
        Call the arg_parse to extract needed input from commandline
        """
        self.arg_parse()

    def arg_parse(self):
        """
        Extract needed startup info from commandline
        """

        if len(sys.argv) != 4:
            print "Invalid arguments: python %s <host> <port> <topic>" % sys.argv[0]
            exit(1)
        try:
            self.HOST = sys.argv[1]
            self.PORT = int(sys.argv[2])
            self.TOPIC = sys.argv[3]
        except ValueError:
            print "Invalid arguments: python %s <host> <port> <topic>" % sys.argv[0]
            exit(1)

    def send_notify(self, message):
        """
        Sends a notify to the provided host and port with argument message
        """

        # Generate the payload
        payload = NOTIFY % (self.TOPIC, message)
        headers = {"Content-Type": 'text/xml; charset="UTF-8"'}
        url = "http://%s:%d/" % (self.HOST, self.PORT)

        if (DEBUG):
            print payload

        # Fire the post request
        response = requests.post(url, headers=headers, data=payload)

        if (DEBUG):
            print "--- RESPONSE HEADERS-------------------------------"
            print response.headers
            print "--- RESPONSE PAYLOAD ------------------------------"
            print response.text
            print "--- RESPONSE END ----------------------------------"


# Start the bruteforcer
if __name__ == "__main__":

    if len(sys.argv) != 4:
        exit("Invalid arguments: python %s <host> <port> <topic>" % sys.argv[0])

    wsn_request = WSNRequest()

    i = 0

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

    print "[X] Complete."
