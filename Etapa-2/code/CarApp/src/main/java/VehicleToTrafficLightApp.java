package src.main.java;

import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.AdHocModuleConfiguration;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.CamBuilder;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedAcknowledgement;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedV2xMessage;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.Application;
import org.eclipse.mosaic.fed.application.app.api.CommunicationApplication;
import org.eclipse.mosaic.fed.application.app.api.os.VehicleOperatingSystem;
import org.eclipse.mosaic.interactions.communication.V2xMessageTransmission;
import org.eclipse.mosaic.lib.enums.AdHocChannel;
import org.eclipse.mosaic.lib.geo.CartesianPoint;
import org.eclipse.mosaic.lib.geo.MutableCartesianPoint;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.vehicle.VehicleData;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.rti.TIME;
import org.eclipse.mosaic.lib.objects.vehicle.VehicleRoute;
import org.eclipse.mosaic.fed.application.app.api.VehicleApplication;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VehicleToTrafficLightApp extends AbstractApplication<VehicleOperatingSystem> implements VehicleApplication, CommunicationApplication{

    private final static long TIME_INTERVAL = TIME.SECOND;
    private boolean ackRSU = false;

    private HashMap<String, String> vizinhos = new HashMap<>();   // key -> name_carro ; value -> position_carro

    /*
    ##########################################################################################################################################3
    */

    public void putVizinho(String id, String pos){
        this.vizinhos.put(id, pos);
    }

    /*
    ##########################################################################################################################################3
    */

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

    /*
    ##########################################################################################################################################3
    */

    // Tenta enviar mensagem à RSU em broadcast
    private void sendMsgToRSU(String message_to_send) {
        getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");

        final MessageRouting routing = getOperatingSystem()
                .getAdHocModule()
                .createMessageRouting()
                .topoCast("rsu_0", 1);

        getOs().getAdHocModule().sendV2xMessage(new GreenWaveMsg(routing, message_to_send));
        getLog().infoSimTime(this, "Sent to RSU = '{}'", message_to_send);

        getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
    }

    /*
    ##########################################################################################################################################3
    */

    public static CartesianPoint parseXY(String input) {
        // input = CartesianPoint{x=120.30,y=528.64,z=0.00}
        // Define o padrão da expressão regular para capturar os valores de x e y
        Pattern pattern = Pattern.compile("x=([\\d.]+),y=([\\d.]+)");
        Matcher matcher = pattern.matcher(input);

        double[] xy = new double[2];

        if (matcher.find()) {
            // Captura os valores de x e y a partir dos grupos de captura
            xy[0] = Double.parseDouble(matcher.group(1));
            xy[1] = Double.parseDouble(matcher.group(2));
        } else {
            throw new IllegalArgumentException("A string fornecida não está no formato esperado.");
        }

        return new MutableCartesianPoint(xy[0], xy[1], 0);
    }

    // Tenta enviar mensagem ao carro mais longe na sua range
    public void sendMsgToCars(String message_to_send) {
        getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");

        //List<SurroundingVehicle> vizinhos = getOs().getVehicleData().getVehiclesInSight();

         // imprimir vizinhos
        for(String car_name : vizinhos.keySet()){
            getLog().infoSimTime(this, "Vizinho: {}", car_name);
        }

         if(vizinhos.isEmpty()){
             getLog().infoSimTime(this, "I have no neighbours to send!");
             getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
         }

         else {

             CartesianPoint mypos = getOs().getVehicleData().getPosition().toCartesian();
             double temp = 0;

             double currentDirection = getOs().getVehicleData().getHeading();

             String carroMaisLonge = "";    // carro mais proximo da RSU dentro da range do carro

             for (Map.Entry e : vizinhos.entrySet()) {
                 String pos = (String) e.getValue();
                 String name = (String) e.getKey();

                 getLog().infoSimTime(this, "Position = {}", pos);

                 CartesianPoint pos_cart = parseXY(pos);

                 if (pos_cart.distanceTo(mypos) >= temp && isInFront(mypos, pos_cart, currentDirection)) {
                     temp = pos_cart.distanceTo(mypos);
                     carroMaisLonge = name;
                 }
             }

             final MessageRouting routing = getOperatingSystem()
                     .getAdHocModule()
                     .createMessageRouting()
                     .topoCast(carroMaisLonge, 1);


             getOs().getAdHocModule().sendV2xMessage(new GreenWaveMsg(routing, message_to_send));
             getLog().infoSimTime(this, "Sent secret passphrase to {}", carroMaisLonge);


             getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
         }
    }

    /*
    ##########################################################################################################################################3
    */

    @Override
    public void onMessageReceived(ReceivedV2xMessage receivedV2xMessage) {

        if (receivedV2xMessage.getMessage() instanceof RSUMsg && receivedV2xMessage.getMessage().toString().equals("ACK")) {
            // quando recebe da RSU
            getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
            getLog().infoSimTime(this, "Received ACK from RSU at ", getOs().getSimulationTime());
            this.ackRSU = true;
            getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
        }

        if(receivedV2xMessage.getMessage() instanceof GreenWaveMsg) {
            getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");

            String message = receivedV2xMessage.getMessage().toString();

            // primeiro manda para a RSU
            sendMsgToRSU(message);

            String id_carro = receivedV2xMessage.getMessage().getRouting().getSource().getSourceName();
            getLog().infoSimTime(this, "Received GreenWaveMsg  = '{}' from {}", message, id_carro);

            // reencaminha a mensagem para o que estiver mais longe dele até chegar à RSU
            sendMsgToCars(message);

            //--------------------------------------------------------------------
            getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
        }

        if(receivedV2xMessage.getMessage() instanceof SendVizinhoMsg) {
            getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");

            String id = ((SendVizinhoMsg) receivedV2xMessage.getMessage()).getID();
            String pos = ((SendVizinhoMsg) receivedV2xMessage.getMessage()).getPos();
            getLog().infoSimTime(this, "Received message from myself");

            // reencaminha a mensagem para o que estiver mais longe dele até chegar à RSU
            this.vizinhos.put(id, pos);

            //--------------------------------------------------------------------
            getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
        }
    }

    /*
    ##########################################################################################################################################3
    */

    private void sendGreenWaveMessage(){
        getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");

        // se já tem o ACK não comunica com ninguém
        if (this.ackRSU) {
            getLog().infoSimTime(this, "Already have ACK");
            getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
        }

        else {
            // cria a mensagem a mandar
            VehicleRoute car_route = getOs().getNavigationModule().getCurrentRoute();
            String route_id = "";
            String message_to_send = "";
            String id_carro = getOs().getVehicleData().getName();

            if (car_route != null) {
                route_id = car_route.getId();

                getLog().infoSimTime(this, "My Route = " + route_id);
                message_to_send = TrafficLightApp.SECRET + " | " + route_id + " | " + id_carro;
            }

            // se a manesagem tiver conteudo envia
            if (!message_to_send.equals("")) {

                sendMsgToRSU(message_to_send);
                getLog().infoSimTime(this, "Tried sending message to RSU");
                getLog().infoSimTime(this, "ACK = {}", this.ackRSU);

                // se não receber um ACK da RSU tenta mandar para o veículo à sua frente que esteja mais
                // longe mas dentro do seu range
                if (!this.ackRSU) {
                    sendMsgToCars(message_to_send);
                    getLog().infoSimTime(this, "Tried sending message to other car");
                    getLog().infoSimTime(this, "ACK = {}", this.ackRSU);
                }
            }

            getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
        }
    }

    /*
    ##########################################################################################################################################3
    */

    private void sample() {
        getOs().getEventManager().addEvent(getOs().getSimulationTime() + TIME_INTERVAL, this);
        if(!(Objects.requireNonNull(getOs().getVehicleData()).getSpeed() > 0)){
            sendGreenWaveMessage();
        }
    }

    /*
    ##########################################################################################################################################3
    */

    @Override
    public void onVehicleUpdated(/*@Nullable*/ VehicleData previousVehicleData, /*@Nonnull*/ VehicleData updatedVehicleData) {
        final List<? extends Application> applications = getOs().getApplications();
        sample();
    }

    /*
    ##########################################################################################################################################3
    */

    @Override
    public void onStartup() {
        getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");

        getLog().infoSimTime(this, "Initialize application");
        AdHocModuleConfiguration configuration = new AdHocModuleConfiguration()
                .addRadio()
                .channel(AdHocChannel.CCH)
                .power(16)
                .distance(20)
                .create();
        getOs().getAdHocModule().enable(configuration);
        getLog().infoSimTime(this, "Activated WLAN Module");
        getLog().infoSimTime(this, "ACK = {}", this.ackRSU);
        //sample();
        getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
    }

    /*
    ##########################################################################################################################################3
    */

    @Override
    public void onShutdown() {
        getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
        getLog().infoSimTime(this, "Shutdown application");
        getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
    }

    /*
    ##########################################################################################################################################3
    */

    @Override
    public void processEvent(Event event) throws Exception {
        getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
        if (!isValidStateAndLog()) {
            return;
        }
        getLog().infoSimTime(this, "Processing event");
        sample();
        getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
    }

    /*
    ##########################################################################################################################################3
    */

    @Override
    public void onAcknowledgementReceived(ReceivedAcknowledgement receivedAcknowledgement) {

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
