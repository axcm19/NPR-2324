package src.main.java;

import org.eclipse.mosaic.lib.objects.v2x.EncodedPayload;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.v2x.V2xMessage;


public class SendVizinhoMsg extends V2xMessage {
    private final EncodedPayload payload = new EncodedPayload(16L, 128L);
    private static final long minLen = 128L;
    private String id;
    private String pos;

    public SendVizinhoMsg(MessageRouting routing, String id, String pos) {
        super(routing);
        this.id = id;
        this.pos = pos;
    }

    public String getMessage() {
        return id + pos;
    }

    public String getID() {
        return this.id;
    }

    public String getPos() {
        return this.pos;
    }

    public EncodedPayload getPayload() {
        return this.payload;
    }

    public EncodedPayload getPayLoad() {
        return null;
    }


    public String toString() {
        return id + pos;
    }
}
