package com.azihsoyn.flutter.mlkit;

import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.view.FlutterView;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import android.support.annotation.NonNull;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.text.FirebaseVisionTextDetector;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.cloud.FirebaseVisionCloudDetectorOptions;
import com.google.firebase.ml.vision.cloud.text.FirebaseVisionCloudDocumentTextDetector;
import com.google.firebase.ml.vision.cloud.text.FirebaseVisionCloudText;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;

import android.graphics.Bitmap;
import android.media.Image;
import java.io.IOException;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MlkitPlugin
 */
public class MlkitPlugin implements MethodCallHandler {
  private static Context context;
  /**
   * Plugin registration.
   */
  public static void registerWith(Registrar registrar) {
    final MethodChannel channel = new MethodChannel(registrar.messenger(), "plugins.flutter.io/mlkit");
    channel.setMethodCallHandler(new MlkitPlugin());
    context = registrar.context();
  }

  @Override
  public void onMethodCall(MethodCall call, final Result result) {
    if (call.method.equals("FirebaseVisionTextDetector#detectFromPath")) {
      String path = call.argument("filepath");
      try {
        File file = new File(path);
        FirebaseVisionImage image = FirebaseVisionImage.fromFilePath(context, Uri.fromFile(file));
        FirebaseVisionTextDetector detector = FirebaseVision.getInstance()
                .getVisionTextDetector();
        detector.detectInImage(image)
                .addOnSuccessListener(
                        new OnSuccessListener<FirebaseVisionText>() {
                          @Override
                          public void onSuccess(FirebaseVisionText texts) {
                            result.success(processTextRecognitionResult(texts));
                          }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                          @Override
                          public void onFailure(@NonNull Exception e) {
                            // Task failed with an exception
                            e.printStackTrace();
                          }
                        });
      } catch (IOException e) {
        Log.e("error", e.getMessage());
        return;
      }
    } else if (call.method.equals("FirebaseVisionBarcodeDetector#detectFromPath")) {
      String path = call.argument("filepath");
      try {
        File file = new File(path);
        FirebaseVisionImage image = FirebaseVisionImage.fromFilePath(context, Uri.fromFile(file));
        FirebaseVisionBarcodeDetector detector = FirebaseVision.getInstance()
                .getVisionBarcodeDetector();
        detector.detectInImage(image)
                .addOnSuccessListener(
                        new OnSuccessListener<List<FirebaseVisionBarcode>>() {
                          @Override
                          public void onSuccess(List<FirebaseVisionBarcode> barcodes) {
                            result.success(processBarcodeRecognitionResult(barcodes));
                          }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                          @Override
                          public void onFailure(@NonNull Exception e) {
                            // Task failed with an exception
                            e.printStackTrace();
                          }
                        });
      } catch (IOException e) {
        Log.e("error", e.getMessage());
        return;
      }
    } else {
      result.notImplemented();
    }
  }

  private ImmutableList<ImmutableMap<String, Object>> processBarcodeRecognitionResult(List<FirebaseVisionBarcode> barcodes) {
    ImmutableList.Builder<ImmutableMap<String, Object>> dataBuilder =
            ImmutableList.<ImmutableMap<String, Object>>builder();

    for (FirebaseVisionBarcode barcode: barcodes) {
      ImmutableMap.Builder<String, Object> barcodeBuilder = ImmutableMap.<String, Object>builder();

      Rect bounds = barcode.getBoundingBox();
      barcodeBuilder.put("rect_bottom", (double)bounds.bottom);
      barcodeBuilder.put("rect_top", (double)bounds.top);
      barcodeBuilder.put("rect_right", (double)bounds.right);
      barcodeBuilder.put("rect_left", (double)bounds.left);

      ImmutableList.Builder<ImmutableMap<String, Integer>> pointsBuilder =
              ImmutableList.<ImmutableMap<String, Integer>>builder();
      for (Point corner : barcode.getCornerPoints()) {
        ImmutableMap.Builder<String, Integer> pointBuilder = ImmutableMap.<String, Integer>builder();
        pointBuilder.put("x", corner.x);
        pointBuilder.put("y", corner.y);
        pointsBuilder.add(pointBuilder.build());
      }
      barcodeBuilder.put("points", pointsBuilder.build());
      barcodeBuilder.put("raw_value", barcode.getRawValue());
      barcodeBuilder.put("display_value", barcode.getDisplayValue());
      barcodeBuilder.put("format", barcode.getFormat());

      int valueType = barcode.getValueType();
      barcodeBuilder.put("value_type", valueType);

      ImmutableMap.Builder<String, Object> typeValueBuilder = ImmutableMap.<String, Object>builder();
      switch (valueType) {
        case FirebaseVisionBarcode.TYPE_EMAIL:
          typeValueBuilder.put("type", barcode.getEmail().getType());
          typeValueBuilder.put("address", barcode.getEmail().getAddress());
          typeValueBuilder.put("body", barcode.getEmail().getBody());
          typeValueBuilder.put("subject", barcode.getEmail().getSubject());
          barcodeBuilder.put("email", typeValueBuilder.build());
          break;
        case FirebaseVisionBarcode.TYPE_PHONE:
          typeValueBuilder.put("number", barcode.getPhone().getNumber());
          typeValueBuilder.put("type", barcode.getPhone().getType());
          barcodeBuilder.put("phone", typeValueBuilder.build());
          break;
        case FirebaseVisionBarcode.TYPE_SMS:
          typeValueBuilder.put("message", barcode.getSms().getMessage());
          typeValueBuilder.put("phone_number", barcode.getSms().getPhoneNumber());
          barcodeBuilder.put("sms", typeValueBuilder.build());
          break;
        case FirebaseVisionBarcode.TYPE_URL:
          typeValueBuilder.put("title", barcode.getUrl().getTitle());
          typeValueBuilder.put("url", barcode.getUrl().getUrl());
          barcodeBuilder.put("url", typeValueBuilder.build());
          break;
        case FirebaseVisionBarcode.TYPE_WIFI:
          typeValueBuilder.put("ssid", barcode.getWifi().getSsid());
          typeValueBuilder.put("password", barcode.getWifi().getPassword());
          typeValueBuilder.put("encryption_type", barcode.getWifi().getEncryptionType());
          barcodeBuilder.put("wifi", typeValueBuilder.build());
          break;
        case FirebaseVisionBarcode.TYPE_GEO:
          typeValueBuilder.put("latitude", barcode.getGeoPoint().getLat());
          typeValueBuilder.put("longitude", barcode.getGeoPoint().getLng());
          barcodeBuilder.put("geo_point", typeValueBuilder.build());
          break;
        case FirebaseVisionBarcode.TYPE_CONTACT_INFO:
          ImmutableList.Builder<ImmutableMap<String, Object>> addressesBuilder =
                  ImmutableList.builder();
          for (FirebaseVisionBarcode.Address address : barcode.getContactInfo().getAddresses()) {
            ImmutableMap.Builder<String, Object> addressBuilder = ImmutableMap.builder();
            addressBuilder.put("address_lines", address.getAddressLines());
            addressBuilder.put("type", address.getType());
            addressesBuilder.add(addressBuilder.build());
          }
          typeValueBuilder.put("addresses", addressesBuilder.build());

          ImmutableList.Builder<ImmutableMap<String, Object>> emailsBuilder =
                  ImmutableList.builder();
          for (FirebaseVisionBarcode.Email email : barcode.getContactInfo().getEmails()) {
            ImmutableMap.Builder<String, Object> emailBuilder = ImmutableMap.builder();
            emailBuilder.put("address", email.getAddress());
            emailBuilder.put("type", email.getType());
            emailBuilder.put("body", email.getBody());
            emailBuilder.put("subject", email.getSubject());
            emailsBuilder.add(emailBuilder.build());
          }
          typeValueBuilder.put("emails", emailsBuilder.build());

          ImmutableMap.Builder<String, Object> nameBuilder = ImmutableMap.builder();
          nameBuilder.put("formatted_name",  barcode.getContactInfo().getName().getFormattedName());
          nameBuilder.put("first", barcode.getContactInfo().getName().getFirst());
          nameBuilder.put("last", barcode.getContactInfo().getName().getLast());
          nameBuilder.put("middle", barcode.getContactInfo().getName().getMiddle());
          nameBuilder.put("prefix", barcode.getContactInfo().getName().getPrefix());
          nameBuilder.put("pronounciation", barcode.getContactInfo().getName().getPronunciation());
          nameBuilder.put("suffix", barcode.getContactInfo().getName().getSuffix());
          typeValueBuilder.put("name", nameBuilder.build());


          ImmutableList.Builder<ImmutableMap<String, Object>> phonesBuilder =
                  ImmutableList.builder();
          for (FirebaseVisionBarcode.Phone phone : barcode.getContactInfo().getPhones()) {
            ImmutableMap.Builder<String, Object> phoneBuilder = ImmutableMap.builder();
            phoneBuilder.put("number", phone.getNumber());
            phoneBuilder.put("type", phone.getType());
            phonesBuilder.add(phoneBuilder.build());
          }
          typeValueBuilder.put("phones", phonesBuilder.build());

          typeValueBuilder.put("urls", barcode.getContactInfo().getUrls());
          typeValueBuilder.put("job_title", barcode.getContactInfo().getTitle());
          typeValueBuilder.put("organization", barcode.getContactInfo().getOrganization());

          barcodeBuilder.put("contact_info", typeValueBuilder.build());
          break;
        case FirebaseVisionBarcode.TYPE_CALENDAR_EVENT:
          typeValueBuilder.put("event_description", barcode.getCalendarEvent().getDescription());
          typeValueBuilder.put("location", barcode.getCalendarEvent().getLocation());
          typeValueBuilder.put("organizer", barcode.getCalendarEvent().getOrganizer());
          typeValueBuilder.put("status", barcode.getCalendarEvent().getStatus());
          typeValueBuilder.put("summary", barcode.getCalendarEvent().getSummary());
          typeValueBuilder.put("start", barcode.getCalendarEvent().getStart());
          typeValueBuilder.put("end", barcode.getCalendarEvent().getEnd());
          barcodeBuilder.put("calendar_event", typeValueBuilder.build());
          break;
        case FirebaseVisionBarcode.TYPE_DRIVER_LICENSE:
          typeValueBuilder.put("first_name", barcode.getDriverLicense().getFirstName());
          typeValueBuilder.put("middle_name", barcode.getDriverLicense().getMiddleName());
          typeValueBuilder.put("last_name", barcode.getDriverLicense().getLastName());
          typeValueBuilder.put("gender", barcode.getDriverLicense().getGender());
          typeValueBuilder.put("address_city", barcode.getDriverLicense().getAddressCity());
          typeValueBuilder.put("address_state", barcode.getDriverLicense().getAddressState());
          typeValueBuilder.put("address_zip", barcode.getDriverLicense().getAddressZip());
          typeValueBuilder.put("birth_date", barcode.getDriverLicense().getBirthDate());
          typeValueBuilder.put("document_type", barcode.getDriverLicense().getDocumentType());
          typeValueBuilder.put("license_number", barcode.getDriverLicense().getLicenseNumber());
          typeValueBuilder.put("expiry_date", barcode.getDriverLicense().getExpiryDate());
          typeValueBuilder.put("issuing_date", barcode.getDriverLicense().getIssueDate());
          typeValueBuilder.put("issuing_country", barcode.getDriverLicense().getIssuingCountry());
          barcodeBuilder.put("calendar_event", typeValueBuilder.build());
          break;
      }

      dataBuilder.add(barcodeBuilder.build());
    }

    return dataBuilder.build();
  }

  private ImmutableList<ImmutableMap<String, Object>> processTextRecognitionResult(FirebaseVisionText texts) {
    ImmutableList.Builder<ImmutableMap<String, Object>> dataBuilder =
            ImmutableList.<ImmutableMap<String, Object>>builder();

    List<FirebaseVisionText.Block> blocks = texts.getBlocks();
    if (blocks.size() == 0) {
      return null;
    }
    for (int i = 0; i < blocks.size(); i++) {
      ImmutableMap.Builder<String, Object> blockBuilder = ImmutableMap.<String, Object>builder();
      blockBuilder.put("text", blocks.get(i).getText());
      blockBuilder.put("rect_bottom", (double)blocks.get(i).getBoundingBox().bottom);
      blockBuilder.put("rect_top", (double)blocks.get(i).getBoundingBox().top);
      blockBuilder.put("rect_right", (double)blocks.get(i).getBoundingBox().right);
      blockBuilder.put("rect_left", (double)blocks.get(i).getBoundingBox().left);
      ImmutableList.Builder<ImmutableMap<String, Integer>> blockPointsBuilder =
              ImmutableList.<ImmutableMap<String, Integer>>builder();
      for (int p = 0; p < blocks.get(i).getCornerPoints().length; p++) {
        ImmutableMap.Builder<String, Integer> pointBuilder = ImmutableMap.<String, Integer>builder();
        pointBuilder.put("x", blocks.get(i).getCornerPoints()[p].x);
        pointBuilder.put("y", blocks.get(i).getCornerPoints()[p].y);
        blockPointsBuilder.add(pointBuilder.build());
      }
      blockBuilder.put("points", blockPointsBuilder.build());

      List<FirebaseVisionText.Line> lines = blocks.get(i).getLines();
      ImmutableList.Builder<ImmutableMap<String, Object>> linesBuilder = ImmutableList.<ImmutableMap<String, Object>>builder();
      for (int j = 0; j < lines.size(); j++) {
        ImmutableMap.Builder<String, Object> lineBuilder = ImmutableMap.<String, Object>builder();
        lineBuilder.put("text", lines.get(j).getText());
        lineBuilder.put("rect_bottom", (double)lines.get(j).getBoundingBox().bottom);
        lineBuilder.put("rect_top", (double)lines.get(j).getBoundingBox().top);
        lineBuilder.put("rect_right", (double)lines.get(j).getBoundingBox().right);
        lineBuilder.put("rect_left", (double)lines.get(j).getBoundingBox().left);
        ImmutableList.Builder<ImmutableMap<String, Integer>> linePointsBuilder = ImmutableList.<ImmutableMap<String, Integer>>builder();
        for (int p = 0; p < lines.get(j).getCornerPoints().length; p++) {
          ImmutableMap.Builder<String, Integer> pointBuilder = ImmutableMap.<String, Integer>builder();
          pointBuilder.put("x", lines.get(j).getCornerPoints()[p].x);
          pointBuilder.put("y", lines.get(j).getCornerPoints()[p].y);
          linePointsBuilder.add(pointBuilder.build());
        }
        lineBuilder.put("points", linePointsBuilder.build());

        List<FirebaseVisionText.Element> elements = lines.get(j).getElements();

        ImmutableList.Builder<ImmutableMap<String, Object>> elementsBuilder = ImmutableList.<ImmutableMap<String, Object>>builder();
        for (int k = 0; k < elements.size(); k++) {
          ImmutableMap.Builder<String, Object> elementBuilder = ImmutableMap.<String, Object>builder();
          elementBuilder.put("text", elements.get(k).getText());
          elementBuilder.put("rect_bottom", (double)elements.get(k).getBoundingBox().bottom);
          elementBuilder.put("rect_top", (double)elements.get(k).getBoundingBox().top);
          elementBuilder.put("rect_right", (double)elements.get(k).getBoundingBox().right);
          elementBuilder.put("rect_left", (double)elements.get(k).getBoundingBox().left);
          ImmutableList.Builder<ImmutableMap<String, Integer>> elementPointsBuilder = ImmutableList.<ImmutableMap<String, Integer>>builder();
          for (int p = 0; p < elements.get(k).getCornerPoints().length; p++) {
            ImmutableMap.Builder<String, Integer> pointBuilder = ImmutableMap.<String, Integer>builder();
            pointBuilder.put("x", elements.get(k).getCornerPoints()[p].x);
            pointBuilder.put("y", elements.get(k).getCornerPoints()[p].y);
            elementPointsBuilder.add(pointBuilder.build());
          }
          elementBuilder.put("points", elementPointsBuilder.build());
          elementsBuilder.add(elementBuilder.build());
        }
        lineBuilder.put("elements", elementsBuilder.build());
        linesBuilder.add(lineBuilder.build());
      }
      blockBuilder.put("lines", linesBuilder.build());
      dataBuilder.add(blockBuilder.build());
    }
    return dataBuilder.build();
  }
}
