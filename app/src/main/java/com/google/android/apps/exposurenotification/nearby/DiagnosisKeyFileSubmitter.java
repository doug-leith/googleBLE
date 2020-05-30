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

package com.google.android.apps.exposurenotification.nearby;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.apps.exposurenotification.common.AppExecutors;
import com.google.android.apps.exposurenotification.common.TaskToFutureAdapter;
import com.google.android.apps.exposurenotification.network.KeyFileBatch;
import com.google.android.gms.nearby.exposurenotification.ExposureInformation;
import com.google.android.gms.nearby.exposurenotification.ExposureSummary;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.threeten.bp.Duration;

/**
 * A thin class to take responsibility for submitting downloaded Diagnosis Key files to the Google
 * Play Services Exposure Notifications API.
 */
public class DiagnosisKeyFileSubmitter {
  private static final String TAG = "KeyFileSubmitter";
  private static final Duration API_TIMEOUT = Duration.ofSeconds(10);

  private final ExposureNotificationClientWrapper client;

  //DL
  private static FileOutputStream logfile = null;
  private static Context context = null;

  public DiagnosisKeyFileSubmitter(Context contxt) {
    client = ExposureNotificationClientWrapper.get(contxt);
    context = contxt;
   }

  /**
   * Accepts batches of key files, and submits them to provideDiagnosisKeys(), and returns a future
   * representing the completion of that task.
   *
   * <p>This naive implementation is not robust to individual failures. In fact, a single failure
   * will fail the entire operation. A more robust implementation would support retries, partial
   * completion, and other robustness measures.
   *
   * <p>Returns early if given an empty list of batches.
   */
  public ListenableFuture<?> submitFiles(List<KeyFileBatch> batches, String token,
      int lowThresh, int highThresh) {
    if (batches.isEmpty()) {
      Log.d(TAG, "No files to provide to google play services.");
      return Futures.immediateFuture(null);
    }
    Log.d(TAG, "Providing  " + batches.size() + " diagnosis key batches to google play services.");
    List<ListenableFuture<?>> batchCompletions = new ArrayList<>();
    for (KeyFileBatch b : batches) {
        batchCompletions.add(submitBatch(b, token,
            lowThresh,highThresh));
    }

    ListenableFuture<?> allDone = Futures.allAsList(batchCompletions);
    allDone.addListener(
        () -> {
          for (KeyFileBatch b : batches) {
            for (File f : b.files()) {
              f.delete();
            }
          }
        },
        AppExecutors.getBackgroundExecutor());

    return allDone;
  }

  //DL
  private void writeLogFile(String msg) {
    File path = Environment.getExternalStorageDirectory();
    File file = new File(path, "exposure_notifications.log");
    try {
      logfile = new FileOutputStream(file,true);
      try {
        logfile.write(msg.getBytes());
        logfile.close();
      } catch (Exception e) {
        Log.d("DL","Problem writing to log file: "+e.getMessage());
      }
    } catch (Exception e) {
      logfile = null;
      Log.d("DL","Problem opening log file: "+e.getMessage());
    }
  }
  private ListenableFuture<?> submitBatch(KeyFileBatch batch, String token, int lowThresh, int highThresh) {
    return TaskToFutureAdapter.getFutureWithTimeout(
        client.provideDiagnosisKeys(batch.files(), token, lowThresh, highThresh)
            .addOnCompleteListener( (t) ->{
              //Log.d("DL","provideDiagnosisKeys "+token+" completed.");
              try {
                // when provideDiagnosisKeys() completes getExposureInformation() needs more time,
                // this wait seems to be essential.
                Thread.sleep(250);
              } catch (InterruptedException e) {
                e.printStackTrace();
              }
              client.getExposureInformation(token)
                  .addOnCompleteListener((t2)->{
                    for (ExposureInformation exposureInformation : t2.getResult()) {
                      Log.d("DL",token+":"+exposureInformation.toString());
                      DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                      Date date = new Date();
                      String msg = dateFormat.format(date)+" "+token+":"+exposureInformation.toString()+"\n";
                      writeLogFile(msg);
                     };
                  });
              client.getExposureSummary(token)
                  .addOnCompleteListener((t3)->{
                    ExposureSummary exposureSummary = t3.getResult();
                    Log.d("DL",token+":"+exposureSummary.toString());
                    DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                    Date date = new Date();
                    String msg = dateFormat.format(date)+" "+token+":"+exposureSummary.toString()+"\n";
                    writeLogFile(msg);
                  });
            }),
        API_TIMEOUT.toMillis(),
        TimeUnit.MILLISECONDS,
        AppExecutors.getScheduledExecutor());
  }
}
