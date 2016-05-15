package od.demetrios.netimit;

class Carrier {
  private int freq;
  private Subscriber[] subs;

  Carrier (int ifreq, int icpc) {
    freq = ifreq;
    setChannelsPerCarrier(icpc);
  }

  public boolean setChannelsPerCarrier (int icpc) {
    if (icpc <= 0) {
      System.out.println("ERROR: Number of channels per carrier must be greater than zero.");
      return false;
    };
    if (subs == null) {
      subs = new Subscriber[icpc];
      return true;
    }
    if (icpc == subs.length) return true;
    if (icpc > subs.length) {   // увеличиваем число каналов на несущей
      Subscriber[] buf = new Subscriber[subs.length];
      for (int i=0; i<subs.length; i++) buf[i] = subs[i];
      subs = new Subscriber[icpc];
      for (int i=0; i<buf.length; i++) subs[i] = buf[i];
      return true;
    } else {   // уменьшаем число каналов на несущей
      if (numberOfBusyChannels() > icpc) {
        System.out.println("ERROR: Cannot decrease number of channels at the carrier. Too many busy channels.");
        return false;
      } else {
        Subscriber[] buf = new Subscriber[icpc];
        for (int i=0; i<subs.length; i++) {
          if (subs[i] != null) buf[i] = subs[i];
        };
        subs = new Subscriber[icpc];
        for (int i=0; i<buf.length; i++) subs[i] = buf[i];
        return true;
      }
    }
  }

  public int numberOfBusyChannels () {
    int i = 0;
    for (Subscriber sub: subs) {
      if (sub != null) i++;
    };
    return i;
  }
  
  public int numberOfChannelsPerCarrier () {
    return subs.length;
  }

  public PhyChannel getChannelForSubscriebr(Subscriber sub) {
    for (int i=0; i<subs.length; i++) {
      if (subs[i] == null) {
        subs[i] = sub;
        return new PhyChannel(this, i);
      };
    };
    return null;
  }

  public boolean stopUseChannel(int n) {
    if (n < 0 || n >= subs.length || subs[n] == null)
      return false;
    subs[n].clearChannel();
    subs[n] = null;
    return true;
  }

  public boolean removeSubscriber (Subscriber sub) {
    boolean ans = false;
    for (int i=0; i<subs.length; i++) {
      if (subs[i] == sub) {
        subs[i] = null;
        sub.clearChannel();
        ans = true;
        break;
      };
    };
    return ans;
  }
  
  public void removeAllSubscribers () {
    for (int i=0; i<subs.length; i++) {
      subs[i].clearChannel();
      subs[i] = null;
    };
  }

  public boolean isEmpty() {
    boolean x = true;
    for (Subscriber sub: subs) {
      x = x && (sub == null);
    };
    return x;
  }

  public boolean isFull() {
    boolean x = true;
    for (Subscriber sub: subs) {
      x = x && (sub != null);
    };
    return x;
  }
  
  public int getFreq () {
    return freq;
  }
}
