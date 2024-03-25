import org.eclipse.mosaic.lib.objects.v2x.EncodedPayload;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.v2x.V2xMessage;

import javax.annotation.Nonnull;

public final class GreenWaveMsg extends V2xMessage {
    private final String         message;
    private final EncodedPayload payload;
    private final static long    MIN_LEN = 8L;

    public GreenWaveMsg(MessageRouting routing, String message) {
        super(routing);
        this.message = message;
        payload = new EncodedPayload(message.length(), MIN_LEN);
    }

    public String getMessage() {
        return message;
    }

    @Nonnull
    @Override
    public EncodedPayload getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("GreenWaveMsg{");
        sb.append("message='").append(message).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
