/** 
 * Imitating a cellular network
 * 
 * (c) Bukhan D.Yu. 2011-2014
 */
package od.demetrios.netimit;

public class Netimit {
  
  public static void main(String[] args) {
    System.out.println("Start modeling a cellular network");

    // Ввод исходных данных и инициализация массивов
    BSController[] BSCs = new BSController[1];   // количество КБС, пока всегда равно 1
    BaseStation[] BTSs = new BaseStation [3];    // количество БС
    Subscriber[] subs = new Subscriber[3];       // количество абонентов
    int Niters = 30;                             // количество итераций
    float[] borders = {0.0f, 0.0f, 5.0f, 5.0f};
    float[] speedlim = {0.05f, 0.5f};
    float cp = 0.2f;
    int maxDuration = 5;       // call duration
    float[] bsx = {1.50f, 3.50f, 2.50f};
    float[] bsy = {1.75f, 1.75f, 3.75f};
    float[] rad = {1.25f, 1.25f, 1.25f};
    int cluster = 3;                        // размерность кластера
    Carrier[] cars = new Carrier[6];        // число несущих
    for (int i=0; i<cars.length; i++) {
      cars[i] = new Carrier(i, 7);          // число каналов на несущей
    };

    // Создание сети и абонентов
    for (int i=0; i<BSCs.length; i++) {
      BSCs[i] = new BSController("dynamic", BTSs.length, cars, cluster);
    };

    for (int i=0; i<BTSs.length; i++) {
      BTSs[i] = new BaseStation(bsx[i], bsy[i], rad[i]);
      if (!BTSs[i].bindToBSC(BSCs[0])) {
        System.out.println("Base station wasn\'t binded to BSC");
      };
    };

    if (!Subscriber.defineServiceArea(borders)) return;
    for (int i=0; i<subs.length; i++) {
      subs[i] = new Subscriber(speedlim, cp, maxDuration);
    };

    // Начинаем итерационный процесс
    Counter.clear();
    for (int i=0; i<Niters; i++) {
      //System.out.println("\nIter "+i);
      for (Subscriber s: subs) {
        s.move();
      };
    };
    
    System.out.println("Simulation finished for " + Niters + " iterations");
    Counter.printAll();
    System.out.println("Probability of call loss: " + Counter.probabCallLoss()*100 + "%");
  }
  
}
