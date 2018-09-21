package com.originaldream.firebase;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Scanner;

public class FirebaseMessageUtil {
    private static final String PROJECT_ID = "yangkaile-web-test";
    private static final String BASE_URL = "https://fcm.googleapis.com";
    private static final String FCM_SEND_ENDPOINT = "/v1/projects/" + PROJECT_ID + "/messages:send";

    private static final String MESSAGING_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
    private static final String[] SCOPES = { MESSAGING_SCOPE };

    public static final String MESSAGE_KEY = "message";

    private static final String KEY = "/Users/yangkaile/key/yangkaile-web-test-firebase-adminsdk-y9uor-2ffffb82be.json";

    private static void init() throws Exception {
        FileInputStream serviceAccount =
                new FileInputStream(KEY);

        FirebaseOptions options = new FirebaseOptions.Builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .setDatabaseUrl("https://yangkaile-web-test.firebaseio.com")
                .build();

        FirebaseApp.initializeApp(options);
    }

    private static String getAccessToken() throws IOException {
        GoogleCredential googleCredential = GoogleCredential
                .fromStream(new FileInputStream(KEY))
                .createScoped(Arrays.asList(SCOPES));
        googleCredential.refreshToken();
        return googleCredential.getAccessToken();
    }
    public static HttpURLConnection getConnection() throws IOException {
        URL url = new URL(BASE_URL + FCM_SEND_ENDPOINT);
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        httpURLConnection.setRequestProperty("Authorization", "Bearer " + getAccessToken());
        httpURLConnection.setRequestProperty("Content-Type", "application/json; UTF-8");
        return httpURLConnection;
    }
    private static void prettyPrint(JsonObject jsonObject) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        System.out.println(gson.toJson(jsonObject) + "\n");
    }
    private static String inputstreamToString(InputStream inputStream) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        Scanner scanner = new Scanner(inputStream);
        while (scanner.hasNext()) {
            stringBuilder.append(scanner.nextLine());
        }
        return stringBuilder.toString();
    }

    private static void sendMessage(JsonObject fcmMessage) throws IOException {
        HttpURLConnection connection = getConnection();
        connection.setDoOutput(true);
        DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
        outputStream.writeBytes(fcmMessage.toString());
        outputStream.flush();
        outputStream.close();

        int responseCode = connection.getResponseCode();
        if (responseCode == 200) {
            String response = inputstreamToString(connection.getInputStream());
            System.out.println("Message sent to Firebase for delivery, response:");
            System.out.println(response);
        } else {
            System.out.println("Unable to send message to Firebase:");
            String response = inputstreamToString(connection.getErrorStream());
            System.out.println(response);
        }
    }

    private static void sendOverrideMessage(FirebaseEntity entity) throws IOException {
        JsonObject overrideMessage = buildOverrideMessage(entity);
        System.out.println("FCM request body for override message:");
        prettyPrint(overrideMessage);
        sendMessage(overrideMessage);
    }



    private static JsonObject buildOverrideMessage(FirebaseEntity entity) {
        JsonObject jNotificationMessage = buildNotificationMessage(entity);

        JsonObject messagePayload = jNotificationMessage.get(MESSAGE_KEY).getAsJsonObject();
        messagePayload.add("android", buildAndroidOverridePayload());

        JsonObject apnsPayload = new JsonObject();
        apnsPayload.add("headers", buildApnsHeadersOverridePayload());
        apnsPayload.add("payload", buildApsOverridePayload());

        messagePayload.add("apns", apnsPayload);

        jNotificationMessage.add(MESSAGE_KEY, messagePayload);

        return jNotificationMessage;
    }

    /**
     * Build the android payload that will customize how a message is received on Android.
     *
     * @return android payload of an FCM request.
     */
    private static JsonObject buildAndroidOverridePayload() {
        JsonObject androidNotification = new JsonObject();
        androidNotification.addProperty("click_action", "android.intent.action.MAIN");

        JsonObject androidNotificationPayload = new JsonObject();
        androidNotificationPayload.add("notification", androidNotification);

        return androidNotificationPayload;
    }

    /**
     * Build the apns payload that will customize how a message is received on iOS.
     *
     * @return apns payload of an FCM request.
     */
    private static JsonObject buildApnsHeadersOverridePayload() {
        JsonObject apnsHeaders = new JsonObject();
        apnsHeaders.addProperty("apns-priority", "10");

        return apnsHeaders;
    }

    /**
     * Build aps payload that will add a badge field to the message being sent to
     * iOS devices.
     *
     * @return JSON object with aps payload defined.
     */
    private static JsonObject buildApsOverridePayload() {
        JsonObject badgePayload = new JsonObject();
        badgePayload.addProperty("badge", 1);

        JsonObject apsPayload = new JsonObject();
        apsPayload.add("aps", badgePayload);

        return apsPayload;
    }

    /**
     * Send notification message to FCM for delivery to registered devices.
     *
     * @throws IOException
     */
    public static void sendCommonMessage(FirebaseEntity firebaseEntity) throws IOException {
        JsonObject notificationMessage = buildNotificationMessage(firebaseEntity);
        System.out.println("FCM request body for message using common notification object:");
        prettyPrint(notificationMessage);
        sendMessage(notificationMessage);
    }

    /**
     * Construct the body of a notification message request.
     *
     * @return JSON of notification message.
     */
    private static JsonObject buildNotificationMessage(FirebaseEntity entity) {
        JsonObject jNotification = new JsonObject();
        jNotification.addProperty("title", entity.getTitle());
        jNotification.addProperty("body", entity.getBody());

        JsonObject jMessage = new JsonObject();
        if(entity.getToken() != null && !entity.getToken().trim().isEmpty()){
            jMessage.addProperty("token",entity.getToken());
        }else {
            jMessage.addProperty("topic", entity.getTopic());
        }
        jMessage.add("notification", jNotification);
        JsonObject jFcm = new JsonObject();
        jFcm.add(MESSAGE_KEY, jMessage);
        return jFcm;
    }



    public static void main(String[] args) throws Exception {
        String myChromesToken =  "djKFtkMxDmE:APA91bEjUMuCQaF1n-WrFqlrLkbGeCiS-vJguvmW-zDmfAhZzBpgErj5pxgC4n0HN4oWHnXxaJPbsLmvgLksUuwWFxStuIJr9OwLBDvJiTf3Cm8pBf7xnSNetpqb3pYPqO0EcVQYWdg7";
//                               "djKFtkMxDmE:APA91bEjUMuCQaF1n-WrFqlrLkbGeCiS-vJguvmW-zDmfAhZzBpgErj5pxgC4n0HN4oWHnXxaJPbsLmvgLksUuwWFxStuIJr9OwLBDvJiTf3Cm8pBf7xnSNetpqb3pYPqO0EcVQYWdg7";
        init();

        /**
         * TODO
         * Body和Title不支持传中文
         * 需要研究下是不是需要转固定格式
         */
        FirebaseEntity entity = new FirebaseEntity();
        entity.setBody("testBody");
        entity.setTitle("testTitle");
        entity.setToken(myChromesToken);
        sendCommonMessage(entity);
//        sendOverrideMessage(entity);

    }

}
