package od.demetrios.netimit;

class Counter {
  public static int abonCome, abonLeave, abonHandAct, abonHandPas,
    callReqSuc, callReqFail, callRelNorm, callRelHand, callRelLeave;
  
  static float probabCallLoss () {
    float d = callReqFail + callReqSuc;
    if (d == 0) return 0;
    else return (callReqFail + callRelHand) / d;
  }
  
  static void clear () {
    abonCome     = 0;
    abonLeave    = 0;
    abonHandAct  = 0;
    abonHandPas  = 0;
    callReqSuc   = 0;
    callReqFail  = 0;
    callRelNorm  = 0;
    callRelHand  = 0;
    callRelLeave = 0;
  }
  
  static void printAll () {
    System.out.println("abonCome     = " + abonCome);
    System.out.println("abonLeave    = " + abonLeave);
    System.out.println("abonHandAct  = " + abonHandAct);
    System.out.println("abonHandPas  = " + abonHandPas);
    System.out.println("callReqSuc   = " + callReqSuc);
    System.out.println("callReqFail  = " + callReqFail);
    System.out.println("callRelNorm  = " + callRelNorm);
    System.out.println("callRelHand  = " + callRelHand);
    System.out.println("callRelLeave = " + callRelLeave);
  }
}
