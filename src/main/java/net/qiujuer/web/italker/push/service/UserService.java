package net.qiujuer.web.italker.push.service;

import com.google.common.base.Strings;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.qiujuer.web.italker.push.bean.api.base.PushModel;
import net.qiujuer.web.italker.push.bean.api.base.ResponseModel;
import net.qiujuer.web.italker.push.bean.api.user.UpdateInfoModel;
import net.qiujuer.web.italker.push.bean.card.UserCard;
import net.qiujuer.web.italker.push.bean.db.User;
import net.qiujuer.web.italker.push.factory.UserFactory;
import net.qiujuer.web.italker.push.utils.PushDispatcher;

import javax.validation.constraints.Null;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户信息处理的Service
 *
 * @author qiujuer Email:qiujuer@live.cn
 * @version 1.0.0
 */
// 127.0.0.1/api/user/...
@Path("/user")
public class UserService extends BaseService {

    // 用户信息修改接口
    // 返回自己的个人信息
    @PUT
    //@Path("") //127.0.0.1/api/user 不需要写，就是当前目录
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<UserCard> update(UpdateInfoModel model) {
        if (!UpdateInfoModel.check(model)) {
            return ResponseModel.buildParameterError();
        }

        User self = getSelf();
        // 更新用户信息
        self = model.updateToUser(self);
        self = UserFactory.update(self);
        // 构架自己的用户信息
        UserCard card = new UserCard(self, true);
        // 返回
        return ResponseModel.buildOk(card);
    }




    // 拉取
    @GET
    @Path("/contact")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<List<UserCard> > contact(){
        User selfUser  = getSelf();

        List<User>  users = UserFactory.contacts(selfUser);
        List<UserCard> userCards = users.stream()
                .map(user -> new UserCard(user, true))
                .collect(Collectors.toList());
                // map就是转置操作
        return ResponseModel.buildOk(userCards);
    }

    //关注人
    @PUT  //修改使用put
    @Path("/follow/{followId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseModel<UserCard>  follow(@PathParam("followId") String followId){
        User self = getSelf();
        if(self.getId().equalsIgnoreCase(followId)||Strings.isNullOrEmpty(followId)){
            return  ResponseModel.buildParameterError();
        }

        User  followUser = UserFactory.findById(followId);
        if(followUser==null){
            return ResponseModel.buildNotFoundUserError(null);
        }

        followUser = UserFactory.follow(self,followUser,/*备注默认没有*/null);

        if(followUser==null){
            return ResponseModel.buildServiceError();
        }

        //TODO 通知我关注的人

        return ResponseModel.buildOk(new UserCard(followUser,true));
    }

    //获取某人的信息
    @GET
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public  ResponseModel<UserCard>  getUser(@PathParam("id")String id){
        if(Strings.isNullOrEmpty(id)){
            return ResponseModel.buildParameterError();
        }
        User self = getSelf();

        if(self.getId().equalsIgnoreCase(id)){
            return ResponseModel.buildOk(new UserCard(self,true));
        }

        User user = UserFactory.findById(id);
        if(user ==null){
            return ResponseModel.buildNotFoundUserError(null);
        }

        boolean isFollow = UserFactory.getUserFollow(self,user)!=null;
        return  ResponseModel.buildOk(new UserCard(user,isFollow));
    }


    //搜索人
    @GET  //不设计数据更改 的用get
    @Path("/search/{name:(.*)?}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public  ResponseModel<List<UserCard>>  search(@DefaultValue("") @PathParam("name") String name){
            User self = getSelf();

            List<User>   searchUsers =UserFactory.search(name);
            //是否有已经关注了

            final List<User> contacts =UserFactory.contacts(self);
            List<UserCard>  userCards = searchUsers.stream()
                    //在联系人不多的情况是可以这样子做的 ，但是人多的时候性能有限，这里的操作就类似于一个 双重循环
                    .map(user -> {
                        boolean isFollow = user.getId().equalsIgnoreCase(self.getId())
                                ||contacts.stream().anyMatch(
                                        contactUser -> contactUser.getId()
                                .equalsIgnoreCase(user.getId())
                        );

                        return new UserCard(user,isFollow);
                    }).collect(Collectors.toList());
            return ResponseModel.buildOk(userCards);
    }

}
