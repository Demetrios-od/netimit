/* Imitating a cellular network

(c) Bukhan D.Yu. 2011-2014
*/

import java.util.*;
import java.lang.Math.*;

class Netimit {
  public static void main(String[] args) {
    System.out.println("It works!");
  }
}

class Subscriber {
  private static float xmin, xmax, ymin, ymax;
  private float cp;   // call appearance probability
  private float x, y;
  private float dir, speed;
  private int active;
  private Carrier car;
  private BaseStation bts;
  // BaseStation[] neigBTS;

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
    if (speedmin > speedmax) {
      System.out.println("ERROR: Incorrect speed limits.");
      return;
    };
    Random rnd = new Random();
    speed = vmin + (vmax-vmin)*rnd.nextFloat();
  }

  Subscriber (float speedmin, float speedmax, float _cp) {
    Random rnd = new Random();
    x = xmin + (xmax-xmin)*rnd.nextFloat();
    y = ymin + (ymax-ymin)*rnd.nextFloat();
    dir = 2*(float)Math.PI*rnd.nextFloat();
    setSpeed(speedmin, speedmax);
    active = 0;
    car = null;
    bts = BSController.findClosestBTS(x, y);
    setCallProbability(_cp);
  }

  Subscriber (float _x, float _y, float _dir, float _speed, float _cp) {
    if (_x < xmin) {x = xmin;}
      else if (_x > xmax) {x = xmax;}
        else {x = _x;};
    if (_y < ymin) {y = ymin;}
      else if (_y > ymax) {y = ymax;}
        else {y = _y;};
    dir = _dir;
    setSpeed(_speed);
    active = 0;
    car = null;
    bts = BSController.findClosestBTS(x, y);
    setCallProbability(_cp);
  }

  private boolean isOutOfX (float x, float y) {
    return (x < xmin) || (x > xmax);
  }

  private boolean isOutOfY (float x, float y) {
    return (y < ymin) || (y > ymax);
  }

  static void defineServiceArea (float x1, float y1, float x2, float y2) {
    if ((x1 >= x2) || (y1 >= y2)) {
      System.out.println("ERROR: Incorrect area limits.");
      return;
    };
    xmin = x1;
    xmax = x2;
    ymin = y1;
    ymax = y2;
  }

  void move () {
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
    double x1 = x - bts.x;
    double y1 = y - bts.y;
    double dist = Math.sqrt(x1*x1 + y1*y1);
    if (dist > bts.rad) {   // condition to raise handover
      BaseStation oldbts = bts;
      bts = BSController.findClosestBTS(x, y);
      if (bts != null) {
        car = bts.bsc.handover(oldbts, bts);   // came to a new BTS
      } else {
        callTerminate(oldbts);   // came out of the service zone
      };
    };

    if (active > 0) {
      if (--active == 0) {
        callTerminate(bts);
      };
    } else {
      Random rnd = new Random();
      if (cp > rnd.nextFloat) {
        call();
      };
    };
  }

  void move (float ddir, float dspeed) {
    // ddir - difference added to direction, radians
    // dspeed - difference to speed, -1 < dspeed < 1 (recommended)
    dir += ddir;
    speed += speed*dspeed;
    move();
  }

  boolean call () {
    Carrier c1 = bts.givePhysChannel();

    return true;
  }

  boolean callTerminate (BaseStation bs) {
    if (car.releaseChannel()) {
      if ()
    };
  }

  Carrier askHandover (BaseStation bs) {
    return new Carrier();
  }

  BaseStation findMyBTS () {
    return new BaseStation();
  }
}

class BaseStation {
  float x, y, rad;
  BSController bsc;
  Carrier[] cars;

  BaseStation (float _x, float _y, float _rad, BSController _bsc) {
    x = _x;
    y = _y;
    rad = _rad;
    bsc = _bsc;
  }

  Carrier givePhysChannel () {
    return new Carrier;
  }

  boolean releaseChannel () {
    return true;
  }

  Carrier requestCarrier () {
    return new Carrier();
  }

  boolean releaseCarrier () {
    return true;
  }
}

class BSController {
  Carrier[] allCars;
  Carrier[] usedCars;
  static BaseStation[] btss = null;
  BaseStation[] neigBTS;
  boolean isDCD;     // dynamic carrier distribution

  BSController () {}

  static BaseStation findClosestBTS (float x, float y) {
    double dmin = 1e30;
    BaseStation foundBTS = null;
    for (BaseStation bs: btss) {
      double x1 = x - bs.x;
      double y1 = y - bs.y;
      double dist = Math.sqrt(x1*x1 + y1*y1);
      if ((dist<bs.rad) || (dist<dmin)) {
        dmin = dist;
        foundBTS = bs;
      };
    };
    return foundBTS;   // base station or null
  }

  BaseStation[] getNeigBTS (BaseStation bts) {
    return new BaseStation[3];
  };

  BaseStation addBTS () {
    return new BaseStation();
  }

  boolean delBTS () {
    return true;
  }

  Carrier getCarrier () {
    return new Carrier();
  }

  boolean releaseCarrier () {
    return true;
  }

  Carrier handover (BaseStation oldbts, BaseStation newbts) {
    return new Carrier;
  }
}

class Carrier {
  int num;
  int[] channels;

  Carrier (int _num, int cpc) {
    num = _num;
  }

  boolean useChannel (int n) {
    return true;
  }

  boolean releaseChannel (int n) {
    return true;
  }
}

