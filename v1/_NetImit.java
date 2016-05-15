/* *******************************************************
              Imitation a cellular network

(c) Bukhan D.Yu. 2011-2013
******************************************************* */

import java.util.*;
import java.lang.Math.*;

// classes for saving data

class ServiceZone {
// coordinates of the area where abons can move, in meters
  int xmin = 1800,
      xmax = 9400,
      ymin = 1100,
      ymax = 7900;
}

class MovingSpeed {
// average speed, meters per iteration (minute). 100 m/min = 6 km/h
  int aver = 100,
      disp = 30;
}

class CoverageZone {
// coordinates of the left and bottom corner of the coverage zone
  int lbcX = 0,
      lbcY = 0;
}

class CallDuration {
  int aver = 14,  // average call duration, iterations (minutes)
      disp = 3;   // dispersion of call duration, iterations (minutes)
}

class Counters {
  // initialization the event counters
  static int ABON_COME = 0,        // abon comes to the service zone
             ABON_LEAVE = 0,       // abon leaves the service zone
             ABON_HAND_ACT = 0,    // active hand-off
             ABON_HAND_PAS = 0,    // passive hand-off
             CALL_REQ_SUC = 0,     // successful call request
             CALL_REQ_FAIL = 0,    // failed call request
             CALL_REQ_OUT = 0,     // call requests outside the service zone
             CALL_REL_NORM = 0,    // call ends inside cell
             CALL_REL_HAND = 0,    // call ends on hand-off
             CALL_REL_LEAVE = 0;   // call ends on leaving the service zone

  // statistic counters aren't initialized
  // CNT.CELL_CAR=zeros(1,NBS);     % number of busy carriers in every cell
  // CNT.CELL_CHAN=zeros(1,NBS);    % number of busy channels in every cell
}

// classes for network objects

class Abonent {
  private float x, y, speed, dir, activ;
  private int bts, channel;

  Abonent(ServiceZone area, MovingSpeed sp) {
    Random rand = new Random();
    x = area.xmin + rand.nextFloat() * (area.xmax - area.xmin);
    y = area.ymin + rand.nextFloat() * (area.ymax - area.ymin);
    speed = sp.aver + sp.disp * rand.nextFloat();
    dir = (float) (rand.nextFloat() * 2 * Math.PI);
    activ = 0;
    channel = 0;
    bts = 0;  // define BTS here
  }

  public void move(ServiceZone area) {
    // find new abon's coordimates
    float delx = (float) (speed * Math.cos(dir));
    float dely = (float) (speed * Math.sin(dir));
    x = x + delx;
    y = y + dely;
    // if abon appears outside the moving zone, it 'reflests' from the edge of zone
    if ((x < area.xmin) || (x > area.xmax)) {
      x = x - delx;
      dir = (float) (Math.PI - dir);
    }
    if ((y < area.ymin) || (y > area.ymax)) {
      y = y - dely;
      dir = (float) (2 * Math.PI - dir);
    }

    // then maybe: changing BTS, handover
  }

}

class SetOfAbonents {
  private static Abonent[] abons;

  SetOfAbonents(int n, ServiceZone area, MovingSpeed sp) {
    abons = new Abonent[n];
    for (int i=0; i<n; i++) abons[i] = new Abonent(area, sp);
  }

  public Abonent getAbon(int n) {return abons[n];}
  public int amount() {return abons.length;}

  public void moveAll(ServiceZone area) {
    for (int i=0; i<abons.length; i++) abons[i].move(area);
  }
}

class BaseStation {
  private float x, y, rad;
  private int[] carriers;

  BaseStation(float xpos, float ypos, float radius) {
    x = xpos;
    y = ypos;
    rad = radius;
    carriers = null;
  }
}

class SetOfBaseStations {
  private BaseStation[] bss;  // list of BSs
  private boolean[][] bsneig;  // neighboring BSs

  SetOfBaseStations(int n, float rad, CoverageZone covzone) {
  // this constructor places BSs in closest-to-square form
  // n - number of BSs
    // find quantity of BSs for vertical and horizontal directions
    int ns = (int) (Math.ceil(Math.sqrt(n)));
    // initializing array 'bsneig' as a eye-matrix
    bsneig = new boolean[n][n];
    for (int i=0; i<n; i++) {
      for (int j=0; j<n; j++) {
        bsneig[i][j] = false;
      }
      bsneig[i][i] = true;
    }
    // create base stations and define neighboring ones
    bss = new BaseStation[n];
    for (int i=1, k=1; i<=ns; i++) {
      for (int j=1; j<=ns; j++, k++) {
        // define coordinates of cell centers (BS locations)
        float xpos = (float) (Math.abs(i/2-Math.floor(i/2))*rad*Math.sqrt(3) + covzone.lbcX + j*rad*Math.sqrt(3));
        float ypos = covzone.lbcY+i*3*rad/2;
        bss[i-1] = new BaseStation(xpos, ypos, rad);
        // now define neighboring BS and save them in the matrix 'bsneig'
        // min guard distanse — three closest BS with smaller numbers
        // (cluster dimension K = 3)
        if ((j-1)>=1) {   // point (i; j-1) — at the left side
          bsneig[k-1][k-2] = true;
          bsneig[k-2][k-1] = true;
        }
        if ((i-1)>=1) {   // point (i-1; j) - at the bottom
          bsneig[k-1][k-ns-1] = true;
          bsneig[k-ns-1][k-1] = true;
        }
        // System.out.println("i="+i+", j="+j+", k="+k+", ns="+ns);
        if ((i-1)>=1) {     // point (i-1; j+/-1) - at the left/right and below
          int k1 = k-ns;
          if (i%2 == 1) {   // if row is odd, take righter cell
            if ((k % ns) != 0) k1++ ;
          } else {          // if row is even, take lefter cell
            if (((k-1) % ns) != 0) k1-- ;
          }
          bsneig[k-1][k1-1] = true;
          bsneig[k1-1][k-1] = true;
        }
        if (k == n) break;
      }
    }
  }
}

class Carrier {
  int bts[], channels[];

  Carrier(int ncpc) {
    bts = new int[1];
    channels = new int[ncpc];
  }
}

class SetOfCarriers {
  private Carrier[] cars;

  SetOfCarriers(int ncar, int ncpc) {
    cars = new Carrier[ncar];
    for (int i=0; i<ncar; i++) cars[i] = new Carrier(ncpc);
  }
}

// *****************************************************************************
//                                 The imitation
// *****************************************************************************

class NetImit {
  static int num_abons = 10,     // quantity of abons
             num_bs = 25,         // number of BS
             maxiters = 20,     // quantity of iterations in modeling
             ncar = 6,            // number of carriers
             ncpc = 7;            // number of traffic channels per carrier
  static float betta = (float) 0.02,     /* probability of call appearance at iteration
                                            (if multiplied by call duration, gives load
                                             created by single abon, in Erlangs) */
        rad = (float) 1000.0;     // cell radius, meters

  static ServiceZone area = new ServiceZone();
  static MovingSpeed speed = new MovingSpeed();
  static CoverageZone covzone = new CoverageZone();
  static CallDuration calldur = new CallDuration();

  // network initialization here
  static SetOfBaseStations bsset = new SetOfBaseStations(num_bs, rad, covzone);
  static SetOfAbonents abonset = new SetOfAbonents(num_abons, area, speed);
  static SetOfCarriers carset = new SetOfCarriers(ncar, ncpc);

  public static void main(String[] args) {

  //------------------------ Iterational process begins --------------------------
  System.out.print("Netimit is started");
  for (int iter=1; iter<=maxiters; iter++) {  // iterations
    for (int i=1; i<=num_abons; i++) {        // for every abon
 //     abon[i].move;

    }  // end considering an abon
  }    // end iteration

  }

}

