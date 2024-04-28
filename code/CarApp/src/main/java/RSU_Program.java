package src.main.java;//import InterVehicleMsg;

import org.apache.commons.lang3.Validate;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.AdHocModuleConfiguration;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.CamBuilder;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedAcknowledgement;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedV2xMessage;
import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.CommunicationApplication;
import org.eclipse.mosaic.fed.application.app.api.os.RoadSideUnitOperatingSystem;
import org.eclipse.mosaic.interactions.communication.V2xMessageTransmission;
import org.eclipse.mosaic.lib.enums.AdHocChannel;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.lib.util.scheduling.EventProcessor;

import java.util.*;

import static src.main.java.TrafficLightApp.*;

public class RSU_Program extends AbstractApplication<RoadSideUnitOperatingSystem> implements CommunicationApplication {
   private static final long TIME_INTERVAL = 2000000000L;
   private HashMap<String, Set<String>> carros = new HashMap<>();   // key -> id_route ; value -> set de ids de carros
   private Set<String> temp = new HashSet<>();   // value -> set de ids de todos os carros que já passaram


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
      Set r0_cars = new HashSet<String>();
      Set r1_cars = new HashSet<String>();
      this.carros.put("r_0", r0_cars);
      this.carros.put("r_1", r1_cars);
   }

   public void onShutdown() {
      this.getLog().infoSimTime(this, "Shutdown application", new Object[0]);
   }

   public void processEvent(Event event) throws Exception {
      this.sample();
   }


   private void sendTopoBroadcastMessage(String message) {

      final MessageRouting routing = getOperatingSystem()
              .getAdHocModule()
              .createMessageRouting()
              .topoBroadCast();


      getOs().getAdHocModule().sendV2xMessage(new RSUMsg(routing, message));
      getLog().infoSimTime(this, "Sent message for Traffic Ligth");
   }


   @Override
   public void onMessageReceived(ReceivedV2xMessage receivedV2xMessage) {

      String secret = "";
      String route = "";
      String id_carro = "";


      id_carro = receivedV2xMessage.getMessage().getRouting().getSource().getSourceName();

      if (!(receivedV2xMessage.getMessage() instanceof GreenWaveMsg)) {
         return;
      }

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

      // adiciona o id do carro ao set da route
      if(!(temp.contains(id_carro))) {
         temp.add(id_carro);
         carros.get(route).add(id_carro);
      }

      getLog().infoSimTime(this, "Route {} = {}", route, carros.get(route));

      Validate.notNull(receivedV2xMessage.getMessage().getRouting().getSource().getSourcePosition(),
              "The source position of the sender cannot be null");
      if (!(receivedV2xMessage.getMessage().getRouting().getSource().getSourcePosition()
              .distanceTo(getOs().getPosition()) <= MIN_DISTANCE)) {
         getLog().infoSimTime(this, "Vehicle that sent message is too far away.");
         return;
      }

      int counter_r0 = carros.get("r_0").size();
      int counter_r1 = carros.get("r_1").size();

      if (5 <= counter_r0 && counter_r1 < counter_r0) {
         getLog().infoSimTime(this, "Counter R0 = {} , Counter R1 = {}", counter_r0, counter_r1);
         sendTopoBroadcastMessage("0");
         carros.get("r_0").clear();
         getLog().infoSimTime(this, "Cleared: Counter R0 = {} , Counter R1 = {}", carros.get("r_0").size(), counter_r1);
      }
      //else if (DEFAULT_PROGRAM.equals(getOs().getCurrentProgram().getProgramId()) && counter_r0 < counter_r1) {
      else if (5 <= counter_r1 && counter_r1 > counter_r0){
         getLog().infoSimTime(this, "Counter R0 = {} , Counter R1 = {}", counter_r0, counter_r1);
         sendTopoBroadcastMessage("2");
         carros.get("r_1").clear();
         getLog().infoSimTime(this, "Cleared: Counter R0 = {} , Counter R1 = {}", counter_r0, carros.get("r_1").size());
      }

   }

   @Override
   public void onAcknowledgementReceived(ReceivedAcknowledgement receivedAcknowledgement) {

   }

   @Override
   public void onCamBuilding(CamBuilder camBuilder) {

   }

   @Override
   public void onMessageTransmitted(V2xMessageTransmission v2xMessageTransmission) {

   }

}
