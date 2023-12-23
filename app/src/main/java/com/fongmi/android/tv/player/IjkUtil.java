package com.fongmi.android.tv.player;

import android.net.Uri;

import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.utils.Sniffer;
import com.fongmi.android.tv.utils.Utils;

import java.net.URLEncoder;
import java.util.Map;

import tv.danmaku.ijk.media.player.MediaSource;

//bellow add by jim
import java.io.UnsupportedEncodingException;
//end if

public class IjkUtil {

    public static MediaSource getSource(Result result) {
        return getSource(result.getHeaders(), result.getRealUrl());
    }

    public static MediaSource getSource(Channel channel) {
        return getSource(channel.getHeaders(), channel.getUrl());
    }

    public static MediaSource getSource(Map<String, String> headers, String url) {
        Uri uri = Uri.parse(url.trim().replace("\\", ""));
        //bellow edit by jim
        //if (Sniffer.isAds(uri)) uri = Uri.parse(Server.get().getAddress().concat("/m3u8?url=").concat(URLEncoder.encode(url)));
        try {
            String encodedUrl = URLEncoder.encode(url, "UTF-8");
            uri = Uri.parse(Server.get().getAddress().concat("/m3u8?url=").concat(encodedUrl));
        } catch (UnsupportedEncodingException e) {
            // 处理编码异常
            e.printStackTrace();
        }
        //end if
        return new MediaSource(Utils.checkUa(headers), uri);
    }
}
