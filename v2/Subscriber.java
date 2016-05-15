package od.demetrios.netimit;

import java.util.Random;

class Subscriber {
  private static int idall = 0;
  private static float xmin, xmax, ymin, ymax;
  private int id;
  private float x, y;
  private float dir, speed;
  private int act;
  private float cp;   // call appearance probability
  private int maxdur;    // max call duration
  private PhyChannel chan;
  private BaseStation bts;

  public int getId() {
    return id;
  }
  
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
      speed = 0;
      return;
    };
    speed = v;
  }

  private void setSpeed (float vmin, float vmax) {
    if (vmin > vmax) {
      System.out.println("ERROR: Incorrect speed limits.");
      speed = 0;
      return;
    };
    Random rnd = new Random();
    speed = vmin + (vmax-vmin)*rnd.nextFloat();
  }

  private void setMaxCallDuration (int imaxdur) {
    if (imaxdur <= 0) {
      System.out.println("ERROR: Incorrect call duration.");
      maxdur = 1;
      return;
    };
    maxdur = imaxdur;
  }
  
  Subscriber (float[] speedlim, float icp, int imaxdur) {
    id = idall++;
    Random rnd = new Random();
    x = xmin + (xmax-xmin)*rnd.nextFloat();
    y = ymin + (ymax-ymin)*rnd.nextFloat();
    dir = 2*(float)Math.PI*rnd.nextFloat();
    setSpeed(speedlim[0], speedlim[1]);
    act = 0;
    chan = null;
    bts = BSController.findClosestBTS(x, y);
    setCallProbability(icp);
    setMaxCallDuration(imaxdur);
    /* System.out.println("Subscriber " + id + " created: x=" + x + ", y=" + y +
        ", BTS=" + btsid()); */
  }

  Subscriber (float ix, float iy, float idir, float ispeed, float icp, int imaxdur) {
    id = idall++;
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
    setMaxCallDuration(imaxdur);
    /* System.out.println("Subscriber " + id + " created: x=" + x + ", y=" + y +
        ", BTS=" + btsid()); */
  }

  /*
  private int btsid () {
    int b = (bts == null)? -1 : bts.getId();
    return b;
  } */
  
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
    //System.out.println("Moving subscriber " + id + ", BTS=" + btsid() + ", act=" + act);
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
    //System.out.println("Moved to x=" + x + ", y=" + y);
    // here we could change 'cp' if it depends on coordinates

    // checking the current BTS
    if (bts == null) {   // we're out of the service zone
      //System.out.println("Out of service");
      bts = BSController.findClosestBTS(x, y);   // maybe just came in
      if (bts == null) {
        //System.out.println("Still out of service");
        return;   // still out of service
      } else {
        //System.out.println("Come into the service area");
        Counter.abonCome++;
      }
    };
    
    // мы в зоне обслуживания, своя БС существует (не null)
    //System.out.println("It is in the service area, BTS=" + btsid());
    if (!bts.isInServiceArea(this)) {      // если вышел из соты
      BaseStation newbts = BSController.findClosestBTS(this);
      if (newbts == null) {     // если вышел из зоны обслуживания
        //System.out.println("Come out of the service area");
        Counter.abonLeave++;
        if (act != 0) {
          terminate();   // came out of the service zone
          Counter.callRelLeave++;
        };
        bts = null;
        return;
      };
      // если перешёл в другую соту
      chan = bts.handover(this, newbts);   // came to a new BTS
      bts = newbts;
    };

    if (isActive()) {
      //System.out.println("Activity: " + act);
      if (--act == 0) {
        //System.out.println("Activity is finished");
        terminate();
        Counter.callRelNorm++;
      };
    } else {
      Random rnd = new Random();
      if (cp > rnd.nextFloat()) {
        chan = call(rnd.nextInt(maxdur-1)+1);
      };
    };
    //System.out.println("Subscriber " + id + " done. BTS=" + btsid());
  }

  public void move (float ddir, float dspeed) {
    // ddir - difference added to direction, radians
    // dspeed - difference to speed, -1 < dspeed < 1 (recommended)
    dir += ddir;
    speed += speed*dspeed;
    move();
  }

  public boolean isActive () {
    return act > 0;
  }
  
  public PhyChannel call (int duration) {
    System.out.println("Call request from subscriber " + id + " on BTS " + bts.getId());
    PhyChannel ch = bts.findChannel(this);
    if (ch != null) {
      act = duration;
      System.out.println("Call request successful: activity=" + act);
      Counter.callReqSuc++;
    } else {
      System.out.println("Call request failed");
      Counter.callReqFail++;
    };
    return ch;
  }

  public boolean terminate () {
    if (bts.releaseChannel(chan)) {
      act = 0;
      System.out.println("Call is terminated");
      return true;
    } else {
      System.out.println("Call wasn\'t terminated");
      return false;
    }
    
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
