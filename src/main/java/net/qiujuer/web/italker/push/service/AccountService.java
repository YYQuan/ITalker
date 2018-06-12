package net.qiujuer.web.italker.push.service;

import com.google.common.base.Strings;
import net.qiujuer.web.italker.push.bean.api.account.AccountRspModel;
import net.qiujuer.web.italker.push.bean.api.account.LoginModel;
import net.qiujuer.web.italker.push.bean.api.account.RegisterModel;
import net.qiujuer.web.italker.push.bean.api.base.ResponseModel;
import net.qiujuer.web.italker.push.bean.card.UserCard;
import net.qiujuer.web.italker.push.bean.db.User;
import net.qiujuer.web.italker.push.factory.UserFactory;
import net.qiujuer.web.italker.push.utils.Hib;
import sun.rmi.runtime.Log;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * @author qiujuer
 */
// 127.0.0.1/api/account/...
@Path("/account")
public class AccountService {

    //登录
    @POST
    @Path("/login")
    // 指定请求与返回的相应体为JSON
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<AccountRspModel> login(LoginModel model){
        if(!LoginModel.check(model)){
            return ResponseModel.buildParameterError();
        }

        User user = UserFactory.login(model.getAccount(),model.getPassword());
            if(user != null){
                //如果有携带PushId
                if(!Strings.isNullOrEmpty(model.getPushId())){
                    return bind(user,model.getPushId());
                }
                AccountRspModel rspModel  = new AccountRspModel(user);
                return  ResponseModel.buildOk(rspModel);
            }else{
                return ResponseModel.buildLoginError();
            }
    }

    @POST
    @Path("/register")
    // 指定请求与返回的相应体为JSON
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<AccountRspModel> register(RegisterModel model) {
        if(!RegisterModel.check(model)){
            return ResponseModel.buildParameterError();
        }

        User user = UserFactory.findByPhone(model.getAccount().trim());

        if (user != null) {
            return ResponseModel.buildHaveAccountError();
        }

        user = UserFactory.findByName(model.getName().trim());
        if (user != null) {

            return ResponseModel.buildHaveNameError();
        }

        //开始注册逻辑
        user = UserFactory.register(model.getAccount(),
                model.getPassword(),
                model.getName());

        if (user != null) {
            //如果有携带PushId
            if(!Strings.isNullOrEmpty(model.getPushId())){
                return bind(user,model.getPushId());
            }
            AccountRspModel rspModel  =new AccountRspModel(user);
            return ResponseModel.buildOk(rspModel);
        }else{
            return ResponseModel.buildRegisterError();
        }

    }



    @POST
    @Path("/bind/{pushId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    //pushID 从URL 地址中获取
    public ResponseModel<AccountRspModel> bind (@HeaderParam("token") String token,
                                                @PathParam("pushId") String pushID){
        if(Strings.isNullOrEmpty(token)||Strings.isNullOrEmpty(pushID)){
            return ResponseModel.buildParameterError();
        }

        User user = UserFactory.findByToken(token);
        if(user != null){
            return  bind(user,pushID);
        }else{
            return ResponseModel.buildAccountError();
        }
    }



    private ResponseModel<AccountRspModel> bind(User self ,String pushId){

        User user = UserFactory.bindPushId(self,pushId);

        if(user == null){
            //绑定失败，则是服务器异常
            return ResponseModel.buildServiceError();
        }


        AccountRspModel rspModel = new AccountRspModel(user,true);
        return  ResponseModel.buildOk(rspModel);


    }



}
