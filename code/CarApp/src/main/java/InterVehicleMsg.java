package src.main.java;

import org.eclipse.mosaic.lib.geo.GeoPoint;
import org.eclipse.mosaic.lib.objects.v2x.EncodedPayload;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.v2x.V2xMessage;

public final class InterVehicleMsg extends V2xMessage {
    private final GeoPoint senderPosition;
    private final EncodedPayload payload = new EncodedPayload(16L, 128L);
    private static final long minLen = 128L;

    public InterVehicleMsg(MessageRouting routing, GeoPoint senderPosition) {
        super(routing);
        this.senderPosition = senderPosition;
    }

    public GeoPoint getSenderPosition() {
        return this.senderPosition;
    }

    public EncodedPayload getPayload() {
        return this.payload;
    }

    public EncodedPayload getPayLoad() {
        return null;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("InterVehicleMsg{");
        sb.append("senderPosition=").append(this.senderPosition);
        sb.append('}');
        return sb.toString();
    }
}