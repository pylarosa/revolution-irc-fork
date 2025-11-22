package io.mrarm.irc.chatlib;


import java.util.Date;
import java.util.List;

import io.mrarm.irc.chatlib.dto.MessageSenderInfo;
import io.mrarm.irc.chatlib.dto.NickWithPrefix;

public interface ChannelInfoListener {

    void onMemberListChanged(List<NickWithPrefix> newMembers);

    void onTopicChanged(String newTopic, MessageSenderInfo newTopicSetBy, Date newTopicSetOn);

}
