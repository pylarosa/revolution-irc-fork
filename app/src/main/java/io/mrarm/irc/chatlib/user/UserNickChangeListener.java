package io.mrarm.irc.chatlib.user;


public interface UserNickChangeListener {

    void onNickChanged(UserInfo userInfo, String fromNick, String toNick);

}
