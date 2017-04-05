package net.evilezh.mesosconsul;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.client.api.Response;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;

public class EventReader implements Response.ContentListener {
    private static final Logger logger = LogManager.getLogger(EventReader.class);
    private boolean start = false;
    private int eventSize = 0;
    private StringBuilder sb = new StringBuilder();
    private static final ObjectMapper mapper = new ObjectMapper();
    TypeReference<HashMap<String, Object>> mapRef = new TypeReference<HashMap<String, Object>>() {
    };

    EventProcessor eventProcessor;

    public EventReader(EventProcessor eventProcessor) {
        this.eventProcessor = eventProcessor;
    }

    @Override
    public void onContent(Response response, ByteBuffer content) {
        if (!start) {
            start = true;
            int spos = content.position();
            byte b = 0;
            int size = 0;
            while (content.remaining() > 0 && b != 10) {
                b = content.get();
                if (b == 10) {
                    int rlen = content.position() - spos - 1;
                    byte[] data = new byte[rlen];
                    content.position(spos);
                    content.get(data);
                    eventSize = Integer.parseInt(new String(data));
                    content.get();
                }
            }
        }
        eventSize -= content.remaining();
        byte[] data = new byte[content.remaining()];
        content.get(data);
        sb.append(new String(data));
        if (eventSize == 0) {
            logger.info("New event:" + sb.toString().substring(0, 300));
            eventProcessor.process(sb.toString().getBytes());
            //queue.add(sb.toString());
            start = false;
            sb = new StringBuilder();
        }
    }
}
