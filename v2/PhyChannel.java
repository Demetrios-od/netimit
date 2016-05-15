package od.demetrios.netimit;

class PhyChannel {
  private Carrier car;
  private int chan;

  PhyChannel (Carrier icar, int ichan) {
    if (icar == null || ichan <= 0 || ichan > icar.numberOfChannelsPerCarrier()) {
      System.out.println("ERROR: Incorrect physical channel parameters");
      car = null;
      chan = 0;
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
