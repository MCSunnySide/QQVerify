package com.mcsunnyside.qqverify;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.meowy.cqp.jcq.entity.*;
import org.meowy.cqp.jcq.event.JcqAppAbstract;

import java.net.URL;
import java.util.concurrent.TimeUnit;

public class qqverify extends JcqAppAbstract implements ICQVer, IMsg, IRequest {
    private final Cache<Long,String> mapping = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).build();
    String appDirectory;
    public String appInfo() {
        String AppID = "com.mcsunnyside.qqverify.qqverify";// 记住编译后的文件和json也要使用appid做文件名
        return CQAPIVER + "," + AppID;
    }

    /**
     * 使用新的方式加载CQ （建议使用这种方式）
     *
     * @param CQ CQ初始化
     */
    public qqverify(CoolQ CQ) {
        super(CQ);
    }


    public int startup() {
        // 获取应用数据目录(无需储存数据时，请将此行注释)
        appDirectory = CQ.getAppDirectory();
        // 返回如：D:\CoolQ\app\com.sobte.cqp.jcq\app\com.example.demo\
        // 应用的所有数据、配置【必须】存放于此目录，避免给用户带来困扰。
        return 0;
    }

    public int exit() {
        return 0;
    }

    public int enable() {
        return 0;
    }

    public int disable() {
        return 0;
    }

    @Override
    public int privateMsg(int i, int i1, long l, String s, int i2) {
        return 0;
    }


    /**
     * 群消息 (Type=2)<br>
     * 本方法会在酷Q【线程】中被调用。<br>
     *
     * @param subType       子类型，目前固定为1
     * @param msgId         消息ID
     * @param fromGroup     来源群号
     * @param fromQQ        来源QQ号
     * @param fromAnonymous 来源匿名者
     * @param msg           消息内容
     * @param font          字体
     * @return 关于返回值说明, 见 {@link #privateMsg 私聊消息} 的方法
     */
    public int groupMsg(int subType, int msgId, long fromGroup, long fromQQ, String fromAnonymous, String msg,
                        int font) {
        // 如果消息来自匿名者
        if (fromQQ == 80000000L && !fromAnonymous.equals("")) {
            // 将匿名用户信息放到 anonymous 变量中
            Anonymous anonymous = CQ.getAnonymous(fromAnonymous);
            return 0;
        }

        String name = mapping.getIfPresent(fromQQ);
        if (name == null) {
            return MSG_IGNORE;
        }
        CQ.setGroupCard(fromGroup, fromQQ, name);
        mapping.invalidate(fromQQ);
        return MSG_IGNORE;
    }

    @Override
    public int discussMsg(int i, int i1, long l, long l1, String s, int i2) {
        return 0;
    }

    @Override
    public int groupUpload(int i, int i1, long l, long l1, String s) {
        return 0;
    }

    @Override
    public int groupAdmin(int i, int i1, long l, long l1) {
        return 0;
    }

    @Override
    public int groupMemberDecrease(int i, int i1, long l, long l1, long l2) {
        return 0;
    }

    @Override
    public int groupMemberIncrease(int subtype, int sendTime, long fromGroup, long fromQQ, long beingOperateQQ) {
        String name = mapping.getIfPresent(beingOperateQQ);
        if(name == null){
            name = mapping.getIfPresent(fromQQ);
        }
        if(name == null){
            return MSG_IGNORE;
        }
        CQ.setGroupCard(fromGroup,beingOperateQQ,name);
        return MSG_IGNORE;
    }

    @Override
    public int groupBan(int i, int i1, long l, long l1, long l2, long l3) {
        return 0;
    }

    @Override
    public int friendAdd(int i, int i1, long l) {
        return 0;
    }

    @Override
    public int requestAddFriend(int i, int i1, long l, String s, String s1) {
        //CQ.setFriendAddRequest(s1, REQUEST_ADOPT, null);
        CQ.setFriendAddRequest(s1, REQUEST_ADOPT, null);
        return MSG_IGNORE;
    }

    /**
     * 请求-群添加 (Type=302)<br>
     * 本方法会在酷Q【线程】中被调用。<br>
     *
     * @param subtype      子类型，1/他人申请入群 2/自己(即登录号)受邀入群
     * @param sendTime     发送时间(时间戳)
     * @param fromGroup    来源群号
     * @param fromQQ       来源QQ
     * @param msg          附言
     * @param responseFlag 反馈标识(处理请求用)
     * @return 关于返回值说明, 见 {@link #privateMsg 私聊消息} 的方法
     */
    public int requestAddGroup(int subtype, int sendTime, long fromGroup, long fromQQ, String msg,
                               String responseFlag) {
        if(subtype != 1){
            CQ.setGroupAddRequest(responseFlag, IRequest.REQUEST_GROUP_INVITE, REQUEST_ADOPT, null);
            return MSG_INTERCEPT;
        }
        if(CQ.getGroupMemberInfo(fromGroup, CQ.getLoginQQ()).getCard().contains("[Disable Verify]")){
            CQ.logDebug("ID检查","取消检查，在群"+fromGroup+"功能被禁用");
            return MSG_INTERCEPT;
        }
        String[] spilt = msg.split("答案：");
        if(spilt.length < 2){
            CQ.logDebug("ID检查","ID分割无效："+msg);
            return MSG_IGNORE;
        }
        String username = spilt[1];
        CQ.logDebug("ID验证",username);
        int code;
        HttpRequest request;
        try {
            request = HttpRequest.get(new URL("https://api.mojang.com/users/profiles/minecraft/" +username))
                    .execute();
            code = request.getResponseCode();
        }catch (Throwable th){
            CQ.logDebug("ID检查","错误："+th.getMessage());
            return MSG_IGNORE;
        }
        if(code == 200){
            CQ.logDebug("ID验证通过",username);
            CQ.setGroupAddRequest(responseFlag, REQUEST_GROUP_ADD, REQUEST_ADOPT, null);
            mapping.put(fromQQ,username);
            CQ.setGroupCard(fromGroup,fromQQ,username);
            return MSG_INTERCEPT;
        }
        if(code == 204){
            CQ.logDebug("ID验证失败","账号未找到: "+username);
            if(CQ.getGroupMemberInfo(fromGroup, CQ.getLoginQQ()).getCard().contains("[Disable Reject]")){
                return MSG_INTERCEPT;
            }
            CQ.setGroupAddRequest(responseFlag, REQUEST_GROUP_ADD, REQUEST_REFUSE, "无效ID:"+username);
            return MSG_INTERCEPT;
        }
        if(code == 400){
            CQ.logDebug("ID验证失败","请求400错误，请求已达上限");
            if(CQ.getGroupMemberInfo(fromGroup, CQ.getLoginQQ()).getCard().contains("[Disable Reject]")){
                CQ.logDebug("ID检查","取消检查，在群"+fromGroup+"功能被禁用");
                return MSG_INTERCEPT;
            }
            CQ.setGroupAddRequest(responseFlag, REQUEST_GROUP_ADD, REQUEST_REFUSE, "每秒请求数已达上限，请稍后再试");
            return MSG_INTERCEPT;
        }
        return MSG_IGNORE;
//		if(subtype == 1){ // 本号为群管理，判断是否为他人申请入群
//			CQ.setGroupAddRequest(responseFlag, REQUEST_GROUP_ADD, REQUEST_ADOPT, null);// 同意入群
//		}
//
//
//        return MSG_IGNORE;
    }
}
