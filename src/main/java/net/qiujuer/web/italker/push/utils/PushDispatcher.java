package net.qiujuer.web.italker.push.utils;

import com.gexin.rp.sdk.base.IBatch;
import com.gexin.rp.sdk.base.IIGtPush;
import com.gexin.rp.sdk.base.IPushResult;
import com.gexin.rp.sdk.base.impl.SingleMessage;
import com.gexin.rp.sdk.base.impl.Target;
import com.gexin.rp.sdk.http.IGtPush;
import com.gexin.rp.sdk.template.LinkTemplate;
import com.gexin.rp.sdk.template.TransmissionTemplate;
import com.google.common.base.Strings;
import net.qiujuer.web.italker.push.bean.api.base.PushModel;
import net.qiujuer.web.italker.push.bean.db.User;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 消息推送工具类
 *
 * @author qiujuer Email:qiujuer@live.cn
 * @version 1.0.0
 */
public class PushDispatcher {
    private static final String appId = "Nw5YzBuDDI7ON5rsjj5wE1";
    private static final String appKey = "BsyQUtd5Pz5z5USjq4f1p5";
    private static final String masterSecret = "XjJ2gwhdGt51kohga6HP88";
    private static final String host = "http://sdk.open.api.igexin.com/apiex.htm";

    static String CID_A = "";
    static String CID_B = "";

    private final IGtPush pusher;

    public PushDispatcher() {
        // 最根本的发送者
        pusher = new IGtPush(host, appKey, masterSecret);
    }

    // 要收到消息的人和内容的列表
    private final List<BatchBean> beans = new ArrayList<>();



    //构建客户的透传消息
    private static void constructClientTransMsg(String cid, String msg ,IBatch batch) throws Exception {

//        透传消息 ，不是通知栏显示而是在客户端的MessageReceiver中收到
        TransmissionTemplate template = new TransmissionTemplate();
        template.setAppId(appId);
        template.setAppkey(appKey);
        template.setTransmissionContent(msg);
        template.setTransmissionType(1); // 这个Type为int型，填写1则自动启动app
//        构建消息模板
        SingleMessage message = new SingleMessage();
//        把透传消息设置到但消息模板中
        message.setData(template);
//        是否离线发送
        message.setOffline(true);
//        离线消息时长 单位：MS
        message.setOfflineExpireTime(24*3600 * 1000);
// 设置推送目标，填入appid和clientId
        Target target = new Target();
        target.setAppId(appId);
        target.setClientId(cid);
        batch.add(message, target);
    }


    //构建客户的点击通知打开网页消息
    private static void constructClientLinkMsg(String cid, String msg ,IBatch batch) throws Exception {
        SingleMessage message = new SingleMessage();
        LinkTemplate template = new LinkTemplate();
        template.setAppId(appId);
        template.setAppkey(appKey);
        template.setTitle("title");
        template.setText("msg");
        template.setLogo("push.png");
        template.setLogoUrl("logoUrl");
        template.setUrl("url");
        message.setData(template);
        message.setOffline(true);
        message.setOfflineExpireTime(1 * 1000);
// 设置推送目标，填入appid和clientId
        Target target = new Target();
        target.setAppId(appId);
        target.setClientId(cid);
        batch.add(message, target);
    }

    /**
     * 添加一条消息
     *
     * @param receiver 接收者
     * @param model    接收的推送Model
     * @return 是否添加成功
     */
    public boolean add(User receiver, PushModel model) {
        // 基础检查，必须有接收者的设备的Id
        if (receiver == null || model == null ||
                Strings.isNullOrEmpty(receiver.getPushId()))
            return false;

        String pushString = model.getPushString();
        if (Strings.isNullOrEmpty(pushString))
            return false;


        // 构建一个目标+内容
        BatchBean bean = buildMessage(receiver.getPushId(), pushString);
        beans.add(bean);
        return true;
    }

    /**
     * 对要发送的数据进行格式化封装
     *
     * @param clientId 接收者的设备Id
     * @param text     要接收的数据
     * @return BatchBean
     */
    private BatchBean buildMessage(String clientId, String text) {
        // 透传消息，不是通知栏显示，而是在MessageReceiver收到
        TransmissionTemplate template = new TransmissionTemplate();
        template.setAppId(appId);
        template.setAppkey(appKey);
        template.setTransmissionContent(text);
        template.setTransmissionType(0); //这个Type为int型，填写1则自动启动app

        SingleMessage message = new SingleMessage();
        message.setData(template); // 把透传消息设置到单消息模版中
        message.setOffline(true); // 是否运行离线发送
        message.setOfflineExpireTime(24 * 3600 * 1000); // 离线消息时常

        // 设置推送目标，填入appid和clientId
        Target target = new Target();
        target.setAppId(appId);
        target.setClientId(clientId);

        // 返回一个封装
        return new BatchBean(message, target);
    }

    // 进行消息最终发送
    public boolean submit() {
        // 构建打包的工具类
        IBatch batch = pusher.getBatch();

        // 是否有数据需要发送
        boolean haveData = false;

        for (BatchBean bean : beans) {
            try {
                batch.add(bean.message, bean.target);
                haveData = true;
            } catch (Exception e) {
                e.printStackTrace();

            }
        }

        // 没有数据就直接返回
        if (!haveData)
            return false;

        IPushResult result = null;
        try {
            result = batch.submit();
        } catch (IOException e) {
            e.printStackTrace();

            // 失败情况下尝试重复发送一次
            try {
                batch.retry();
            } catch (IOException e1) {
                e1.printStackTrace();
            }

        }

        if (result != null) {
            try {
                Logger.getLogger("PushDispatcher")
                        .log(Level.INFO, (String) result.getResponse().get("result"));
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Logger.getLogger("PushDispatcher")
                .log(Level.WARNING, "推送服务器响应异常！！！");
        return false;

    }

    // 给每个人发送消息的一个Bean封装
    private static class BatchBean {
        SingleMessage message;
        Target target;

        BatchBean(SingleMessage message, Target target) {
            this.message = message;
            this.target = target;
        }
    }
}