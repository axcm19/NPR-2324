package src.main.java;

import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.AdHocModuleConfiguration;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.CamBuilder;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedAcknowledgement;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedV2xMessage;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.Application;
import org.eclipse.mosaic.fed.application.app.api.CommunicationApplication;
import org.eclipse.mosaic.fed.application.app.api.VehicleApplication;
import org.eclipse.mosaic.fed.application.app.api.os.VehicleOperatingSystem;
import org.eclipse.mosaic.interactions.communication.V2xMessageTransmission;
import org.eclipse.mosaic.lib.enums.AdHocChannel;
import org.eclipse.mosaic.lib.enums.SensorType;
import org.eclipse.mosaic.lib.geo.CartesianPoint;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.vehicle.VehicleData;
import org.eclipse.mosaic.lib.util.scheduling.Event;

import java.util.HashMap;
import java.util.List;

public class CarSendsCamAPP extends AbstractApplication<VehicleOperatingSystem> implements VehicleApplication, CommunicationApplication {
    /**
     * Used for choosing a RAND id for the message that is sent intra-vehicle.
     */
    private final static int MAX_ID = 1000;

    private HashMap<String, String> vizinhos = new HashMap<>();   // key -> name_carro ; value -> position_carro

    /*
    ##########################################################################################################################################3
    */

    @Override
    public void onStartup() {
        getLog().infoSimTime(this, "Initialize application");
        getOs().getAdHocModule().enable(new AdHocModuleConfiguration()
                .addRadio()
                .channel(AdHocChannel.CCH)
                .power(16)
                .create());
        getLog().infoSimTime(this, "Activated AdHoc Module");
    }

    /*
    ##########################################################################################################################################3
    */

    @Override
    public void onVehicleUpdated(VehicleData previousVehicleData, VehicleData updatedVehicleData) {
        final List<? extends Application> applications = getOs().getApplications();
        final IntraVehicleMsg message = new IntraVehicleMsg(getOs().getId(), getRandom().nextInt(0, MAX_ID));

        // Example usage for how to detect sensor readings
        if (getOs().getStateOfEnvironmentSensor(SensorType.OBSTACLE) > 0) {
            getLog().infoSimTime(this, "Reading sensor");
        }

        for (Application application : applications) {
            final Event event = new Event(getOs().getSimulationTime() + 10, application, message);
            this.getOs().getEventManager().addEvent(event);
        }

        final MessageRouting routing = getOperatingSystem()
                .getAdHocModule()
                .createMessageRouting()
                .topoBroadCast();

        String name = getOs().getVehicleData().getName();
        String lane = getOs().getVehicleData().getLaneAreaId(); // devolve NULL
        CartesianPoint position = getOs().getVehicleData().getPosition().toCartesian();

        double pos_x = position.getX();
        double pos_y = position.getY();

        String position_string = "(" + pos_x + ", " + pos_y + ")";

        String message_to_send = name + " | " + lane + " | " + position_string;

        getLog().infoSimTime(this, "Sent message to others cars = '{}'", message_to_send);

        getOs().getAdHocModule().sendV2xMessage(new InterVehicleMsg(routing, message_to_send));
    }

    /*
    ##########################################################################################################################################3
    */

    @Override
    public void onMessageReceived(ReceivedV2xMessage receivedV2xMessage) {

        String position = "";
        String id_carro = "";
        String lane = "";


        if (!(receivedV2xMessage.getMessage() instanceof InterVehicleMsg)) {
            return;
        }

        String padrao = "\\|"; // Divide na barra vertical, removendo espaços em branco antes e depois

        // Dividir a frase em secret e route
        String[] partes = ((InterVehicleMsg) receivedV2xMessage.getMessage()).getMessage().split(padrao);

        if (partes.length == 2) {
            id_carro = "" + partes[0].trim(); // Remover espaços em branco antes e depois do secret
            lane = "" + partes[1].trim(); // Remover espaços em branco antes e depois do route
            position = "" + partes[2].trim(); // Remover espaços em branco antes e depois do route

        } else {
            System.out.println("Formato da frase inválido.");
        }

        vizinhos.put(id_carro, position);

        String message = receivedV2xMessage.getMessage().toString();
        getLog().infoSimTime(this, "Received InterVehicleMsg  = '{}'", message);
        getLog().infoSimTime(this, "My neighbours  = '{}'", vizinhos.toString());
    }

    /*
    ##########################################################################################################################################3
    */

    @Override
    public void processEvent(Event event) throws Exception {
        getLog().infoSimTime(this, "Received event: {}", getOs().getSimulationTimeMs(), event.getResourceClassSimpleName());
    }

    @Override
    public void onShutdown() {
        getLog().infoSimTime(this, "Shutdown application");
    }

    @Override
    public void onAcknowledgementReceived(ReceivedAcknowledgement acknowledgedMessage) {
    }

    @Override
    public void onCamBuilding(CamBuilder camBuilder) {
    }

    @Override
    public void onMessageTransmitted(V2xMessageTransmission v2xMessageTransmission) {
    }

    /*
    ##########################################################################################################################################3
    */

}