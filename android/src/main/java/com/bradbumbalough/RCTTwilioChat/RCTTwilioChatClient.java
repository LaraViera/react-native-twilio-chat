package com.bradbumbalough.RCTTwilioChat;

import android.support.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.ReadableMap;

import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.twilio.accessmanager.AccessManager;
import com.twilio.chat.Channel;
import com.twilio.chat.StatusListener;
import com.twilio.chat.CallbackListener;
import com.twilio.chat.ErrorInfo;
import com.twilio.chat.ChatClientListener;
import com.twilio.chat.ChatClient;
import com.twilio.chat.User;
import com.twilio.chat.UserDescriptor;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import android.util.Log;


public class RCTTwilioChatClient extends ReactContextBaseJavaModule implements ChatClientListener {

    @Override
    public String getName() {
        return "TwilioChatClient";
    }

    // Constants

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();

        Map<String, String> channelStatus = new HashMap<>();
        channelStatus.put("Invited", Channel.ChannelStatus.INVITED.toString());
        channelStatus.put("Joined",Channel.ChannelStatus.JOINED.toString());
        channelStatus.put("NotParticipating",Channel.ChannelStatus.NOT_PARTICIPATING.toString());
        constants.put("TCHChannelStatus", channelStatus);

        Map<String, String> channelSyncStatus = new HashMap<>();
        channelSyncStatus.put("None",Channel.SynchronizationStatus.NONE.toString());
        channelSyncStatus.put("Identifier",Channel.SynchronizationStatus.IDENTIFIER.toString());
        channelSyncStatus.put("Metadata",Channel.SynchronizationStatus.METADATA.toString());
        channelSyncStatus.put("All",Channel.SynchronizationStatus.ALL.toString());
        channelSyncStatus.put("Failed",Channel.SynchronizationStatus.FAILED.toString());
        constants.put("TCHChannelSynchronizationStatus", channelSyncStatus);

        Map<String, String> channelType = new HashMap<>();
        channelType.put("Public",Channel.ChannelType.PUBLIC.toString());
        channelType.put("Private",Channel.ChannelType.PRIVATE.toString());
        constants.put("TCHChannelType", channelType);

        Map<String, String> clientSyncStatus = new HashMap<>();
        clientSyncStatus.put("Started",ChatClient.SynchronizationStatus.STARTED.toString());
        clientSyncStatus.put("ChannelListCompleted",ChatClient.SynchronizationStatus.CHANNELS_COMPLETED.toString());
        clientSyncStatus.put("Completed",ChatClient.SynchronizationStatus.COMPLETED.toString());
        clientSyncStatus.put("Failed",ChatClient.SynchronizationStatus.FAILED.toString());
        constants.put("TCHClientSynchronizationStatus", clientSyncStatus);

        Map<String, String> channelOption = new HashMap<>();
        channelOption.put("FriendlyName", "friendlyName");
        channelOption.put("UniqueName", "uniqueName");
        channelOption.put("Type", "type");
        channelOption.put("Attributes", "attributes");
        constants.put("TCHChannelOption", channelOption);

        Map<String, String> connectionState = new HashMap<>();
        connectionState.put("Connecting", ChatClient.ConnectionState.CONNECTING.toString());
        connectionState.put("Connected", ChatClient.ConnectionState.CONNECTED.toString());
        connectionState.put("Disconnected", ChatClient.ConnectionState.DISCONNECTED.toString());
        connectionState.put("Denied", ChatClient.ConnectionState.DENIED.toString());
        connectionState.put("Error", ChatClient.ConnectionState.FATAL_ERROR.toString());
        constants.put("TCHClientConnectionState", connectionState);

        Map<String, Integer> logLevel = new HashMap<>();
        logLevel.put("Fatal", Log.ERROR);
        logLevel.put("Critical", Log.ERROR);
        logLevel.put("Warning", Log.WARN);
        logLevel.put("Info", Log.INFO);
        logLevel.put("Debug", Log.DEBUG);
        constants.put("TCHLogLevel", logLevel);

        Map<String, String> user = new HashMap<>();
        user.put("Attributes", User.UpdateReason.ATTRIBUTES.toString());
        user.put("FriendlyName", User.UpdateReason.FRIENDLY_NAME.toString());
        user.put("ReachabilityNotifiable", User.UpdateReason.REACHABILITY_NOTIFIABLE.toString());
        user.put("ReachabilityOnline", User.UpdateReason.REACHABILITY_ONLINE.toString());
        constants.put("TCHUserInfoUpdate", user);

        return constants;
    }

    public ChatClient client = null;
    private ReactApplicationContext reactContext;

    private static RCTTwilioChatClient rctTwilioChatClient;

    public static RCTTwilioChatClient getInstance() {
        return rctTwilioChatClient;
    }

    public RCTTwilioChatClient(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        rctTwilioChatClient = this;
    }

    private void sendEvent(String eventName, @Nullable WritableMap params) {
        RCTTwilioChatClient tmp = RCTTwilioChatClient.getInstance();
        tmp.reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }

    private void sendEvent(String eventName, @Nullable String body) {
        RCTTwilioChatClient tmp = RCTTwilioChatClient.getInstance();
        tmp.reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, body);
    }

    private StatusListener generateStatusListener(final Promise promise, final String errorCode, final String errorMessage) {
        return new StatusListener() {
            @Override
            public void onError(ErrorInfo errorInfo) {
                super.onError(errorInfo);
                promise.reject(errorCode, errorMessage + " Error Message: " + errorInfo.getMessage());
            }

            @Override
            public void onSuccess() {
                promise.resolve(true);
            }
        };
    }

    // Methods

    @ReactMethod
    public void createClient(String token, ReadableMap props, final Promise promise) {
        final RCTTwilioChatClient tmp = RCTTwilioChatClient.getInstance();
        ChatClient.Properties.Builder builder = new ChatClient.Properties.Builder();
        ChatClient.create(reactContext, token, builder.createProperties(), new CallbackListener<ChatClient>() {
            @Override
            public void onError(ErrorInfo errorInfo) {
                super.onError(errorInfo);
                promise.reject("create-client-error", "Error occurred while attempting to create the client. Error Message: " + errorInfo.getMessage());
            }

            @Override
            public void onSuccess(ChatClient twilioChatClient) {
                tmp.client = twilioChatClient;
                tmp.client.setListener(tmp);
                promise.resolve(RCTConvert.ChatClient(tmp.client));
            }
        });
    }

    @ReactMethod
    public void user(Promise promise) {
        RCTTwilioChatClient tmp = RCTTwilioChatClient.getInstance();
        User user = tmp.client.getUsers().getMyUser();
        promise.resolve(RCTConvert.User(user));
    }

    @ReactMethod
    public void getUser(String identity, final Promise promise) {
        RCTTwilioChatClient.getInstance().client.getUsers().getUserDescriptor(identity, new CallbackListener<UserDescriptor>() {
            @Override
            public void onError(ErrorInfo errorInfo) {
                super.onError(errorInfo);
                promise.reject("get-user-error", "Error occurred while attempting to get user.");
            }

            @Override
            public void onSuccess(UserDescriptor userDescriptor) {
                promise.resolve(RCTConvert.UserDescriptor(userDescriptor));
            }
        });
    }

    @ReactMethod
    public void register(String token, String type) {
        RCTTwilioChatClient tmp = RCTTwilioChatClient.getInstance();

        StatusListener listener = new StatusListener() {
            @Override
            public void onError(ErrorInfo errorInfo) {
                super.onError(errorInfo);
            }

            @Override
            public void onSuccess() {

            }
        };
        if (type == "fcm") {
            tmp.client.registerFCMToken(token, listener);
        } else {
            tmp.client.registerGCMToken(token, listener);
        }
    }

    @ReactMethod
    public void unregister(String token, String type) {
        RCTTwilioChatClient tmp = RCTTwilioChatClient.getInstance();

        StatusListener listener = new StatusListener() {
            @Override
            public void onError(ErrorInfo errorInfo) {
                super.onError(errorInfo);
            }

            @Override
            public void onSuccess() {

            }
        };
        if (type == "fcm") {
            tmp.client.unregisterFCMToken(token, listener);
        } else {
            tmp.client.unregisterGCMToken(token, listener);
        }
    }

    @ReactMethod
    public void handleNotification(ReadableMap notification) {
        RCTTwilioChatClient tmp = RCTTwilioChatClient.getInstance();
        HashMap map = RCTConvert.readableMapToHashMap(notification);
//        TODO
//        tmp.client.handleNotification(map);
    }

    @ReactMethod
    public void shutdown() {
        RCTTwilioChatClient tmp = RCTTwilioChatClient.getInstance();
        tmp.client.shutdown();
    }

    @ReactMethod
    public void setLogLevel(Integer logLevel) {
        ChatClient.setLogLevel(logLevel);
    }

    // UserInfo methods

    @ReactMethod
    public void setFriendlyName(String friendlyName, final Promise promise) {
        RCTTwilioChatClient tmp = RCTTwilioChatClient.getInstance();

        StatusListener listener = new StatusListener() {
            @Override
            public void onError(ErrorInfo errorInfo) {
                super.onError(errorInfo);
                promise.reject("set-friendly-name-error", "Error occurred while attempting to setFriendlyName on user.");
            }

            @Override
            public void onSuccess() {
                promise.resolve(true);
            }
        };

        tmp.client.getUsers().getMyUser().setFriendlyName(friendlyName, listener);
    }

    @ReactMethod
    public void setAttributes(ReadableMap attributes, final Promise promise) {
        RCTTwilioChatClient tmp = RCTTwilioChatClient.getInstance();
        JSONObject json = RCTConvert.readableMapToJson(attributes);

        StatusListener listener = new StatusListener() {
            @Override
            public void onError(ErrorInfo errorInfo) {
                super.onError(errorInfo);
                promise.reject("set-attributes-error", "Error occurred while attempting to setAttributes on user.");
            }

            @Override
            public void onSuccess() {
                promise.resolve(true);
            }
        };

        tmp.client.getUsers().getMyUser().setAttributes(json, listener);
    }

    // Listeners

    @Override
    public void onConnectionStateChange(ChatClient.ConnectionState state) {
        sendEvent("chatClient:connectionStateChanged", state.toString());
    }

    @Override
    public void onChannelAdded(final Channel channel) {
        sendEvent("chatClient:channelAdded", RCTConvert.Channel(channel));
    }

    @Override
    public void onChannelJoined(final Channel channel) {
        sendEvent("chatClient:channelAdded", RCTConvert.Channel(channel));
    }

    @Override
    public void onChannelUpdated(final Channel channel, Channel.UpdateReason reason) {
        sendEvent("chatClient:channelChanged", RCTConvert.Channel(channel));
    }

    @Override
    public void onUserSubscribed(final User user) {
       System.out.println("chatClient:onUserSubscribed");
    }

    @Override
    public void onUserUnsubscribed(final User user) {}

    @Override
    public void onNotificationSubscribed() {
       System.out.println("chatClient:notificationSubscribed");
       // sendEvent("chatClient:notificationSubscribed");
    }

    @Override
    public void onNotificationFailed(final ErrorInfo info) {}

    @Override
    public void onNotification(String var1, String var2) {
       System.out.println("onNotification");
       System.out.println("var1");
       System.out.println("var2");
    }

    @Override
    public void onChannelInvited(Channel channel) {
        sendEvent("chatClient:channelInvited", RCTConvert.Channel(channel));
    }

    @Override
    public void onChannelDeleted(Channel channel){
        sendEvent("chatClient:channelRemoved", RCTConvert.Channel(channel));
    }

    @Override
    public void onChannelSynchronizationChange(Channel channel) {
        WritableMap map = Arguments.createMap();
        map.putString("channelSid",channel.getSid());
        map.putString("status", channel.getSynchronizationStatus().toString());

        sendEvent("chatClient:channel:synchronizationStatusChanged", map);
    }

    @Override
    public void onClientSynchronization(ChatClient.SynchronizationStatus synchronizationStatus) {
        sendEvent("chatClient:synchronizationStatusChanged", synchronizationStatus.toString());
    }

    @Override
    public void onError(ErrorInfo errorInfo) {
        WritableMap map = Arguments.createMap();
        map.putString("error",errorInfo.getMessage());
        map.putString("userInfo", errorInfo.toString());

        sendEvent("chatClient:errorReceived", map);
    }

    @Override
    public void onUserUpdated(User user, User.UpdateReason updateReason) {
        WritableMap map = Arguments.createMap();
        map.putString("updated", updateReason.toString());
        map.putMap("userInfo", RCTConvert.User(user));
        sendEvent("chatClient:userInfoUpdated", map);
    }
}
