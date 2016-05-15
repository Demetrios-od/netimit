package od.demetrios.netimit;

class BaseStation {
  private static int idall = 0;
  private int id;
  private float x, y, rad;
  private BSController bsc;
  private Carrier[] cars;
  
  public int getId() {
    return id;
  }

  BaseStation (float ix, float iy, float irad) {
    id = idall++;
    x = ix;
    y = iy;
    rad = irad;
    //cars = new Carrier[];
    //System.out.println("BTS " + id + " created");
  }

  public boolean bindToBSC (BSController ibsc) {
    if (bsc.bindBTS(this)) {
      bsc = ibsc;
      cars = bsc.getCarriers(this, cars);
      return true;
    };
    return false;
  }

  public boolean unbindFromBSC () {
    if (bsc.unbindBTS(this)) {
      bsc = null;
      cars = null;
      return true;
    };
    return false;
  }

  private float distanceTo (Subscriber sub) {
    float[] cs = sub.getCoordinates();
    return (float)Math.sqrt((cs[0]-x)*(cs[0]-x) + (cs[1]-y)*(cs[1]-y));
  }

  private float distanceTo (float ix, float iy) {
    return (float)Math.sqrt((ix-x)*(ix-x) + (iy-y)*(iy-y));
  }

  public boolean isInServiceArea (Subscriber sub) {
    float d = distanceTo(sub);
    //System.out.println("Distance from BTS " + id + " to subscriber " + sub.getId() + ": " + d);
    return rad > d;
  }

  public boolean isInServiceArea (float ix, float iy) {
    float d = distanceTo(ix, iy);
    //System.out.println("Distance from BTS " + id + ": " + d);
    return rad > d;
  }

  public float signalLevel (Subscriber sub) {
    return 1/distanceTo(sub);
  }

  public float signalLevel (float ix, float iy) {
    return 1/distanceTo(ix, iy);
  }

  public PhyChannel findChannel (Subscriber sub) {
    if (bsc == null || cars == null || cars.length == 0) return null;
    for (Carrier c: cars) {
      if (c != null && !c.isFull()) {
        System.out.println("Channel found");
        return c.getChannelForSubscriebr(sub);
      }
    };
    // Если все имеющиеся несущие заняты, или все null,
    // надо спросить новую несущую на контроллере.
    Carrier newcar = bsc.getCarriers(this, cars);
    // Если ФРК, то newcar всегда null
    if (newcar != null) {
      if (addCarrier(newcar)) {
        System.out.println("New carrier found");
        return newcar.getChannelForSubscriebr(sub);
      } else {
        System.out.println("Carrier wasn\'t added");
      }
    } else {
      System.out.println("Channel not found");
    }
    return null;
  }
  
  public boolean releaseChannel(PhyChannel chan) {
    if ( chan.getCarrier().stopUseChannel(chan.getChannelNumber()) ) {
      if (chan.getCarrier().isEmpty() && (cars.length > 1)) {
        System.out.println("Deleting carrier from BTS");
        if (releaseCarrier(chan.getCarrier())) {
          System.out.println("Carrier deleted from BTS");
        } else {
          System.out.println("Carrier wasn\'t deleted from BTS");
        }
      };
      System.out.println("Channel released");
      return true;
    } else {
      System.out.println("Channel wasn\'t released");
      return false;
    }
    
  }
  
  private boolean addCarrier (Carrier icar) {
    Carrier[] buf = new Carrier[cars.length+1];
    for (int i=0; i<cars.length; i++) {
      if (cars[i] == icar) return false;
      else  buf[i] = cars[i];
    };
    buf[cars.length] = icar;
    cars = buf;
    return true;
  }
  
  private boolean releaseCarrier (Carrier car) {
    for (int i=0; i<cars.length; i++) {
      if (cars[i] == car && bsc.releaseCarrier(car)) {
        cars[i] = null;
        return true;
      };
    };
    return false;
  }
  
  public PhyChannel handover (Subscriber sub, BaseStation newbts) {
    if (sub.isActive()) {
      System.out.println("Active handover from BTS " + id + " to BTS " + newbts.id);
      Counter.abonHandAct++;
      PhyChannel ch = bsc.handover(this, newbts);
      if (ch == null) Counter.callRelHand++;
      return ch;
    } else {
      //System.out.println("Passive handover from BTS " + id + " to BTS " + newbts.id);
      Counter.abonHandPas++;
      return null;
    }
  }
  
  public boolean isNeighboring (BaseStation bs, int cluster) {
    float d = (float)Math.sqrt(3*cluster*rad);
    return d < distanceTo(bs.x, bs.y);
  }

}
