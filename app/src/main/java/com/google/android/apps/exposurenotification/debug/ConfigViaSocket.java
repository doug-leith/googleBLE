/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.google.android.apps.exposurenotification.debug;

import static com.google.android.apps.exposurenotification.nearby.ProvideDiagnosisKeysWorker.DEFAULT_API_TIMEOUT;

import android.content.Context;
import android.util.Log;
import com.google.android.apps.exposurenotification.common.AppExecutors;
import com.google.android.apps.exposurenotification.common.TaskToFutureAdapter;
import com.google.android.apps.exposurenotification.debug.ProvideMatchingViewModel.NotEnabledException;
import com.google.android.apps.exposurenotification.nearby.DiagnosisKeyFileSubmitter;
import com.google.android.apps.exposurenotification.nearby.ExposureNotificationClientWrapper;
import com.google.android.apps.exposurenotification.network.KeyFileBatch;
import com.google.android.apps.exposurenotification.storage.TokenRepository;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.nearby.exposurenotification.ExposureInformation;
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationStatusCodes;
import com.google.android.gms.nearby.exposurenotification.ExposureSummary;
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey;
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey.TemporaryExposureKeyBuilder;
import com.google.android.gms.tasks.Tasks;
import com.google.common.collect.Lists;
import com.google.common.io.BaseEncoding;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.threeten.bp.Duration;
import org.threeten.bp.Instant;
import org.threeten.bp.ZoneId;
import org.threeten.bp.format.DateTimeFormatter;

//DL
public class ConfigViaSocket {

  private static final int socketServerPORT = 8081;
  private ServerSocket serverSocket;
  private Context context;
  private static final boolean DEBUGGING = true;  // generate extra debug output ?
  private static final BaseEncoding BASE16 = BaseEncoding.base16().lowerCase();
  private static PrintStream out;
  private static final Duration API_TIMEOUT = Duration.ofSeconds(10);

  public ConfigViaSocket(Context activity){ // {
    this.context = activity;
    Thread socketServerThread = new Thread(new SocketServerThread());
    Log.d("DL","ConfigViaSocket Server running at "+getIpAddress() + ":" + getPort());
    socketServerThread.start();
  }

  public int getPort() {
    return socketServerPORT;
  }

  public String getIpAddress() {
    String ip = "";
    int count = 0;

    try {
      Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
          .getNetworkInterfaces();
      while (enumNetworkInterfaces.hasMoreElements()) {
        NetworkInterface networkInterface = enumNetworkInterfaces
            .nextElement();
        Enumeration<InetAddress> enumInetAddress = networkInterface
            .getInetAddresses();

        while (enumInetAddress.hasMoreElements()) {
          InetAddress inetAddress = enumInetAddress
              .nextElement();

          if (inetAddress.isSiteLocalAddress()) {
            count++;
            ip += inetAddress.getHostAddress();
          }
        }

      }

      if (count==0) {ip += "(no wireless connection) "; }

    } catch (SocketException e) {
      Log.d("DL","WARN ConfigViaSocket: "+e.getMessage());
      ip += "\nWARN ConfigViaSocket: " + e.toString() + "\n";
    }
    return ip;
  }

  public void onDestroy() {
    if (serverSocket != null) {
      try {
        serverSocket.close();
        Log.d("DL","ConfigViaSocketStopped");
      } catch (IOException e) {
        Log.d("DL","WARN ConfigViaSocket: "+e.toString());
      }
    }
  }

  private class SocketServerThread extends Thread {

    private Socket clientSocket;
    private String readLine(InputStream in) {
      // Read next line
      String input=null;
      int res;
      ByteArrayOutputStream line = new ByteArrayOutputStream();
      try {
        while ((res = in.read()) > 0) {
          if (res == 10) break; //end of line, 10=LF
          line.write((byte) res);
        }
        if (line.size()>0) input = line.toString("ASCII");
      } catch (Exception e) {
        Log.d("DL","WARN: "+e.toString());
        return null;
      }
      return input;
    }

    @Override
    public void run() {
      try {
        // create ServerSocket using specified port
        serverSocket = new ServerSocket(socketServerPORT);

        while (true) {
          clientSocket = serverSocket.accept(); //waits for an incoming request and return socket object
          //Log.d("DL","new connection");

          // Client established connection.
          // Create input and output streams
          InputStream in = clientSocket.getInputStream(); // we need to work with the raw input stream
          out = new PrintStream(clientSocket.getOutputStream());

          String input = readLine(in);
          while (input!=null) {
            Log.d("DL","ConfigViaSocket received: " + input);
            String[] parts = input.split(" ");
            processcmd:
            try {
              if (parts[0].equals("PUT")) {
                // submit TEK for checking
                // PUT <TEK key> <start interval> <duration> <token>
                // key parts[1] start parts[2] dur parts[3]
                //out.println("ok"); out.flush();
                put(parts[4], parts[1], Integer.valueOf(parts[2]), Integer.valueOf(parts[3]),false);
              } else if (parts[0].equals("GET")) {
                // get TEK history of this device
                //out.println("ok"); out.flush();
                get();
              } else if (parts[0].equals("PUTLONG")) {
                // get TEK history of this device
                //out.println("ok"); out.flush();
                put(parts[4], parts[1], Integer.valueOf(parts[2]), Integer.valueOf(parts[3]),true);
              } else {
                out.println("unknown command"); out.flush();
              }
            } catch (Exception e) {
              Log.d("DL","WARN ConfigViaSocket: " + e.toString());
              out.println("ERR: "+e.getMessage()); out.flush();
              e.printStackTrace();
              break;
            }
            out.flush();
            input = readLine(in);
          }
          clientSocket.close();
        }
      } catch (IOException e) {
        Log.d("DL","WARN ConfigViaSocket: "+e.toString());
      }
    }
  }

  public void respond(String msg) {
    // have to put this in own thread, sigh
    Log.d("DL","respond "+msg);
    Thread thread = new Thread(new Runnable() {

      @Override
      public void run() {
        try  {
          Log.d("DL","respond out.println "+msg);
          out.println(msg);
          out.flush();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });

    thread.start();
  }

  public void get() {
    ExposureNotificationClientWrapper client =
        ExposureNotificationClientWrapper.get(context);
        client
        .isEnabled()
        .continueWithTask(
            isEnabled -> {
              if (isEnabled.getResult()) {
                return client.getTemporaryExposureKeyHistory();
              } else {
                return Tasks.forResult(new ArrayList<>());
              }
            })
        .addOnSuccessListener(
            temporaryExposureKeys -> {
              //out.println(temporaryExposureKeys.toString());
              String res = temporaryExposureKeys.toString();
              Log.d("DL","GET "+res);
              respond(res);
            })
        .addOnFailureListener(
            exception -> {
              if (!(exception instanceof ApiException)) {
                Log.e("DL", "Unknown error when attempting to start API", exception);
                return;
              }
              ApiException apiException = (ApiException) exception;
              Log.e("DL", "Error, has in flight resolution", exception);
              if (apiException.getStatusCode()
                  == ExposureNotificationStatusCodes.RESOLUTION_REQUIRED) {
                  Log.e("DL", "Error, has in flight resolution", exception);
              } else {
                Log.w("DL", "No RESOLUTION_REQUIRED in result", apiException);
              }
            });
  }

  public List<File> getFile(String key, int start, int dur) {
    Log.d("DL",key);

    KeyFileWriter keyFileWriter = new KeyFileWriter(context);
    TemporaryExposureKey temporaryExposureKey;
    try {
      temporaryExposureKey =
          new TemporaryExposureKeyBuilder()
              .setKeyData(BASE16.decode(key))
              .setRollingPeriod(dur)
              .setTransmissionRiskLevel(1)
              .setRollingStartIntervalNumber(start)
              .build();
      Log.d("DL",temporaryExposureKey.toString());
    } catch(IllegalArgumentException e) {
      Log.e("DL", "Error creating TemporaryExposureKey", e);
      return null;
    }

    List<TemporaryExposureKey> keys = Lists.newArrayList(temporaryExposureKey);
    /*Instant startTime = Instant.ofEpochSecond​(start*10*60);
    Instant endTime = Instant.ofEpochSecond​((start+dur)*10*60);
    Log.d("DL", "key start time "
        + startTime.atZone(ZoneId.of("Europe/London")).format(
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss z"))
        +", end time "
        +endTime.atZone(ZoneId.of("Europe/London")).format(
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss z")));*/
    List<File> files =
        keyFileWriter.writeForKeys(
            keys, Instant.now().minus(Duration.ofDays(14)), Instant.now(), "GB");
            //keys, startTime, endTime , "GB");

    return files;
  }

  public void put(String token, String key, int start, int dur, boolean verbose) {

    List<File> files = getFile(key,start,dur);

    Log.d("DL", String.format("About to provide %d key files.", files.size()));
    DiagnosisKeyFileSubmitter submitter = new DiagnosisKeyFileSubmitter(context);
    TokenRepository repository = new TokenRepository(context);

    KeyFileBatch batch = KeyFileBatch.ofFiles("US", 1, files);
    ExposureNotificationClientWrapper client = ExposureNotificationClientWrapper.get(context);
    //DL
    int[] lowThresh_5dB =  {48,48,48,48,48}; //,48};
    int[] highThresh_5dB = {55,63,68,73,78}; //,83};
    int[] lowThresh_1dB = {
        48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48,
        48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48};
    int[] highThresh_1dB = {
        49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68,
        69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 80, 85, 90, 100};

    int[] lowThresh; int[] highThresh;
    if (verbose) {
      lowThresh = lowThresh_1dB;
      highThresh = highThresh_1dB;
    } else {
      lowThresh = lowThresh_5dB;
      highThresh = highThresh_5dB;
    }

    int i;
    int len = lowThresh.length;
    AtomicInteger count = new AtomicInteger(0);
    AtomicReference<String> response= new AtomicReference<>("");
    for (i = 0; i < len; i++) {
      String toke = token + ":" + lowThresh[i] + "-" + highThresh[i];
      int low = lowThresh[i];
      int high = highThresh[i];
      count.addAndGet(2);
      FluentFuture.from(
          TaskToFutureAdapter.getFutureWithTimeout(
              client.isEnabled(),
              DEFAULT_API_TIMEOUT.toMillis(),
              TimeUnit.MILLISECONDS,
              AppExecutors.getScheduledExecutor()))
          .transform(
              (isEnabled) -> {
                // Only continue if it is enabled.
                if (isEnabled) {
                  return TaskToFutureAdapter.getFutureWithTimeout(
                      client.provideDiagnosisKeys(batch.files(), toke, low, high)
                          .addOnCompleteListener( (t) ->{
                            Log.d("DL","provideDiagnosisKeys "+token+" completed.");
                            try {
                              // when provideDiagnosisKeys() completes getExposureInformation() needs more time,
                              // this wait seems to be essential.
                              Thread.sleep(250);
                            } catch (InterruptedException e) {
                              e.printStackTrace();
                              count.set(0);
                            }
                            for (File f : batch.files()) {
                              f.delete();
                            }
                            client.getExposureInformation(toke)
                                .addOnCompleteListener((t2)->{
                                  int num = 0;
                                  for (ExposureInformation exposureInformation : t2.getResult()) {
                                    Log.d("DL",toke+":"+exposureInformation.toString());
                                    DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                                    Date date = new Date();
                                    //String msg = dateFormat.format(date)+" "+toke+":"+exposureInformation.toString()+"\n";
                                    String msg = toke+":"+exposureInformation.toString();
                                    //out.println(msg);
                                    response.set(response + msg);
                                    num = num+1;
                                  };
                                  if (num==0) {
                                    response.set(response + toke+":<empty>");
                                  }
                                  count.decrementAndGet();
                                });
                            /*client.getExposureSummary(toke)
                                .addOnCompleteListener((t3)->{
                                  ExposureSummary exposureSummary = t3.getResult();
                                  Log.d("DL",toke+":"+exposureSummary.toString());
                                  DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                                  Date date = new Date();
                                  //String msg = dateFormat.format(date)+" "+toke+":"+exposureSummary.toString()+"\n";
                                  String msg = toke+":"+exposureSummary.toString();
                                  //out.println(msg);
                                  response.set(response + msg);
                                  count.decrementAndGet();
                                });*/
                            count.decrementAndGet();
                          }),
                      API_TIMEOUT.toMillis(),
                      TimeUnit.MILLISECONDS,
                      AppExecutors.getScheduledExecutor());
                } else {
                  count.decrementAndGet();
                  count.decrementAndGet();
                  return Futures.immediateFailedFuture(new NotEnabledException());
                }
              },
              AppExecutors.getBackgroundExecutor())
          .transform(
              done -> {
                return null;
              },
              AppExecutors.getLightweightExecutor())
          .catching(
              NotEnabledException.class,
              x -> {
                Log.w("DL", "Error, isEnabled is false", x);
                return null;
              },
              AppExecutors.getBackgroundExecutor())
          .catching(
              Exception.class,
              x -> {
                Log.w("DL", "Unknown exception when providing", x);
                return null;
              },
              AppExecutors.getBackgroundExecutor());
    }
    while (count.get()>0) {
      // busy wait
      try {
        Thread.sleep(250);
      } catch (InterruptedException e) {
        e.printStackTrace();
        count.set(0);
        break;
      }
    }
    Log.d("DL","get done.");
    respond(response.get()+"#");
  }

  /***************************************************************************************/
  // debugging
  private static class Debug {
    static void println(String s) {
      if (DEBUGGING) Log.d("DL","ConfigViaSocket: "+s);
    }
  }
}


