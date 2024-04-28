package src.main.java;

import org.apache.commons.lang3.Validate;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.CamBuilder;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedAcknowledgement;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedV2xMessage;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.CommunicationApplication;
import org.eclipse.mosaic.fed.application.app.api.os.TrafficLightOperatingSystem;
import org.eclipse.mosaic.interactions.communication.V2xMessageTransmission;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.rti.TIME;

public final class TrafficLightApp extends AbstractApplication<TrafficLightOperatingSystem> implements CommunicationApplication {
    public final static String SECRET = "open sesame!";
    private final static short GREEN_DURATION = 10;

    static final String DEFAULT_PROGRAM = "1";
    private static final String GREEN_PROGRAM_R0 = "0";
    private static final String GREEN_PROGRAM_R1 = "2";

    static final Integer MIN_DISTANCE = 100;

    private int counter_r0 = 0; // vertical
    private int counter_r1 = 0; // horizontal

    @Override
    public void onStartup() {
        getLog().infoSimTime(this, "Initialize application");
        getOs().getAdHocModule().enable();
        getLog().infoSimTime(this, "Activated Wifi Module");
        setRed();
    }

    @Override
    public void onShutdown() {
        getLog().infoSimTime(this, "Shutdown application");
    }

    private void setGreen_r0() {
        getOs().switchToProgram(GREEN_PROGRAM_R0);
        getLog().infoSimTime(this, "Setting traffic lights to GREEN in r0");

        getOs().getEventManager().addEvent(
                getOs().getSimulationTime() + GREEN_DURATION * TIME.SECOND,
                (e) -> setRed()
        );
    }

    private void setGreen_r1() {
        getOs().switchToProgram(GREEN_PROGRAM_R1);
        getLog().infoSimTime(this, "Setting traffic lights to GREEN in r1");

        getOs().getEventManager().addEvent(
                getOs().getSimulationTime() + GREEN_DURATION * TIME.SECOND,
                (e) -> setRed()
        );
    }

    private void setRed() {
        getOs().switchToProgram(DEFAULT_PROGRAM);
        getLog().infoSimTime(this, "Setting traffic lights to RED");
    }

    @Override
    public void onMessageReceived(ReceivedV2xMessage receivedV2xMessage) {

        String secret = "";
        String route = "";

        if (!(receivedV2xMessage.getMessage() instanceof RSUMsg)) {
            return;
        }

        /*
        String padrao = "\\|\\s*"; // Divide na barra vertical, removendo espaços em branco antes e depois

        // Dividir a frase em secret e route
        String[] partes = ((GreenWaveMsg) receivedV2xMessage.getMessage()).getMessage().split(padrao);

        if (partes.length == 2) {
            secret = "" + partes[0].trim(); // Remover espaços em branco antes e depois do secret
            route = "" + partes[1].trim(); // Remover espaços em branco antes e depois do route

        } else {
            System.out.println("Formato da frase inválido.");
        }

        getLog().infoSimTime(this, "Received GreenWaveMsg from {}", route);

        //if (!((GreenWaveMsg) receivedV2xMessage.getMessage()).getMessage().equals(SECRET)) {
        if (!(secret.equals(SECRET))) {
            return;
        }
        getLog().infoSimTime(this, "Received correct passphrase: {}", SECRET);


        if(route.equals("r_0")){
            counter_r0++;
        }
        if(route.equals("r_1")){
            counter_r1++;
        }

        */
        getLog().infoSimTime(this, "Received message from RSU {}", receivedV2xMessage.getMessage().toString());

        Validate.notNull(receivedV2xMessage.getMessage().getRouting().getSource().getSourcePosition(),
                "The source position of the sender cannot be null");
        if (!(receivedV2xMessage.getMessage().getRouting().getSource().getSourcePosition()
                .distanceTo(getOs().getPosition()) <= MIN_DISTANCE)) {
            getLog().infoSimTime(this, "Vehicle that sent message is too far away.");
            return;
        }

        /*
        //if (DEFAULT_PROGRAM.equals(getOs().getCurrentProgram().getProgramId()) && counter_r0 >= counter_r1) {
        if (DEFAULT_PROGRAM.equals(getOs().getCurrentProgram().getProgramId()) && counter_r0 >= 10) {
            getLog().infoSimTime(this, "Counter R0 = {} , Counter R1 = {}", counter_r0, counter_r1);
            setGreen_r0();
            counter_r0 = 0;
        }
        //else if (DEFAULT_PROGRAM.equals(getOs().getCurrentProgram().getProgramId()) && counter_r0 < counter_r1) {
        else if (DEFAULT_PROGRAM.equals(getOs().getCurrentProgram().getProgramId()) && counter_r1 >= 10) {
            getLog().infoSimTime(this, "Counter R0 = {} , Counter R1 = {}", counter_r0, counter_r1);
            setGreen_r1();
            counter_r1 = 0;
        }*/


        if (DEFAULT_PROGRAM.equals(getOs().getCurrentProgram().getProgramId()) && receivedV2xMessage.getMessage().toString().equals("0")) {
            getLog().infoSimTime(this, "Changing to program 0");
            setGreen_r0();
        }
        else if (DEFAULT_PROGRAM.equals(getOs().getCurrentProgram().getProgramId()) && receivedV2xMessage.getMessage().toString().equals("2")) {
            getLog().infoSimTime(this, "Changing to program 2");
            setGreen_r1();
        }

    }

    @Override
    public void onAcknowledgementReceived(ReceivedAcknowledgement acknowledgedMessage) {
        // nop
    }

    @Override
    public void onCamBuilding(CamBuilder camBuilder) {
        // nop
    }

    @Override
    public void onMessageTransmitted(V2xMessageTransmission v2xMessageTransmission) {
        // nop
    }

    @Override
    public void processEvent(Event event) throws Exception {
        // nop
    }
}

