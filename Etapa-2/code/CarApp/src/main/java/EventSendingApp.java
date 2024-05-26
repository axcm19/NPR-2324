package src.main.java;

//import com.sun.istack.internal.Nullable;
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
import org.eclipse.mosaic.lib.objects.vehicle.SurroundingVehicle;
import org.eclipse.mosaic.lib.objects.vehicle.VehicleData;
import org.eclipse.mosaic.lib.objects.vehicle.VehicleRoute;
import org.eclipse.mosaic.lib.util.scheduling.Event;


import java.util.HashMap;
import java.util.List;

public class EventSendingApp extends AbstractApplication<VehicleOperatingSystem> implements VehicleApplication, CommunicationApplication {
    /**
     * Used for choosing a RAND id for the message that is sent intra-vehicle.
     */
    private final static int MAX_ID = 1000;

    public static boolean ackRSU = false;

    private HashMap<String, String> vizinhos = new HashMap<>();   // key -> name_carro ; value -> position_carro

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

    private boolean isInFront(CartesianPoint mypos, CartesianPoint otherPos, double currentDirection) {
        // Vetor da posição do veículo atual ao carro vizinho
        double dx = otherPos.getX() - mypos.getX();
        double dy = otherPos.getY() - mypos.getY();

        // Ângulo do vetor em relação ao norte
        double angleToCar = Math.toDegrees(Math.atan2(dy, dx));
        if (angleToCar < 0) {
            angleToCar += 360;
        }

        // Verifique se o ângulo está dentro de um intervalo de 45 graus da direção do veículo
        double minAngle = (currentDirection - 45 + 360) % 360;
        double maxAngle = (currentDirection + 45) % 360;

        if (minAngle > maxAngle) {
            // O intervalo cruza o norte (0 graus)
            return angleToCar >= minAngle || angleToCar <= maxAngle;
        } else {
            return angleToCar >= minAngle && angleToCar <= maxAngle;
        }
    }

    @Override
    public void onVehicleUpdated(/*@Nullable*/ VehicleData previousVehicleData, /*@Nonnull*/ VehicleData updatedVehicleData) {
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

    @Override
    public void onMessageReceived(ReceivedV2xMessage receivedV2xMessage) {

        String position = "";
        String id_carro = "";
        String lane = "";


        if (receivedV2xMessage.getMessage() instanceof RSUMsg && receivedV2xMessage.getMessage().toString().equals("ACK")) {
            // quando recebe da RSU

            getLog().infoSimTime(this, "Received ACK from RSU at ", getOs().getSimulationTime());
            ackRSU = true;

            return;
        }

        if(receivedV2xMessage.getMessage() instanceof InterVehicleMsg) {

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

            //--------------------------------------------------------------------
            // reencaminha a mensagem para o que estiver mais longe dele até chegar à RSU

            List<SurroundingVehicle> vizinhos = getOs().getVehicleData().getVehiclesInSight();
            CartesianPoint mypos = getOs().getVehicleData().getPosition().toCartesian();
            double temp = 0;

            double currentDirection = getOs().getVehicleData().getHeading();

            String carroMaisLonge = "";    // carro mais proximo da RSU dentro da range do carro

            for (SurroundingVehicle car : vizinhos) {
                CartesianPoint pos = car.getGeographicPosition().toCartesian();

                if (pos.distanceTo(mypos) >= temp && isInFront(mypos, pos, currentDirection)) {
                    temp = pos.distanceTo(mypos);
                    carroMaisLonge = car.getId();
                }
            }

            final MessageRouting routing = getOperatingSystem()
                    .getAdHocModule()
                    .createMessageRouting()
                    .topoCast(carroMaisLonge, 1);

            VehicleRoute car_route = getOs().getNavigationModule().getCurrentRoute();
            String route_id = "";

            if (car_route != null) {
                route_id = car_route.getId();
            }

            getLog().infoSimTime(this, "My Route = " + route_id);

            String message_to_send = TrafficLightApp.SECRET + " | " + route_id;
            getOs().getAdHocModule().sendV2xMessage(new InterVehicleMsg(routing, message_to_send));
            getLog().infoSimTime(this, "Sent secret passphrase");

            //--------------------------------------------------------------------
        }
    }

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

}
