package com.crazy.web.handler.api.rest.user;

import com.crazy.util.Base62;
import com.crazy.util.MessageEnum;
import com.crazy.util.RandomKey;
import com.crazy.util.UKTools;
import com.crazy.web.handler.Handler;
import com.crazy.web.model.PlayUser;
import com.crazy.web.model.ResultData;
import com.crazy.web.service.repository.es.PlayUserESRepository;
import com.crazy.web.service.repository.jpa.PlayUserRepository;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Date;

@RestController
@RequestMapping("/api/register")
public class ApiRegisterController extends Handler {

	@Autowired
	private PlayUserESRepository playUserESRes;
	
	@Autowired
	private PlayUserRepository playUserRes ;

	@RequestMapping
    public ResponseEntity<ResultData> register(HttpServletRequest request , @Valid PlayUser player) {
		player = register(player) ;
        return new ResponseEntity<>(new ResultData( player!=null , player != null ? MessageEnum.USER_REGISTER_SUCCESS: MessageEnum.USER_REGISTER_FAILD_USERNAME , player), HttpStatus.OK);
    }
	/**
	 * 注册用户
	 * @param player
	 * @return
	 */
	public PlayUser register(PlayUser player){
		if(player!= null && !StringUtils.isBlank(player.getMobile()) && !StringUtils.isBlank(player.getPassword())){
    		if(StringUtils.isBlank(player.getUsername())){
    			player.setUsername("Guest_"+Base62.encode(UKTools.getUUID().toLowerCase()));
    		}
    		if(!StringUtils.isBlank(player.getPassword())){
    			player.setPassword(UKTools.md5(player.getPassword()));
    		}else{
    			player.setPassword(UKTools.md5(RandomKey.genRandomNum(6)));//随机生成一个6位数的密码 ，备用
    		}
    		player.setCreatetime(new Date());
    		player.setUpdatetime(new Date());
    		player.setLastlogintime(new Date());
    		
    		int users = playUserESRes.countByUsername(player.getUsername()) ;
    		if(users == 0){
    			playUserESRes.save(player) ;
    		}else{
    			player = null ;
    		}
    	}
		return player ;
	}
	
}