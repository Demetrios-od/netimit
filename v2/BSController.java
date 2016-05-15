package od.demetrios.netimit;

class BSController {
  private String mode = "fixed";
  private static BaseStation[] btss;   // не должно быть static
  private Carrier[] cars;
  private int cluster;

  BSController (String imode, int nbts, Carrier[] icars, int icluster) {
    setMode(imode);
    btss = new BaseStation[nbts];
    cars = icars;
    for (int i=1; i<5; i++) {
      for (int j=0; j<5; j++) {
        if (i*i+i*j+j*j == icluster) {
          cluster = icluster;
          return;
        }
      }
    }
    System.out.println("WARNING: Cluster dimension is incorrect, setted to default value (3).");
    cluster = 3;
    //System.out.println("BSC created");
  }
  
  public void setMode (String imode) {
    switch (imode) {
    case "fixed":
    case "dynamic":
      mode = imode;
      break;
    default: mode = "fixed"; 
    }
  }

  public boolean bindBTS (BaseStation bs) {
    for (int i=0; i<btss.length; i++) {
      if (btss[i] == null) {
        btss[i] = bs;
        
        return true;
      };
    };
    return false;
  }

  public boolean unbindBTS (BaseStation bs) {
    for (int i=0; i<btss.length; i++) {
      if (btss[i] == bs) {
        btss[i] = null;
        return true;
      };
    };
    return false;
  }

  public static BaseStation findClosestBTS (float x, float y) {
    float slmin = 3.4e38f;   // signal level
    BaseStation foundBTS = null;
    for (BaseStation bs: btss) {
      float sl = bs.signalLevel(x, y);
      if (bs.isInServiceArea(x, y) && slmin > sl) {
        slmin = sl;
        foundBTS = bs;
      };
    };
    return foundBTS;   // base station or null
  }

  public static BaseStation findClosestBTS (Subscriber sub) {
    float slmin = 3.4e38f;   // signal level
    BaseStation foundBTS = null;
    float[] cs = sub.getCoordinates();
    for (BaseStation bs: btss) {
      float sl = bs.signalLevel(cs[0], cs[1]);
      if (bs.isInServiceArea(cs[0], cs[1]) && slmin > sl) {
        slmin = sl;
        foundBTS = bs;
      };
    };
    return foundBTS;   // base station or null
  }

  public PhyChannel handover (BaseStation oldbts, BaseStation newbts) {
    return new PhyChannel (new Carrier(0, 7), 1);
  }

  public Carrier[] getCarriers (BaseStation bts, Carrier[] icars) {
    if (icars == null) {    // надо выдать несколько несущих, если ФРК, и одну, если ДРК
      if (mode == "fixed") {
        Carrier[] ans = new Carrier[cars.length/cluster];
        for (BaseStation bs: btss) {
          if (!bts.isNeighboring(bs, cluster)) {
            
          }
        }
      }
    }
    if (mode == "fixed") return null;
    // if mode == dynamic
    
    return null;
  }

  public boolean releaseCarrier (Carrier car) {
    return true;
  }
  
}
