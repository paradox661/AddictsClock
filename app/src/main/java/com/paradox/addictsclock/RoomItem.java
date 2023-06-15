package com.paradox.addictsclock;

import org.json.*;

import java.util.Iterator;

public class RoomItem {
    public int     roomId;
    public boolean isCheck;
    public String  uname;
    public String  title;
    public boolean isLive;

    public ErrorFlag isError;

    enum ErrorFlag {
        NO_ERROR,
        NETWORK_ERROR,
        ROOM_NOT_FOUND;
    }


    public RoomItem(int roomId) {
        this.roomId  = roomId;

        this.isCheck = false;
        this.uname   = "";
        this.title   = "";
        this.isLive  = false;

        this.isError = ErrorFlag.NO_ERROR;

        String res;

        try {
            res = HttpRequest.sendGet("https://api.live.bilibili.com/xlive/web-room/v1/index/getRoomBaseInfo", "room_ids="+roomId+"&req_biz=video");
        } catch (Exception e) {
            this.isError = ErrorFlag.NETWORK_ERROR;
            return;
        }

        try {
            JSONObject data = new JSONObject(res).getJSONObject("data").getJSONObject("by_room_ids");

            JSONObject roomData = null;

            for (Iterator<String> it = data.keys(); it.hasNext(); ) {
                String key = it.next();
                roomData = data.getJSONObject(key);

                if(roomData.getInt("room_id") == roomId || roomData.getInt("short_id") == roomId) {
                    this.roomId = Integer.parseInt(key);
                    break;
                }
            }

            assert roomData != null;
            this.uname = roomData.getString("uname");

        } catch (Exception e) {
            this.isError = ErrorFlag.ROOM_NOT_FOUND;
        }
    }


    public void check_live() {
        if(!isCheck) {
            title   = "";
            isLive  = false;
            isError = ErrorFlag.NO_ERROR;

            return;
        }

        title   = "";
        isError = ErrorFlag.NO_ERROR;

        try {
            String res = HttpRequest.sendGet("https://api.live.bilibili.com/xlive/web-room/v1/index/getRoomBaseInfo", "room_ids="+roomId+"&req_biz=video");

            JSONObject data = new JSONObject(res).getJSONObject("data").getJSONObject("by_room_ids").getJSONObject(String.valueOf(roomId));

            uname  = data.getString("uname");
            isLive = data.getInt("live_status")==1;

            if(isLive)
                title = data.getString("title");
            else
                title = "未开播";

        } catch (Exception e) {
            isError = ErrorFlag.NETWORK_ERROR;
        }
    }
}
