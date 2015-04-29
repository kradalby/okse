package no.ntnu.okse.protocol.amqp;

import no.ntnu.okse.core.messaging.Message;
import no.ntnu.okse.core.topic.Topic;
import no.ntnu.okse.core.topic.TopicService;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class AMQPServerTest {


    @Test
    public void testConvertAMQPMessageToMessageBytes() throws Exception {

    }

    @Test
    public void testConvertOkseMessageToAMQP() {
        TopicService.getInstance().addTopic("test");
        Topic t = TopicService.getInstance().getTopic("test");
        no.ntnu.okse.core.messaging.Message okseMessage = new no.ntnu.okse.core.messaging.Message("Hei", t, null);
        org.apache.qpid.proton.message.Message amqpMessage;

        amqpMessage = AMQPServer.convertOkseMessageToAMQP(okseMessage);

        assertEquals(okseMessage.getMessage(), amqpMessage.getBody().toString());
    }

}