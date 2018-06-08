package net.qiujuer.web.italker.push.factory;

import com.google.common.base.Strings;
import net.qiujuer.web.italker.push.bean.db.User;
import net.qiujuer.web.italker.push.utils.Hib;
import net.qiujuer.web.italker.push.utils.TextUtil;
import org.hibernate.Session;

import java.util.List;
import java.util.UUID;

/**
 * @author qiujuer Email:qiujuer@live.cn
 * @version 1.0.0
 */
public class UserFactory {
    public static User findByPhone(String phone) {
        return Hib.query(session -> (User) session
                .createQuery("from User where phone=:inPhone")
                .setParameter("inPhone", phone)
                .uniqueResult());
    }

    public static User findByName(String name) {
        return Hib.query(session -> (User) session
                .createQuery("from User where name=:name")
                .setParameter("name", name)
                .uniqueResult());
    }



    //通过Token字段查询用户信息
    // 只能自己使用  查询的信息是个人信息 ，非他人信息
    public  static  User findByToken(String token){
        return Hib.query(session -> (User)session
                .createQuery("from  User  where  token=:token")
                .setParameter("token",token)
                .uniqueResult());
    }


    /**
     * 使用账户和密码进行登录
     * @param account
     * @param password
     * @return
     */
    public static User login(String account , String password ){
        String  accountStr= account.trim();
        //存储在 数据库中的密码 是 MD5和 base64 处理过的
        final  String  encodePassword  = encodePassword(password);


        // 寻找
        User  user = Hib.query(session -> (User) session.createQuery("from  User WHERE  phone=:phone and password=:password")
                .setParameter("phone",accountStr)
                .setParameter("password",encodePassword)
                .uniqueResult());

        if(user!=null){
             user  = login(user);
        }

        return user;
    }


    /**
     * 用户注册
     * 注册的操作需要写入数据库，并返回数据库中的User信息
     *
     * @param account  账户
     * @param password 密码
     * @param name     用户名
     * @return User
     */
    public static User register(String account, String password, String name) {
        // 去除账户中的首位空格
        account = account.trim();
        // 处理密码
        password = encodePassword(password);

        User user = createUser(account,password,name);

       if(user != null){
           user = login(user);
       }

       return user;
    }


    private static String encodePassword(String password) {
        // 密码去除首位空格
        password = password.trim();
        // 进行MD5非对称加密，加盐会更安全，盐也需要存储  （加盐：就是加上一个随机的当前时间，一同储存）
        password = TextUtil.getMD5(password);
        // 再进行一次对称的Base64加密，当然可以采取加盐的方案
        return TextUtil.encodeBase64(password);
    }


    /**
     * 注册部分的新建用户逻辑
     * @param account  手机号
     * @param password 加密后的密码
     * @param name 用户名
     * @return
     */
    private static User createUser(String account ,String password ,String name){
        User user = new User();
        user.setName(name);
        user.setPassword(password);
        user.setPhone(account);

        //数据库存储
        return Hib.query(session->(User)session.save(user));

    }

    /**
     * 把一个用户进行登录操作
     * 本质就是对Token进行操作
     * @param user
     * @return
     */
    private static User login(User user){
        //使用一个随机UUID来充当Token
        String newToken = UUID.randomUUID().toString();
        //进行一次Bse64  格式化
        newToken = TextUtil.encodeBase64(newToken);

        user.setToken(newToken);
        return Hib.query(session -> {
            session.saveOrUpdate(user);
            return user;}
        );

    }

    /**
     * 给当前账户绑定PushId
     * @param user
     * @param pushId
     * @return
     */
    public static  User bindPushId(User user ,String pushId){
        if(Strings.isNullOrEmpty(pushId)){
            return null;
        }

        //第一步 先查询是否有其他账户绑定了这个设备
        //取消绑定 ，避免 推送混乱
        Hib.queryOnly(session -> {
            @SuppressWarnings("unchecked") List<User> userList = session.createQuery(
                    "from User where lower(pushId)=:pushId and id!=:userId")
                    .setParameter("pushId",pushId.toLowerCase())
                    .setParameter("userId",user.getId())
                    .list();

            for (User u : userList){
                u.setPushId(null);
                session.saveOrUpdate(u);
            }
        });

        if(pushId.equals(user.getPushId())){
            return user;
        }else{
            //如果当前账户之前的设备ID，和需要绑定的不同
            // 那么需要单点登录，让之前的设备退出账户，
            // 给之前的设备推送一条退出消息
            if(Strings.isNullOrEmpty(user.getPushId())){
                //推送 退出消息
            }

            user.setPushId(pushId);
            return Hib.query(session -> {
                session.saveOrUpdate(user);
                return user;
            });
        }
    }


}
