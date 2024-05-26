package src.main.java;

import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.AdHocModuleConfiguration;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.os.VehicleOperatingSystem;
import org.eclipse.mosaic.lib.enums.AdHocChannel;
import org.eclipse.mosaic.lib.geo.CartesianPoint;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.vehicle.SurroundingVehicle;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.rti.TIME;
import org.eclipse.mosaic.lib.objects.vehicle.VehicleRoute;

import java.util.List;
import java.util.Objects;

public final class VehicleToTrafficLightApp extends AbstractApplication<VehicleOperatingSystem> {
    private final static long TIME_INTERVAL = TIME.SECOND;

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

    //Use TopoBroadcast instead of GeoBroadcast because latter is not compatible with OMNeT++ or ns-3
    private void sendTopoBroadcastMessage() {

         if(true) {

             final MessageRouting routing = getOperatingSystem()
                     .getAdHocModule()
                     .createMessageRouting()
                     .topoBroadCast();

             VehicleRoute car_route = getOs().getNavigationModule().getCurrentRoute();
             String route_id = "";

             if (car_route != null) {
                 route_id = car_route.getId();

                 getLog().infoSimTime(this, "My Route = " + route_id);

                 String message_to_send = TrafficLightApp.SECRET + " | " + route_id;
                 getOs().getAdHocModule().sendV2xMessage(new GreenWaveMsg(routing, message_to_send));
                 getLog().infoSimTime(this, "Sent secret passphrase");

            }
         }

         if(!EventSendingApp.ackRSU){
             // fora do range da RSU entao manda o SECRET para o carro que estiver mais proximo na sua frente

             List<SurroundingVehicle> vizinhos = getOs().getVehicleData().getVehiclesInSight();
             CartesianPoint mypos = getOs().getVehicleData().getPosition().toCartesian();
             double temp = 0;

             double currentDirection = getOs().getVehicleData().getHeading();

             String carroMaisLonge = "";    // carro mais proximo da RSU dentro da range do carro

             for(SurroundingVehicle car : vizinhos){
                 CartesianPoint pos = car.getGeographicPosition().toCartesian();

                 if(pos.distanceTo(mypos) >= temp && isInFront(mypos, pos, currentDirection)){
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
         }
    }

    private void sample() {
        getOs().getEventManager().addEvent(getOs().getSimulationTime() + TIME_INTERVAL, this);
        if(getOs().getVehicleData().isStopped()) {
            sendTopoBroadcastMessage();
        }
    }

    @Override
    public void onStartup() {
        getLog().infoSimTime(this, "Initialize application");
        AdHocModuleConfiguration configuration = new AdHocModuleConfiguration()
                .addRadio()
                .channel(AdHocChannel.CCH)
                .power(17)
                .distance(300)
                .create();
        getOs().getAdHocModule().enable(configuration);
        getLog().infoSimTime(this, "Activated WLAN Module");
        getLog().infoSimTime(this, "ACK = {}", EventSendingApp.ackRSU);
        sample();
    }

    @Override
    public void onShutdown() {
        getLog().infoSimTime(this, "Shutdown application");
    }

    @Override
    public void processEvent(Event event) throws Exception {
        if (!isValidStateAndLog()) {
            return;
        }
        getLog().infoSimTime(this, "Processing event");
        sample();
    }

}
