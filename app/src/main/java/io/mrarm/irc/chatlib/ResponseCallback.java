package io.mrarm.irc.chatlib;

public interface ResponseCallback<T> {

    void onResponse(T response);

}
