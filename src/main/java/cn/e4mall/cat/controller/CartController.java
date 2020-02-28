package cn.e4mall.cat.controller;


import cn.e4mall.cat.service.Cartservice;
import cn.e4mall.common.utils.CookieUtils;
import cn.e4mall.common.utils.E3Result;
import cn.e4mall.common.utils.JsonUtils;
import cn.e4mall.pojo.TbItem;
import cn.e4mall.pojo.TbUser;
import cn.e4mall.service.ItemService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

@Controller
public class CartController {

    @Autowired
    private ItemService  itemService;

    @Value("${TT_CART}")
    private String TT_CART;

    @Value("${CART_EXPIRE}")
    private  Integer  CART_EXPIRE;

    @Autowired
    private Cartservice cartservice;

    /**
     *  购物车中的添加成功的页面
     * @param itemId
     * @param num
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("/cart/add/{itemId}")
    public   String  addCart(@PathVariable  Long  itemId,
                             @RequestParam(defaultValue = "1") Integer num,
                             HttpServletRequest request, HttpServletResponse response){
        // 如果存在
        // 用户是否登陆状态
         TbUser  user = (TbUser) request.getAttribute("user");
         if(user!=null){
              //保存到服务端
             cartservice.addCart(user.getId(),itemId,num);
             return  "cartSuccess";
         }
        // 如果用户未登陆cookie
        //  从cookie 中取购物车获取cookie 列表
        List <TbItem>  cartList=  getCartListFromCookie(request);
        boolean flag =  false;
          //  判断商品在商品列表中是否存在
        for (TbItem  tbItem : cartList){
            // 对象不能直接比较
            if(tbItem.getId() ==  itemId.longValue()){
                  flag= true;
                  // 找到商品 数据
                  tbItem.setNum(tbItem.getNum() +num);
                  break;
            }

        }
        if (!flag){
             // 不存在,根据商品id 查询的商品信息
            TbItem  tbItem =  itemService.getItemById(itemId);
            // 取一张照片
            String  image =  tbItem.getImage();
            if(StringUtils.isNoneBlank(image)){
                  String []  images = image.split(",");
                  tbItem.setImage(images[0]);
            }
            //设置购买商品数量
             tbItem.setNum(num);
            //把商品的id查询的数据
            cartList.add(tbItem);
        }
        //6 把购物商品列表写入cookie
        CookieUtils.setCookie(request, response, TT_CART, JsonUtils.objectToJson(cartList),
                 CART_EXPIRE, true);
        // 返回成功添加的页面
        return  "cartSuccess";
    }

    /**
     *   cookie中取购物列表
     */

    private List<TbItem>   getCartListFromCookie(HttpServletRequest  request){
        String json = CookieUtils.getCookieValue(request,TT_CART, true);
        // 判读json 是否为空
        if(StringUtils.isBlank(json)){
            return  new ArrayList<>();
        }
        // 把json转换成一个商品列表
        List<TbItem> list = JsonUtils.jsonToList(json,TbItem.class);
        return list;
    }


    /**
     *  显示购物车商品列表
     */
    @RequestMapping("/cart/cart")
    public  String  showCartList(HttpServletRequest request ,HttpServletResponse response){

        List <TbItem> cartListFromCookie =  getCartListFromCookie(request);
        // 判断用户是否为登陆状态
         TbUser  user = (TbUser) request.getAttribute("user");
         // 如果是登陆状态
          if(user !=null) {
              //cookie 中购物车列表
              // 不为空， 把cookie中的购物车商品和服务端的购物车商品合并
              cartservice.mergeCart(user.getId(),cartListFromCookie);
              //  把cookie 🀄 的购物车删除
              CookieUtils.deleteCookie(request, response, "COOKIE_TOKEN_KEY");
              // 服务端取购物车列表
              cartListFromCookie  = cartservice.getCartList(user.getId());

          }

          //取购物车商品列表
//         List <TbItem> cartListFromCookie =  getCartListFromCookie(request);
         // 传递给页面
         request.setAttribute("castList", cartListFromCookie);
//         model.addAttribute("castList",cartListFromCookie);
         return "cart";
    }


    /**
     * 更新商品的数量
     * @param itemId
     * @param num
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("/cart/update/num/{itemId}/{num}")
    @ResponseBody
    public E3Result  updateNum(@PathVariable Long  itemId, @PathVariable Integer  num,
                               HttpServletRequest request, HttpServletResponse response){
        //接收两个参数
        //从cookie中商品列表
        TbUser  user= (TbUser) request.getAttribute("user");
        if(user!=null){
             cartservice.updateCartNum(user.getId(), itemId, num);
             return  E3Result.ok();
        }
        List<TbItem> cartListFromCookie = getCartListFromCookie(request);
        // 3 遍历商品列表找到对应商品
        for (TbItem  tbItem:cartListFromCookie){
             if(tbItem.getId() ==  itemId.longValue()){
                 // 更新商品数量
                 tbItem.setNum(num);
             }
        }
        // 5把商品列表写入cookie
        CookieUtils.setCookie(request,response,TT_CART, JsonUtils.objectToJson(cartListFromCookie),CART_EXPIRE, true);
        //6 响应e3result。json数据
        return  E3Result.ok();
    }

    /**
     * 删除购物车的商品
     *
     */
    @RequestMapping("/cart/delete/{itemId}")
    public String  deleteCartItem(@PathVariable  Long  itemId, HttpServletRequest request,
                                  HttpServletResponse response){

        //从cookie中商品列表
        TbUser  user= (TbUser) request.getAttribute("user");
        if(user!=null){
            cartservice.deleteCartItem(user.getId(), itemId);
            return "redirect:/cart/cart.html";
        }
        // 1、从url中取商品id
        // 2、从cookie中取购物车商品列表
        List<TbItem>  cartListFromCookie  = getCartListFromCookie(request);
        //3 遍历列表找到对象的商品
        for (TbItem  tbItem: cartListFromCookie){
             if(tbItem.getId()  ==  itemId.longValue()){
                  //删除商品
                  cartListFromCookie.remove(tbItem);
                  break;
             }
        }
        // 把商品写入cookie
        CookieUtils.setCookie(request,response,TT_CART,JsonUtils.objectToJson(cartListFromCookie), CART_EXPIRE,true);

        return "redirect:/cart/cart.html";
    }
}
