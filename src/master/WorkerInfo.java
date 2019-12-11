import java.io.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import java.net.*;

class WorkerInfo{
  private final Socket Ws;
  private final InputStream Wdis;
  private final OutputStream Wdos;
  public WorkerInfo(Socket Ws, InputStream Wdis, OutputStream Wdos){
    this.Ws = Ws;
    this.Wdis = Wdis;
    this.Wdos = Wdos;
  }
  public Socket GetWs(){
    return Ws;
  }
  public InputStream GetWdis(){
    return Wdis;
  }
  public OutputStream GetWdos(){
    return Wdos;
  }
}
