package cn.e4mall.cat.interceptor;

import cn.e4mall.common.utils.CookieUtils;
import cn.e4mall.common.utils.E3Result;
import cn.e4mall.pojo.TbUser;
import cn.e4mall.sso.service.TokenService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LoginInterceptor  implements HandlerInterceptor {
    @Value("${COOKIE_TOKEN_KEY}")
    private String COOKIE_TOKEN_KEY;

    @Autowired
    private TokenService  tokenService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 前处理 执行handler 之前执行此方法
        // 返回true 放行，  false：拦截
        // cookie 中去取token
        String  token = CookieUtils.getCookieValue(request, COOKIE_TOKEN_KEY);
        // 2如果没有token，未登陆状态 直接放行
        if(StringUtils.isBlank(token))
        {return true;}
        // 3取到token 需要调用sso系统服务等，根据token取用户信息
         E3Result e3Result = tokenService.getUserByToken(token);
         if(e3Result.getStatus() != 200){
              return true;
         }
        // 4没有取到用户信息，登陆过期， 直接放行
        // 5 取到用户信息，登陆状态
         TbUser  user  = (TbUser) e3Result.getData();
        // 6 把用户信息放到request中， 只需要zai controller  中盘底 request中是否包含user信息 放行
         request.setAttribute("user",user);
         return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

    }
}
