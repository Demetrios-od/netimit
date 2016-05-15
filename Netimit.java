/* Imitating a cellular network

(c) Bukhan D.Yu. 2011-2014
*/

import java.util.*;
import java.lang.Math.*;

class Netimit {
  public static void main(String[] args) {
    System.out.println("Start modeling a cellular network");

    // Ввод исходных данных и инициализация массивов
    BSController[] BSCs = new BSController[1];   // количество КБС, пока всегда равно 1
    BaseStation[] BTSs = new BaseStation [3];    // количество БС
    Subscriber[] subs = new Subscriber[3];       // количество абонентов
    int Niters = 2;                              // количество итераций
    float[] borders = {0.0f, 0.0f, 5.0f, 5.0f};
    float[] speedlim = {0.02f, 0.2f};
    float cp = 0.02f;
    float[] bsx = {1.50f, 3.50f, 2.50f};
    float[] bsy = {1.75f, 1.75f, 3.75f};
    float rad = 1.25f;

    // Создание сети и абонентов
    for (int i=0; i<BSCs.length; i++) {
      BSCs[i] = new BSController(BTSs.length);
    };

    for (int i=0; i<BTSs.length; i++) {
      BTSs[i] = new BaseStation(bsx[i], bsy[i], rad);
      if (!BTSs[i].bindToBSC(BSCs[0])) {
        System.out.println("Base station wasn\'t binded to BSC");
      };
    };

    if (!Subscriber.defineServiceArea(borders)) return;
    for (int i=0; i<subs.length; i++) {
      subs[i] = new Subscriber(borders, cp);
    };

    // Начинаем итерационный процесс
    for (int i=0; i<Niters; i++) {
      System.out.print("Iter ");
      System.out.println(i);
      for (Subscriber s: subs) {
        s.move();
      };
    };
    System.out.println("Simulation finished");
  }
}

class Subscriber {
  private static float xmin, xmax, ymin, ymax;
  private float x, y;
  private float dir, speed;
  private int act;
  private float cp;   // call appearance probability
  private PhyChannel chan;
  private BaseStation bts;

  private void setCallProbability (float p) {
    if ((p >= 1) || (p < 0)) {
      System.out.println("ERROR: Incorrect probbaility of call appearance.");
      cp = 0;
      return;
    };
    cp = p;
  }

  private void setSpeed (float v) {
    if (v < 0) {
      System.out.println("ERROR: Incorrect speed value.");
      return;
    };
    speed = v;
  }

  private void setSpeed (float vmin, float vmax) {
    if (vmin > vmax) {
      System.out.println("ERROR: Incorrect speed limits.");
      return;
    };
    Random rnd = new Random();
    speed = vmin + (vmax-vmin)*rnd.nextFloat();
  }

  Subscriber (float[] speedlim, float icp) {
    Random rnd = new Random();
    x = xmin + (xmax-xmin)*rnd.nextFloat();
    y = ymin + (ymax-ymin)*rnd.nextFloat();
    dir = 2*(float)Math.PI*rnd.nextFloat();
    setSpeed(speedlim[0], speedlim[1]);
    act = 0;
    chan = null;
    bts = BSController.findClosestBTS(x, y);
    setCallProbability(icp);
    System.out.print("Subscriber created: x=");
    System.out.print(x);
    System.out.print(", y=");
    System.out.println(y);
  }

  Subscriber (float ix, float iy, float idir, float ispeed, float icp) {
    if (ix < xmin) {x = xmin;}
      else if (ix > xmax) {x = xmax;}
        else {x = ix;};
    if (iy < ymin) {y = ymin;}
      else if (iy > ymax) {y = ymax;}
        else {y = iy;};
    dir = idir;
    setSpeed(ispeed);
    act = 0;
    chan = null;
    bts = BSController.findClosestBTS(x, y);
    setCallProbability(icp);
  }

  public static boolean defineServiceArea (float[] borders) {
    if ((borders[0] >= borders[2]) || (borders[1] >= borders[3])) {
      System.out.println("ERROR: Incorrect area limits.");
      return false;
    };
    xmin = borders[0];
    xmax = borders[2];
    ymin = borders[1];
    ymax = borders[3];
    return true;
  }

  private boolean isOutOfX (float x, float y) {
    return (x < xmin) || (x > xmax);
  }

  private boolean isOutOfY (float x, float y) {
    return (y < ymin) || (y > ymax);
  }

  public void move() {
    // direction and speed don't change
    float dx = speed*(float)Math.cos(dir);
    float dy = speed*(float)Math.sin(dir);
    x += dx;
    y += dy;
    if (isOutOfX(x, y)) {
      x -= dx;
      dir = (float)Math.PI-dir;
    };
    if (isOutOfY(x, y)) {
      y -= dy;
      dir = 2*(float)Math.PI-dir;
    };
    // here we could change 'cp' if it depends on coordinates

    // checking the current BTS
    if (bts == null) {   // we're out of the service zone
      bts = BSController.findClosestBTS(x, y);   // maybe just came in
    };
    if (bts == null) return;   // still out of service
    // here we're in the service zone, and BTS is defined
    if (!bts.isInServiceArea(this)) {   // condition to raise handover
      BaseStation newbts = BSController.findClosestBTS(this);
      if (newbts != null) {
        chan = bts.handover(newbts);   // came to a new BTS
      } else {
        terminate();   // came out of the service zone
      };
      bts = newbts;
    };

    if (act > 0) {
      if (--act == 0) {
        terminate();
      };
    } else {
      Random rnd = new Random();
      if (cp > rnd.nextFloat()) {
        call();
      };
    };
    System.out.println("Subscriber moved");
  }

  public void move (float ddir, float dspeed) {
    // ddir - difference added to direction, radians
    // dspeed - difference to speed, -1 < dspeed < 1 (recommended)
    dir += ddir;
    speed += speed*dspeed;
    move();
  }

  public boolean call () {
    return true;
  }

  public boolean terminate () {
    return true;
  }

  public PhyChannel getChannel () {
    return chan;
  }

  public void setChannel (PhyChannel ich) {
    chan = ich;
  }

  public void clearChannel () {
    chan = null;
  }

  public float[] getCoordinates () {
    float[] coords = {x, y};
    return coords;
  }
}

class BaseStation {
  private float x, y, rad;
  private BSController BSC;
  private Carrier[] cars;

  BaseStation (float ix, float iy, float irad) {
    x = ix;
    y = iy;
    rad = irad;
    cars = new Carrier[1];
    System.out.println("BTS created");
  }

  public boolean bindToBSC (BSController bsc) {
    boolean ans = false;
    if (bsc.bindNewBTS(this)) {
      BSC = bsc;
      ans = true;
    };
    return ans;
  }

  public boolean unbindFromBSC (BSController bsc) {
    boolean ans = false;
    if (bsc.unbindBTS(this)) {
      BSC = null;
      ans = true;
    };
    return ans;
  }

  private float distanceTo (Subscriber sub) {
    float[] cs = sub.getCoordinates();
    return (float)Math.sqrt((cs[0]-x)*(cs[0]-x) + (cs[1]-y)*(cs[1]-y));
  }

  private float distanceTo (float ix, float iy) {
    return (float)Math.sqrt((ix-x)*(ix-x) + (iy-y)*(iy-y));
  }

  public boolean isInServiceArea (Subscriber sub) {
    return rad > distanceTo(sub);
  }

  public boolean isInServiceArea (float ix, float iy) {
    return rad > distanceTo(ix, iy);
  }

  public float signalLevel (Subscriber sub) {
    return 1/distanceTo(sub);
  }

  public float signalLevel (float ix, float iy) {
    return 1/distanceTo(ix, iy);
  }

  public PhyChannel handover (BaseStation oldbts) {
    return BSC.handover(oldbts, this);
  }
}

class BSController {
  private static BaseStation[] BTSs;   // не должно быть static

  BSController (int nbts) {
    BTSs = new BaseStation[nbts];
    System.out.println("BSC created");
  }

  public boolean bindNewBTS (BaseStation newBS) {
    boolean ans = false;
    for (int i=0; i<BTSs.length; i++) {
      if (BTSs[i] == null) {
        BTSs[i] = newBS;
        ans = true;
        break;
      };
    };
    return ans;
  }

  public boolean unbindBTS (BaseStation bsToUnbind) {
    boolean ans = false;
    for (int i=0; i<BTSs.length; i++) {
      if (BTSs[i] == bsToUnbind) {
        BTSs[i] = null;
        ans = true;
        break;
      };
    };
    return ans;
  }

  public static BaseStation findClosestBTS (float x, float y) {
    float slmin = 0;   // signal level
    BaseStation foundBTS = null;
    for (BaseStation bs: BTSs) {
      float sl = bs.signalLevel(x, y);
      if (bs.isInServiceArea(x, y) || slmin > sl) {
        slmin = sl;
        foundBTS = bs;
      };
    };
    return foundBTS;   // base station or null
  }

  public static BaseStation findClosestBTS (Subscriber sub) {
    float slmin = 0;   // signal level
    BaseStation foundBTS = null;
    float[] cs = sub.getCoordinates();
    for (BaseStation bs: BTSs) {
      float sl = bs.signalLevel(cs[0], cs[1]);
      if (bs.isInServiceArea(cs[0], cs[1]) || slmin > sl) {
        slmin = sl;
        foundBTS = bs;
      };
    };
    return foundBTS;   // base station or null
  }

  public PhyChannel handover (BaseStation oldbts, BaseStation newbts) {
    return new PhyChannel (new Carrier(7), 1);
  }
}

class Carrier {
  private int channelsPerCarrier;
  private Subscriber[] connectedSubscribers;

  Carrier (int icpc) {
    if (!setChannelsPerCarrier(icpc)) return;
    connectedSubscribers = new Subscriber[icpc];
  }

  public boolean setChannelsPerCarrier (int icpc) {
    if (icpc <= 0) {
      System.out.println("ERROR: Incorrect number of channels per carrier");
      return false;
    };
    channelsPerCarrier = icpc;
    return true;
  }

  public int getChannelsPerCarrier () {
    return channelsPerCarrier;
  }

  public PhyChannel getChannelForSubscriebr(Subscriber sub) {
    int i = -1;
    while (++i<connectedSubscribers.length) {
      if (connectedSubscribers[i] == null) {
        connectedSubscribers[i] = sub;
        break;
      };
    };
    i = (i == connectedSubscribers.length)? 0 : i;
    return new PhyChannel(this, i);
  }

  public boolean stopUseChannel(int n) {
    if (n <= 0 ||
        n > channelsPerCarrier ||
        connectedSubscribers[n] == null)
      return false;
    connectedSubscribers[n].clearChannel();
    connectedSubscribers[n] = null;
    return true;
  }

  public boolean getOffSubscriber (Subscriber sub) {
    boolean ans = false;
    for (int i=0; i<connectedSubscribers.length; i++) {
      if (connectedSubscribers[i] == sub) {
        connectedSubscribers[i] = null;
        sub.clearChannel();
        ans = true;
        break;
      };
    };
    return ans;
  }

  public boolean isEmpty() {
    boolean x = true;
    for (Subscriber sub: connectedSubscribers) {
      x = x && (sub == null);
    };
    return x;
  }

  public boolean isFull() {
    boolean x = true;
    for (Subscriber sub: connectedSubscribers) {
      x = x && (sub != null);
    };
    return x;
  }
}

class PhyChannel {
  private Carrier car;
  private int chan;

  PhyChannel (Carrier icar, int ichan) {
    if (icar == null || ichan <= 0 || ichan > icar.getChannelsPerCarrier()) {
      System.out.println("ERROR: Incorrect physical channel parameters");
      return;
    };
    car = icar;
    chan = ichan;
  }

  public Carrier getCarrier () {
    return car;
  }

  public int getChannelNumber () {
    return chan;
  }
}

