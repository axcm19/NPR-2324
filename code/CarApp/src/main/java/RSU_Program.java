//import InterVehicleMsg;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.AdHocModuleConfiguration;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.os.RoadSideUnitOperatingSystem;
import org.eclipse.mosaic.lib.enums.AdHocChannel;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.lib.util.scheduling.EventProcessor;

public class RSU_Program extends AbstractApplication<RoadSideUnitOperatingSystem> {
   private static final long TIME_INTERVAL = 2000000000L;

   private void sendAdHocBroadcast() {
      MessageRouting routing = ((RoadSideUnitOperatingSystem)this.getOs()).getAdHocModule().createMessageRouting().viaChannel(AdHocChannel.CCH).topoBroadCast();
      InterVehicleMsg message = new InterVehicleMsg(routing, ((RoadSideUnitOperatingSystem)this.getOs()).getPosition());
      ((RoadSideUnitOperatingSystem)this.getOs()).getAdHocModule().sendV2xMessage(message);
   }

   public void sample() {
      ((RoadSideUnitOperatingSystem)this.getOs()).getEventManager().addEvent(((RoadSideUnitOperatingSystem)this.getOs()).getSimulationTime() + 2000000000L, new EventProcessor[]{this});
      this.getLog().infoSimTime(this, "Sending out AdHoc broadcast", new Object[0]);
      this.sendAdHocBroadcast();
   }

   public void onStartup() {
      this.getLog().infoSimTime(this, "Initialize application", new Object[0]);
      ((RoadSideUnitOperatingSystem)this.getOs()).getAdHocModule().enable((new AdHocModuleConfiguration()).addRadio().channel(AdHocChannel.CCH).power(50.0D).create());
      this.getLog().infoSimTime(this, "Activated WLAN Module", new Object[0]);
      this.sample();
   }

   public void onShutdown() {
      this.getLog().infoSimTime(this, "Shutdown application", new Object[0]);
   }

   public void processEvent(Event event) throws Exception {
      this.sample();
   }
}
