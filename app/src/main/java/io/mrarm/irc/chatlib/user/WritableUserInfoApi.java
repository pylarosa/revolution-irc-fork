package io.mrarm.irc.chatlib.user;


import java.util.UUID;
import java.util.concurrent.Future;

import io.mrarm.irc.chatlib.ResponseCallback;
import io.mrarm.irc.chatlib.ResponseErrorCallback;

public interface WritableUserInfoApi extends UserInfoApi {

    Future<Void> setUserNick(UUID user, String newNick, ResponseCallback<Void> callback,
                             ResponseErrorCallback errorCallback);

    Future<Void> setUserChannelPresence(UUID user, String channel, boolean present, ResponseCallback<Void> callback,
                                        ResponseErrorCallback errorCallback);

    Future<Void> clearAllUsersChannelPresences(ResponseCallback<Void> callback, ResponseErrorCallback errorCallback);

}
