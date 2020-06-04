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

import android.content.Context;
import android.util.Log;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.exposurenotification.ExposureConfiguration;
import com.google.android.gms.nearby.exposurenotification.ExposureInformation;
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient;
import com.google.android.gms.nearby.exposurenotification.ExposureSummary;
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.common.collect.Lists;
import java.io.File;
import java.util.List;

/**
 * Wrapper around {@link com.google.android.gms.nearby.Nearby} APIs.
 */
public class ExposureNotificationClientWrapper {

  private static ExposureNotificationClientWrapper INSTANCE;

  //DL
  public final ExposureNotificationClient exposureNotificationClient;

  public static final String FAKE_TOKEN_1 = "FAKE_TOKEN_1";
  public static final String FAKE_TOKEN_2 = "FAKE_TOKEN_2";
  public static final String FAKE_TOKEN_3 = "FAKE_TOKEN_3";

  public static ExposureNotificationClientWrapper get(Context context) {
    if (INSTANCE == null) {
      INSTANCE = new ExposureNotificationClientWrapper(context);
    }
    return INSTANCE;
  }

  ExposureNotificationClientWrapper(Context context) {
    exposureNotificationClient = Nearby.getExposureNotificationClient(context);
  }

  public Task<Void> start() {
    return exposureNotificationClient.start();
  }

  public Task<Void> stop() {
    return exposureNotificationClient.stop();
  }

  public Task<Boolean> isEnabled() {
    return exposureNotificationClient.isEnabled();
  }

  public Task<List<TemporaryExposureKey>> getTemporaryExposureKeyHistory() {
    return exposureNotificationClient.getTemporaryExposureKeyHistory();
  }

  /**
   * Provides diagnosis key files with a stable token and default {@link ExposureConfiguration}.
   */
  public Task<Void> provideDiagnosisKeys(List<File> files, String token, int lowThresh, int highThresh) {
    // TODO: add some configuration
    // DL
    // Time/Distance: Any individual who has had greater than 15 minutes face-to-face (<2 meters distance) contact with a case, in any setting.
    // Configuration:
    // default values: lowThresh=48, highThresh=58
    ExposureConfiguration exposureConfiguration =
        new ExposureConfiguration.ExposureConfigurationBuilder()
            /*.setAttenuationScores(1, 1, 1, 1, 1, 1, 1, 1)
            .setDaysSinceLastExposureScores(1, 1, 1, 1, 1, 1, 1, 1)
            .setTransmissionRiskScores(1, 1, 1, 1, 1, 1, 1, 1)
            .setDurationScores(1, 1, 1, 1, 1, 1, 1, 1)*/
            .setMinimumRiskScore(1)
            .setDurationAtAttenuationThresholds(lowThresh, highThresh).build();

    //ExposureConfiguration exposureConfiguration =
    //    new ExposureConfiguration.ExposureConfigurationBuilder().build();
    /*
        private int zza = 4;  minimumRiskScore
        private int[] zzb = new int[]{4, 4, 4, 4, 4, 4, 4, 4}; attenuationScores
        private int zzc = 50;  attenuationWeight
        private int[] zzd = new int[]{4, 4, 4, 4, 4, 4, 4, 4}; daysSinceLastExposureScores
        private int zze = 50; daysSinceLastExposureWeight
        private int[] zzf = new int[]{4, 4, 4, 4, 4, 4, 4, 4}; durationScores
        private int zzg = 50;  durationWeight
        private int[] zzh = new int[]{4, 4, 4, 4, 4, 4, 4, 4};  transmissionRiskScores
        private int zzi = 50;  transmissionRiskWeight
        private int[] zzj = new int[]{50, 74};  durationAtAttenuationThresholds
     */
    Log.d("DL","provideDiagnosisKeys "+token);
    return exposureNotificationClient
        .provideDiagnosisKeys(files, exposureConfiguration, token);
  }

  /**
   * Gets the {@link ExposureSummary} using the stable token.
   *
   * <p>If the token matches the fake tokens, it will return fake results.
   */
  public Task<ExposureSummary> getExposureSummary(String token) {
    // Check for fake matches.
    if (FAKE_TOKEN_1.equals(token)) {
      return Tasks.forResult(
          new ExposureSummary.ExposureSummaryBuilder()
              .setMatchedKeyCount(2)
              .setDaysSinceLastExposure(1)
              .build());
    } else if (FAKE_TOKEN_2.equals(token)) {
      return Tasks.forResult(
          new ExposureSummary.ExposureSummaryBuilder()
              .setMatchedKeyCount(1)
              .setDaysSinceLastExposure(2)
              .build());
    } else if (FAKE_TOKEN_3.equals(token)) {
      return Tasks.forResult(
          new ExposureSummary.ExposureSummaryBuilder()
              .setMatchedKeyCount(0)
              .setDaysSinceLastExposure(3)
              .build());
    }
    // Otherwise return the real API.
    return exposureNotificationClient.getExposureSummary(token);
  }

  /**
   * Gets the {@link List} of {@link ExposureInformation} using the stable token.
   *
   * <p>If the token matches the fake tokens, it will return fake results.
   */
  public Task<List<ExposureInformation>> getExposureInformation(String token) {
    if (FAKE_TOKEN_1.equals(token)) {
      long millisInDay = 24L * 60L * 60L * 1000L;
      return Tasks.forResult(
          Lists.newArrayList(
              new ExposureInformation.ExposureInformationBuilder()
                  .setAttenuationValue(1)
                  .setDateMillisSinceEpoch(
                      millisInDay * (System.currentTimeMillis() / millisInDay))
                  .setDurationMinutes(5)
                  .build(),
              new ExposureInformation.ExposureInformationBuilder()
                  .setAttenuationValue(1)
                  .setDateMillisSinceEpoch(1588377600000L)
                  .setDurationMinutes(10)
                  .build()));
    } else if (FAKE_TOKEN_2.equals(token)) {
      return Tasks.forResult(
          Lists.newArrayList(
              new ExposureInformation.ExposureInformationBuilder()
                  .setAttenuationValue(1)
                  .setDateMillisSinceEpoch(1588636800000L)
                  .setDurationMinutes(5)
                  .build()));
    }
    return exposureNotificationClient.getExposureInformation(token);
  }

}
