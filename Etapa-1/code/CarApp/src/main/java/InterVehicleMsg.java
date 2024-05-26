package src.main.java;

import org.eclipse.mosaic.lib.objects.v2x.EncodedPayload;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.v2x.V2xMessage;

public final class InterVehicleMsg extends V2xMessage {
    private final EncodedPayload payload = new EncodedPayload(16L, 128L);
    private static final long minLen = 128L;
    private String info;

    public InterVehicleMsg(MessageRouting routing, String msg) {
        super(routing);
        this.info = msg;
    }

    public String getMessage() {
        return info;
    }

    public EncodedPayload getPayload() {
        return this.payload;
    }

    public EncodedPayload getPayLoad() {
        return null;
    }


    public String toString() {
        return info;
    }
}