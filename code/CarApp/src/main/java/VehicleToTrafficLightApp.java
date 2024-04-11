import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.AdHocModuleConfiguration;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.os.VehicleOperatingSystem;
import org.eclipse.mosaic.lib.enums.AdHocChannel;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.rti.TIME;
import org.eclipse.mosaic.lib.objects.vehicle.VehicleRoute;

public final class VehicleToTrafficLightApp extends AbstractApplication<VehicleOperatingSystem> {
    private final static long TIME_INTERVAL = TIME.SECOND;

    //Use TopoBroadcast instead of GeoBroadcast because latter is not compatible with OMNeT++ or ns-3
    private void sendTopoBroadcastMessage() {


        final MessageRouting routing = getOperatingSystem()
                .getAdHocModule()
                .createMessageRouting()
                .topoBroadCast();

        VehicleRoute car_route = getOs().getNavigationModule().getCurrentRoute();
        String route_id = "";

        if(car_route != null) {
            route_id = car_route.getId();
        }

        getLog().infoSimTime(this, "My Route = " + route_id);

        String message_to_send = TrafficLightApp.SECRET + " | " + route_id;
        getOs().getAdHocModule().sendV2xMessage(new GreenWaveMsg(routing, message_to_send));
        getLog().infoSimTime(this, "Sent secret passphrase");
    }

    private void sample() {
        getOs().getEventManager().addEvent(
                getOs().getSimulationTime() + TIME_INTERVAL, this
        );
        sendTopoBroadcastMessage();
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
