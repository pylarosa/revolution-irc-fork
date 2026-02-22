package io.mrarm.irc.util;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.view.View;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinkHelper {

    private static final Pattern sChannelLinksPattern = Pattern.compile("(^| )(#[^ ,\u0007]+)");

    public interface ChannelLinkHandler {
        void onChannelClicked(String channel, View view);
    }

    private static ChannelLinkHandler sChannelLinkHandler;

    public static void setChannelLinkHandler(ChannelLinkHandler handler) {
        sChannelLinkHandler = handler;
    }

    public static CharSequence addLinks(CharSequence spannable) {
        if (!(spannable instanceof Spannable))
            spannable = new SpannableString(spannable);
        Linkify.addLinks((Spannable) spannable, Linkify.WEB_URLS);
        Matcher matcher = sChannelLinksPattern.matcher(spannable);
        while (matcher.find()) {
            int start = matcher.start(2);
            int end = matcher.end(2);
            String text = matcher.group(2);
            for (Object o : ((Spannable) spannable).getSpans(start, end, URLSpan.class))
                ((Spannable) spannable).removeSpan(o);
            ((Spannable) spannable).setSpan(new ChannelLinkSpan(text), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return spannable;
    }

    public static class ChannelLinkSpan extends ClickableSpan {

        private final String mChannel;

        public ChannelLinkSpan(String channel) {
            mChannel = channel;
        }

        @Override
        public void onClick(View view) {
            if (sChannelLinkHandler != null) {
                sChannelLinkHandler.onChannelClicked(mChannel, view);
            }
        }

    }

}
