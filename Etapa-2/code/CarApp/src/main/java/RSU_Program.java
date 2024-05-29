package src.main.java;

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
import org.eclipse.mosaic.lib.geo.CartesianPoint;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.lib.util.scheduling.EventProcessor;

import java.util.*;

import static src.main.java.TrafficLightApp.*;

public class RSU_Program extends AbstractApplication<RoadSideUnitOperatingSystem> implements CommunicationApplication {

   private static final long TIME_INTERVAL = 2000000000L;
   private final HashMap<String, Set<String>> carros = new HashMap<>();   // key -> id_route ; value -> set de ids de carros
   private final Set<String> temp = new HashSet<>();   // value -> set de ids de todos os carros que já passaram
   static final Integer MIN_DISTANCE_RSU = 20;

   static CartesianPoint RSU_pos;

   /*
    ##########################################################################################################################################3
    */

   private void RSU_position() {
      RSU_pos = getOs().getPosition().toCartesian();
   }

   /*
    ##########################################################################################################################################3
    */

   private static CartesianPoint get_RSU_position() {
      return RSU_pos;
   }

   /*
    ##########################################################################################################################################3
    */

   private void sendAdHocBroadcast() {
      MessageRouting routing = ((RoadSideUnitOperatingSystem)this.getOs()).getAdHocModule().createMessageRouting().viaChannel(AdHocChannel.CCH).topoBroadCast();
      RSUMsg message = new RSUMsg(routing, ((RoadSideUnitOperatingSystem)this.getOs()).getPosition().toString());
      ((RoadSideUnitOperatingSystem)this.getOs()).getAdHocModule().sendV2xMessage(message);
   }

   /*
    ##########################################################################################################################################3
    */

   public void sample() {
      ((RoadSideUnitOperatingSystem)this.getOs()).getEventManager().addEvent(((RoadSideUnitOperatingSystem)this.getOs()).getSimulationTime() + TIME_INTERVAL, new EventProcessor[]{this});
   }

   public void checkCounters(){
      if(!this.carros.isEmpty()) {

         int counter_r0 = this.carros.get("r_0").size();
         int counter_r1 = this.carros.get("r_1").size();

         getLog().infoSimTime(this, "Counter R0 = {} , Counter R1 = {}", counter_r0, counter_r1);

         if (1 <= counter_r0 && counter_r1 <= counter_r0) {
            sendTopoCastMessage("tl_0", 1, "0");
            this.carros.get("r_0").clear();
            getLog().infoSimTime(this, "Cleared: Counter R0 = {} , Counter R1 = {}", this.carros.get("r_0").size(), counter_r1);
         } else if (1 <= counter_r1) {
            sendTopoCastMessage("tl_0", 1, "2");
            this.carros.get("r_1").clear();
            getLog().infoSimTime(this, "Cleared: Counter R0 = {} , Counter R1 = {}", counter_r0, this.carros.get("r_1").size());
         }
      }
   }

   /*
    ##########################################################################################################################################3
    */

   public void onStartup() {
      getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");

      this.getLog().infoSimTime(this, "Initialize application", new Object[0]);
      ((RoadSideUnitOperatingSystem)this.getOs()).getAdHocModule().enable((new AdHocModuleConfiguration()).addRadio().channel(AdHocChannel.CCH).power(50.0D).create());
      this.getLog().infoSimTime(this, "Activated WLAN Module", new Object[0]);
      this.sample();
      Set<String> r0_cars = new HashSet<>();
      Set<String> r1_cars = new HashSet<>();
      this.carros.put("r_0", r0_cars);
      this.carros.put("r_1", r1_cars);

      RSU_position();

      getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
   }

   /*
    ##########################################################################################################################################3
    */

   public void onShutdown() {
      getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
      this.getLog().infoSimTime(this, "Shutdown application", new Object[0]);
      getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
   }

   public void processEvent(Event event) throws Exception {
      this.sample();
      checkCounters();
   }

   /*
    ##########################################################################################################################################3
    */

   private void sendTopoBroadcastMessage(String message) {
      getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");

      final MessageRouting routing = getOperatingSystem()
              .getAdHocModule()
              .createMessageRouting()
              .topoBroadCast();


      getOs().getAdHocModule().sendV2xMessage(new RSUMsg(routing, message));
      getLog().infoSimTime(this, "Sent message for Traffic Ligth");
      getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
   }

   /*
    ##########################################################################################################################################3
    */

   private void sendTopoCastMessage(String receiver, int hops, String message) {
      getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");

      final MessageRouting routing = getOperatingSystem()
              .getAdHocModule()
              .createMessageRouting()
              .topoCast(receiver, hops);


      getOs().getAdHocModule().sendV2xMessage(new RSUMsg(routing, message));
      getLog().infoSimTime(this, "Sent message for {}", receiver);

      getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");
   }

   /*
    ##########################################################################################################################################3
    */

   @Override
   public void onMessageReceived(ReceivedV2xMessage receivedV2xMessage) {
      getLog().infoSimTime(this, "-------------------------------------------------------------------------------------");

      // validar se a posição do carro está dentro do range da RSU
      Validate.notNull(receivedV2xMessage.getMessage().getRouting().getSource().getSourcePosition(),
              "The source position of the sender cannot be null");
      if (!(receivedV2xMessage.getMessage().getRouting().getSource().getSourcePosition()
              .distanceTo(getOs().getPosition()) <= MIN_DISTANCE_RSU)) {
         getLog().infoSimTime(this, "Vehicle that sent message is too far away.");
      }

      else {

         String secret = "";
         String route = "";
         String id_carro = "";

         if (!(receivedV2xMessage.getMessage() instanceof GreenWaveMsg)) {
            return;
         }

         String who_sent = receivedV2xMessage.getMessage().getRouting().getSource().getSourceName();

         String message = receivedV2xMessage.getMessage().toString();
         getLog().infoSimTime(this, "Received GreenWaveMsg = '{}' from {}", message, who_sent);

         String padrao = "\\|"; // Divide na barra vertical, removendo espaços em branco antes e depois

         // Dividir a frase em secret e route
         String[] partes = ((GreenWaveMsg) receivedV2xMessage.getMessage()).getMessage().split(padrao);

         if (partes.length == 3) {
            secret = "" + partes[0].trim(); // Remover espaços em branco antes e depois do secret
            route = "" + partes[1].trim(); // Remover espaços em branco antes e depois do route
            id_carro = partes[2].trim();;

         } else {
            getLog().infoSimTime(this, "Formato da frase inválido");
            return;
         }

         if (!(secret.equals(SECRET))) {
            return;
         }

         getLog().infoSimTime(this, "Received correct passphrase: {}", SECRET);

         // adiciona o id do carro ao set da route
         if (!(temp.contains(id_carro))) {
            temp.add(id_carro);
            carros.get(route).add(id_carro);
         }

         getLog().infoSimTime(this, "Route {} = {}", route, carros.get(route));

         // só envia a mensagem para o carro se este estiver dentro do range (depois de passar pelo IF anterior)
         getLog().infoSimTime(this, "Sending ACK to {}", id_carro);
         sendTopoCastMessage(id_carro, 4, "ACK");

         /*
         int counter_r0 = carros.get("r_0").size();
         int counter_r1 = carros.get("r_1").size();

         getLog().infoSimTime(this, "Counter R0 = {} , Counter R1 = {}", counter_r0, counter_r1);

         if (5 <= counter_r0 && counter_r1 < counter_r0) {
            sendTopoCastMessage("tl_0", 1, "0");
            carros.get("r_0").clear();
            getLog().infoSimTime(this, "Cleared: Counter R0 = {} , Counter R1 = {}", carros.get("r_0").size(), counter_r1);
         }

         else if (5 <= counter_r1) {
            sendTopoCastMessage("tl_0", 1, "2");
            carros.get("r_1").clear();
            getLog().infoSimTime(this, "Cleared: Counter R0 = {} , Counter R1 = {}", counter_r0, carros.get("r_1").size());
         }
         */
      }

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